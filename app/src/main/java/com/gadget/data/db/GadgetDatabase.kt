package com.gadget.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [MetricReading::class, MetricSession::class],
    version = 1,
    exportSchema = true,
)
abstract class GadgetDatabase : RoomDatabase() {
    abstract fun metricDao(): MetricDao
}
