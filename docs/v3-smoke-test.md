# v3 smoke test

One line per tool: the **core job** a person opens it for. Tick it on a real
phone, note the device and anything odd. Edit this file straight from the
GitHub app — no tooling needed. A tool's status in
[MASTER-PLAN.md](../MASTER-PLAN.md) only becomes ✅ once its line is ticked.

**Getting a build:** GitHub app → Actions → the latest *CI* run on
`claude/v3-pragmatic-overhaul` → artifact `gadget-debug-apks-<sha>` (zip with
the standard and rooted debug APKs; kept 7 days). They install as a **separate app,
"Gadget v3"** (`dev.ranzlappen.gadget.v3`; rooted: "Gadget v3 Root",
`dev.ranzlappen.gadget.rooted.v3`), beside any other Gadget, with their own
data. Each new test build updates the previous one (same release key,
versionCode 1000 + run). Builds before run 347 can't install — skip them.
Direct download (no zip): `releases/download/v3-dev/Gadget-v3-standard.apk`
/ `…/Gadget-v3-rooted.apk` (rolling prerelease, replaced on every push).

**Play Protect blocks the install** ("App not installed" /
`INSTALL_FAILED_VERIFICATION_FAILURE`): Gadget declares a notification
listener, which Play Protect's enhanced fraud protection refuses for
sideloaded apps. Decision (2026-11-21): keep the listener; to install, Play
Store → profile icon → Play Protect → ⚙️ → turn off "Scan apps with Play
Protect", install, turn it back on.

**Standard flavor:** Xiaomi 14T Pro · HyperOS · not rooted · build: _(APK / commit)_
**Rooted flavor:** Samsung Galaxy A17 · rooted · build: _(APK / commit)_

Format: `- [ ] Tool — job · notes`

## Shell
- [ ] Navigation — bottom bar shows Home / Tools / Sensors / Connect / More; every tool is reachable in ≤ 2 taps; back returns to the section hub; re-tapping the active section returns to its hub; tool screens show a back arrow (hubs and Home don't); landscape shows a labelled rail instead ·
- [ ] Deep links — tapping the battery widget opens Battery, the storage widget opens Storage, a logbook reminder opens Logbook (also when the app is already open) ·
- [ ] Home — battery, storage, Wi-Fi and last log entry show real values and update; each card opens its tool; Torch button toggles the flashlight; Buzz vibrates; pinned tools appear after pinning in "Edit pinned tools" ·
- [ ] Settings — theme / palette change applies immediately ·

## Tools
- [ ] Torch — on/off from the screen, the QS tile and a widget; strobe runs and stops ·
- [ ] Vibration — Click / Tick / Double / Heavy effects play; custom pattern plays; saved pattern reloads; a rule with "Play haptic effect" (heavy click) vibrates ·
- [ ] Camera — Photo: preview shows, lens chips switch lenses (or 0.6× / 2× zoom chips on a single logical camera), tap focuses, pinch zooms, exposure slider brightens/darkens, torch toggles, "Take photo" saves to Pictures/Gadget and Open / Share work; Scan: a QR code is read and copyable ·
- [ ] Audio — dB and spectrum bars move with noise; whistling shows a matching peak Hz; record a clip (timer counts); the clip appears in Recordings, plays, shares, and deletes after confirming; leaving the app turns the mic indicator off within ~2 s; (rooted) "Advanced microphone tools" opens the mic tools with a back arrow ·
- [ ] Display — brightness slider changes screen brightness ·
- [ ] Apps — create a folder, add apps, launch an app from it; folder widget pins and opens ·
- [ ] Logbook — add an entry; add a checkpoint with a due date; reminder fires ·
- [ ] Files — pick a camera photo: size, type, dimensions and camera details show; edit Artist, "Save to file", re-open it and the change is there; "Remove location" clears GPS; pick an MP3/MP4: duration, bitrate, tags show (read-only) ·

## Sensors
- [ ] Sensors — the list shows every sensor (magnetometer, barometer, … — compare with a sensor app); a sensor's detail shows live values per axis with moving sparklines and a rate in Hz; "Copy specs and readings" and "Copy sensor list" paste correctly; Step counter asks for activity permission, then counts; Significant motion shows the trigger note ·
- [ ] Battery — level, charging state, temperature, voltage look right ·
- [ ] Storage — free / used / total match system settings ·
- [ ] GPS — position appears on the map; speed/altitude update while moving ·

## Connect
- [ ] Wi-Fi — SSID, signal, frequency shown and live ·
- [ ] Bluetooth — adapter state and bonded devices listed ·
- [ ] Cellular — carrier, network type and signal bars shown (after permission) ·
- [ ] NFC — with the NFC screen open, a tag shows UID, chip (e.g. NTAG215), NDEF bytes used/max, records and write protection; "Write" a URL, tap the tag, it reads back the new URL; tapping a tag while Gadget is closed opens the NFC screen with it; (2nd phone) HCE text is readable ·
- [ ] IR — _(needs an IR emitter)_ send a saved code to a TV ·
- [ ] Sub-GHz — attached SDR/transceiver is detected ·
- [ ] Flipper — connects over USB or BLE; ping succeeds ·

## More
- [ ] Automation — create "battery below 20 % → notification"; it fires ·
- [ ] Notifications — test notification posts with chosen title/body ·
- [ ] Lock — activate device admin; "Lock now" locks the screen ·
- [ ] Device & debug — opens from More; its ADB, USB and Permission health links open with a back arrow; ADB/USB debug state matches Developer options; Permission health requests a missing permission ·
- [ ] Manual — opens and reads correctly ·
- [ ] Backup — export a backup ZIP, import it on a clean install ·

## Rooted flavor only (Samsung Galaxy A17)
_List each rooted action here as it is verified; anything not listed stays
🧪 Experimental (see MASTER-PLAN §4.6)._
- [ ] Root grant — app requests su, safety acknowledgement shows once ·
