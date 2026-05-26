// :core:model — skeleton; configuration via gadget.jvm.library.

plugins {
    id("gadget.jvm.library")
}

dependencies {
    // Pure-Kotlin coroutines (NOT the -android artifact) so the dependency-
    // free model module can express an optional push-based MetricSource
    // (`stream(): Flow<Float>?`) without pulling in Android or a feature.
    implementation(libs.kotlinx.coroutines.core)
}
