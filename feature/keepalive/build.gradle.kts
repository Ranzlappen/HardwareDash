// :feature:keepalive — persistent keep-alive contract + standard no-op +
// the shared foreground service.
//
// Screenless capability feature (the "Keep Alive" toggle is surfaced from
// Settings): the KeepAliveController contract, its result/config types, the
// standard implementation, and the PersistentKeepAliveService both flavors
// start live here so shared code binds one interface in both flavors. The
// privileged impl (Doze whitelist + pm grant) ships in
// :feature:keepalive-rooted. Migrated out of the legacy com.gadget.keepalive /
// com.gadget.services packages.

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.keepalive"
}

dependencies {
    // ContextCompat.startForegroundService for the standard controller.
    implementation(libs.androidx.core.ktx)
}
