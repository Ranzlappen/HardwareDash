# Gadget

**Your phone's hardware toolkit — monitor, control, and test every sensor, radio, and feature from one app.**

Gadget is an open-source Android app built with Kotlin and Jetpack Compose that gives you direct access to your device's hardware capabilities. No ads, no tracking, no cloud — everything runs locally on your device.

## Features

### Logbook
Log events with timestamps, organize processes with checkpoints, track due dates with reminders. Import/export JSON for cross-device sync.

### Torch & Flashlight
Toggle the flashlight, run strobe mode with adjustable frequency (1-20 Hz), and control display brightness.

### Camera
Live preview with multi-lens selection (ultrawide, main, telephoto, front), tap-to-focus, zoom, and exposure controls. Capture photos instantly.

### Vibration Motor
Test predefined haptic effects, build custom waveform patterns with a visual step editor, draw vibration patterns with your finger, and save/load patterns.

### Microphone
Live dB meter with real-time spectrum analyzer (FFT visualization), audio recording to WAV files, and playback of saved recordings.

### Radios & Connectivity
Wi-Fi signal details, Bluetooth status, NFC tag reader/writer with NDEF support, GPS tracker with live map (OpenStreetMap), cellular signal strength, and network speed monitoring.

### Sensors
Real-time readings from all available sensors: accelerometer, gyroscope, magnetometer, proximity, light, barometer, step counter, and more. Copy readings to clipboard.

### Battery
Detailed battery information: level, status, health, temperature, voltage, technology, current draw, and estimated charge time.

### Lock Screen & Notifications
Custom notification builder with action buttons, progress bars, styles, and scheduling. Lock screen notification designer. Device admin for screen locking. Phone ring with full-screen caller screen.

### Settings
Multi-language support (English, German, Spanish, French). Widget customizer for phone ring duration and notification delay.

### File Metadata
Browse any file on your device, view all metadata (name, size, MIME type, dates), read and edit EXIF data for images, and view media metadata for audio/video files.

### Home Screen Widgets (10)
- **Gadget Metric** — Live hardware metric display (battery, sensors, WiFi, etc.)
- **Quick Log** — One-tap entry to your Logbook
- **Flashlight Toggle** — Turn flashlight on/off
- **Strobe Toggle** — Start/stop strobe light
- **Camera Snapshot** — Take a quick photo
- **Video Toggle** — Start/stop video recording
- **Voice Record** — Start/stop voice recording
- **Phone Ring** — Ring your phone after a delay
- **Quick Notify** — Send a notification after a delay
- **dB Meter** — Monitor ambient noise level

## User Manual

### Getting Started

Gadget is organized into five tabs at the bottom of the screen: **Dashboard**, **Tools**, **Monitor**, **Logbook**, and **More**. On first launch the app opens to the Dashboard. Grant permissions as prompted — each feature explains what it needs. The app works fully offline except for map tiles in GPS.

### Navigation

| Tab | Contents |
|-----|----------|
| **Dashboard** | Home screen with status cards and quick actions |
| **Tools** | Torch, Camera, Vibration, Microphone |
| **Monitor** | Sensors, Battery, Radios & Connectivity |
| **Logbook** | Timestamped event log with processes and checkpoints |
| **More** | User Manual, Notifications, Automation, Files, Settings, Bug Report |

### Dashboard

Your home screen showing battery level, WiFi status, quick action shortcuts to all tools, and your most recent logbook entry. Tap any quick action chip to jump directly to that feature.

### Torch (Tools > Torch)

Toggle your device's flashlight, run strobe mode with adjustable frequency, and control display brightness.

- Tap the large toggle button to turn the flashlight on or off
- Enable Strobe and adjust the frequency slider (1–20 Hz)
- Use the brightness slider to control display brightness (0–100%) and toggle auto-brightness

> **Prerequisites:** Flash hardware on device. WRITE_SETTINGS permission for brightness control.
>
> **Limitations:** Strobe frequency depends on hardware capability. Torch is unavailable while the camera is in use by another app.

### Camera (Tools > Camera)

Live camera preview with multi-lens selection, tap-to-focus, zoom, exposure compensation, and photo capture.

- Select a lens from the row at the top (ultrawide, main, telephoto, front)
- Tap the preview to set focus (3-second autofocus)
- Use the zoom slider (1x to max) and exposure slider (-8 to +8 EV)
- Tap the capture button to save a photo to your gallery

> **Prerequisites:** CAMERA permission.
>
> **Limitations:** Available lenses depend on your device hardware. Video recording is available via the Video Toggle home screen widget.

### Vibration (Tools > Vibration)

Test predefined haptic effects, build custom waveform patterns with a visual editor, draw patterns with your finger, and save or load patterns.

- Tap a predefined effect (Click, Double Click, Heavy Click, Tick) to feel it instantly
- In the Waveform Builder, add steps with amplitude and duration, then tap Play
- Use the Draw canvas to trace a pattern with your finger (X = time 0–2s, Y = intensity 0–100%)
- Save patterns by name and load them later (up to 20 saved patterns)

> **Prerequisites:** VIBRATE permission.
>
> **Limitations:** Amplitude control is not available on all devices; patterns play at full strength on unsupported hardware. Maximum 20 saved patterns.

### Microphone (Tools > Mic)

Real-time dB meter with FFT spectrum analyzer, waveform history, audio recording to WAV, and playback.

- Tap Start Monitoring to see live dB levels and the frequency spectrum
- Tap the record button to capture audio as a WAV file (44.1 kHz mono PCM)
- Saved recordings appear in the list below for playback or deletion

> **Prerequisites:** RECORD_AUDIO permission.
>
> **Limitations:** Measurement accuracy depends on device microphone hardware. FFT analysis covers frequencies up to 22.05 kHz.

### Sensors (Monitor > Sensors)

Real-time readings from all available hardware sensors with live charts and clipboard export.

- Scroll through available sensors (accelerometer, gyroscope, magnetometer, proximity, light, barometer, step counter, etc.)
- Tap any sensor card to expand it and see a live multi-axis chart
- Tap the copy button at the top to copy all current readings to the clipboard

> **Limitations:** Available sensors vary by device. Some sensors may report data at lower update rates.

### Battery (Monitor > Battery)

Detailed battery information including level, status, health, temperature, voltage, technology, current draw, and estimated charge time.

- Open the Battery screen to see all metrics update in real time
- The current draw chart shows a rolling history
- Scroll down for detailed health and technology information

> **Limitations:** Some metrics such as current draw and charge time estimate may not be available on all devices.

### Radios & Connectivity (Monitor > Radios)

Monitor WiFi, Bluetooth, NFC, GPS, cellular signal, and network speed from a single screen.

- **WiFi:** SSID, signal strength (dBm/%), link speed, frequency band
- **Bluetooth:** Status and device name
- **NFC:** Read NDEF tags (Text/URI/MIME), write custom data, save up to 50 tags, HCE emulation
- **GPS:** Live OpenStreetMap view with coordinates, altitude, speed, accuracy, bearing, and GPS log
- **Cellular:** Signal strength, network type (LTE/5G/HSPA)
- **Network Speed:** Download/upload speed measurement

> **Prerequisites:** ACCESS_WIFI_STATE, BLUETOOTH_CONNECT, NFC, ACCESS_FINE_LOCATION, and INTERNET (for map tiles).
>
> **Limitations:** WiFi SSID may show as unknown on some devices. NFC requires compatible hardware. GPS accuracy depends on environment and device.

### Logbook

Log timestamped events with notes, organize processes with checkpoints and due dates, set reminders, and import or export data as JSON.

- Type a note and tap Log to create an entry
- Switch to the Processes tab to create multi-step workflows with checkpoints
- Set due dates and enable reminders for upcoming deadlines
- Filter by type (All/Auto-logged/Custom/Edited), search, sort, and date range
- Use the menu to import or export your data as JSON (compatible with Ticked web app)
- Use built-in templates (Daily Routine, Content Creation, Bug Fix)

> **Prerequisites:** POST_NOTIFICATIONS permission for reminders.
>
> **Limitations:** No cloud sync — use JSON export/import for cross-device transfer. Reminders may be delayed by system battery optimization.

### Notifications & Lock Screen (More > Notifications)

Send demo notifications, build custom notifications with actions and styles, schedule alerts, control the lock screen, and trigger a phone ring.

- Try the demo buttons to see different notification styles (Simple, Heads-Up, Action Buttons, Progress Bar, Big Picture)
- Use the Custom Builder to set title, body, priority, visibility, action buttons, and progress bars
- Schedule notifications or phone rings for a specific time
- Activate Device Admin to enable screen locking

> **Prerequisites:** POST_NOTIFICATIONS, SYSTEM_ALERT_WINDOW (for overlay), and Device Admin activation for screen lock.
>
> **Limitations:** Lock screen overlay is restricted on Android 12+. Phone ring and vibration may be silenced by Do Not Disturb — enable Bypass DND in Settings.

### Automation / Link (More > Automation)

Create IF/THEN rules that trigger actions automatically when hardware metrics meet specified conditions.

- Tap Add Link to create a rule
- Select a metric (e.g. battery level), an operator (>, <, =, !=, >=, <=, between, outside), and a threshold value
- Choose an action: Torch On/Off, Strobe, Vibrate, Notification, Lock Screen, Phone Ring, or Log Entry
- Set a cooldown to prevent rapid re-triggering
- Tap Start Monitoring to activate all enabled rules in the background

> **Prerequisites:** Varies by metric (e.g. location permission for GPS metrics).
>
> **Limitations:** Rules use simple conditions only (no AND/OR logic). Monitoring polls every 500 ms. Limited to built-in actions.

### File Metadata (More > Files)

Browse files on your device, view metadata, read and edit EXIF data for images, and view media metadata for audio and video.

- Tap Select a File to open the system file picker
- View metadata: name, size, MIME type, dates
- For images, expand the EXIF section to view or edit 20+ tags (camera info, GPS, dates, exposure, etc.)
- For audio/video, view media metadata (duration, bitrate, codec, dimensions)

> **Prerequisites:** File access via the system file picker.
>
> **Limitations:** EXIF editing requires write access to the file. Media metadata (audio/video) is read-only.

### Home Screen Widgets

Long-press your home screen > Widgets > Gadget. Ten widgets are available:

| Widget | Function |
|--------|----------|
| **Gadget Metric** | Live display of any hardware metric (battery, sensors, WiFi, etc.) |
| **Quick Log** | One-tap logbook entry |
| **Flashlight Toggle** | Turn flashlight on/off |
| **Strobe Toggle** | Start/stop strobe light |
| **Camera Snapshot** | Take a quick photo |
| **Video Toggle** | Start/stop video recording |
| **Voice Record** | Start/stop voice recording to WAV |
| **Phone Ring** | Ring phone after configured delay |
| **Quick Notify** | Send notification after configured delay |
| **dB Meter** | Monitor ambient noise level |

Configure phone ring duration and notification delay in Settings > Widget Customizer.

> **Prerequisites:** Relevant permissions per widget function (e.g. CAMERA for Camera Snapshot).
>
> **Limitations:** Metric widgets refresh every 30 minutes. Instant actions (toggle, record) respond immediately on tap.

### Settings (More > Settings)

- **Language:** Switch between English, German, Spanish, French (instant UI reload)
- **Widget Customizer:** Configure phone ring duration (5–120s) and notification delay (0–120s)
- **Bypass DND:** Use alarm audio channel to override Do Not Disturb for ring, vibration, and notifications
- **Metric Logging:** Select which device metrics to capture with each logbook entry
- **Accessibility:** High contrast mode, large text, reduced motion (disables animations)

### Bug Report (More > Bug Report)

- View all permission statuses (granted/denied)
- View system modes (ringer, DND, battery saver, music)
- View device information (model, Android version, screen, app version)
- Describe bugs and generate a markdown-formatted report
- Copy report to clipboard or open GitHub Issues directly

### Accessibility

Gadget includes built-in accessibility features:

- **High Contrast:** Increases color contrast for better visibility
- **Large Text:** Increases text size throughout the app
- **Reduced Motion:** Disables all animations and transitions
- **TalkBack:** Full support with screen announcements and semantic labels on all interactive elements

Configure these options in Settings > Accessibility.

## Build

```bash
git clone https://github.com/Ranzlappen/HardwareDash.git
cd HardwareDash
./gradlew assembleDebug
```

Requires Android Studio with SDK 35 and JDK 17.

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Navigation:** Compose Navigation
- **Camera:** CameraX
- **Maps:** OSMDroid (no API key required)
- **Location:** Google Play Services FusedLocationProvider
- **Persistence:** DataStore Preferences, SharedPreferences
- **Background:** WorkManager, AlarmManager
- **Min SDK:** 29 (Android 10)
- **Target SDK:** 35 (Android 15)

## Permissions

| Permission | Purpose |
|---|---|
| CAMERA | Camera preview and photo/video capture |
| RECORD_AUDIO | Microphone meter and voice recording |
| VIBRATE | Vibration motor testing |
| ACCESS_WIFI_STATE | WiFi signal details |
| BLUETOOTH_CONNECT | Bluetooth status |
| NFC | NFC tag reading/writing |
| ACCESS_FINE_LOCATION | GPS tracking and map |
| POST_NOTIFICATIONS | Custom notifications |
| SYSTEM_ALERT_WINDOW | Lock screen overlay |
| INTERNET | Map tile downloads |

## Google Play Description

> Monitor and control your phone's hardware from one powerful app.
>
> Gadget puts your device's full hardware capabilities at your fingertips. Toggle the flashlight, test vibration patterns, read NFC tags, track your GPS location on a live map, monitor battery health, analyze audio with a real-time spectrum analyzer, and much more.
>
> Key features:
> - Logbook for timestamped event logging with processes and checkpoints
> - Flashlight with strobe mode and brightness control
> - Camera with multi-lens selection, zoom, and exposure controls
> - Vibration pattern builder with visual waveform editor
> - Microphone dB meter with FFT spectrum analyzer
> - WiFi, Bluetooth, NFC reader/writer, GPS tracker with live map
> - Full sensor dashboard with real-time readings
> - Battery monitor with detailed health metrics
> - Custom notification builder with action buttons and scheduling
> - File metadata viewer and EXIF editor
> - 10 home screen widgets for quick actions
> - Multi-language support (English, German, Spanish, French)
>
> No ads. No tracking. No cloud. Everything runs locally on your device.
>
> Built with modern Android technologies: Kotlin, Jetpack Compose, Material 3, CameraX, and OpenStreetMap.

## License

This project is proprietary software. All rights reserved. See [LICENSE](LICENSE) for details.

Redistribution, modification, and reverse-engineering are prohibited without prior written permission.
