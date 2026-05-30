// :feature:torch-rooted — rooted-only Torch capability surface.
//
// Sibling to :feature:torch, pulled in by the rooted flavor of :app via
// `rootedImplementation(project(":feature:torch-rooted"))`. The standard APK
// is physically unable to compile against this module — see CLAUDE.md's
// "Standard-APK leak gate".
//
// Status (refactor-2026 Phase 2 / E1, E2): real rooted impl. Contains the
// RootedTorchController (libsu-backed sysfs writes), the
// RootedTorchRootCapabilities adapter that exposes the rooted controller
// through :feature:torch's modular TorchRootCapabilities interface, and the
// three helpers (DutyCycleStrobe, MultiLedOrchestrator, ThermalOverrideController)
// + TorchSysfsPaths the controller delegates to. The rooted-flavor Hilt
// binding for both surfaces lives in RootedTorchModule below.
//
// Why :feature:torch (not :feature:torch-standard): the modular
// TorchController + TorchRootCapabilities + TorchRootResult interfaces this
// module implements live in :feature:torch's src/main (since the standard
// impl already exists there). The legacy TorchController interface (the
// rooted-only sysfs surface) lives in :feature:torch's legacy/ subpackage
// so both flavors' bindings can reference it.

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.torch.rooted"
}

dependencies {
    // Modular Torch interfaces this module implements + the legacy
    // LegacyTorchController interface the rooted impl satisfies.
    implementation(project(":feature:torch"))
    // Root-safety framework (RootSafetyGate, RootCapabilityRegistry,
    // RootFeatureKey, RootShell, RootGateDecision) the controller routes
    // every privileged sysfs write through.
    implementation(project(":core:root"))

    // libsu — the rooted shell binder backing RootShell. Scoped to this
    // module's rooted-flavor binding; the standard APK never reaches this
    // dep (it pulls a standard sibling that satisfies the same interfaces
    // with no-op impls).
    implementation(libs.libsu.core)
    implementation(libs.libsu.service)
}
