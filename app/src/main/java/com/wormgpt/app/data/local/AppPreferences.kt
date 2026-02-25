package com.wormgpt.app.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

private const val PREFS_NAME = "wormgpt_prefs"
private const val KEY_DEEPSEEK_API_KEY = "deepseek_api_key"

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getDeepSeekApiKey(): String? = prefs.getString(KEY_DEEPSEEK_API_KEY, null)?.takeIf { it.isNotBlank() }

    fun setDeepSeekApiKey(key: String?) {
        prefs.edit { putString(KEY_DEEPSEEK_API_KEY, key?.trim()?.ifEmpty { null }) }
    }
}
