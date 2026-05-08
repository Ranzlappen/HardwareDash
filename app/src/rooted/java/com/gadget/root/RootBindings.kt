package com.gadget.root

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Rooted-flavor Hilt bindings. Same fully-qualified name as the standard
 * flavor's binding file; Gradle picks one based on the active product flavor
 * so the two never collide in a single APK.
 */
@Module
@InstallIn(SingletonComponent::class)
object RootBindings {

    @Provides
    @Singleton
    fun provideRootCapabilityRegistry(): RootCapabilityRegistry =
        RootedRootCapabilityRegistry()

    @Provides
    @Singleton
    fun provideRootSafetyGate(): RootSafetyGate = RootedRootSafetyGate()

    @Provides
    @Singleton
    fun provideRootSoftLimiter(): RootSoftLimiter = RootedRootSoftLimiter()
}
