package dev.ranzlappen.gadget.core.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [MonitorSample::class],
    version = 1,
    exportSchema = true,
)
abstract class MonitoringDatabase : RoomDatabase() {
    abstract fun monitorSampleDao(): MonitorSampleDao
}
