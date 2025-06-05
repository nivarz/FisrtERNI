package com.eriknivar.firebasedatabase.view.inventoryreports

import android.content.Context
import com.eriknivar.firebasedatabase.view.storagetype.DataFields
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun exportToExcel(context: Context, data: List<DataFields>): File? {
    return try {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Reporte Inventario")

        // Crear estilos para encabezados
        val headerStyle = workbook.createCellStyle().apply {
            val font = workbook.createFont().apply {
                bold = true
                fontHeightInPoints = 12
                color = IndexedColors.WHITE.index
            }
            setFont(font)
            fillForegroundColor = IndexedColors.DARK_BLUE.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            alignment = HorizontalAlignment.CENTER
        }

        val redFontStyle = workbook.createCellStyle().apply {
            val redFont = workbook.createFont().apply {
                color = IndexedColors.RED.index
            }
            setFont(redFont)
        }

        val boldStyle = workbook.createCellStyle().apply {
            val font = workbook.createFont().apply {
                bold = true
            }
            setFont(font)
        }

        // Crear encabezado
        val headerRow = sheet.createRow(0)
        val headers = listOf("Ubicacion", "SKU", "Descripción", "Lote", "F.Vencimiento", "Cantidad", "U.Medida", "Usuario", "F.Registro", "Localidad", "Foto")
        headers.forEachIndexed { index, title ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(title)
            cell.cellStyle = headerStyle
        }

        // Fijar fila de encabezado
        sheet.createFreezePane(0, 1)

        // Fecha actual para validación
        val currentDate = LocalDate.now()
        val inputFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val outputFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")

        var totalCantidad = 0.0

        // Agregar los datos
        data.forEachIndexed { rowIndex, item ->
            val row = sheet.createRow(rowIndex + 1)
            row.createCell(0).setCellValue(item.location)
            row.createCell(1).setCellValue(item.sku)
            row.createCell(2).setCellValue(item.description)
            row.createCell(3).setCellValue(item.lote)

            val cellVencimiento = row.createCell(4)
            cellVencimiento.setCellValue(item.expirationDate)

            try {
                val vencimiento = LocalDate.parse(item.expirationDate, inputFormatter)
                if (vencimiento.isBefore(currentDate)) {
                    cellVencimiento.cellStyle = redFontStyle
                }
            } catch (_: Exception) {}

            row.createCell(5).setCellValue(item.quantity)
            totalCantidad += item.quantity
            row.createCell(6).setCellValue(item.unidadMedida)
            row.createCell(7).setCellValue(item.usuario)

            val fechaFormateada = item.fechaRegistro?.toDate()?.let {
                outputFormatter.format(it.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime())
            } ?: ""
            row.createCell(8).setCellValue(fechaFormateada)

            row.createCell(9).setCellValue(item.localidad)
            row.createCell(10).setCellValue(item.fotoUrl ?: "")

        }

        // Fila de resumen
        val summaryRow = sheet.createRow(data.size + 2)
        summaryRow.createCell(0).apply {
            setCellValue("Total de registros:")
            cellStyle = boldStyle
        }
        summaryRow.createCell(1).setCellValue(data.size.toDouble())

        summaryRow.createCell(4).apply {
            setCellValue("Suma total cantidades:")
            cellStyle = boldStyle
        }
        summaryRow.createCell(5).setCellValue(totalCantidad)

        // Fila con fecha y hora de generación del reporte
        val timestampRow = sheet.createRow(data.size + 4)
        timestampRow.createCell(0).apply {
            setCellValue("Generado el:")
            cellStyle = boldStyle
        }
        timestampRow.createCell(1).setCellValue(
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
        )

        // Ajustar ancho de columnas
        for (i in headers.indices) {
            sheet.setColumnWidth(i, 6000)
        }

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


