package com.velithorne.innerway.memory

import androidx.room.TypeConverter

class MemoryTypeConverters {

    @TypeConverter
    fun fromKind(value: MemoryKind): String = value.name

    @TypeConverter
    fun toKind(value: String): MemoryKind = runCatching { MemoryKind.valueOf(value) }
        .getOrDefault(MemoryKind.GENERAL)
}
