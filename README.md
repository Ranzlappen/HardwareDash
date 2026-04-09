# HardwareDash

**Your phone's hardware toolkit — monitor, control, and test every sensor, radio, and feature from one app.**

HardwareDash is an open-source Android app built with Kotlin and Jetpack Compose that gives you direct access to your device's hardware capabilities. No ads, no tracking, no cloud — everything runs locally on your device.

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
- **HardwareDash Metric** — Live hardware metric display (battery, sensors, WiFi, etc.)
- **Quick Log** — One-tap entry to your Logbook
- **Flashlight Toggle** — Turn flashlight on/off
- **Strobe Toggle** — Start/stop strobe light
- **Camera Snapshot** — Take a quick photo
- **Video Toggle** — Start/stop video recording
- **Voice Record** — Start/stop voice recording
- **Phone Ring** — Ring your phone after a delay
- **Quick Notify** — Send a notification after a delay
- **dB Meter** — Monitor ambient noise level

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
> HardwareDash puts your device's full hardware capabilities at your fingertips. Toggle the flashlight, test vibration patterns, read NFC tags, track your GPS location on a live map, monitor battery health, analyze audio with a real-time spectrum analyzer, and much more.
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
