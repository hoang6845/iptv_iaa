package com.iptvplayer.m3u.stream.model.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.iptvplayer.m3u.stream.model.entity.Channel
import com.iptvplayer.m3u.stream.model.entity.SyncState

class Converter {

    @TypeConverter
    fun fromSyncState(state: SyncState): String {
        return state.name
    }

    @TypeConverter
    fun toSyncState(value: String): SyncState {
        return SyncState.valueOf(value)
    }

    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        if (value.isNullOrEmpty()) return emptyList()
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }


    @TypeConverter
    fun fromChannels(list: List<Channel>): String {
        return gson.toJson(list)
    }

    @TypeConverter
    fun toChannels(json: String): List<Channel> {
        val type = object : TypeToken<List<Channel>>() {}.type
        return gson.fromJson(json, type)
    }
}