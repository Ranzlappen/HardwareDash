package dev.ranzlappen.gadget.buildlogic

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/**
 * Apply the pure-Kotlin / pure-JVM configuration shared by every Gadget
 * non-Android module (e.g. `core:model`, `core:common`, `core:domain`):
 *
 *   * Java 17 source/target compatibility
 *   * Kotlin compiler `jvmTarget = JVM_17`
 *
 * No Android-specific configuration. If a module needs `Context` or any
 * other Android API, it must use `gadget.android.library` instead of
 * `gadget.jvm.library`.
 */
internal fun Project.configureKotlinJvm() {
    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    extensions.configure<KotlinJvmProjectExtension> {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
}
