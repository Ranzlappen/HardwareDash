package com.gadget.di

import android.content.Context
import androidx.room.Room
import com.gadget.data.db.GadgetDatabase
import com.gadget.data.db.MIGRATION_1_2
import com.gadget.data.db.MIGRATION_2_3
import com.gadget.data.db.MIGRATION_3_4
import com.gadget.data.db.MetricDao
import com.gadget.data.db.apps.AppsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): GadgetDatabase {
        return Room.databaseBuilder(
            context,
            GadgetDatabase::class.java,
            "gadget_db"
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .build()
    }

    @Provides
    fun provideMetricDao(db: GadgetDatabase): MetricDao = db.metricDao()

    @Provides
    fun provideAppsDao(db: GadgetDatabase): AppsDao = db.appsDao()
}
