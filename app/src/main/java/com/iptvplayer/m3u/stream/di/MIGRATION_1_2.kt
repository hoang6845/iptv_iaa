package com.iptvplayer.m3u.stream.di

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            ALTER TABLE RecentChannel 
            ADD COLUMN `group` TEXT
        """.trimIndent())
    }
}