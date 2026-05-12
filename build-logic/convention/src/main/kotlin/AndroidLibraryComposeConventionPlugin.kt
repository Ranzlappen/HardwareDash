import com.android.build.api.dsl.LibraryExtension
import dev.ranzlappen.gadget.buildlogic.configureAndroidCompose
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType

/**
 * Convention plugin `gadget.android.library.compose`.
 *
 * Layers Compose configuration onto an Android library:
 *   * `buildFeatures.compose = true`
 *   * Compose Compiler extension version pinned via
 *     `libs.versions.composeCompiler`.
 *   * Compose BOM + UI tooling dependencies.
 *
 * Must be paired with `gadget.android.library`. Apply this plugin in any
 * `core/<name>` or `feature/<name>` module that uses `@Composable`. The
 * `gadget.android.feature` plugin applies it transitively, so feature
 * modules do not need to apply it explicitly.
 */
class AndroidLibraryComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")

            val extension = extensions.getByType<LibraryExtension>()
            configureAndroidCompose(extension)
        }
    }
}
