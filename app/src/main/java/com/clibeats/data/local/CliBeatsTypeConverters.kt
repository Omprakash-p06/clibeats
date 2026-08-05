package com.clibeats.data.local

import androidx.room.TypeConverter

class CliBeatsTypeConverters {

    @TypeConverter
    fun fromStringList(value: List<String>): String = value.joinToString(",")

    @TypeConverter
    fun toStringList(value: String): List<String> =
        if (value.isBlank()) emptyList() else value.split(",")
}
