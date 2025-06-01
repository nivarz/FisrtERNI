package com.eriknivar.firebasedatabase.view.utility

import android.content.Context

object SessionUtils {

    private const val PREFS_NAME = "session_prefs"
    private const val KEY_LAST_INTERACTION = "lastInteractionTime"

    fun guardarUltimaInteraccion(context: Context, time: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_INTERACTION, time).apply()
    }

    fun obtenerUltimaInteraccion(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_LAST_INTERACTION, System.currentTimeMillis())
    }
}
