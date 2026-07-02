// :feature:gps-rooted — rooted sibling of :feature:gps.
//
// Hosts the privileged GPS / GNSS controller (read-only NMEA raw tap +
// constellation dump over vendor sysfs / tty nodes via the root shell),
// gated by RootSafetyGate. Pulled into the rooted flavor of :app only via
// `rootedImplementation(project(":feature:gps-rooted"))`, so the standard
// APK never sees it.

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.gps.rooted"
}

dependencies {
    implementation(project(":core:root"))
    // The GPS controller + spoofing contract types live in the base
    // :feature:gps module so both flavors share them.
    implementation(project(":feature:gps"))
    // LsposedModuleInstaller uses Dispatchers / withContext directly; core:root
    // exposes coroutines as implementation only, so declare it explicitly.
    implementation(libs.kotlinx.coroutines.core)
}
