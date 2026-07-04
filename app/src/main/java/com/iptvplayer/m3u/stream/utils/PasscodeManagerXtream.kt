package com.iptvplayer.m3u.stream.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PasscodeManagerXtream @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("passcode_xtream_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PASSCODE = "key_xtream_passcode"
    }

    fun hasPasscode(): Boolean = prefs.getString(KEY_PASSCODE, null) != null

    fun getPasscode(): String? = prefs.getString(KEY_PASSCODE, null)

    fun savePasscode(passcode: String) {
        Log.d("check edit", "initData: called)")

        prefs.edit().putString(KEY_PASSCODE, passcode).apply()
    }

    fun clearPasscode() {
        prefs.edit().remove(KEY_PASSCODE).apply()
    }
}