// =========================================================================
// :app — application module.
// =========================================================================
//
// Migrated from Groovy (app/build.gradle) to Kotlin DSL in Batch 2. The
// configuration that used to live inline — compileSdk, minSdk, JDK 17,
// Compose feature flag, Compose Compiler version, Hilt + KSP wiring — is
// now provided by the convention plugins under build-logic/.
//
// Only application-specific things live here:
//   * applicationId + flavor-specific suffixes / versionCode offsets
//   * the dependency list
//   * the LSPosed module asset wiring (conditional on -PenableLsposedModule)
//   * APK output renaming
//   * detekt + ktlint configuration
//
// The Kotlin `namespace` stays `com.gadget` because the legacy code under
// `app/src/main/` is still under that package. Each migrated screen will
// move into a `feature/*` module with namespace
// `dev.ranzlappen.gadget.feature.<name>`. The applicationId (the install
// identifier on a device, distinct from `namespace`) is the part that
// changes here.

import com.android.build.gradle.internal.api.BaseVariantOutputImpl

plugins {
    id("gadget.android.application")
    id("gadget.android.application.compose")
    id("gadget.android.hilt")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

android {
    // Legacy code package. Migrates per-feature into modules with namespace
    // `dev.ranzlappen.gadget.<core|feature>.<name>` in later batches.
    namespace = "com.gadget"

    defaultConfig {
        // ─── applicationId (install identifier) ────────────────────────
        // Standard flavor ships as `dev.ranzlappen.gadget`. The rooted
        // flavor adds the `.rooted` suffix below, yielding
        // `dev.ranzlappen.gadget.rooted`.
        //
        // This is a one-time change from the legacy `com.gadget` /
        // `com.gadget.root` IDs. Users on the old standard build will see
        // the new install as a separate app the first time they update
        // through the new APK; this is intentional and noted in the
        // release-notes for this migration.
        applicationId = "dev.ranzlappen.gadget"

        // CI injects CI_VERSION_CODE / CI_VERSION_NAME via the workflow.
        // Per-flavor offset is added below (standard=+0, rooted=+1) so the
        // two APKs can ship side-by-side without ever colliding on Play.
        versionCode = (providers.gradleProperty("CI_VERSION_CODE").orNull?.toInt() ?: 1) * 10
        versionName = providers.gradleProperty("CI_VERSION_NAME").getOrElse("1.0-dev")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true

        ndk {
            // :feature:youtubedownloader bundles the yt-dlp + ffmpeg + Python
            // runtime as per-ABI native libs (~49 MB each). Ship only 64-bit
            // ARM (real devices) + x86_64 (the CI emulator); drop the 32-bit
            // ABIs (armeabi-v7a, x86) to roughly halve the APK. See ADR-0003.
            abiFilters += listOf("arm64-v8a", "x86_64")
        }

        // Build metadata (injected by CI, defaults for local dev).
        buildConfigField(
            "String", "BUILD_AUTHOR",
            "\"${providers.gradleProperty("BUILD_AUTHOR").getOrElse("local")}\""
        )
        buildConfigField(
            "String", "BUILD_DATE",
            "\"${providers.gradleProperty("BUILD_DATE").getOrElse("unknown")}\""
        )
        buildConfigField(
            "String", "BUILD_COMMIT",
            "\"${providers.gradleProperty("BUILD_COMMIT").getOrElse("unknown")}\""
        )
        buildConfigField(
            "String", "BUILD_BRANCH",
            "\"${providers.gradleProperty("BUILD_BRANCH").getOrElse("unknown")}\""
        )
        buildConfigField(
            "String", "APP_DESCRIPTION",
            "\"${providers.gradleProperty("APP_DESCRIPTION").getOrElse("Gadget — monitor and control device sensors, camera, audio, and more")}\""
        )
    }

    // ════════════════════ PRODUCT FLAVORS ════════════════════
    // Two flavors on a single dimension. Shared code stays in src/main/.
    // Flavor-specific code lives in src/standard/ and src/rooted/ — AGP
    // automatically scopes those source sets to their flavor's variants,
    // so the standard APK is physically incapable of containing anything
    // from src/rooted/ (and vice versa).
    flavorDimensions += "capability"
    productFlavors {
        create("standard") {
            dimension = "capability"
            // applicationId stays `dev.ranzlappen.gadget` (no suffix).
            // Inherits versionCode from defaultConfig (offset = +0).
            buildConfigField("boolean", "IS_ROOTED", "false")
            buildConfigField("String", "FLAVOR_NAME", "\"standard\"")
        }
        create("rooted") {
            dimension = "capability"
            applicationIdSuffix = ".rooted"          // -> dev.ranzlappen.gadget.rooted
            versionNameSuffix = "-rooted"
            // Offset = +1 keeps the rooted APK monotonic alongside standard.
            versionCode = (defaultConfig.versionCode ?: 10) + 1
            buildConfigField("boolean", "IS_ROOTED", "true")
            buildConfigField("String", "FLAVOR_NAME", "\"rooted\"")
        }
    }

    buildTypes {
        debug {
            // Explicit empty debug block so assemble<Flavor>Debug tasks
            // exist with predictable output names. Debug variants use the
            // AGP-managed debug keystore by default — no extra
            // configuration needed.
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        // Compose feature flag and Compose Compiler version are enabled by
        // the `gadget.android.application.compose` convention plugin.
        // BuildConfig is opt-in in AGP 8 — keep it on because the
        // `buildConfigField` calls in defaultConfig and productFlavors
        // generate fields consumed at runtime.
        buildConfig = true
    }

    ksp {
        // Room migration test fixtures live next to the database; checked
        // in via git so migration tests can diff schema versions.
        arg("room.schemaLocation", "$projectDir/schemas")
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // APK output rename: Gadget-{flavor}-v{version}-{buildType}.apk.
    //
    // `applicationVariants.all` is deprecated in AGP 8 in favour of the
    // androidComponents.onVariants Variant API, but still functional and
    // far less verbose for a simple rename. Migrating to the new Variant
    // API is queued for a later batch.
    @Suppress("DEPRECATION")
    applicationVariants.all {
        val variantFlavorName = flavorName
        val variantVersionName = versionName
        val variantBuildTypeName = buildType.name
        outputs.all {
            (this as BaseVariantOutputImpl).outputFileName =
                "Gadget-${variantFlavorName}-v${variantVersionName}-${variantBuildTypeName}.apk"
        }
    }
}

dependencies {
    // ─── Modular skeleton (:core / :feature) ────────────────────
    // Wires :app into the GadgetAppShell + Dashboard introduced in
    // PR #79. :core:designsystem flows in transitively via
    // :core:navigation; :core:ui via :feature:dashboard.
    implementation(project(":core:navigation"))
    implementation(project(":feature:dashboard"))
    // Direct :core:ui so :app's own composables (e.g. the Settings BackupCard
    // dropped into :feature:settings' backupSection slot) can use the design
    // system. Transitive-via-dashboard doesn't expose it at compile time.
    implementation(project(":core:ui"))
    // Phase 2 / Batch 1 — first real-feature migrations from
    // legacy-main per docs/migration-guide.md.
    implementation(project(":core:datastore"))
    // refactor-2026 Phase 2 / D1: root-safety framework — every rooted
    // controller routes its privileged mutations through RootSafetyGate +
    // RootCapabilityRegistry living in this module. Legacy
    // app/src/{standard,rooted}/java/com/gadget/root/* impls bind it.
    implementation(project(":core:root"))
    // Direct deps for the :app-level automation wiring
    // (AutomationBootRearmHandler binds :core:widgetkit's BootRearmHandler to
    // :core:automation's runtime — the two modules deliberately don't see
    // each other; :app is the assembly point).
    implementation(project(":core:automation"))
    implementation(project(":core:widgetkit"))
    // BackupManager injects :core:data's DatabaseCheckpointer to WAL-
    // checkpoint the modular DBs before the backup sweep (issue #153) —
    // transitive-via-features doesn't expose it at compile time.
    implementation(project(":core:data"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:torch"))
    implementation(project(":feature:vibration"))
    implementation(project(":feature:apps"))
    implementation(project(":feature:sensors"))
    implementation(project(":feature:battery"))
    implementation(project(":feature:gps"))
    implementation(project(":feature:storage"))
    implementation(project(":feature:radios-ir"))
    implementation(project(":feature:camera"))
    implementation(project(":feature:motion"))
    implementation(project(":feature:audio"))
    implementation(project(":feature:radios-nfc"))
    implementation(project(":feature:radios-bt"))
    implementation(project(":feature:radios-wifi"))
    implementation(project(":feature:radios-subghz"))
    implementation(project(":feature:flipper"))
    implementation(project(":feature:ambient"))
    implementation(project(":feature:lock"))
    implementation(project(":feature:actuators"))
    implementation(project(":feature:diagnostics"))
    implementation(project(":feature:bugreport"))
    implementation(project(":feature:manual"))
    implementation(project(":feature:youtubedownloader"))
    // Batch H: the automation rules list + builder (also pulls
    // :core:hardware — the read-side registry — into the :app Hilt graph).
    implementation(project(":feature:automation-ui"))

    // ─── Compose UI components ───────────────────────────────────
    // The Compose BOM + ui-tooling-preview + ui-tooling are pulled in by
    // the `gadget.android.application.compose` convention plugin; the
    // libraries listed here sit on top of the BOM.
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    // ─── Core Android ──────────────────────────────────────────
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // ─── Navigation ────────────────────────────────────────────
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.navigation.compose)

    // ─── CameraX ───────────────────────────────────────────────
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.video)

    // ─── Accompanist permissions helper ────────────────────────────
    implementation(libs.accompanist.permissions)

    // ─── Coroutines ─────────────────────────────────────────────
    implementation(libs.kotlinx.coroutines.android)

    // ─── WorkManager + Hilt-Work (widget periodic updates + reminders) ──
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    // ─── Biometric / device-credential auth (App-Organizer locked folders) ─
    implementation(libs.androidx.biometric)

    // ─── Serialization + DataStore ────────────────────────────────
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.datastore.preferences)

    // ─── GPS (FusedLocationProvider) ───────────────────────────────
    implementation(libs.play.services.location)

    // ─── Map (OSMDroid — no API key needed) ───────────────────────────
    implementation(libs.osmdroid.android)

    // ─── EXIF metadata editing for images ────────────────────────────
    implementation(libs.androidx.exifinterface)

    // ─── Room (still in :app for legacy reasons; later batch extracts to
    //     core:data via the `gadget.android.room` convention plugin) ─────
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // ─── Timber logging ───────────────────────────────────────────
    implementation(libs.timber)

    // ─── Vico charts (Compose-native, Material 3) ──────────────────────
    implementation(libs.vico.compose.m3)

    // ─── USB serial (CDC-ACM) — Flipper Zero USB transport ─────────────
    // Note: catalog key is `usb-serial-android` (not `usb-serial-for-android`)
    // because Kotlin's `for` is a reserved word; the accessor would otherwise
    // expand to `libs.usb.serial.for.android` and fail to parse. The Maven
    // coordinates (`com.github.mik3y:usb-serial-for-android`) are unchanged.
    implementation(libs.usb.serial.android)

    // ════════════════════ STANDARD-ONLY DEPENDENCIES ════════════════════
    // refactor-2026 Phase 2 / E2: standard Torch capability module — the
    // mirror of :feature:torch-rooted. Hosts the no-op StandardTorchSysfsController
    // + StandardTorchRootCapabilities and their StandardTorchModule @Binds, so
    // the standard variant has the same TorchSysfsController / TorchRootCapabilities
    // bindings the rooted variant gets from :feature:torch-rooted. Contains no
    // root code, so the leak gate has nothing to catch here.
    "standardImplementation"(project(":feature:torch-standard"))
    // Standard Vibration capability module — no-op VibrationRootCapabilities.
    "standardImplementation"(project(":feature:vibration-standard"))

    // ════════════════════ ROOTED-ONLY DEPENDENCIES ════════════════════
    // libsu is the privileged-shell + RootService backend used by the
    // rooted flavor. NEVER promote these to plain `implementation` — the
    // standard APK must ship without libsu in its classpath. The CI leak
    // gate asserts this on every push.
    "rootedImplementation"(libs.libsu.core)
    "rootedImplementation"(libs.libsu.service)
    // refactor-2026 Phase 2 / E2: rooted Torch capability module. Hosts
    // RootedTorchController (libsu sysfs writes) + RootedTorchRootCapabilities
    // (modular TorchRootCapabilities adapter) + the three helper coroutine
    // workers. Standard APK never sees this module — sourceSet scoping
    // physically prevents the dep from reaching the standard variant.
    "rootedImplementation"(project(":feature:torch-rooted"))
    // Rooted Vibration capability module — RootedVibrationRootCapabilities
    // (libsu sysfs PWM) + the dual-actuator / rumble-monitor helpers. Standard
    // APK never sees this module (sourceSet scoping).
    "rootedImplementation"(project(":feature:vibration-rooted"))
    // Rooted Storage action handler — diskstats, mounts, fstrim, drop_caches.
    "rootedImplementation"(project(":feature:storage-rooted"))
    // Rooted Diagnostics action handler — logcat tail, meminfo, cpuinfo, procstats.
    "rootedImplementation"(project(":feature:diagnostics-rooted"))
    // Rooted Lock overlay — secure-keyguard TYPE_APPLICATION_OVERLAY action.
    "rootedImplementation"(project(":feature:lock-rooted"))
    // Rooted Flipper — root-granted USB device-node access (no permission dialog).
    "rootedImplementation"(project(":feature:flipper-rooted"))
    // Rooted BugReport — force-grant runtime permissions via `pm grant`.
    "rootedImplementation"(project(":feature:bugreport-rooted"))
    // Rooted NFC — raw NCI command exchange over the vendor sysfs node.
    "rootedImplementation"(project(":feature:radios-nfc-rooted"))

    // ─── Unit tests ─────────────────────────────────────────────
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.json)

    // ─── Instrumented tests ───────────────────────────────────────
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

// ═══════════════════ LSPosed module asset wiring ════════════════════════
//
// When `-PenableLsposedModule=true` is passed to Gradle, the
// `:lsposed-module` project is included via settings.gradle.kts and its
// release APK is copied into `app/src/rooted/assets/lsposed-spoofer.apk`
// so the rooted flavor can stage + `pm install` it via libsu at runtime.
//
// CRITICAL: this hook MUST only run for the rooted variant. The standard
// APK must never bundle the LSPosed module asset. CI's standard-flavor job
// asserts this via a strings-check on the assembled APK.
if (providers.gradleProperty("enableLsposedModule").orNull == "true") {
    val assetTarget = file("src/rooted/assets/lsposed-spoofer.apk")
    val copyLsposedAsset = tasks.register<Copy>("copyLsposedAsset") {
        from(
            project(":lsposed-module").file(
                "build/outputs/apk/release/lsposed-module-release.apk"
            )
        )
        into(assetTarget.parentFile)
        rename(".*", "lsposed-spoofer.apk")
        dependsOn(":lsposed-module:assembleRelease")
    }

    afterEvaluate {
        @Suppress("DEPRECATION")
        android.applicationVariants.all {
            if (flavorName == "rooted") {
                preBuildProvider.configure { dependsOn(copyLsposedAsset) }
            }
        }
    }
}

detekt {
    config.setFrom("$rootDir/config/detekt/detekt.yml")
    buildUponDefaultConfig = true
    allRules = false
}

ktlint {
    android.set(true)
    ignoreFailures.set(true)
}
