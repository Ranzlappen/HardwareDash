import com.android.build.api.dsl.ApplicationExtension
import dev.ranzlappen.gadget.buildlogic.configureKotlinAndroid
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Convention plugin `gadget.android.application`.
 *
 * Applies:
 *   * `com.android.application`
 *   * `org.jetbrains.kotlin.android`
 *
 * Configures:
 *   * `compileSdk`, `minSdk`, JDK 17, Kotlin `jvmTarget` (via
 *     `configureKotlinAndroid`).
 *   * `targetSdk = 35` (set on the application extension here because it
 *     lives on `ApplicationDefaultConfig`, not on `CommonExtension`).
 *
 * Does **not** configure flavors, signing, `applicationId`, or
 * `versionCode` — those are application-specific and live in `:app/build
 * .gradle.kts`. Does **not** enable Compose; pair this plugin with
 * `gadget.android.application.compose` for that.
 */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.application")
                apply("org.jetbrains.kotlin.android")
            }

            extensions.configure<ApplicationExtension> {
                configureKotlinAndroid(this)
                defaultConfig.targetSdk = 35
            }
        }
    }
}
