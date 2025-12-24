package com.eriknivar.firebasedatabase.view.utility

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException

object ImageUtils {

    /**
     * Crea un Uri temporal para usar con TakePicture().
     *
     * - En Android 10+ usa MediaStore (scoped storage, sin permisos extra).
     * - En Android 9- usa un archivo en externalFilesDir + FileProvider.
     */
    fun createTempImageUri(context: Context): Uri {
        val name = "erni_${System.currentTimeMillis()}.jpg"

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // ✅ ANDROID 10+
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                // Carpeta lógica en la galería
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/ERNI"
                )
            }

            requireNotNull(
                context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values
                )
            ) {
                "No se pudo crear Uri en MediaStore"
            }
        } else {
            // ✅ ANDROID 9 O MENOS → sin WRITE_EXTERNAL_STORAGE
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                ?: context.filesDir

            val imageFile = File(dir, name)
            try {
                if (!imageFile.exists()) {
                    imageFile.createNewFile()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }

            // Asegúrate de tener configurado el provider:
            // authority = "${applicationId}.fileprovider"
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile
            )
        }
    }
}