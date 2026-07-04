package com.iptvplayer.m3u.stream.utils

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PasscodeManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("passcode_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PASSCODE = "key_passcode"
    }

    fun hasPasscode(): Boolean = prefs.getString(KEY_PASSCODE, null) != null

    fun getPasscode(): String? = prefs.getString(KEY_PASSCODE, null)

    fun savePasscode(passcode: String) {
        prefs.edit().putString(KEY_PASSCODE, passcode).apply()
    }

    fun clearPasscode() {
        prefs.edit().remove(KEY_PASSCODE).apply()
    }
}