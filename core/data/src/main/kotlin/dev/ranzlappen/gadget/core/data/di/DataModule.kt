package dev.ranzlappen.gadget.core.data.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.ranzlappen.gadget.core.data.MonitorSampleDao
import dev.ranzlappen.gadget.core.data.MonitoringDatabase
import dev.ranzlappen.gadget.core.data.apps.AppsDao
import dev.ranzlappen.gadget.core.data.apps.AppsDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideMonitoringDatabase(
        @ApplicationContext context: Context,
    ): MonitoringDatabase = Room.databaseBuilder(
        context,
        MonitoringDatabase::class.java,
        "monitoring.db",
    ).build()

    @Provides
    fun provideMonitorSampleDao(database: MonitoringDatabase): MonitorSampleDao =
        database.monitorSampleDao()

    @Provides
    @Singleton
    fun provideAppsDatabase(
        @ApplicationContext context: Context,
    ): AppsDatabase = Room.databaseBuilder(
        context,
        AppsDatabase::class.java,
        "apps.db",
    ).build()

    @Provides
    fun provideAppsDao(database: AppsDatabase): AppsDao = database.appsDao()
}
