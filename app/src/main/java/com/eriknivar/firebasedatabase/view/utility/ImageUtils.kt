package com.eriknivar.firebasedatabase.view.utility


import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore

object ImageUtils {
    /**
     * Crea un Uri de foto en MediaStore (no requiere FileProvider).
     * Devuelve un content:// listo para usar con TakePicture().
     */
    fun createTempImageUri(context: Context): Uri {
        val name = "erni_${System.currentTimeMillis()}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }
        return requireNotNull(
            context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            )
        )
    }
}

