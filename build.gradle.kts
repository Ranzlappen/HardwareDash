// =========================================================================
// Top-level Gradle build file.
// =========================================================================
//
// The `apply false` block locks the versions of every Gradle plugin used by
// subprojects (and by the convention plugins under build-logic/). When a
// convention plugin in build-logic calls `pluginManager.apply("com.android
// .library")`, Gradle looks up the matching version declaration here. Adding
// a new convention plugin that applies plugin X means adding plugin X to
// this list as `apply false`.
//
// Module build files do NOT re-declare versions; they apply by id only
// (either an AGP/Kotlin id, or one of the `gadget.*` convention plugin ids
// registered in build-logic/convention/build.gradle.kts).

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.androidx.room) apply false
}
