package dev.ranzlappen.gadget.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Apply Compose configuration to either an application or library module.
 *
 *   * Enables the Compose build feature.
 *   * Pins the Compose Compiler Gradle extension version from the
 *     `composeCompiler` entry in `gradle/libs.versions.toml`. Must be
 *     kept in lockstep with the Kotlin Gradle plugin version — see
 *     https://developer.android.com/jetpack/androidx/releases/compose-kotlin
 *   * Adds the Compose BOM as a `platform()` dependency on both
 *     `implementation` and `androidTestImplementation` so every Compose
 *     library follows the BOM-pinned version.
 *   * Adds `ui-tooling-preview` (compile-time) and `ui-tooling`
 *     (debug-only) for `@Preview` support.
 *
 * Callers must apply this from a convention plugin that has already
 * applied either `com.android.application` or `com.android.library`.
 */
internal fun Project.configureAndroidCompose(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
) {
    commonExtension.apply {
        buildFeatures {
            compose = true
        }

        composeOptions {
            kotlinCompilerExtensionVersion =
                libs.findVersion("composeCompiler").get().toString()
        }
    }

    val bom = libs.findLibrary("androidx-compose-bom").get()

    dependencies {
        add("implementation", platform(bom))
        add("androidTestImplementation", platform(bom))
        add("implementation", libs.findLibrary("androidx-ui-tooling-preview").get())
        add("debugImplementation", libs.findLibrary("androidx-ui-tooling").get())
        // androidx.lifecycle.compose.collectAsStateWithLifecycle — pauses
        // flow collection when the host lifecycle drops below STARTED.
        // Lives in lifecycle-runtime-compose (not -ktx); every Compose
        // module gets it here so callsites just import + use.
        add("implementation", libs.findLibrary("androidx-lifecycle-runtime-compose").get())
    }
}
