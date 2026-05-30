package dev.ranzlappen.gadget.feature.torch.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.ranzlappen.gadget.core.widgetkit.feedback.WidgetFeedbackConfig
import dev.ranzlappen.gadget.core.widgetkit.render.WidgetIconResolver
import dev.ranzlappen.gadget.feature.torch.R
import dev.ranzlappen.gadget.feature.torch.StandardTorchController
import dev.ranzlappen.gadget.feature.torch.TorchController
import dev.ranzlappen.gadget.feature.torch.widget.customization.WidgetIconCatalog
import javax.inject.Singleton

/**
 * Hilt module that binds the singleton [TorchController]
 * implementation.
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
 * rather than the legacy `com.gadget.torch`. It still physically
 * resides in `app/src/standard/` (and the rooted impl in
 * `app/src/rooted/`) because the rooted side depends on the
 * `com.gadget.root.*` safety framework that has not yet been extracted
 * to a `:core:root` module; once that lands, both bindings relocate to
 * the sibling modules.
 *
 * In-module `src/standard/` + `src/rooted/` source sets are NOT used
 * for feature-level rooted code — the codebase convention is sibling
 * `<name>-rooted` modules. See
 * https://github.com/Ranzlappen/HardwareDash/issues/94 for the
 * pickup plan.
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
     * [WidgetIconResolver]. The kit's [WidgetAppearanceRenderer]
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

    companion object {
        /**
         * Torch's per-feature [WidgetFeedbackConfig] consumed by the
         * kit-side `WidgetFeedbackDispatcher`.
         *
         * Channel id is **pinned to the legacy `"widget_feedback"`** so
         * any system-settings overrides a user already set on the channel
         * (sound, badge, importance) carry across the kit migration — a
         * renamed channel would silently lose them. Future widget-bearing
         * features must use their own feature-prefixed id to avoid
         * colliding with this one. Small icon + channel strings come from
         * torch's res. The notification-id base scopes hashed IDs into a
         * torch-specific integer range ("TW" prefix).
         *
         * As the second widget-bearing feature lands, this will be
         * promoted to a `Map<FeatureId, WidgetFeedbackConfig>`
         * multibinding so a single dispatcher serves both.
         */
        @Provides
        @Singleton
        fun provideWidgetFeedbackConfig(
            @ApplicationContext context: Context,
        ): WidgetFeedbackConfig = WidgetFeedbackConfig(
            channelId = "widget_feedback",
            channelName = context.getString(R.string.widget_feedback_channel_name),
            channelDescription = context.getString(R.string.widget_feedback_channel_description),
            smallIcon = R.drawable.ic_strobe,
            notificationIdBase = 0x57_46_00_00, // "WF" — matches legacy WidgetFeedbackDispatcher
        )
    }
}
