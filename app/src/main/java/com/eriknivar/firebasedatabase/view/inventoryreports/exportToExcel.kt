package com.eriknivar.firebasedatabase.view.inventoryreports

import android.content.Context
import com.eriknivar.firebasedatabase.view.storagetype.DataFields
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream

fun exportToExcel(context: Context, data: List<DataFields>): File? {
    return try {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Reporte Inventario")

        // Crear encabezado
        val headerRow = sheet.createRow(0)
        val headers = listOf("Ubicacion", "SKU", "Descripción", "Lote", "F.Vencimiento", "Cantidad", "U.Medida", "Usuario", "F.Registro", "Localidad")
        headers.forEachIndexed { index, title ->
            headerRow.createCell(index).setCellValue(title)
        }

        // Agregar los datos
        data.forEachIndexed { rowIndex, item ->
            val row = sheet.createRow(rowIndex + 1)
            row.createCell(0).setCellValue(item.location)
            row.createCell(1).setCellValue(item.sku)
            row.createCell(2).setCellValue(item.description)
            row.createCell(3).setCellValue(item.lote)
            row.createCell(4).setCellValue(item.expirationDate)
            row.createCell(5).setCellValue(item.quantity)
            row.createCell(6).setCellValue(item.unidadMedida)
            row.createCell(7).setCellValue(item.usuario)
            row.createCell(8).setCellValue(item.fechaRegistro?.toDate().toString())
            row.createCell(9).setCellValue(item.localidad)
        }

        // Guardar archivo
        val fileName = "Reporte_de_inventario.xlsx"
        val file = File(context.cacheDir, fileName)
        val outputStream = FileOutputStream(file)
        workbook.write(outputStream)
        outputStream.close()
        workbook.close()

        file
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
