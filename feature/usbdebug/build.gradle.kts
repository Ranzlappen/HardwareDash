// :feature:usbdebug — USB-debugging controller contract + standard no-op (function switch, device dump, serial-service dump).
//
// Screenless rooted-extras feature: the controller contract + standard no-op
// live here so shared code binds one interface in both flavors; the privileged
// impl ships in :feature:usbdebug-rooted. Surfaced in-app via the matching
// Rooted*ExtrasSection composable in :app.

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.usbdebug"
}
