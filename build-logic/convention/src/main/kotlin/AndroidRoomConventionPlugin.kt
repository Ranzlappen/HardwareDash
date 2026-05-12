import androidx.room.gradle.RoomExtension
import dev.ranzlappen.gadget.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Convention plugin `gadget.android.room`.
 *
 * Applies:
 *   * `androidx.room`             — the Room Gradle plugin (2.6+)
 *   * `com.google.devtools.ksp`   — annotation processing
 *
 * Configures:
 *   * `schemaDirectory` to `<projectDir>/schemas` so migrations have a
 *     stable snapshot location. The schemas directory MUST be committed
 *     to git; migrations rely on diffing successive schema versions.
 *
 * Adds:
 *   * `implementation` → `room-runtime` + `room-ktx`
 *   * `ksp`            → `room-compiler`
 *
 * Typically consumed by `core:data`. Other modules should read through
 * `core:data` repositories rather than depending on Room directly.
 */
class AndroidRoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("androidx.room")
                apply("com.google.devtools.ksp")
            }

            extensions.configure<RoomExtension> {
                schemaDirectory("$projectDir/schemas")
            }

            dependencies {
                add("implementation", libs.findLibrary("room-runtime").get())
                add("implementation", libs.findLibrary("room-ktx").get())
                add("ksp", libs.findLibrary("room-compiler").get())
            }
        }
    }
}
