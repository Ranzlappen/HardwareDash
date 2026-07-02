// :feature:adbdebug — ADB-debugging controller contract + standard no-op (network toggle, prop dump, setprop).
//
// Screenless rooted-extras feature: the controller contract + standard no-op
// live here so shared code binds one interface in both flavors; the privileged
// impl ships in :feature:adbdebug-rooted. Surfaced in-app via the matching
// Rooted*ExtrasSection composable in :app.

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.adbdebug"
}
