package com.gadget.root.ui

/**
 * Marker file for the `com.gadget.root.ui` Compose surface
 * (rooted-features cards, fatal-launch screen, emergency-reset dialog,
 * 13 `Rooted*ExtrasSections` per-feature surfaces).
 *
 * **Why this package stays at `com.gadget.root.ui` in `:app/src/main/`
 * (refactor-2026 Phase 2 / D4 policy).** Every composable in here
 * reaches the legacy feature controllers (torch, vibration, camera,
 * audio, …) via [com.gadget.root.RootFeaturesEntryPoint] —
 * `EntryPointAccessors.fromApplication(ctx, RootFeaturesEntryPoint::class.java)`.
 * The entry point can't move out of `:app/src/main/` until those 22
 * legacy controllers are themselves modularised (see that file's
 * KDoc), so the UI files that consume it stay alongside it.
 *
 * The kit-side root-safety contract (`RootSafetyGate`,
 * `RootCapabilityRegistry`, `RootFeatureToggles`, …) moved to
 * `:core:root` in D1; every file in this package picks those up via
 * the wildcard `import dev.ranzlappen.gadget.core.root.*` line at the
 * top of the file (a D1-fixup addition).
 *
 * **Replacement plan.** Once each feature controller migrates to a
 * `:feature:<name>` module, its rooted-extras section becomes part of
 * that feature's own UI tree (e.g. `RootedTorchExtrasSection` would
 * live in `:feature:torch-rooted`'s `ui/`). The `Rooted*Card` aggregator
 * surfaces (this package's `RootedFeatureTogglesCard`,
 * `EmergencyResetCard`, etc.) would then compose those sections via
 * the same `Map<FeatureId, ?>`-style multibinding the widget kit and
 * automation contract already established. Tracked at
 * https://github.com/Ranzlappen/HardwareDash/issues/94.
 *
 * Until that lands, this file is the deferral marker — it has no
 * runtime behaviour; its purpose is to make the policy decision
 * visible to anyone grepping the package.
 */
@Suppress("unused")
private object D4PackageNote
