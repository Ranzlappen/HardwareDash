// :feature:notification — notification controller contract + standard no-op (sticky override, listener access, lock-screen overlay).
//
// Screenless rooted-extras feature: the controller contract + standard no-op
// live here so shared code binds one interface in both flavors; the privileged
// impl ships in :feature:notification-rooted. Surfaced in-app via the matching
// Rooted*ExtrasSection composable in :app.

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.notification"
}
