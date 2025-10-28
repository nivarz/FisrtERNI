package com.eriknivar.firebasedatabase.view.inventoryreports

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

fun shareExcelFile(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider", // <- authority correcto (debe coincidir con el Manifest)
        file
    )

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        // Opcional: asunto y texto
        putExtra(Intent.EXTRA_SUBJECT, "Reporte de Inventario")
        putExtra(Intent.EXTRA_TEXT, "Adjunto reporte en formato Excel.")
        // Algunos receptores leen ClipData
        clipData = android.content.ClipData.newUri(context.contentResolver, "xlsx", uri)
    }

    // Concede permiso de lectura a las apps destino (extra robusto)
    val resInfoList = context.packageManager.queryIntentActivities(intent, 0)
    for (ri in resInfoList) {
        context.grantUriPermission(
            ri.activityInfo.packageName,
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    }

    context.startActivity(Intent.createChooser(intent, "Compartir archivo Excel"))
}

