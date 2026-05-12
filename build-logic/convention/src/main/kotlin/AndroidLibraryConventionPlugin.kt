import com.android.build.api.dsl.LibraryExtension
import dev.ranzlappen.gadget.buildlogic.configureKotlinAndroid
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Convention plugin `gadget.android.library`.
 *
 * The base Android-library plugin used by every `core/<name>` and `feature/<name>`
 * module. Applies:
 *   * `com.android.library`
 *   * `org.jetbrains.kotlin.android`
 *
 * Configures the same Java 17 / Kotlin `jvmTarget` / compileSdk / minSdk
 * baseline as the application plugin. Sets `testOptions.targetSdk` to
 * keep instrumented tests aligned with the application's runtime.
 *
 * Does NOT enable Compose. For Compose-using libraries, layer
 * `gadget.android.library.compose` on top (the feature plugin does this
 * automatically).
 */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.library")
                apply("org.jetbrains.kotlin.android")
            }

            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
                defaultConfig.targetSdk = 35
                testOptions.targetSdk = 35
            }
        }
    }
}
