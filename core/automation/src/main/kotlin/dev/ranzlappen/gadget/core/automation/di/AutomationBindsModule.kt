package dev.ranzlappen.gadget.core.automation.di

import dagger.Module
import dagger.multibindings.Multibinds
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.ranzlappen.gadget.core.automation.ActionHandler

/**
 * Declares the action-handler multibinding map so [ModuleActionRegistry]
 * is injectable even before any feature contributes (empty map). Features
 * add entries via `@Binds @IntoMap @StringKey(featureId)`.
 */
@Module
@InstallIn(SingletonComponent::class)
interface AutomationBindsModule {

    @Multibinds
    fun actionHandlers(): Map<String, ActionHandler>
}
