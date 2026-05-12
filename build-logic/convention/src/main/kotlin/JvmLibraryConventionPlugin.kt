import dev.ranzlappen.gadget.buildlogic.configureKotlinJvm
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Convention plugin `gadget.jvm.library`.
 *
 * Pure-Kotlin / JVM library — no Android APIs. Applies:
 *   * `org.jetbrains.kotlin.jvm`
 *
 * Configures Java 17 source/target and Kotlin compiler `jvmTarget` via
 * `configureKotlinJvm`.
 *
 * Use this plugin for modules whose code doesn't (and shouldn't) import
 * anything from the `android.*` namespace:
 *
 *   * `core:model`   — cross-feature data classes
 *   * `core:domain`  — use-cases / policy
 *   * `core:common`  — pure utilities (Result types, dispatchers, …)
 *
 * If a `core/<name>` module needs `Context`, `Uri`, or other Android types,
 * switch it to `gadget.android.library` instead.
 */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")
            configureKotlinJvm()
        }
    }
}
