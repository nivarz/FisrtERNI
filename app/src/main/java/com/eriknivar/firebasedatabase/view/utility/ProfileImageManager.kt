package com.eriknivar.firebasedatabase.view.utility

import android.content.Context

class ProfileImageManager(context: Context) {
    private val prefs = context.getSharedPreferences("profile_prefs", Context.MODE_PRIVATE)

    fun saveImageUri(username: String, uri: String) {
        prefs.edit().putString("profile_image_uri_$username", uri).apply()
    }

    fun getImageUri(username: String): String? {
        return prefs.getString("profile_image_uri_$username", null)
    }

}

