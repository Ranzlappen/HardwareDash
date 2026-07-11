package dev.ranzlappen.gadget.core.permissions.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import dev.ranzlappen.gadget.core.permissions.FeaturePermissions

/**
 * Declares the per-feature permission multibinding map. A feature contributes
 * its needs with:
 *
 * ```kotlin
 * @Binds @IntoMap @StringKey("myFeature")
 * fun bindMyFeaturePermissions(impl: MyFeaturePermissionsProvider): FeaturePermissions
 * ```
 *
 * The `@Multibinds` declaration means the map can be **empty** — features
 * opt in incrementally, and [dev.ranzlappen.gadget.core.permissions.PermissionRegistry]
 * always contributes the app-wide baseline regardless.
 */
@Module
@InstallIn(SingletonComponent::class)
interface PermissionsModule {

    @Multibinds
    fun featurePermissions(): Map<String, FeaturePermissions>
}
