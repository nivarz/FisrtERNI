package com.eriknivar.firebasedatabase.view.inventoryentry

import android.app.DatePickerDialog
import android.icu.text.SimpleDateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eriknivar.firebasedatabase.view.storagetype.DataFields
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import java.util.Locale
import com.google.firebase.Timestamp

@Composable
fun MessageCard(
    documentId: String,
    location: String,
    sku: String,
    lote: String,
    expirationDate: String,
    quantity: Double,
    unidadMedida: String,
    firestore: FirebaseFirestore,
    allData: MutableList<DataFields>,
    fechaRegistro: Timestamp? = null,
    descripcion: String
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    var confirmDeletion by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }

    var editedLocation by remember { mutableStateOf(location) }
    var editedLote by remember { mutableStateOf(lote) }
    var editedExpirationDate by remember { mutableStateOf(expirationDate) }
    var editedQuantity by remember { mutableStateOf(quantity.toString()) }

    val sdf = remember { SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()) }
    val fechaFormateada = fechaRegistro?.toDate()?.let { sdf.format(it) } ?: "Sin fecha"

    val backgroundColor = if (isEditing) Color(0xFFFFF3E0) else Color.White

    LaunchedEffect(confirmDeletion) {
        if (confirmDeletion) {
            deleteFromFirestore(firestore, documentId, allData) {
                confirmDeletion = false
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = true },
            title = { Text("Confirmar eliminación") },
            text = { Text("¿Estás seguro de que deseas borrar este registro?") },
            confirmButton = {
                Button(onClick = {
                    showDialog = false
                    deleteFromFirestore(firestore, documentId, allData) {
                        confirmDeletion = false
                    }
                }) {
                    Text("Sí")
                }
            },
            dismissButton = {
                Button(onClick = { showDialog = false }) {
                    Text("No")
                }
            }
        )
    }

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            editedExpirationDate =
                String.format(Locale.US, "%02d/%02d/%04d", dayOfMonth, month + 1, year)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)

    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ⬆️ Mostrar fecha y descripción en la parte superior
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = fechaFormateada,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = descripcion,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.Blue
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ⬇️ Resto del contenido dividido en dos columnas
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    if (isEditing) {
                        // Campos de edición...
                        OutlinedTextField(
                            value = editedLocation,
                            onValueChange = { editedLocation = it },
                            singleLine = true,
                            label = { Text("Editar Ubicación", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = editedLote,
                            onValueChange = { editedLote = it },
                            singleLine = true,
                            label = { Text("Editar Lote", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = editedExpirationDate,
                            onValueChange = { editedExpirationDate = it },
                            label = { Text("Editar Fecha de Vencimiento", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = { datePickerDialog.show() }) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = "Seleccionar Fecha"
                                    )
                                }
                            },
                            readOnly = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = editedQuantity,
                            onValueChange = { editedQuantity = it },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            label = { Text("Editar Cantidad", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(onClick = {
                                updateFirestore(
                                    context,
                                    firestore,
                                    documentId,
                                    editedLocation,
                                    sku,
                                    editedLote,
                                    editedExpirationDate,
                                    editedQuantity.toDoubleOrNull() ?: quantity,
                                    allData
                                )
                                isEditing = false
                            }, colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)) {
                                Text("Guardar")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = {
                                isEditing = false
                                editedLocation = location
                                editedLote = lote
                                editedExpirationDate = expirationDate
                                editedQuantity = quantity.toString()
                            }, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) {
                                Text("Cancelar")
                            }
                        }
                    } else {
                        Row { Text("Ubicación: ", fontSize = 13.sp, color = Color.Blue); Text(location, fontSize = 13.sp , color = Color.Black) }
                        Row { Text("SKU: ", fontSize = 13.sp, color = Color.Blue); Text(sku, fontSize = 13.sp, color = Color.Black) }
                        Row { Text("Lote: ", fontSize = 13.sp, color = Color.Blue); Text(lote, fontSize = 13.sp, color = Color.Black) }
                        Row { Text("Fecha Vencimiento: ", fontSize = 13.sp, color = Color.Blue); Text(expirationDate, fontSize = 13.sp, color = Color.Black) }
                        Row { Text("Cantidad: ", fontSize = 13.sp, color = Color.Blue); Text(quantity.toString(), fontSize = 13.sp, color = Color.Black) }
                        Row { Text("Unidad de Medida: ", fontSize = 13.sp, color = Color.Blue); Text(unidadMedida, fontSize = 13.sp, color = Color.Black) }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.End
                ) {
                    if (!isEditing) {
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color.Blue)
                        }
                        IconButton(onClick = { showDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.Red)
                        }
                    }
                }
            }
        }
    }

}
