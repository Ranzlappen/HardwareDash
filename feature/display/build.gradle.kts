// :feature:display — display controller contract + standard no-op.
//
// Screenless rooted-extras feature: the DisplayController contract (brightness
// / density / refresh-rate overrides + SurfaceFlinger dump) and the standard
// no-op live here so shared code binds one interface in both flavors; the
// privileged impl ships in :feature:display-rooted. Surfaced in-app via the
// RootedDisplayExtrasSections composable in :app.

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.display"
}
