plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.gadget.spoofer.xposed"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.gadget.spoofer.xposed"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        debug {
            // Default debug-keystore signing — LSPosed accepts any signature.
        }
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    // Xposed API: provided at runtime by the LSPosed framework, never
    // bundled into the APK. Must be `compileOnly`.
    //
    // The Xposed-API maven repo (https://api.xposed.info/) is declared in
    // the root settings.gradle.kts conditionally on the same
    // `enableLsposedModule` Gradle property that includes this module.
    compileOnly("de.robv.android.xposed:api:82")

    // We DON'T depend on :app — the handshake class is referenced by FQN
    // string and resolved via XposedHelpers.findClass at runtime in the
    // hooked process's classloader.
}
