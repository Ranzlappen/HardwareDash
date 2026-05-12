# Sensor / Actuator API

> **Status: placeholder.** Detailed design lands in Phase 1
> (sensor-actuator abstraction batch). This file exists in Batch 0 so the
> ADR and `core/hardware/` module skeleton have a real target to point at.

## Why this exists

HardwareDash currently treats every hardware surface as a one-off:

- The IR screen reaches directly at `ConsumerIrManager`.
- The GPS screen reaches at `FusedLocationProvider`.
- The audio screen reaches at `AudioManager` /
  `MediaRecorder`.
- The camera, flashlight, vibrator, NFC adapter, Wi-Fi manager, etc.
  each have their own ad-hoc wiring.

This works for one-off screens but blocks:

1. **An automation engine.** Rules like "if light < 5 lux then turn
   flashlight on" need to subscribe to *any* sensor and dispatch to
   *any* actuator without knowing the device-specific API.
2. **Test fakes.** Today, testing a sensor-driven feature in CI either
   requires an emulator with the right capability or a hand-rolled
   shim. A shared abstraction lets every feature swap in a fake at the
   same seam.
3. **Rooted-only hardware.** Some sensors / actuators (kernel
   thermals, raw input devices) only work on the rooted flavor. They
   plug into the registry behind `RootCapabilityRegistry` so the
   standard APK literally cannot bind to them at runtime.

## Shape (preview)

```
core/hardware/
  ├── Sensor.kt              // sealed interface; one impl per surface
  ├── Actuator.kt            // sealed interface; one impl per surface
  ├── SensorEvent.kt         // typed payload shape
  ├── ActuatorCommand.kt     // typed command shape
  ├── HardwareRegistry.kt    // registers and looks up by SensorId /
  │                          // ActuatorId; Hilt-multibound
  └── di/
        HardwareModule.kt    // @Provides registry, binds default impls
```

A `Sensor` exposes a `Flow<SensorEvent>`. An `Actuator` accepts an
`ActuatorCommand` and returns a result. The registry is Hilt-
multibound — feature modules contribute their own sensor/actuator
implementations via `@IntoSet`.

Rooted-only entries go through a `rootedImplementation`-only multibind
in `feature/*-rooted/`. The registry's lookup never throws if a rooted
sensor is missing on the standard flavor; it returns `null` and the UI
shows the user-facing "feature requires rooted build" copy from
`localization/Strings.kt`.

## When this expands

The full design + Hilt wiring + first concrete sensor migration land in
Phase 1, Batch 1 (planned ordering — subject to revision).
