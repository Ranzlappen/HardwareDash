import dev.ranzlappen.gadget.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Convention plugin `gadget.android.hilt`.
 *
 * Applies:
 *   * `com.google.devtools.ksp`   — annotation processing
 *   * `com.google.dagger.hilt.android` — the Hilt Gradle plugin
 *
 * Adds:
 *   * `implementation` → `hilt-android` runtime
 *   * `ksp`            → `hilt-compiler`
 *   * `kspTest`, `kspAndroidTest` → `hilt-compiler` (so test source sets
 *     can use `@HiltAndroidTest`)
 *
 * This plugin is consumed by `gadget.android.feature` and by any
 * non-feature module that needs Hilt (e.g. `core:data` for repository
 * injection). It does NOT apply `com.android.library` — the caller is
 * expected to apply either `gadget.android.library` or
 * `gadget.android.application` first.
 */
class AndroidHiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.google.devtools.ksp")
                apply("com.google.dagger.hilt.android")
            }

            dependencies {
                add("implementation", libs.findLibrary("hilt-android").get())
                add("ksp", libs.findLibrary("hilt-compiler").get())
                add("kspTest", libs.findLibrary("hilt-compiler").get())
                add("kspAndroidTest", libs.findLibrary("hilt-compiler").get())
            }
        }
    }
}
