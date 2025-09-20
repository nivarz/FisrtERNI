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
import com.eriknivar.firebasedatabase.data.UbicacionesRepo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue


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

    var editedLocation by remember(item.documentId, item.location) { mutableStateOf(item.location) }
    var editedLote by remember(item.documentId, item.lote) { mutableStateOf(item.lote) }
    var editedExpirationDate by remember(
        item.documentId,
        item.expirationDate
    ) { mutableStateOf(item.expirationDate) }
    var editedQuantity by remember(
        item.documentId,
        item.quantity
    ) { mutableStateOf(item.quantity.toString()) }

    LaunchedEffect(item.documentId, item.location, item.lote, item.expirationDate, item.quantity) {
        editedLocation = item.location
        editedLote = item.lote
        editedExpirationDate = item.expirationDate
        editedQuantity = item.quantity.toString()
    }

    // 🔐 1) Resolver datos base
    val cid = userViewModel.clienteId.value.orEmpty().trim().uppercase()
    val rol = (userViewModel.tipo.value ?: "").lowercase()

    val sdf = remember { SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()) }
    val fechaFormateada = item.fechaRegistro?.toDate()?.let { sdf.format(it) } ?: "Sin fecha"

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()


    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = true },
            title = { Text("Confirmar eliminación") },
            text = { Text("¿Estás seguro de que deseas borrar este registro?") },
            confirmButton = {

                Button(
                    onClick = {
                        val cid = (userViewModel.clienteId.value ?: "").trim().uppercase()
                        val rol = (userViewModel.tipo.value ?: "").lowercase()
                        val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

                        val docRef = Firebase.firestore
                            .collection("clientes").document(cid)
                            .collection("inventario").document(item.documentId)

                        // 1) Leer el doc para validar permisos a nivel de app (evita intentos fallidos)
                        docRef.get()
                            .addOnSuccessListener { snap ->
                                val creador =
                                    snap.getString("creadoPorUid") ?: snap.getString("createdByUid")
                                val tsCreado =
                                    snap.getTimestamp("creadoEn")
                                        ?: snap.getTimestamp("createdAt")
                                        ?: snap.getTimestamp("fechaRegistro")
                                        ?: snap.getTimestamp("fecha")

                                fun sameDay(a: java.util.Date, b: java.util.Date): Boolean {
                                    val ca = java.util.Calendar.getInstance().apply { time = a }
                                    val cb = java.util.Calendar.getInstance().apply { time = b }
                                    return ca.get(java.util.Calendar.YEAR) == cb.get(java.util.Calendar.YEAR) &&
                                            ca.get(java.util.Calendar.DAY_OF_YEAR) == cb.get(java.util.Calendar.DAY_OF_YEAR)
                                }

                                val now = com.google.firebase.Timestamp.now().toDate()
                                val rol = (userViewModel.tipo.value ?: "").lowercase()
                                val cid = (userViewModel.clienteId.value ?: "").trim().uppercase()
                                val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
                                val clienteDelDoc = snap.getString("clienteId") ?: cid

                                val puedeBorrar = when {
                                    rol == "superuser" -> true
                                    rol == "admin" -> clienteDelDoc.equals(cid, ignoreCase = true)
                                    rol == "invitado" -> (creador == uid) && (tsCreado != null && sameDay(
                                        tsCreado.toDate(),
                                        now
                                    ))

                                    else -> false
                                }

                                if (!puedeBorrar) {
                                    Toast.makeText(
                                        context,
                                        "Solo puedes borrar registros que creaste hoy.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    return@addOnSuccessListener
                                }

                                // 2) Borrar en la SUBCOLECCIÓN correcta
                                docRef.delete()
                                    .addOnSuccessListener {
                                        // Quitar de la UI (optimistic)
                                        val idx =
                                            allData.indexOfFirst { it.documentId == item.documentId }
                                        if (idx >= 0) allData.removeAt(idx)

                                        // (Opcional) Auditoría
                                        val uidAudit = FirebaseAuth.getInstance().currentUser?.uid
                                        registrarAuditoriaConteo(
                                            clienteId = cid,
                                            registroId = item.documentId,
                                            tipoAccion = "Eliminación",
                                            usuarioNombre = userViewModel.nombre.value
                                                ?: "Desconocido",
                                            usuarioUid = uidAudit,
                                            valoresAntes = mapOf(
                                                "ubicacion" to item.location,
                                                "sku" to item.sku,
                                                "lote" to item.lote,
                                                "fecha_vencimiento" to item.expirationDate,
                                                "cantidad" to item.quantity.toString(),
                                                "unidad_medida" to item.unidadMedida,
                                                "descripcion" to item.description
                                            )
                                        )

                                        showDialog = false
                                        confirmDeletion = false
                                    }


                                    .addOnFailureListener { e ->
                                        Log.e("DeleteInv", "❌ Error al borrar", e)
                                        Toast.makeText(
                                            context,
                                            "No se pudo borrar (permisos).",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                            }
                            .addOnFailureListener { e ->
                                Log.e("DeleteInv", "❌ No se pudo leer doc antes de borrar", e)
                                Toast.makeText(
                                    context,
                                    "No se pudo verificar permisos.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    }


                ) { Text("Sí") }
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
                                            Log.d(
                                                "FotoDebug",
                                                "🟢 VER presionado en Reporte: ${item.fotoUrl}"
                                            )
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
                                            Log.d(
                                                "FotoDebug",
                                                "📷 Mostrando imagen en Reporte desde URL: ${item.fotoUrl}"
                                            )
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

            val rol = (userViewModel.tipo.value ?: "").lowercase()
            val isInvitado = (rol == "invitado")

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
                                    readOnly = false,
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
                                    readOnly = false,
                                    singleLine = isInvitado,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = editedExpirationDate,
                                    onValueChange = { editedExpirationDate = it },
                                    label = { Text("Editar Fecha Vencimiento") },
                                    readOnly = isInvitado,
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
                                    // === 0) Datos base ===
                                    val rol = (userViewModel.tipo.value ?: "").lowercase()
                                    val isInvitado = (rol == "invitado")
                                    val cid =
                                        (userViewModel.clienteId.value ?: "").trim().uppercase()

                                    if (cid.isBlank()) {
                                        Toast.makeText(
                                            context,
                                            "Falta clienteId (no se puede guardar).",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        return@Button
                                    }

                                    // Normalizaciones y detección de cambios
                                    val nuevaUbi = editedLocation.trim().uppercase()
                                    val qtyParsed = editedQuantity.toDoubleOrNull()
                                    val qty = qtyParsed ?: item.quantity

                                    val locationChanged = (nuevaUbi != item.location)
                                    val quantityChanged = (qty != item.quantity)

                                    // === 1) Validaciones ===
                                    if (isInvitado) {
                                        // Invitado: puede cambiar cantidad y/o ubicación. Si no hay cambios, nada que guardar.
                                        if (!locationChanged && !quantityChanged) {
                                            Toast.makeText(
                                                context,
                                                "No hay cambios para guardar.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            return@Button
                                        }
                                        // Invitado: no exigir ubicación si no la cambió; si la cambió, la validamos más abajo.
                                    } else {
                                        // Admin/Superuser: todos los campos requeridos
                                        if (
                                            editedLocation.isBlank() ||
                                            editedLote.isBlank() ||
                                            editedExpirationDate.isBlank() ||
                                            editedQuantity.isBlank()
                                        ) {
                                            Toast.makeText(
                                                context,
                                                "Todos los campos deben estar completos",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            return@Button
                                        }
                                    }

                                    // === 2) Auditoría (para registro)
                                    val valoresAntes = mapOf(
                                        "ubicacion" to item.location,
                                        "lote" to item.lote,
                                        "fechaVencimiento" to item.expirationDate,
                                        "cantidad" to item.quantity.toString()
                                    )
                                    val valoresDespues = mapOf(
                                        "ubicacion" to nuevaUbi,
                                        "lote" to editedLote.trim().ifBlank { "-" }.uppercase(),
                                        "fechaVencimiento" to editedExpirationDate.trim(),
                                        "cantidad" to qty.toString()
                                    )
                                    val huboCambios = valoresAntes != valoresDespues

                                    // === 3) Función que realiza el update (según rol)
                                    fun continuarConUpdate() {
                                        // Subcolección: /clientes/{cid}/inventario/{id}
                                        val docRef = Firebase.firestore
                                            .collection("clientes").document(cid)
                                            .collection("inventario").document(item.documentId)

                                        // Construir updates EXACTOS (ESPAÑOL)
                                        val updatesInvitado = buildMap<String, Any> {
                                            // Invitado: sólo los campos permitidos por reglas
                                            if (quantityChanged) put("cantidad", qty)
                                            if (locationChanged) put("ubicacion", nuevaUbi)
                                            // (no mandar metadatos aquí para no chocar con reglas estrictas)
                                        }

                                        val updatesFull = mapOf(
                                            "cantidad" to qty,
                                            "ubicacion" to nuevaUbi,
                                            "lote" to editedLote.trim().ifBlank { "-" }.uppercase(),
                                            "fechaVencimiento" to editedExpirationDate.trim(),
                                            "updatedAt" to FieldValue.serverTimestamp(),
                                            "updatedBy" to (userViewModel.documentId.value ?: ""),
                                            "clienteId" to cid
                                        )

                                        val task = if (isInvitado) {
                                            if (updatesInvitado.isEmpty()) {
                                                Toast.makeText(
                                                    context,
                                                    "No hay cambios válidos para guardar.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                return
                                            }
                                            docRef.update(updatesInvitado)
                                        } else {
                                            docRef.update(updatesFull)
                                        }

                                        task
                                            .addOnSuccessListener {
                                                // === 4) Optimistic UI: refrescar card localmente
                                                val idx =
                                                    allData.indexOfFirst { it.documentId == item.documentId }
                                                if (idx >= 0) {
                                                    val updatedItem = item.copy(
                                                        quantity = qty,                                       // DataFields usa 'quantity'
                                                        location = if (locationChanged) nuevaUbi else item.location,
                                                        lote = if (isInvitado) item.lote else editedLote.trim()
                                                            .ifBlank { "-" }.uppercase(),
                                                        expirationDate = if (isInvitado) item.expirationDate else editedExpirationDate.trim()
                                                    )
                                                    allData[idx] = updatedItem
                                                }

                                                // (Si ya tienes auditoría, déjala igual aquí)
                                                onSuccess()
                                                isEditing = false
                                            }
                                            .addOnFailureListener { e ->
                                                Log.e("UpdateInv", "❌ Error al actualizar", e)
                                                val msg = if (isInvitado)
                                                    "Como invitado solo puedes editar CANTIDAD y UBICACIÓN el mismo día."
                                                else e.message ?: "No se pudo actualizar"
                                                Toast.makeText(context, msg, Toast.LENGTH_LONG)
                                                    .show()
                                            }
                                    }

                                    // === 4) Validar ubicación SÓLO si cambió (aplica para invitado y admin/superuser)
                                    // dentro del onClick del botón Guardar, donde haces la validación:
                                    if (locationChanged) {
                                        coroutineScope.launch {
                                            val ok = UbicacionesRepo.existeUbicacion(
                                                clienteId = cid,
                                                localidad = item.localidad.ifBlank { /* o pide la localidad seleccionada */ "ALM_REP" },
                                                codigoIngresado = nuevaUbi
                                            )
                                            if (!ok) {
                                                snackbarHostState.showSnackbar("La ubicación no existe en la base de datos")
                                                return@launch
                                            }
                                            continuarConUpdate()
                                        }
                                    } else {
                                        continuarConUpdate()
                                    }


                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF003366),
                                    contentColor = Color.White
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


