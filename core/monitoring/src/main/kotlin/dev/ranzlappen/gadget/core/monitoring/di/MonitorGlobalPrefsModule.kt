package dev.ranzlappen.gadget.core.monitoring.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.ranzlappen.gadget.core.monitoring.MonitorGlobalDataStore
import javax.inject.Singleton

private val Context.monitorGlobalDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "monitor_global",
)

@Module
@InstallIn(SingletonComponent::class)
object MonitorGlobalPrefsModule {

    @Provides
    @Singleton
    @MonitorGlobalDataStore
    fun provideMonitorGlobalDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.monitorGlobalDataStore
}
