package com.eriknivar.firebasedatabase.view.utility

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.material3.SnackbarHostState
import androidx.core.content.FileProvider
import com.google.firebase.Firebase
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale

fun exportarAuditoriaAExcel(
    context: Context,
    snackbarHostState: SnackbarHostState,
    coroutineScope: CoroutineScope,
    onFinish: () -> Unit
)
{

    val firestore = Firebase.firestore
    firestore.collection("auditoria_conteos")
        .orderBy("fecha", Query.Direction.DESCENDING)
        .get()
        .addOnSuccessListener { result ->

            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("Auditoría")

            // Encabezados
            val header = sheet.createRow(0)
            listOf("Acción", "Usuario", "Fecha", "Campo", "Valor Antes", "Valor Después").forEachIndexed { index, title ->
                header.createCell(index).setCellValue(title)
            }

            var rowIndex = 1

            for (doc in result) {
                val tipo = doc.getString("tipo_accion") ?: ""
                val usuario = doc.getString("usuario") ?: ""
                val fecha = (doc.getTimestamp("fecha")?.toDate())
                    ?.let { SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(it) } ?: ""
                val antes = doc.get("valores_antes") as? Map<*, *>
                val despues = doc.get("valores_despues") as? Map<*, *>

                val campos = (antes?.keys ?: emptySet()) + (despues?.keys ?: emptySet())

                for (campo in campos.distinct()) {
                    val row = sheet.createRow(rowIndex++)
                    row.createCell(0).setCellValue(tipo)
                    row.createCell(1).setCellValue(usuario)
                    row.createCell(2).setCellValue(fecha)
                    row.createCell(3).setCellValue(campo.toString())
                    row.createCell(4).setCellValue(antes?.get(campo)?.toString() ?: "-")
                    row.createCell(5).setCellValue(despues?.get(campo)?.toString() ?: "-")
                }
            }

            val file = File(context.cacheDir, "auditoria_conteos.xlsx")
            FileOutputStream(file).use { workbook.write(it) }

            // Compartir archivo
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(intent, "Compartir Excel"))
            onFinish()
        }
        .addOnFailureListener {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Error al exportar auditoría")
            }
            onFinish()
        }
}
