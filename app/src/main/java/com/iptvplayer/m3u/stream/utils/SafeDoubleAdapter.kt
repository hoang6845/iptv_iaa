package com.iptvplayer.m3u.stream.utils

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter

class SafeDoubleAdapter : TypeAdapter<Double?>() {
    override fun write(out: JsonWriter, value: Double?) {
        if (value == null) out.nullValue() else out.value(value)
    }

    override fun read(reader: JsonReader): Double? {
        return when (reader.peek()) {
            JsonToken.NULL -> {
                reader.nextNull()
                null
            }
            JsonToken.STRING -> {
                val s = reader.nextString()
                if (s.isBlank()) null else s.toDoubleOrNull()
            }
            JsonToken.NUMBER -> reader.nextDouble()
            else -> {
                reader.skipValue()
                null
            }
        }
    }
}