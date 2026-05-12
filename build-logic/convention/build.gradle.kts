// =========================================================================
// :build-logic:convention — convention plugin module.
// =========================================================================
//
// This module compiles the 8 Gadget convention plugins and exposes each one
// as a Gradle plugin via the `gradlePlugin { plugins { register(...) } }`
// DSL below. The composite-build hook in the root `settings.gradle.kts`
// (`includeBuild("build-logic")`) makes every registered plugin id
// available to downstream module build files as a plain plugin alias.
//
// AGP / Kotlin / KSP / Room Gradle plugin types are referenced as
// `compileOnly` — the convention plugins use those DSL types at compile
// time but the actual plugin jars are resolved per-target-project via
// `pluginManager.apply(...)`.

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}

group = "dev.ranzlappen.gadget.buildlogic"

// JDK 17 for the convention plugin sources themselves. Matches the
// `compileOptions { sourceCompatibility = VERSION_17 }` we set on every
// downstream module. This is the JDK that builds the plugins, not the JDK
// targeted by the apps they configure (which is also 17, but separately).
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.room.gradlePlugin)
}

gradlePlugin {
    plugins {
        // --- Application modules ----------------------------------------
        register("androidApplication") {
            id = "gadget.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidApplicationCompose") {
            id = "gadget.android.application.compose"
            implementationClass = "AndroidApplicationComposeConventionPlugin"
        }

        // --- Library modules --------------------------------------------
        register("androidLibrary") {
            id = "gadget.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidLibraryCompose") {
            id = "gadget.android.library.compose"
            implementationClass = "AndroidLibraryComposeConventionPlugin"
        }

        // --- Feature modules (composite: library + compose + hilt) ------
        register("androidFeature") {
            id = "gadget.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }

        // --- Cross-cutting plugins --------------------------------------
        register("androidHilt") {
            id = "gadget.android.hilt"
            implementationClass = "AndroidHiltConventionPlugin"
        }
        register("androidRoom") {
            id = "gadget.android.room"
            implementationClass = "AndroidRoomConventionPlugin"
        }

        // --- Pure-Kotlin / JVM library ----------------------------------
        register("jvmLibrary") {
            id = "gadget.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
    }
}
