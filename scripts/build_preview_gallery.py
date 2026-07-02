#!/usr/bin/env python3
# =========================================================================
# build_preview_gallery.py — turn Roborazzi-rendered PNGs into the
# HardwareDash "live approximate" app-preview gallery.
# =========================================================================
#
# Stdlib-only (GitHub `ubuntu-latest` ships Python 3). Consumes the PNGs
# produced by `:screenshots:recordRoborazziDebug` and emits, into an output
# root (a checkout of the `app-previews` branch):
#
#   <out>/<version>/img/*.png        — self-contained copy of this version
#   <out>/<version>/gallery.md       — inline markdown gallery (per feature)
#   <out>/<version>/index.html       — interactive container (filter + theme)
#   <out>/<version>/manifest.json    — machine-readable screen list
#   <out>/compare/<prev>..<ver>/     — side-by-side + pixel diff vs previous
#   <out>/index.html                 — version index + cross-version compare
#   <out>/versions.json              — ordered version manifest
#   <out>/README.md                  — human index of archived versions
#
# and (optionally) a repo-root PREVIEW.md whose images point at the published
# GitHub Pages URLs, so it renders inline on github.com without bloating the
# main branch with binaries.
#
# Pixel diffs use ImageMagick `compare` when present (it is on ubuntu-latest);
# otherwise the compare view degrades to before│after with no diff column.

import argparse
import html
import json
import os
import re
import shutil
import subprocess
import sys
from collections import defaultdict

# ------------------------------------------------------------------ parsing

VARIANT_KEYWORDS = [
    ("dark", "Dark"),
    ("light", "Light"),
    ("rtl", "RTL"),
    ("arabic", "RTL"),
    ("largefont", "Large font"),
    ("large_font", "Large font"),
    ("fontscale", "Large font"),
    ("1024", "Expanded (1024dp)"),
    ("700", "Medium (700dp)"),
    ("360", "Compact (360dp)"),
]


def derive_group(stem: str) -> str:
    """Feature/core group a preview belongs to, from its FQN-ish filename."""
    m = re.search(r"feature[._]([a-z0-9-]+)", stem, re.IGNORECASE)
    if m:
        return m.group(1).replace("_", "-")
    m = re.search(r"core[._]([a-z0-9-]+)", stem, re.IGNORECASE)
    if m:
        return "core:" + m.group(1)
    return "other"


def derive_variant(stem: str) -> str:
    low = stem.lower()
    for needle, label in VARIANT_KEYWORDS:
        if needle in low:
            return label
    return "Default"


def derive_screen(stem: str) -> str:
    """Human-ish screen/preview label: drop package, tidy Kt/Preview noise."""
    tail = stem.split(".")[-2:] if "." in stem else [stem]
    label = ".".join(tail)
    label = re.sub(r"Kt\b", "", label)
    label = re.sub(r"(_[0-9]+)$", "", label)      # trailing capture index
    label = re.sub(r"[._]+", " ", label).strip()
    # collapse variant words out of the screen label so grouping is clean
    for needle, _ in VARIANT_KEYWORDS:
        label = re.sub(needle, "", label, flags=re.IGNORECASE)
    label = re.sub(r"\s+", " ", label).strip()
    return label or stem


def scan_pngs(input_dir: str):
    """Return list of dicts: {src, group, screen, variant, key}."""
    items = []
    for root, _dirs, files in os.walk(input_dir):
        for fn in files:
            if not fn.lower().endswith(".png"):
                continue
            stem = fn[:-4]
            group = derive_group(stem)
            variant = derive_variant(stem)
            screen = derive_screen(stem)
            items.append({
                "src": os.path.join(root, fn),
                "group": group,
                "screen": screen,
                "variant": variant,
                # stable id used to pair the same shot across versions
                "key": re.sub(r"[^a-z0-9]+", "-", stem.lower()).strip("-"),
                "file": fn,
            })
    items.sort(key=lambda x: (x["group"], x["screen"], x["variant"]))
    return items


# ------------------------------------------------------------------ writing

def write_version(out_root: str, version: str, items):
    vdir = os.path.join(out_root, version)
    img_dir = os.path.join(vdir, "img")
    os.makedirs(img_dir, exist_ok=True)

    manifest = []
    for it in items:
        dest_name = it["key"] + ".png"
        shutil.copyfile(it["src"], os.path.join(img_dir, dest_name))
        manifest.append({
            "group": it["group"],
            "screen": it["screen"],
            "variant": it["variant"],
            "key": it["key"],
            "img": "img/" + dest_name,
        })

    with open(os.path.join(vdir, "manifest.json"), "w") as f:
        json.dump({"version": version, "shots": manifest}, f, indent=2)

    _write_gallery_md(vdir, version, manifest)
    _write_version_html(vdir, version, manifest)
    return manifest


def _group_sort_key(g: str):
    # core:* groups after features, "other" last
    return (2 if g == "other" else (1 if g.startswith("core:") else 0), g)


def _by_group(manifest):
    groups = defaultdict(list)
    for m in manifest:
        groups[m["group"]].append(m)
    return dict(sorted(groups.items(), key=lambda kv: _group_sort_key(kv[0])))


def _write_gallery_md(vdir, version, manifest, url_prefix=""):
    lines = [
        f"# App preview — `{version}`",
        "",
        f"Auto-generated approximate render of every screen ({len(manifest)} "
        "shots) from the Compose `@Preview` matrix via Roborazzi. "
        "See the interactive container: `index.html`.",
        "",
    ]
    for group, shots in _by_group(manifest).items():
        lines.append(f"## {group}")
        lines.append("")
        # one row per screen, columns = variants
        by_screen = defaultdict(dict)
        for s in shots:
            by_screen[s["screen"]][s["variant"]] = s["img"]
        variants = sorted({s["variant"] for s in shots})
        lines.append("| Screen | " + " | ".join(variants) + " |")
        lines.append("|---|" + "|".join(["---"] * len(variants)) + "|")
        for screen in sorted(by_screen):
            cells = []
            for v in variants:
                img = by_screen[screen].get(v)
                if img:
                    cells.append(f"![{screen} — {v}]({url_prefix}{img})")
                else:
                    cells.append("")
            lines.append(f"| **{screen}** | " + " | ".join(cells) + " |")
        lines.append("")
    with open(os.path.join(vdir, "gallery.md"), "w") as f:
        f.write("\n".join(lines))


def _write_version_html(vdir, version, manifest):
    data = json.dumps(manifest)
    doc = _VERSION_HTML.replace("__VERSION__", html.escape(version)) \
                       .replace("__COUNT__", str(len(manifest))) \
                       .replace("__DATA__", data)
    with open(os.path.join(vdir, "index.html"), "w") as f:
        f.write(doc)


# ------------------------------------------------------------------ diffing

def has_imagemagick():
    for cmd in (["compare", "-version"], ["magick", "-version"]):
        try:
            subprocess.run(cmd, capture_output=True, check=True)
            return cmd[0]
        except (OSError, subprocess.CalledProcessError):
            continue
    return None


def build_compare(out_root, prev, version):
    prev_mf = _load_manifest(out_root, prev)
    cur_mf = _load_manifest(out_root, version)
    if prev_mf is None or cur_mf is None:
        print(f"compare: missing manifest for {prev} or {version}; skipping")
        return None

    prev_keys = {s["key"]: s for s in prev_mf["shots"]}
    cur_keys = {s["key"]: s for s in cur_mf["shots"]}
    shared = sorted(set(prev_keys) & set(cur_keys))
    added = sorted(set(cur_keys) - set(prev_keys))
    removed = sorted(set(prev_keys) - set(cur_keys))

    cdir = os.path.join(out_root, "compare", f"{prev}..{version}")
    diff_dir = os.path.join(cdir, "diff")
    os.makedirs(diff_dir, exist_ok=True)

    magick = has_imagemagick()
    changed = []
    for key in shared:
        a = os.path.join(out_root, prev, prev_keys[key]["img"])
        b = os.path.join(out_root, version, cur_keys[key]["img"])
        diff_rel = f"diff/{key}.png"
        differs = True
        if magick:
            out_png = os.path.join(cdir, diff_rel)
            metric = _magick_compare(magick, a, b, out_png)
            differs = metric is None or metric > 0.0
            if not differs and os.path.exists(out_png):
                os.remove(out_png)
        if differs:
            changed.append({
                "key": key,
                "screen": cur_keys[key]["screen"],
                "group": cur_keys[key]["group"],
                "variant": cur_keys[key]["variant"],
                "before": f"../../{prev}/{prev_keys[key]['img']}",
                "after": f"../../{version}/{cur_keys[key]['img']}",
                "diff": diff_rel if magick else None,
            })

    summary = {
        "from": prev, "to": version,
        "changed": changed,
        "added": [dict(cur_keys[k], side="added") for k in added],
        "removed": [dict(prev_keys[k], side="removed") for k in removed],
        "has_diff": bool(magick),
    }
    with open(os.path.join(cdir, "summary.json"), "w") as f:
        json.dump(summary, f, indent=2)
    _write_compare_md(cdir, summary)
    _write_compare_html(cdir, summary)
    print(f"compare {prev}..{version}: {len(changed)} changed, "
          f"{len(added)} added, {len(removed)} removed")
    return summary


def _magick_compare(magick, a, b, out_png):
    """Return AE metric (changed-pixel count) or None on error."""
    if magick == "magick":
        cmd = ["magick", "compare", "-metric", "AE", a, b, out_png]
    else:
        cmd = ["compare", "-metric", "AE", a, b, out_png]
    proc = subprocess.run(cmd, capture_output=True, text=True)
    # `compare` prints the metric to stderr and exits 1 when images differ.
    raw = (proc.stderr or "").strip().split()[0] if proc.stderr else ""
    try:
        return float(raw.replace(",", "."))
    except ValueError:
        return None


def _write_compare_md(cdir, s):
    lines = [
        f"# Version diff — `{s['from']}` → `{s['to']}`",
        "",
        f"- **Changed:** {len(s['changed'])}",
        f"- **Added:** {len(s['added'])}",
        f"- **Removed:** {len(s['removed'])}",
        "",
    ]
    if s["changed"]:
        lines += ["## Changed screens", ""]
        cols = ["Screen", "Before", "After"] + (["Diff"] if s["has_diff"] else [])
        lines.append("| " + " | ".join(cols) + " |")
        lines.append("|" + "|".join(["---"] * len(cols)) + "|")
        for c in s["changed"]:
            row = [
                f"**{c['screen']}** <br><sub>{c['group']} · {c['variant']}</sub>",
                f"![before]({c['before']})",
                f"![after]({c['after']})",
            ]
            if s["has_diff"]:
                row.append(f"![diff]({c['diff']})" if c["diff"] else "—")
            lines.append("| " + " | ".join(row) + " |")
        lines.append("")
    if s["added"]:
        lines += ["## Added", ""] + [f"- {a['screen']} ({a['group']})" for a in s["added"]] + [""]
    if s["removed"]:
        lines += ["## Removed", ""] + [f"- {r['screen']} ({r['group']})" for r in s["removed"]] + [""]
    with open(os.path.join(cdir, "compare.md"), "w") as f:
        f.write("\n".join(lines))


def _write_compare_html(cdir, s):
    doc = _COMPARE_HTML.replace("__FROM__", html.escape(s["from"])) \
                       .replace("__TO__", html.escape(s["to"])) \
                       .replace("__DATA__", json.dumps(s))
    with open(os.path.join(cdir, "index.html"), "w") as f:
        f.write(doc)


# ------------------------------------------------------------------ index

def _load_manifest(out_root, version):
    p = os.path.join(out_root, version, "manifest.json")
    if not os.path.exists(p):
        return None
    with open(p) as f:
        return json.load(f)


def update_index(out_root, version, count):
    vp = os.path.join(out_root, "versions.json")
    versions = []
    if os.path.exists(vp):
        with open(vp) as f:
            versions = json.load(f).get("versions", [])
    versions = [v for v in versions if v["version"] != version]
    versions.append({"version": version, "count": count})
    # keep insertion order (append = newest last); index renders newest first
    with open(vp, "w") as f:
        json.dump({"versions": versions}, f, indent=2)

    ordered = list(reversed(versions))
    # README index
    lines = ["# HardwareDash — app preview archive", "",
             "Auto-generated per-version galleries. Newest first.", ""]
    for v in ordered:
        lines.append(f"- [`{v['version']}`]({v['version']}/) — {v['count']} shots")
    lines.append("")
    with open(os.path.join(out_root, "README.md"), "w") as f:
        f.write("\n".join(lines))

    doc = _INDEX_HTML.replace("__DATA__", json.dumps(ordered))
    with open(os.path.join(out_root, "index.html"), "w") as f:
        f.write(doc)
    return ordered


def latest_previous(out_root, version):
    vp = os.path.join(out_root, "versions.json")
    if not os.path.exists(vp):
        return None
    with open(vp) as f:
        versions = [v["version"] for v in json.load(f).get("versions", [])]
    versions = [v for v in versions if v != version]
    return versions[-1] if versions else None


def write_preview_md(path, version, manifest, pages_base, repo, prev):
    """Repo-root PREVIEW.md: images by remote Pages URL (no repo bloat)."""
    base = pages_base.rstrip("/")
    vbase = f"{base}/{version}"
    remote_manifest = [dict(m, img=f"{vbase}/{m['img']}") for m in manifest]

    lines = [
        "# HardwareDash — live app preview",
        "",
        f"An auto-generated approximate render of **every screen** "
        f"({len(manifest)} shots) for the latest build (`{version}`), produced "
        "on CI from the Compose `@Preview` matrix via Roborazzi — no device "
        "needed. Regenerated on every push to `main`.",
        "",
        f"- 🖼️ **Interactive gallery (this version):** {vbase}/",
        f"- 🗂️ **All versions + cross-version compare:** {base}/",
    ]
    if prev:
        lines.append(
            f"- 🔀 **Diff vs previous (`{prev}` → `{version}`):** "
            f"{base}/compare/{prev}..{version}/")
    lines += [
        "",
        "> GitHub sanitises scripts/iframes in Markdown, so the fully "
        "interactive container lives on GitHub Pages (linked above). The "
        "tables below are the same shots embedded inline.",
        "",
    ]
    # reuse the md writer against remote URLs
    tmp = defaultdict(list)
    for m in remote_manifest:
        tmp[m["group"]].append(m)
    for group, shots in sorted(tmp.items(), key=lambda kv: _group_sort_key(kv[0])):
        lines.append(f"## {group}")
        lines.append("")
        by_screen = defaultdict(dict)
        for sh in shots:
            by_screen[sh["screen"]][sh["variant"]] = sh["img"]
        variants = sorted({sh["variant"] for sh in shots})
        lines.append("| Screen | " + " | ".join(variants) + " |")
        lines.append("|---|" + "|".join(["---"] * len(variants)) + "|")
        for screen in sorted(by_screen):
            cells = []
            for v in variants:
                img = by_screen[screen].get(v)
                cells.append(f'<img src="{img}" width="200">' if img else "")
            lines.append(f"| **{screen}** | " + " | ".join(cells) + " |")
        lines.append("")
    with open(path, "w") as f:
        f.write("\n".join(lines))


# ------------------------------------------------------------------ html templates

_STYLE = """
:root{--bg:#06080a;--card:#0d1116;--border:#1d242b;--fg:#e6edf3;--muted:#8b98a5;--accent:#00e5c8}
*{box-sizing:border-box}body{margin:0;font:14px/1.5 system-ui,sans-serif;background:var(--bg);color:var(--fg)}
body.light{--bg:#fafbfd;--card:#fff;--border:#e2e6ea;--fg:#0b1015;--muted:#5a6672}
header{position:sticky;top:0;background:var(--bg);border-bottom:1px solid var(--border);padding:14px 18px;z-index:5}
h1{font-size:17px;margin:0 0 4px}.sub{color:var(--muted);font-size:12px}
.controls{display:flex;gap:10px;flex-wrap:wrap;margin-top:10px}
input,select,button{background:var(--card);color:var(--fg);border:1px solid var(--border);border-radius:8px;padding:7px 10px;font:inherit}
button{cursor:pointer}.accent{color:var(--accent)}
.grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(220px,1fr));gap:16px;padding:18px}
.card{background:var(--card);border:1px solid var(--border);border-radius:14px;overflow:hidden}
.card .frame{background:#000;display:flex;align-items:center;justify-content:center;aspect-ratio:9/19;border-radius:10px;margin:10px;overflow:hidden}
.card img{max-width:100%;max-height:100%;display:block}
.card .meta{padding:0 12px 12px}.card .meta b{display:block}.card .meta span{color:var(--muted);font-size:12px}
.ghead{grid-column:1/-1;margin:6px 0 -4px;font-size:13px;color:var(--muted);text-transform:uppercase;letter-spacing:.06em}
.cmp{display:grid;grid-template-columns:1fr 1fr 1fr;gap:8px}
.cmp figure{margin:0}.cmp figcaption{color:var(--muted);font-size:11px;text-align:center;padding:4px}
a{color:var(--accent)}
"""

_VERSION_HTML = """<!doctype html><html><head><meta charset=utf-8>
<meta name=viewport content="width=device-width,initial-scale=1">
<title>App preview __VERSION__</title><style>""" + _STYLE + """</style></head>
<body>
<header>
  <h1>HardwareDash preview <span class=accent>__VERSION__</span></h1>
  <div class=sub>__COUNT__ approximate screen renders · <a href="../">all versions</a></div>
  <div class=controls>
    <input id=q placeholder="Filter screens…" oninput=render()>
    <select id=variant onchange=render()></select>
    <button onclick=toggleTheme()>◐ theme</button>
  </div>
</header>
<div class=grid id=grid></div>
<script>
const DATA=__DATA__;
const variantSel=document.getElementById('variant');
['All variants',...[...new Set(DATA.map(d=>d.variant))].sort()].forEach((v,i)=>{
  const o=document.createElement('option');o.value=i===0?'':v;o.textContent=v;variantSel.appendChild(o);
});
function toggleTheme(){document.body.classList.toggle('light')}
function render(){
  const q=document.getElementById('q').value.toLowerCase();
  const vf=variantSel.value;
  const grid=document.getElementById('grid');grid.innerHTML='';
  const groups={};
  DATA.filter(d=>(!vf||d.variant===vf)&&(!q||(d.screen+d.group).toLowerCase().includes(q)))
      .forEach(d=>{(groups[d.group]=groups[d.group]||[]).push(d)});
  Object.keys(groups).sort().forEach(g=>{
    const h=document.createElement('div');h.className='ghead';h.textContent=g;grid.appendChild(h);
    groups[g].forEach(d=>{
      const c=document.createElement('div');c.className='card';
      c.innerHTML=`<div class=frame><img loading=lazy src="${d.img}"></div>
        <div class=meta><b>${d.screen}</b><span>${d.variant}</span></div>`;
      grid.appendChild(c);
    });
  });
  if(!grid.children.length)grid.innerHTML='<p class=sub style=padding:18px>No matches.</p>';
}
render();
</script></body></html>"""

_COMPARE_HTML = """<!doctype html><html><head><meta charset=utf-8>
<meta name=viewport content="width=device-width,initial-scale=1">
<title>Diff __FROM__ → __TO__</title><style>""" + _STYLE + """</style></head>
<body>
<header>
  <h1>Diff <span class=accent>__FROM__</span> → <span class=accent>__TO__</span></h1>
  <div class=sub id=stat></div>
  <div class=controls><input id=q placeholder="Filter changed screens…" oninput=render()>
  <button onclick=toggleTheme()>◐ theme</button></div>
</header>
<div class=grid id=grid></div>
<script>
const S=__DATA__;
document.getElementById('stat').textContent=
  `${S.changed.length} changed · ${S.added.length} added · ${S.removed.length} removed`;
function toggleTheme(){document.body.classList.toggle('light')}
function render(){
  const q=document.getElementById('q').value.toLowerCase();
  const grid=document.getElementById('grid');grid.innerHTML='';
  S.changed.filter(c=>!q||(c.screen+c.group).toLowerCase().includes(q)).forEach(c=>{
    const card=document.createElement('div');card.className='card';card.style.gridColumn='1/-1';
    const diff=c.diff?`<figure><img loading=lazy src="${c.diff}"><figcaption>diff</figcaption></figure>`:'';
    card.innerHTML=`<div class=meta style=padding:12px><b>${c.screen}</b><span>${c.group} · ${c.variant}</span></div>
      <div class=cmp style=padding:0-12px-12px>
        <figure><img loading=lazy src="${c.before}"><figcaption>before</figcaption></figure>
        <figure><img loading=lazy src="${c.after}"><figcaption>after</figcaption></figure>${diff}</div>`;
    grid.appendChild(card);
  });
  if(!grid.children.length)grid.innerHTML='<p class=sub style=padding:18px>No visual changes.</p>';
}
render();
</script></body></html>"""

_INDEX_HTML = """<!doctype html><html><head><meta charset=utf-8>
<meta name=viewport content="width=device-width,initial-scale=1">
<title>HardwareDash app previews</title><style>""" + _STYLE + """</style></head>
<body>
<header>
  <h1>HardwareDash — app preview archive</h1>
  <div class=sub>Approximate full-app renders per version. Compare any two.</div>
  <div class=controls>
    <select id=a onchange=cmp()></select><span class=sub>vs</span>
    <select id=b onchange=cmp()></select>
    <button onclick=toggleTheme()>◐ theme</button>
  </div>
</header>
<div style=padding:18px>
  <h2 style=font-size:14px>Versions</h2>
  <ul id=list></ul>
  <div id=cmpout></div>
</div>
<script>
const V=__DATA__;  // newest first
const list=document.getElementById('list');
V.forEach(v=>{const li=document.createElement('li');
  li.innerHTML=`<a href="${v.version}/">${v.version}</a> — ${v.count} shots`;list.appendChild(li)});
const a=document.getElementById('a'),b=document.getElementById('b');
V.forEach(v=>{[a,b].forEach(sel=>{const o=document.createElement('option');o.value=v.version;o.textContent=v.version;sel.appendChild(o.cloneNode(true))})});
if(V[1]){a.value=V[1].version;b.value=V[0].version}
function toggleTheme(){document.body.classList.toggle('light')}
async function load(v){const r=await fetch(`${v}/manifest.json`);return (await r.json()).shots}
async function cmp(){
  const out=document.getElementById('cmpout');out.innerHTML='<p class=sub>Loading…</p>';
  if(a.value===b.value){out.innerHTML='<p class=sub>Pick two different versions.</p>';return}
  const [A,B]=await Promise.all([load(a.value),load(b.value)]);
  const ma={},mb={};A.forEach(s=>ma[s.key]=s);B.forEach(s=>mb[s.key]=s);
  const keys=[...new Set([...Object.keys(ma),...Object.keys(mb)])].sort();
  out.innerHTML=`<h2 style=font-size:14px>${a.value} → ${b.value}</h2><div class=grid></div>`;
  const grid=out.querySelector('.grid');
  keys.forEach(k=>{const x=ma[k],y=mb[k];const c=document.createElement('div');c.className='card';c.style.gridColumn='1/-1';
    const meta=(y||x);c.innerHTML=`<div class=meta style=padding:12px><b>${meta.screen}</b><span>${meta.group} · ${meta.variant}</span></div>
      <div class=cmp>
        <figure><img loading=lazy src="${x?a.value+'/'+x.img:''}"><figcaption>${a.value}${x?'':' (absent)'}</figcaption></figure>
        <figure><img loading=lazy src="${y?b.value+'/'+y.img:''}"><figcaption>${b.value}${y?'':' (absent)'}</figcaption></figure></div>`;
    grid.appendChild(c)});
}
cmp();
</script></body></html>"""


# ------------------------------------------------------------------ main

def main():
    ap = argparse.ArgumentParser(description="Build the app-preview gallery.")
    ap.add_argument("--input", required=True, help="dir of Roborazzi PNGs")
    ap.add_argument("--version", required=True, help="version label, e.g. v1.0.42")
    ap.add_argument("--out", required=True, help="gallery output root (app-previews checkout)")
    ap.add_argument("--pages-base-url", default="",
                    help="published Pages base URL for PREVIEW.md image refs")
    ap.add_argument("--preview-md", default="",
                    help="path to write repo-root PREVIEW.md (optional)")
    ap.add_argument("--repo", default="", help="owner/name for links")
    args = ap.parse_args()

    items = scan_pngs(args.input)
    if not items:
        print(f"::error::No PNGs found under {args.input} — did the render run?")
        return 1
    print(f"Found {len(items)} rendered shots.")

    prev = latest_previous(args.out, args.version)
    manifest = write_version(args.out, args.version, items)
    update_index(args.out, args.version, len(manifest))
    if prev:
        build_compare(args.out, prev, args.version)
    else:
        print("No previous version recorded yet — skipping diff.")

    if args.preview_md and args.pages_base_url:
        write_preview_md(args.preview_md, args.version, manifest,
                         args.pages_base_url, args.repo, prev)
        print(f"Wrote {args.preview_md}")

    print(f"Gallery written to {os.path.join(args.out, args.version)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
