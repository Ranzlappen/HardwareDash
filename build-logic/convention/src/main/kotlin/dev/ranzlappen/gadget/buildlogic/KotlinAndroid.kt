package dev.ranzlappen.gadget.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/**
 * Apply the Android-flavor Kotlin configuration shared by every Gadget
 * Android module (application or library):
 *
 *   * `compileSdk = 35` (Android 15)
 *   * `minSdk     = 29` (Android 10, covers the Huawei P30 baseline)
 *   * Java 17 source/target compatibility
 *   * Kotlin compiler `jvmTarget = JVM_17`
 *
 * `targetSdk` is set separately by the application / library convention
 * plugin because it lives on different DSL extensions and is not part of
 * `CommonExtension`.
 *
 * The `CommonExtension<*, *, *, *, *, *>` type signature matches AGP 8.6's
 * six-parameter shape (BuildFeatures, BuildType, DefaultConfig,
 * ProductFlavor, AndroidResources, Installation). A future AGP bump may
 * shift the arity — guard the bump in its own batch.
 */
internal fun Project.configureKotlinAndroid(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
) {
    commonExtension.apply {
        compileSdk = 35

        defaultConfig {
            minSdk = 29
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }

    extensions.configure<KotlinAndroidProjectExtension> {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
}
