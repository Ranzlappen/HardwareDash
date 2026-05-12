import com.android.build.api.dsl.ApplicationExtension
import dev.ranzlappen.gadget.buildlogic.configureAndroidCompose
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType

/**
 * Convention plugin `gadget.android.application.compose`.
 *
 * Applies Compose configuration to the application module:
 *   * `buildFeatures.compose = true`
 *   * Compose Compiler extension version pinned via
 *     `libs.versions.composeCompiler`.
 *   * Compose BOM + UI tooling dependencies.
 *
 * Must be paired with `gadget.android.application` — this plugin assumes
 * the `ApplicationExtension` already exists on the project. Applying it
 * to a library project will fail; use `gadget.android.library.compose`
 * for libraries.
 */
class AndroidApplicationComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")

            val extension = extensions.getByType<ApplicationExtension>()
            configureAndroidCompose(extension)
        }
    }
}
