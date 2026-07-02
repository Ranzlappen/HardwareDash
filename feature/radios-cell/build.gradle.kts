// :feature:radios-cell — Cellular diagnostics controller contract.
//
// Screenless: this module carries only the CellController interface, its
// result type, and the standard-flavor no-op. The rooted implementation
// lives in the sibling :feature:radios-cell-rooted module.

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.radios.cell"
}

dependencies {
    // Shared with the rooted sibling and both flavors' RootBindings.
    implementation(project(":core:root"))
}
