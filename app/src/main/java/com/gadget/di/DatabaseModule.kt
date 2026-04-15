package com.gadget.di

import android.content.Context
import androidx.room.Room
import com.gadget.data.db.GadgetDatabase
import com.gadget.data.db.MetricDao
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
        ).build()
    }

    @Provides
    fun provideMetricDao(db: GadgetDatabase): MetricDao = db.metricDao()
}
