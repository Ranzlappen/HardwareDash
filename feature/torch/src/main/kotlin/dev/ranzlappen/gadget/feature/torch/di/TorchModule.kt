package dev.ranzlappen.gadget.feature.torch.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import dev.ranzlappen.gadget.core.widgetkit.boot.BootRearmHandler
import dev.ranzlappen.gadget.core.widgetkit.render.WidgetIconResolver
import dev.ranzlappen.gadget.feature.torch.StandardTorchController
import dev.ranzlappen.gadget.feature.torch.TorchController
import dev.ranzlappen.gadget.feature.torch.monitor.TorchBootRearmHandler
import dev.ranzlappen.gadget.feature.torch.widget.customization.WidgetIconCatalog
import javax.inject.Singleton

/**
 * Hilt module that binds the singleton [TorchController] implementation
 * and surfaces torch's [WidgetIconCatalog] as the kit-side
 * [WidgetIconResolver].
 *
 * Phase 2 / Batch 1 + 1.1 ship only the standard-flavor
 * [StandardTorchController] (Camera2-based). Rooted extras
 * (DutyCycleStrobe, MultiLedOrchestrator, ThermalOverrideController)
 * are deferred — they need `RootCapabilityRegistry` + `RootSafetyGate`
 * to land first.
 *
 * Flavor-specific binding pattern: rooted-only torch surface ships as
 * the sibling `:feature:torch-rooted` Gradle module (mirroring
 * `:feature:lock-rooted` / `:feature:diagnostics-rooted` etc.) wired
 * into `:app` via `rootedImplementation`. The standard APK is
 * physically unable to compile against the rooted module — see
 * CLAUDE.md's "Standard-APK leak gate" section.
 *
 * The standard no-op for the modular root seam
 * ([dev.ranzlappen.gadget.feature.torch.standard.StandardTorchRootCapabilities],
 * bound in the standard flavor's `RootBindings`) now lives under the
 * modular `dev.ranzlappen.gadget.feature.torch.standard` namespace
 * rather than the legacy `com.gadget.torch`. The root-safety framework
 * (`RootSafetyGate`, `RootCapabilityRegistry`, etc.) extracted to
 * `:core:root` in refactor-2026 D1, so the next step (E2) relocates the
 * standard binding into `:feature:torch-rooted` and the rooted impl into
 * the sibling module proper. Both standard + rooted bindings physically
 * reside in `app/src/{standard,rooted}/` until that lands.
 *
 * In-module `src/standard/` + `src/rooted/` source sets are NOT used
 * for feature-level rooted code — the codebase convention is sibling
 * `<name>-rooted` modules. See
 * https://github.com/Ranzlappen/HardwareDash/issues/94 for the
 * pickup plan.
 *
 * **Why two modules in this directory.** `@Binds` requires an `abstract`
 * class, but `@Provides` in a companion object on an abstract Dagger
 * module is fragile across Hilt/KSP versions (some configurations
 * silently skip it). Per the codebase convention (mirrors
 * `core.data.di.DataModule` etc.), `@Provides` functions live in a
 * sibling top-level `object` module — see [TorchProvidesModule].
 *
 * `@InstallIn(SingletonComponent::class)` so the controller's
 * lifecycle matches the app's — its CameraManager.TorchCallback
 * subscription stays live for the whole process.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class TorchModule {

    @Binds
    @Singleton
    abstract fun bindTorchController(
        impl: StandardTorchController,
    ): TorchController

    /**
     * Surface torch's [WidgetIconCatalog] as the kit-side
     * [WidgetIconResolver]. The kit's `WidgetAppearanceRenderer`
     * injects the resolver interface (not the concrete catalog) so the
     * renderer can live in `:core:widgetkit` without knowing about any
     * particular feature's bundled drawables.
     *
     * As more widget-bearing features land, this binding will become a
     * multibinding keyed by feature id (planned for C5's provider
     * registry batch).
     */
    @Binds
    @Singleton
    abstract fun bindWidgetIconResolver(
        impl: WidgetIconCatalog,
    ): WidgetIconResolver

    /**
     * Bind torch's [BootRearmHandler] into the kit-side
     * `Map<String, BootRearmHandler>` multibinding under
     * [TorchBootRearmHandler.FEATURE_ID]. Fired by the kit's
     * `BootCompletedReceiver` after device boot to rearm the monitor
     * foreground service iff there's a placed monitor widget AND
     * monitoring is enabled.
     */
    @Binds
    @IntoMap
    @StringKey(TorchBootRearmHandler.FEATURE_ID)
    abstract fun bindBootRearmHandler(
        impl: TorchBootRearmHandler,
    ): BootRearmHandler
}
