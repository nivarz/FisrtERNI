package com.eriknivar.firebasedatabase.view.inventoryentry

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eriknivar.firebasedatabase.view.storagetype.DataFields
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import java.util.Locale

@Composable
fun MessageCard(
    documentId: String,
    location: String,
    sku: String,
    lote: String,
    expirationDate: String,
    quantity: Double,
    db: FirebaseFirestore,
    allData: MutableList<DataFields>
) {
    var isEditing by remember { mutableStateOf(false) }
    var editedLocation by remember { mutableStateOf(location) }
    var editedSku by remember { mutableStateOf(sku) }
    var editedLot by remember { mutableStateOf(lote) }
    var editedExpirationDate by remember { mutableStateOf(expirationDate) }
    var editedQuantity by remember { mutableStateOf(quantity.toString()) }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    var showDialog by remember { mutableStateOf(false) }
    var confirmDeletion by remember { mutableStateOf(false) }


    if (showDialog) {
        AlertDialog(onDismissRequest = { showDialog = true },
            title = { Text("Confirmar eliminación") },
            text = { Text("¿Estás seguro de que deseas borrar este registro?") },
            confirmButton = {
                Button(onClick = {
                    showDialog = false
                    deleteFromFirestore(db, documentId, allData) {
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
            })
    }

    LaunchedEffect(confirmDeletion) {
        if (confirmDeletion) {
            deleteFromFirestore(db, documentId, allData) {
                confirmDeletion = false // Se asegura de que el estado vuelva a false
            }
        }
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

    // 📌 FUNCION PARA LOS CARDS

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {

        Column(modifier = Modifier.padding(8.dp)) {

            if (isEditing) {
        OutlinedTextField(
            value = editedLocation,
            onValueChange = { editedLocation = it },
            singleLine = true,
            label = { Text("Editar Ubicacion", fontWeight = FontWeight.Bold) },
            modifier = Modifier.fillMaxWidth()

        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = editedSku,
            onValueChange = { editedSku = it },
            singleLine = true,
            label = { Text("Editar Codigo de Producto", fontWeight = FontWeight.Bold) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = editedLot,
            onValueChange = { editedLot = it },
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
            label = { Text("Editar Cantidad", fontWeight = FontWeight.Bold) },
            modifier = Modifier.fillMaxWidth()
        )

    } else {
        Spacer(modifier = Modifier.height(4.dp))
        Row {
            Text(
                text = "Ubicacion: ",
                fontWeight = FontWeight.Bold,
                color = Color.Blue
            )
            Text(
                text = location, modifier = Modifier.fillMaxWidth(),
                color = Color.Black, fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row {
            Text(
                text = "Cod. de Producto: ",
                fontWeight = FontWeight.Bold,
                color = Color.Blue
            )
            Text(
                text = sku, modifier = Modifier.fillMaxWidth(),
                color = Color.Black, fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row {
            Text(
                text = "Lote: ",
                fontWeight = FontWeight.Bold,
                color = Color.Blue
            )
            Text(
                text = lote, modifier = Modifier.fillMaxWidth(),
                color = Color.Black, fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row {
            Text(
                text = "Fecha de Caducidad: ",
                fontWeight = FontWeight.Bold,
                color = Color.Blue
            )
            Text(
                text = expirationDate, modifier = Modifier.fillMaxWidth(),
                color = Color.Black, fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row {
            Text(
                text = "Cantidad: ",
                fontWeight = FontWeight.Bold,
                color = Color.Blue
            )
            Text(
                text = quantity.toString(), modifier = Modifier.fillMaxWidth(),
                color = Color.Black, fontWeight = FontWeight.Bold
            )
        }

    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        if (isEditing) {
            Button(modifier = Modifier.padding(4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Blue),
                onClick = {
                    updateFirestore(
                        db,
                        documentId,
                        editedLocation,
                        editedSku,
                        editedLot,
                        editedExpirationDate,
                        editedQuantity.toDoubleOrNull() ?: 0.0,
                        allData
                    )
                    isEditing = false
                }) {
                Text("Guardar")
            }
            Button(modifier = Modifier.padding(4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                onClick = {
                    isEditing = false
                    editedLocation = location
                    editedSku = sku
                    editedLot = lote
                    editedExpirationDate = expirationDate
                    editedQuantity = quantity.toString()
                }) {
                Text("Cancelar")
            }
        } else {
            IconButton(onClick = { isEditing = true }) {
                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color.Blue)
            }
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(onClick = {
                showDialog = true
            })
            {
                Icon(
                    Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.Red
                )
            }
        }
    }
}
}
}