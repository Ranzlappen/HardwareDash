// :feature:storage-rooted — rooted storage actions (diskstats, mounts, fstrim, drop_caches).
//
// Sibling to :feature:storage, pulled in by the rooted flavor of :app via
// `rootedImplementation(project(":feature:storage-rooted"))`. Provides
// RootedStorageActionHandler bound under featureId "storage_root", which
// exposes the four safety-gated shell actions to the automation engine.

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.storage.rooted"
}

dependencies {
    implementation(project(":core:root"))
    implementation(project(":core:automation"))
    // The StorageController contract (+ result / config / MountEntry types)
    // lives in the base :feature:storage module so both flavors share it.
    implementation(project(":feature:storage"))
}
