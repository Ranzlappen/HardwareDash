# Roadmap & Status

> **Direction and the batch plan live in
> [`MASTER-PLAN.md`](https://github.com/Ranzlappen/HardwareDash/blob/main/MASTER-PLAN.md).**
> This page is only the readable status: what a person can actually do with
> the app today. The long v2 roadmap (phases 0–3, completion workstreams
> W1–W10) is preserved on the `archive/v2-modular-2026` branch.

## Where we are

**v3 ("pragmatic") — phase V3-1 (navigation & home) in progress** on
`claude/v3-pragmatic-overhaul`.

- **v1** (`legacy-main`) — single-module app, archived.
- **v2** (`archive/v2-modular-2026`) — modular monorepo; foundations kept,
  direction superseded (see MASTER-PLAN §2 for the retrospective).
- **v3** — v2's code, re-centred on usability: five-section navigation,
  merged overlapping modules, v1's practical tools brought back, honest
  root tooling.

## Status legend

✅ Works (smoke-tested on a named device) · 🟡 Partial · ⚪ Unverified (code
exists, not yet confirmed on hardware) · 🧪 Experimental (rooted/vendor, hidden
by default) · ✂️ Merged/cut.

Every feature v2 reported as ✅ starts v3 as ⚪ until its line in
[`docs/v3-smoke-test.md`](https://github.com/Ranzlappen/HardwareDash/blob/main/docs/v3-smoke-test.md)
is ticked.

## Feature status

| Section | Feature | Module(s) | Status | v3 plan |
|---|---|---|---|---|
| Home | Dashboard | `dashboard` | ⚪ | live status cards, quick actions, pinned tools (B3 ✓) |
| Tools | Torch (+ strobe, QS tiles, widgets) | `torch` (+`-rooted`/`-standard`) | ⚪ | keep |
| Tools | Vibration (+ pattern editor, widgets, tile) | `vibration` (+`-rooted`/`-standard`) | ⚪ | absorb `actuators` (B7) |
| Tools | Haptics test | `actuators` | ⚪ | ✂️ → `vibration` (B7) |
| Tools | Camera | `camera` (+`-rooted`) | 🟡 barcode scanner only | preview / lens / zoom / capture (B9) |
| Tools | Audio (dB meter, WAV recording) | `audio` (+`-rooted`) | 🟡 no spectrum, no playback | spectrum + recordings player (B10), absorb `microphone` (B7) |
| Tools | Mic extreme tools | `microphone` (+`-rooted`) | 🧪 | ✂️ → `audio` rooted section (B7) |
| Tools | Display (brightness) | `display` (+`-rooted`) | ⚪ | keep |
| Tools | App organizer + folder widgets | `apps` (+`-rooted`) | ⚪ | keep |
| Tools | Logbook (entries, checkpoints, reminders) | `logbook` | ⚪ | keep |
| Sensors | Sensor explorer (every sensor, live detail, copy) | `sensors` | ⚪ | B5 ✓ — absorbed `ambient` + `motion` (✂️) |
| Sensors | Battery | `battery` (+`-rooted`) | ⚪ | keep |
| Sensors | Storage | `storage` (+`-rooted`) | ⚪ | keep |
| Sensors | GPS + spoofing | `gps` (+`-rooted`), `lsposed-module` | ⚪ | keep |
| Connect | Wi-Fi | `radios-wifi` (+`-rooted`) | ⚪ | keep |
| Connect | Bluetooth | `radios-bt` (+`-rooted`) | ⚪ | keep |
| Connect | Cellular | `radios-cell` (+`-rooted`) | ⚪ | keep |
| Connect | NFC (read, HCE, templates) | `radios-nfc` (+`-rooted`) | ⚪ | parity check vs v1 write tools (B11) |
| Connect | IR blaster | `radios-ir` (+`-rooted`) | ⚪ | keep |
| Connect | Sub-GHz device detection | `radios-subghz` | ⚪ | keep |
| Connect | Flipper Zero bridge | `flipper` (+`-rooted`) | ⚪ | keep |
| More | Automation rules | `automation-ui`, `:core:automation`, `automation` (+`-rooted`) | ⚪ | 29 dead assert/check actions removed (B6 ✓) |
| More | Notifications | `notification` (+`-rooted`) | ⚪ | keep |
| More | Lock (device admin) | `lock` (+`-rooted`) | ⚪ | keep |
| More | ADB / USB debug, diagnostics, permission health | `adbdebug`, `usbdebug`, `diagnostics`, `bugreport` (+ rooted) | ⚪ | ✂️ → `devtools` (B8) |
| More | Manual | `manual` | ⚪ | keep |
| Tools | File metadata / EXIF (v1) | — | ✂️ lost in v2 | bring back (B12) |
| More | Settings, backup, permissions dashboard | `settings`, `:core:permissions` | ⚪ | single permissions UI (B8) |
| More | YouTube downloader | `youtubedownloader` | ⚪ | kept, frozen (MASTER-PLAN §8.1) |
| Home screen | Generic metric widget | `metricwidget` | ⚪ | keep |
| — | Keep-alive service | `keepalive` (+`-rooted`) | ⚪ | keep |
| — | Macrobenchmarks | `benchmark` | empty skeleton | ✂️ remove (V3-5) |

## Infrastructure kept from v2

`:core:designsystem` + `:core:ui` (design system), `:core:root` (root safety
seam), `:core:widgetkit`, `:core:monitoring`, `:core:automation` +
`automation.db`, `:core:data` / `:core:datastore`, `:core:permissions`,
backup format v6, the CI matrix (unit tests, both flavors, standard-APK leak
gate, signed rooted release). See [Architecture](Architecture).

## Release readiness

CI produces `standard-debug.apk`, `standard-release.apk` + `.aab`,
`rooted-debug.apk` and a signed `rooted-release.apk` on pushes to `main`;
`ci-refactor.yml` builds both debug flavors on every PR and on pushes to
`claude/v3-**`. See [Testing & CI](Testing-and-CI).

---

> _Last reviewed: 2026-11-19 · Source: `MASTER-PLAN.md`,
> `docs/v3-smoke-test.md`, `settings.gradle.kts` · Related modules: all._
