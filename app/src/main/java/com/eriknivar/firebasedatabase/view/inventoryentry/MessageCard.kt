package com.eriknivar.firebasedatabase.view.inventoryentry

import android.app.DatePickerDialog
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.eriknivar.firebasedatabase.view.storagetype.DataFields
import com.eriknivar.firebasedatabase.viewmodel.UserViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import java.util.Locale
import com.google.firebase.Timestamp
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import registrarAuditoriaConteo
import coil.request.ImageRequest
import coil.compose.AsyncImage


@Composable
fun MessageCard(
    item: DataFields,
    firestore: FirebaseFirestore,
    allData: MutableList<DataFields>,
    onSuccess: () -> Unit,
    listState: LazyListState,
    index: Int,
    expandedStates: MutableMap<String, Boolean>,
    userViewModel: UserViewModel


) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    var confirmDeletion by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var showImageDialog by remember { mutableStateOf(false) }


    var editedLocation by remember { mutableStateOf(item.location) }
    var editedLote by remember { mutableStateOf(item.lote) }
    var editedExpirationDate by remember { mutableStateOf(item.expirationDate) }
    var editedQuantity by remember { mutableStateOf(item.quantity.toString()) }

    val sdf = remember { SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()) }
    val fechaFormateada = item.fechaRegistro?.toDate()?.let { sdf.format(it) } ?: "Sin fecha"

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(confirmDeletion) {
        if (confirmDeletion) {
            deleteFromFirestore(firestore, item.documentId, allData) {
                confirmDeletion = false
            }
        }
    }

    if (showDialog) {
        AlertDialog(onDismissRequest = { showDialog = true },
            title = { Text("Confirmar eliminación") },
            text = { Text("¿Estás seguro de que deseas borrar este registro?") },
            confirmButton = {
                Button(onClick = {
                    // ✅ Registrar la auditoría antes de borrar
                    val valoresAntes = mapOf(
                        "ubicacion" to item.location,
                        "sku" to item.sku,
                        "lote" to item.lote,
                        "fecha_vencimiento" to item.expirationDate,
                        "cantidad" to item.quantity.toString(),
                        "unidad_medida" to item.unidadMedida,
                        "descripcion" to item.description
                    )

                    registrarAuditoriaConteo(
                        registroId = item.documentId,
                        tipoAccion = "Eliminación",
                        usuario = userViewModel.nombre.value ?: "Desconocido",
                        valoresAntes = valoresAntes
                    )

                    // 🗑️ Borrar
                    showDialog = false
                    deleteFromFirestore(firestore, item.documentId, allData) {
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

    val isExpanded = expandedStates[item.documentId] ?: false
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "RotationAnimation"
    )

    // ✅ Hacemos scroll SOLO si expanded pasa a true
    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            // Le damos un pequeño delay para que Compose reacomode y sea más suave
            delay(200)
            listState.animateScrollToItem(index)
        }
    }

    val backgroundColorCard = if (isExpanded) Color(0xFFE3F2FD) else Color.White
    //val borderColor = if (isExpanded) Color(0xFF2196F3) else Color.Transparent

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .border(
                width = 2.dp,
                color = if (isExpanded) Color(0xFF2196F3) else Color.Transparent,
                shape = RoundedCornerShape(12.dp) // 🔵 Bordes suaves tipo SAP Fiori
            )
            .clickable {
                expandedStates[item.documentId] = !(expandedStates[item.documentId] ?: false)
            },
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColorCard)

    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ⬆️ Mostrar fecha y descripción en la parte superior
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(
                        text = item.description,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.Blue,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = fechaFormateada,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.Black
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        if (!isEditing) { // ✅ Solo mostramos ExpandMore si NO estamos editando
                            Icon(
                                imageVector = Icons.Default.ExpandMore,
                                contentDescription = "Expandir/Contraer",
                                modifier = Modifier
                                    .size(24.dp)
                                    .graphicsLayer {
                                        rotationZ = rotationAngle
                                    },
                                tint = Color.Blue
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            AnimatedVisibility(visible = isExpanded) {
                Column { // ✅ Debe empezar con Column, no con Row
                    // Datos
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Row {
                                Text("Ubicación: ", fontSize = 13.sp, color = Color.Blue)
                                Text(item.location, fontSize = 13.sp, color = Color.Black)
                            }
                            Row {
                                Text("SKU: ", fontSize = 13.sp, color = Color.Blue)
                                Text(item.sku, fontSize = 13.sp, color = Color.Black)
                            }
                            Row {
                                Text("Lote: ", fontSize = 13.sp, color = Color.Blue)
                                Text(item.lote, fontSize = 13.sp, color = Color.Black)
                            }
                            Row {
                                Text("Fecha Vencimiento: ", fontSize = 13.sp, color = Color.Blue)
                                Text(item.expirationDate, fontSize = 13.sp, color = Color.Black)
                            }
                            Row {
                                Text("Cantidad: ", fontSize = 13.sp, color = Color.Blue)
                                Text(
                                    item.quantity.toString(),
                                    fontSize = 13.sp,
                                    color = Color.Black
                                )
                            }
                            Row {
                                Text("Unidad de Medida: ", fontSize = 13.sp, color = Color.Blue)
                                Text(item.unidadMedida, fontSize = 13.sp, color = Color.Black)
                            }

                            // 🟦 Mostrar enlace a foto si existe
                            if (!item.fotoUrl.isNullOrBlank()) {

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Text("Foto: ", fontSize = 13.sp, color = Color.Blue)
                                    Text(
                                        text = "VER",
                                        color = Color.Black,
                                        textDecoration = TextDecoration.Underline,
                                        modifier = Modifier.clickable {
                                            Log.d("FotoDebug", "🟢 VER presionado en Reporte: ${item.fotoUrl}")
                                            showImageDialog = true
                                        }
                                    )
                                }

                                if (showImageDialog) {
                                    AlertDialog(
                                        onDismissRequest = { showImageDialog = false },
                                        confirmButton = {
                                            TextButton(onClick = { showImageDialog = false }) {
                                                Text("Cerrar", color = Color(0xFF003366))
                                            }
                                        },
                                        title = {
                                            Text(
                                                text = "📷 Imagen Asociada",
                                                fontWeight = FontWeight.Bold
                                            )
                                        },
                                        text = {
                                            Log.d("FotoDebug", "📷 Mostrando imagen en Reporte desde URL: ${item.fotoUrl}")
                                            AsyncImage(
                                                model = item.fotoUrl.trim(),
                                                contentDescription = "Imagen asociada",
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(300.dp)
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        // ICONOS A LA DERECHA
                        Column(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .align(Alignment.CenterVertically),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.End
                        ) {
                            if (!isEditing) {
                                IconButton(onClick = { isEditing = true }) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Editar",
                                        tint = Color.Blue
                                    )
                                }
                                IconButton(onClick = { showDialog = true }) {
                                    Icon(
                                        Icons.Default.DeleteForever,
                                        contentDescription = "Eliminar",
                                        tint = Color.Red
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ⬇️ Resto del contenido dividido en dos columnas
            Row(modifier = Modifier.fillMaxWidth()) {

                Column(modifier = Modifier.weight(1f)) {
                    if (isEditing) {
                        AlertDialog(onDismissRequest = {
                            isEditing = false
                            editedLocation = item.location
                            editedLote = item.lote
                            editedExpirationDate = item.expirationDate
                            editedQuantity = item.quantity.toString()
                        }, title = {
                            Text(
                                "Editar Registro", fontWeight = FontWeight.Bold
                            )
                        }, text = {
                            Column {
                                OutlinedTextField(
                                    value = editedLocation,
                                    onValueChange = { editedLocation = it.uppercase().trim() },
                                    label = { Text("Editar Ubicación") },
                                    enabled = true,
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                SnackbarHost(hostState = snackbarHostState)

                                OutlinedTextField(
                                    value = editedLote,
                                    onValueChange = { editedLote = it.uppercase().trim() },
                                    label = { Text("Editar Lote") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(value = editedExpirationDate,
                                    onValueChange = { editedExpirationDate = it },
                                    label = { Text("Editar Fecha Vencimiento") },
                                    readOnly = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    trailingIcon = {
                                        IconButton(onClick = { datePickerDialog.show() }) {
                                            Icon(
                                                Icons.Default.CalendarMonth,
                                                contentDescription = "Seleccionar fecha"
                                            )
                                        }
                                    })
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = editedQuantity,
                                    onValueChange = { editedQuantity = it },
                                    label = { Text("Editar Cantidad") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }, confirmButton = {
                            Button(
                                onClick = {
                                    if (editedLocation.isBlank() || editedLote.isBlank() || editedExpirationDate.isBlank() || editedQuantity.isBlank()) {
                                        Toast.makeText(
                                            context,
                                            "Todos los campos deben estar completos",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        return@Button
                                    }

                                    Firebase.firestore.collection("ubicaciones")
                                        .whereEqualTo(
                                            "codigo_ubi",
                                            editedLocation.trim().uppercase()
                                        )
                                        .get()
                                        .addOnSuccessListener { documents ->
                                            val ubicacionExiste = documents.any()

                                            if (!ubicacionExiste) {

                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar("La ubicación no existe en la base de datos")
                                                }

                                                return@addOnSuccessListener // ✅ Detiene el flujo aquí
                                            }

                                            val valoresAntes = mapOf(
                                                "ubicacion" to item.location,
                                                "lote" to item.lote,
                                                "fechaVencimiento" to item.expirationDate,
                                                "cantidad" to item.quantity.toString()
                                            )

                                            val valoresDespues = mapOf(
                                                "ubicacion" to editedLocation,
                                                "lote" to editedLote,
                                                "fechaVencimiento" to editedExpirationDate,
                                                "cantidad" to editedQuantity
                                            )

                                            val huboCambios = valoresAntes != valoresDespues

                                            if (huboCambios) {
                                                registrarAuditoriaConteo(
                                                    registroId = item.documentId,
                                                    tipoAccion = "Modificación",
                                                    usuario = userViewModel.nombre.value
                                                        ?: "Desconocido",
                                                    valoresAntes = valoresAntes,
                                                    valoresDespues = valoresDespues
                                                )
                                            }

                                            updateFirestore(
                                                firestore,
                                                item.documentId,
                                                editedLocation,
                                                item.sku,
                                                editedLote,
                                                editedExpirationDate,
                                                editedQuantity.toDoubleOrNull() ?: item.quantity,
                                                allData,
                                                onSuccess = onSuccess
                                            )

                                            isEditing = false
                                        }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF003366), contentColor = Color.White
                                )
                            ) {
                                Text("Guardar")
                            }

                        }, dismissButton = {
                            Button(
                                onClick = {
                                    isEditing = false
                                    editedLocation = item.location
                                    editedLote = item.lote
                                    editedExpirationDate = item.expirationDate
                                    editedQuantity = item.quantity.toString()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xD8692121), contentColor = Color.White
                                )
                            ) {
                                Text("Cancelar")
                            }
                        })
                    }
                }
            }
        }
    }
}


