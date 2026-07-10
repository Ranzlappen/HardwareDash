package dev.ranzlappen.gadget.core.data.logbook

import androidx.room.TypeConverter

/**
 * Room [androidx.room.TypeConverters] for [LogbookDatabase] — Room has no
 * built-in enum column support, so [LogbookTagColor] rides through as its
 * stable [Enum.name]. First `@TypeConverter` use in `:core:data`; every
 * other entity so far only uses Room-native column types.
 */
object LogbookConverters {
    @TypeConverter
    @JvmStatic
    fun fromTagColor(value: LogbookTagColor): String = value.name

    @TypeConverter
    @JvmStatic
    fun toTagColor(value: String): LogbookTagColor =
        runCatching { LogbookTagColor.valueOf(value) }.getOrDefault(LogbookTagColor.None)
}
