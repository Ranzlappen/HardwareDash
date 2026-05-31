package dev.ranzlappen.gadget.feature.vibration.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import dev.ranzlappen.gadget.core.widgetkit.boot.BootRearmHandler
import dev.ranzlappen.gadget.core.widgetkit.render.WidgetIconResolver
import dev.ranzlappen.gadget.feature.vibration.StandardVibrationController
import dev.ranzlappen.gadget.feature.vibration.VibrationController
import dev.ranzlappen.gadget.feature.vibration.monitor.VibrationBootRearmHandler
import dev.ranzlappen.gadget.feature.vibration.widget.customization.VibrationIconCatalog
import javax.inject.Singleton

/**
 * `@Binds` Hilt module for `:feature:vibration` — the standard-tier
 * [VibrationController], the kit-side [WidgetIconResolver], and the boot-rearm
 * handler. Sibling to the `@Provides`-only [VibrationProvidesModule] (the
 * split mirrors torch's `TorchModule` / `TorchProvidesModule`, which avoids the
 * fragile companion-`@Provides`-on-abstract-module pitfall).
 *
 * The privileged [dev.ranzlappen.gadget.feature.vibration.VibrationRootCapabilities]
 * binding lives in the per-flavor sibling modules (`:feature:vibration-rooted`
 * / `-standard`), not here.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class VibrationModule {

    @Binds
    @Singleton
    abstract fun bindVibrationController(impl: StandardVibrationController): VibrationController

    @Binds
    @Singleton
    abstract fun bindWidgetIconResolver(impl: VibrationIconCatalog): WidgetIconResolver

    @Binds
    @IntoMap
    @StringKey(VibrationBootRearmHandler.FEATURE_ID)
    abstract fun bindBootRearmHandler(impl: VibrationBootRearmHandler): BootRearmHandler
}
