// :feature:sensors — skeleton; configuration via gadget.android.feature.

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.sensors"
}

dependencies {
    // Added by scripts/new-feature.sh (skeleton-fill) so the generated screen
    // compiles. :core:ui brings the design system; :core:navigation the routes.
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))
}
