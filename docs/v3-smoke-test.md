# v3 smoke test

One line per tool: the **core job** a person opens it for. Tick it on a real
phone, note the device and anything odd. Edit this file straight from the
GitHub app — no tooling needed. A tool's status in
[MASTER-PLAN.md](../MASTER-PLAN.md) only becomes ✅ once its line is ticked.

**Device:** Xiaomi 14T Pro · HyperOS · rooted: ? · build: _(APK / commit)_

Format: `- [ ] Tool — job · notes`

## Shell
- [ ] Navigation — bottom bar shows Home / Tools / Sensors / Connect / More; every tool is reachable in ≤ 2 taps; back returns to the section hub ·
- [ ] Home — shows something useful without tapping anything ·
- [ ] Settings — theme / palette change applies immediately ·

## Tools
- [ ] Torch — on/off from the screen, the QS tile and a widget; strobe runs and stops ·
- [ ] Vibration — preset effect plays; custom pattern plays; saved pattern reloads ·
- [ ] Camera — _(v2: barcode scan only)_ scan a QR code; result is copyable ·
- [ ] Audio — dB meter moves with noise; record a clip; clip is saved ·
- [ ] Display — brightness slider changes screen brightness ·
- [ ] Apps — create a folder, add apps, launch an app from it; folder widget pins and opens ·
- [ ] Logbook — add an entry; add a checkpoint with a due date; reminder fires ·

## Sensors
- [ ] Sensors — proximity, light and acceleration values update live ·
- [ ] Battery — level, charging state, temperature, voltage look right ·
- [ ] Storage — free / used / total match system settings ·
- [ ] GPS — position appears on the map; speed/altitude update while moving ·

## Connect
- [ ] Wi-Fi — SSID, signal, frequency shown and live ·
- [ ] Bluetooth — adapter state and bonded devices listed ·
- [ ] Cellular — carrier, network type and signal bars shown (after permission) ·
- [ ] NFC — read a tag's NDEF content ·
- [ ] IR — _(needs an IR emitter)_ send a saved code to a TV ·
- [ ] Sub-GHz — attached SDR/transceiver is detected ·
- [ ] Flipper — connects over USB or BLE; ping succeeds ·

## More
- [ ] Automation — create "battery below 20 % → notification"; it fires ·
- [ ] Notifications — test notification posts with chosen title/body ·
- [ ] Lock — activate device admin; "Lock now" locks the screen ·
- [ ] Device & debug — ADB/USB debug state matches Developer options ·
- [ ] Manual — opens and reads correctly ·
- [ ] Backup — export a backup ZIP, import it on a clean install ·

## Rooted flavor only
_List each rooted action here as it is verified; anything not listed stays
🧪 Experimental (see MASTER-PLAN §4.6)._
- [ ] Root grant — app requests su, safety acknowledgement shows once ·
