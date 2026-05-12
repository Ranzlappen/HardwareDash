import dev.ranzlappen.gadget.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Convention plugin `gadget.android.feature`.
 *
 * The one-stop plugin for every `feature/*` module. Composes:
 *   * `gadget.android.library`         — base library configuration
 *   * `gadget.android.library.compose` — Compose support
 *   * `gadget.android.hilt`            — Hilt + KSP
 *
 * Then adds the standard set of feature-module dependencies that nearly
 * every UI feature consumes: lifecycle, Hilt navigation, Material 3, and
 * activity-compose. Feature modules can add module-specific dependencies
 * on top.
 *
 * If a feature module needs Room, layer `gadget.android.room` on top.
 * Room is intentionally not in the default set — most feature modules
 * read through `core:data`, not directly from Room.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("gadget.android.library")
                apply("gadget.android.library.compose")
                apply("gadget.android.hilt")
            }

            dependencies {
                add("implementation", libs.findLibrary("androidx-lifecycle-runtime-ktx").get())
                add("implementation", libs.findLibrary("hilt-navigation-compose").get())
                add("implementation", libs.findLibrary("androidx-material3").get())
                add("implementation", libs.findLibrary("androidx-activity-compose").get())
            }
        }
    }
}
