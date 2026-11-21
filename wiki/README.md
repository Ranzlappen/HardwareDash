# HardwareDash / Gadget — Wiki source

Every `*.md` file directly in this directory is a page of the GitHub Wiki
(filename = page title; `_Sidebar.md` / `_Footer.md` render as sidebar and
footer). `.github/workflows/publish-wiki.yml` mirrors them to
`HardwareDash.wiki.git` on every push to `main` that touches `wiki/`.

v3 keeps the wiki small (MASTER-PLAN §6, V3-5): Home, Roadmap & Status,
AI Collaboration, Module Authoring Contract, Design System, Flavors & Root
Safety, Troubleshooting. Older pages live unmaintained in
[`docs/wiki-archive/`](../docs/wiki-archive/).

## Maintenance rules

1. Behaviour change → update the matching page and its line in
   `docs/v3-smoke-test.md` in the same batch.
2. A tool's status changes only in **Roadmap & Status** (✅ needs a ticked
   smoke-test line).
3. Prefer shortening a stale page over adding to it; a page nobody needs
   moves to `docs/wiki-archive/`.
