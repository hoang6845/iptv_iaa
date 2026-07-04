package com.iptvplayer.m3u.stream.model.entity

import androidx.room.Entity
import androidx.room.Fts4

@Fts4(tokenizer = "unicode61")
@Entity(tableName = "search_fts")
data class SearchFtsEntity(
    val name: String,
    val type: String,
    val uniqueId: String
)