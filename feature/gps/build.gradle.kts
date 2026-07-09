// :feature:gps — GPS / Location feature module.

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.gps"

    // The GPX/KML parser unit tests instantiate an XmlPullParserFactory and
    // touch a handful of android.* stubs; mirror the app module's unit-test
    // config so the moved JVM tests behave identically.
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))
    implementation(project(":core:monitoring"))
    implementation(project(":core:root"))
    // :core:automation — the action contract (ModuleAction / ActionHandler).
    // GpsActionHandler exposes tracking + spoofing + rooted diagnostics as
    // invocable actions for the future automation tool.
    implementation(project(":core:automation"))
    // Spoofing subsystem: legal-ack DataStore + the foreground-service
    // notification-channel seam.
    implementation(project(":core:datastore"))
    implementation(project(":core:notifications"))

    // FusedLocationProvider — standard SDK (no root).
    implementation(libs.play.services.location)
    // Runtime permission helper.
    implementation(libs.accompanist.permissions)
    // Map tiles — OSMDroid, no API key needed.
    implementation(libs.osmdroid.android)
    // SpoofEngine / LocationSpoofService coroutine primitives + Android dispatcher.
    implementation(libs.kotlinx.coroutines.android)
    // NotificationCompat for the spoof foreground-service notification.
    implementation(libs.androidx.core.ktx)

    // GpxParser / KmlParser / RouteEngine JVM unit tests.
    testImplementation(libs.junit)
    // GpsActionHandlerTest mocks GpsLocationTracker / GpsSpoofController / GpsController.
    testImplementation(libs.mockk)

    androidTestImplementation(project(":core:testing"))
    androidTestImplementation(libs.androidx.junit)
}
