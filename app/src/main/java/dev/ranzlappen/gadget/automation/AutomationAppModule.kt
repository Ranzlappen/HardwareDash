package dev.ranzlappen.gadget.automation

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import dev.ranzlappen.gadget.core.widgetkit.boot.BootRearmHandler
import javax.inject.Singleton

/**
 * `:app`-level automation wiring that needs both `:core:automation` and
 * `:core:widgetkit` (which `:core:automation` itself must not depend on —
 * it would drag Compose into the engine module). Mirrors the torch /
 * vibration `@Binds @IntoMap @StringKey` boot-rearm contributions.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AutomationAppModule {

    @Binds
    @Singleton
    @IntoMap
    @StringKey(AutomationBootRearmHandler.FEATURE_ID)
    abstract fun bindAutomationBootRearmHandler(
        impl: AutomationBootRearmHandler,
    ): BootRearmHandler
}
