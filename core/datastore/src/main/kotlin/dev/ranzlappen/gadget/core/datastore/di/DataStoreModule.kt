package dev.ranzlappen.gadget.core.datastore.di

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

/**
 * Hilt module providing the singleton
 * [DataStore]&lt;[Preferences]&gt; instance backing
 * `UserPreferencesRepository`.
 *
 * The DataStore is bound to the application context with a fixed
 * `name` so its file path is stable across process restarts:
 * `/data/data/<package>/files/datastore/user_preferences.preferences_pb`.
 *
 * `preferencesDataStore` is a property delegate that lazily creates
 * the DataStore on first access; calling it from a Hilt `@Provides`
 * function is fine because Hilt only invokes the provider once per
 * singleton scope.
 */
private val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences",
)

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideUserPreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.userPreferencesDataStore
}
