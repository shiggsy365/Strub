package com.example.stremiompvplayer.models

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromStringList(value: String?): List<String>? {
        val listType = object : TypeToken<List<String>>() {}.type
        return value?.let { Gson().fromJson(it, listType) }
    }

    @TypeConverter
    fun toStringList(list: List<String>?): String? {
        return Gson().toJson(list)
    }

    @TypeConverter
    fun fromAnyList(value: String?): List<Any>? {
        val listType = object : TypeToken<List<Any>>() {}.type
        return value?.let { Gson().fromJson(it, listType) }
    }

    @TypeConverter
    fun toAnyList(list: List<Any>?): String? {
        return Gson().toJson(list)
    }
}