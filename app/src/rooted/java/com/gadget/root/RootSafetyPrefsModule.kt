package com.gadget.root

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val ROOT_SAFETY_DS_NAME = "root_safety_ds"

private val Context.rootSafetyStore: DataStore<Preferences>
    by preferencesDataStore(name = ROOT_SAFETY_DS_NAME)

/**
 * Provides a dedicated DataStore for the rooted-features safety framework.
 * Isolated from the main `gadget_settings_ds` so a future schema migration
 * here can't disturb unrelated user preferences.
 */
@Module
@InstallIn(SingletonComponent::class)
object RootSafetyPrefsModule {

    @Provides
    @Singleton
    @RootSafetyPrefs
    fun provideRootSafetyDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.rootSafetyStore
}
