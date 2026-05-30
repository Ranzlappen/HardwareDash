// :feature:torch-rooted — rooted-only Torch capability surface.
//
// Sibling to :feature:torch, pulled in by the rooted flavor of :app via
// `rootedImplementation(project(":feature:torch-rooted"))`. The standard APK
// is physically unable to compile against this module — see CLAUDE.md's
// "Standard-APK leak gate".
//
// Status: skeleton (parity with :feature:diagnostics-rooted etc.). The real
// rooted Torch implementation — RootedTorchRootCapabilities + the legacy
// RootedTorchController / DutyCycleStrobe / MultiLedOrchestrator /
// ThermalOverrideController it delegates to — currently lives in
// app/src/rooted/ because it depends on the `com.gadget.root.*` safety
// framework (RootSafetyGate / RootCapabilityRegistry / RootFeatureKey), which
// is not yet a standalone module. Migrating it here is gated on extracting
// that framework to a `:core:root` module; tracked at
// https://github.com/Ranzlappen/HardwareDash/issues/94.

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.torch.rooted"
}
