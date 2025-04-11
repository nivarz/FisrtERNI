package com.eriknivar.firebasedatabase.view.inventoryreports

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eriknivar.firebasedatabase.view.storagetype.DataFields
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun InventoryReportItem(
    item: DataFields,
    tipoUsuario: String,
    onDelete: (String) -> Unit,
    onEdit: (DataFields) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    val sdf = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val fechaFormateada = item.fechaRegistro?.toDate()?.let { sdf.format(it) } ?: "Sin fecha"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 8.dp)
            .clickable { expanded = !expanded },
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = fechaFormateada,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = item.description,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.Blue
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                InfoRow("Ubicación:", item.location)
                InfoRow("Sku:", item.sku)
                InfoRow("Lote:", item.lote)
                InfoRow("Fecha Vencimiento:", item.expirationDate)
                InfoRow("Cantidad:", "${item.quantity}")
                InfoRow("Unidad de medida:", item.unidadMedida)
                InfoRow("Usuario:", item.usuario)
                InfoRow("Localidad:", item.localidad)

                if (tipoUsuario == "admin") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { showEditDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                        ) {
                            Text("Editar")
                        }
                        Button(
                            onClick = { showDeleteDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                        ) {
                            Text("Eliminar")
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirmar eliminación") },
            text = { Text("¿Estás seguro de que deseas eliminar este registro?") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(item.documentId)
                    showDeleteDialog = false
                }) {
                    Text("Sí")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("No")
                }
            }
        )
    }

    if (showEditDialog) {
        var lote by remember { mutableStateOf(item.lote) }
        var cantidad by remember { mutableStateOf(item.quantity.toString()) }
        var fechaVencimiento by remember { mutableStateOf(item.expirationDate) }
        var ubicacion by remember { mutableStateOf(item.location) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Editar Registro") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = lote, onValueChange = { lote = it }, label = { Text("Lote") })
                    OutlinedTextField(value = cantidad, onValueChange = { cantidad = it }, label = { Text("Cantidad") })
                    OutlinedTextField(value = fechaVencimiento, onValueChange = { fechaVencimiento = it }, label = { Text("Fecha Vencimiento") })
                    OutlinedTextField(value = ubicacion, onValueChange = { ubicacion = it }, label = { Text("Ubicación") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val actualizado = item.copy(
                        lote = lote,
                        quantity = cantidad.toDoubleOrNull() ?: item.quantity,
                        expirationDate = fechaVencimiento,
                        location = ubicacion
                    )
                    onEdit(actualizado)
                    showEditDialog = false
                }) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}










