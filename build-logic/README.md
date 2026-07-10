# build-logic

Gradle convention plugins for the HardwareDash (Gadget) build. This is an
**included build** — Gradle wires it into the root project via
`includeBuild("build-logic")` in the root `settings.gradle.kts`. Modules
apply plugins from here by id, exactly like any third-party Gradle plugin:

```kotlin
// in e.g. core/data/build.gradle.kts
plugins {
    id("gadget.android.library")
    id("gadget.android.hilt")
    id("gadget.android.room")
}
```

## Why convention plugins?

Without convention plugins, every module's `build.gradle.kts` repeats the
same `compileSdk`, `minSdk`, Java/Kotlin 17, Compose feature flags, Hilt
KSP wiring, Room schema location, etc. With 44 modules in the refactor,
that's 44 places to fix every time the build configuration drifts.

Convention plugins centralise the configuration. A module just declares
*what kind of module it is* (library, library+compose, feature, …) and
the plugin applies the right configuration.

## The 8 plugins

| Plugin id                              | Purpose                                                                                       | Typical consumer                       |
|----------------------------------------|-----------------------------------------------------------------------------------------------|----------------------------------------|
| `gadget.android.application`           | AGP application + Kotlin Android + JDK 17 + compileSdk/minSdk/targetSdk.                      | `:app`                                 |
| `gadget.android.application.compose`   | Compose feature flag + BOM + UI tooling for the application module.                           | `:app` (composed with .application)    |
| `gadget.android.library`               | AGP library + Kotlin Android + JDK 17 + compileSdk/minSdk/targetSdk.                          | `core/*`, `feature/*` base             |
| `gadget.android.library.compose`       | Compose feature flag + BOM + UI tooling for a library module.                                 | `core:designsystem`, `core:ui`         |
| `gadget.android.feature`               | Composite: applies `library` + `library.compose` + `hilt` + standard feature dependencies.    | `feature/*`                            |
| `gadget.android.hilt`                  | Hilt Gradle plugin + KSP + `hilt-android` runtime + `hilt-compiler` KSP processor.            | Any module using Hilt                  |
| `gadget.android.room`                  | androidx.room Gradle plugin + KSP + Room runtime + KSP processor + schema directory.          | `core:data`                            |
| `gadget.jvm.library`                   | Kotlin JVM + JDK 17. Pure-Kotlin module (no Android dependencies).                            | `core:model`                           |

## Adding a new plugin

1. Add the plugin class under
   `build-logic/convention/src/main/kotlin/<NewConventionPlugin>.kt`.
2. Register it in `build-logic/convention/build.gradle.kts`'s
   `gradlePlugin { plugins { register(...) } }` block.
3. If the plugin applies an AGP/Kotlin/Hilt plugin not yet listed in the
   root `build.gradle.kts`, add it there as `apply false` so Gradle can
   resolve the version.
4. If the plugin's source code references an AGP/Kotlin DSL type that
   isn't on the classpath yet, add the corresponding `*-gradlePlugin`
   entry to `gradle/libs.versions.toml` and `compileOnly`-depend on it
   in `convention/build.gradle.kts`.

## Layout

```
build-logic/
├── settings.gradle.kts      # composite-build settings, re-declares libs catalog
├── README.md                # this file
└── convention/
    ├── build.gradle.kts     # registers the 8 plugins
    └── src/main/kotlin/
        ├── AndroidApplicationConventionPlugin.kt          # packageless
        ├── AndroidApplicationComposeConventionPlugin.kt   # packageless
        ├── AndroidLibraryConventionPlugin.kt              # packageless
        ├── AndroidLibraryComposeConventionPlugin.kt       # packageless
        ├── AndroidFeatureConventionPlugin.kt              # packageless
        ├── AndroidHiltConventionPlugin.kt                 # packageless
        ├── AndroidRoomConventionPlugin.kt                 # packageless
        ├── JvmLibraryConventionPlugin.kt                  # packageless
        └── dev/ranzlappen/gadget/buildlogic/
            ├── KotlinAndroid.kt        # configureKotlinAndroid()
            ├── KotlinJvm.kt            # configureKotlinJvm()
            ├── AndroidCompose.kt       # configureAndroidCompose()
            └── ProjectExtensions.kt    # Project.libs accessor
```

Plugin classes are **packageless** by convention (matches Now-In-Android);
the `implementationClass` value in `build.gradle.kts` is the simple class
name. Helpers live in `dev.ranzlappen.gadget.buildlogic`.

## How module migration unfolds

Batch 1 (this batch) ships the plugins but does **not** migrate any
existing module skeleton onto them. Each `core/*` and `feature/*` module
keeps its inline `plugins { alias(libs.plugins.android.library); alias(
libs.plugins.kotlin.android) }` block until a later batch swaps in the
convention-plugin equivalent.

The migration order (planned):

- **Batch 2:** `:app` migration (Groovy → Kotlin DSL + applicationId
  change). Applies `gadget.android.application` +
  `gadget.android.application.compose`.
- **Batch 3+:** `core/*` modules, one batch per 5-8 modules.
- **Batch 7+:** `feature/*` modules, then `feature/*-rooted/`.
- **Batch N:** benchmark module gets a 9th plugin
  (`gadget.android.benchmark`) when macrobenchmarks land.
