package com.iptvplayer.m3u.stream.ui.local

import android.content.Context
import androidx.core.content.edit
import java.util.Calendar

class AppSharePref(private val context: Context) {
    companion object {
        private const val PREF_IS_RATE_APP = "isRateApp"
        private const val PREF_SESSION_SHOW_RATE_APP = "sessionShowRateApp"
        private const val PREF_SESSION_VALID_SHOW_RATE_APP = "sessionValidShowRateApp"
        private const val PREF_LIST_ID_RECENT = "list_id_recent"
        private const val PREF_TIME_NEXT_EPISODE = "time_next_episode"   // + "_$serverId"
        private const val PREF_IS_AUTO_UPDATE = "is_auto_update"         // + "_$serverId"
        private const val PREF_DAILY_WATCH_CHANNEL_DAY = "daily_watch_channel_day"
        private const val PREF_DAILY_WATCH_CHANNEL_COUNT = "daily_watch_channel_count"

        const val MAX_DAILY_FREE_WATCH_CHANNEL = 3
    }

    private fun getTodayKey(): Int {
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.YEAR) * 1000 + calendar.get(Calendar.DAY_OF_YEAR)
    }

    private fun resetWatchChannelCountIfNewDay() {
        val todayKey = getTodayKey()
        val savedDay = sharePref.getInt(PREF_DAILY_WATCH_CHANNEL_DAY, -1)

        if (savedDay != todayKey) {
            sharePref.edit {
                putInt(PREF_DAILY_WATCH_CHANNEL_DAY, todayKey)
                putInt(PREF_DAILY_WATCH_CHANNEL_COUNT, 0)
            }
        }
    }

    fun getTodayWatchChannelCount(): Int {
        resetWatchChannelCountIfNewDay()
        return sharePref.getInt(PREF_DAILY_WATCH_CHANNEL_COUNT, 0)
    }

    fun canWatchChannelToday(): Boolean {
        return getTodayWatchChannelCount() < MAX_DAILY_FREE_WATCH_CHANNEL
    }

    fun increaseWatchChannelCount() {
        resetWatchChannelCountIfNewDay()

        val currentCount = sharePref.getInt(PREF_DAILY_WATCH_CHANNEL_COUNT, 0)

        sharePref.edit {
            putInt(PREF_DAILY_WATCH_CHANNEL_COUNT, currentCount + 1)
        }
    }

    fun tryConsumeWatchChannelToday(): Boolean {
        resetWatchChannelCountIfNewDay()

        val currentCount = sharePref.getInt(PREF_DAILY_WATCH_CHANNEL_COUNT, 0)

        if (currentCount >= MAX_DAILY_FREE_WATCH_CHANNEL) {
            return false
        }

        sharePref.edit {
            putInt(PREF_DAILY_WATCH_CHANNEL_COUNT, currentCount + 1)
        }

        return true
    }

    private val sharePref by lazy {
        context.getSharedPreferences("TrackingSharePref", Context.MODE_PRIVATE)
    }

    fun getTimeNextEpisode(serverId: Int): Long =
        sharePref.getLong("${PREF_TIME_NEXT_EPISODE}_$serverId", 20L)

    fun setTimeNextEpisode(serverId: Int, value: Long) =
        sharePref.edit { putLong("${PREF_TIME_NEXT_EPISODE}_$serverId", value) }

    fun getIsAutoUpdate(serverId: Int): Boolean =
        sharePref.getBoolean("${PREF_IS_AUTO_UPDATE}_$serverId", false)

    fun setIsAutoUpdate(serverId: Int, value: Boolean) =
        sharePref.edit { putBoolean("${PREF_IS_AUTO_UPDATE}_$serverId", value) }


    var listIdRecent: MutableList<String>
        get(){
            val saved =
                sharePref.getString(PREF_LIST_ID_RECENT, "") ?: ""
            return if (saved.isEmpty()) {
                mutableListOf()
            } else {
                saved.split(",")
                    .map { it.toString() }
                    .toMutableList()
            }
        }
        set(value) {
            val join = value.joinToString(",")
            sharePref.edit { putString(PREF_LIST_ID_RECENT, join) }
        }


    var isRateApp: Boolean
        get() = sharePref.getBoolean(PREF_IS_RATE_APP, false)
        set(value) {
            sharePref.edit { putBoolean(PREF_IS_RATE_APP, value)}
        }

    var sessionShowRateApp: Int
        get() = sharePref.getInt(PREF_SESSION_SHOW_RATE_APP, -1)
        set(value) {
            sharePref.edit { putInt(PREF_SESSION_SHOW_RATE_APP, value)}
        }

    var sessionValidShowRateApp: Int
        get() = sharePref.getInt(PREF_SESSION_VALID_SHOW_RATE_APP, -1)
        set(value) {
            sharePref.edit { putInt(PREF_SESSION_VALID_SHOW_RATE_APP, value)}
        }
}
