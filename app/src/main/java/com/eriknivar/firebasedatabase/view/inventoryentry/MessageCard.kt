package com.eriknivar.firebasedatabase.view.inventoryentry

import android.app.DatePickerDialog
import android.icu.text.SimpleDateFormat
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
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.eriknivar.firebasedatabase.data.UbicacionesRepo
import com.eriknivar.firebasedatabase.view.utility.auditoria.registrarAuditoriaConteo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import com.eriknivar.firebasedatabase.view.utility.normalizeUbi
import com.eriknivar.firebasedatabase.view.utility.validarUbicacionEnMaestro
import com.eriknivar.firebasedatabase.view.common.ConteoMode

@Composable
fun MessageCard(
    item: DataFields,
    firestore: FirebaseFirestore,
    allData: MutableList<DataFields>,
    onSuccess: () -> Unit,
    listState: LazyListState,
    index: Int,
    expandedStates: MutableMap<String, Boolean>,
    userViewModel: UserViewModel,
    conteoMode: ConteoMode
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    var showDialog by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var showImageDialog by remember { mutableStateOf(false) }

    var editedLocation by remember(item.documentId, item.location) { mutableStateOf(item.location) }
    var editedLote by remember(item.documentId, item.lote) { mutableStateOf(item.lote) }
    var editedExpirationDate by remember(
        item.documentId,
        item.expirationForUi
    ) { mutableStateOf(item.expirationForUi.ifBlank { "-" }) }

    var editedQuantity by remember(
        item.documentId, item.quantity
    ) { mutableStateOf(item.quantity.toString()) }

    LaunchedEffect(item.documentId) {
        editedExpirationDate = item.expirationForUi.ifBlank { "-" }
    }

    // Estados para el campo de Ubicación en el diálogo de edición
    val editedLocationState = remember { mutableStateOf(editedLocation.uppercase()) }
    val showErrorLocation = remember { mutableStateOf(false) }
    val showUbicacionNoExisteDialog = remember { mutableStateOf(false) }
    val focusLoc = remember { FocusRequester() }
    val focusNext =
        remember { FocusRequester() } // para saltar al siguiente campo si la validación pasa
    val keyboard = LocalSoftwareKeyboardController.current

    // --- Flags de permisos/modo para edición de Lote/Vencimiento ---
    val isInv = (userViewModel.tipo.value ?: "")
        .equals("invitado", ignoreCase = true)
    // Evita dependencia directa del enum: comparamos por nombre/string
    val isConLote = item.lote.trim() != "-"
    val canEditLoteYVenc = (conteoMode == ConteoMode.CON_LOTE)

    // Para evitar reescrituras en recomposición
    var dialogInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(isEditing, item.documentId) {
        if (isEditing && !dialogInitialized) {
            // Carga los valores actuales del registro
            editedLocationState.value = item.location.trim().uppercase()
            editedLote = item.lote.trim().uppercase()

            editedExpirationDate = item.expirationForUi.ifBlank { "-" }

            editedQuantity = item.quantity.toString()

            dialogInitialized = true
        }
    }

    LaunchedEffect(canEditLoteYVenc) {
        if (!canEditLoteYVenc) editedExpirationDate = "-"    // SIN_LOTE -> “-”
    }


    LaunchedEffect(item.documentId, item.location, item.lote, item.expirationDate, item.quantity) {
        // ✅ No tocar campos si el usuario ya está editando (evita “rebote”)
        if (!isEditing) {
            editedLocation = item.location
            editedLote = item.lote
            editedExpirationDate = item.expirationForUi.ifBlank { "-" }
            editedQuantity = item.quantity.toString()
            editedLocationState.value = item.location.trim().uppercase()
            showErrorLocation.value = false
        }
    }

    // 🔐 1) Resolver datos base
    val cid = userViewModel.clienteId.value.orEmpty().trim().uppercase()
    val rol = (userViewModel.tipo.value ?: "").lowercase()
    val isInvitado = (rol == "invitado")

    val sdf = remember { SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()) }
    val fechaFormateada = item.fechaRegistro?.toDate()?.let { sdf.format(it) } ?: "Sin fecha"

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Confirmar eliminación") },
            text = { Text("¿Estás seguro de que deseas borrar este registro?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val cidLocal = (userViewModel.clienteId.value ?: "").trim().uppercase()
                        val rolLocal = (userViewModel.tipo.value ?: "").lowercase()
                        val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

                        val docRef = Firebase.firestore.collection("clientes").document(cidLocal)
                            .collection("inventario").document(item.documentId)

                        // 1) Leer el doc para validar (app-side)
                        docRef.get().addOnSuccessListener { snap ->
                            val creador =
                                snap.getString("usuarioUid") ?: snap.getString("creadoPorUid")
                                ?: snap.getString("createdByUid") ?: ""

                            val tsCreado = listOf(
                                "creadoEn", "createdAt", "fechaCliente", "fechaRegistro", "fecha"
                            ).firstNotNullOfOrNull { snap.getTimestamp(it) }

                            fun sameDayInZone(
                                a: java.util.Date, b: java.util.Date, zoneId: java.time.ZoneId
                            ): Boolean {
                                val la = a.toInstant().atZone(zoneId).toLocalDate()
                                val lb = b.toInstant().atZone(zoneId).toLocalDate()
                                return la == lb
                            }

                            val zone = java.time.ZoneId.of("America/Santo_Domingo")
                            val ahora = com.google.firebase.Timestamp.now().toDate()

                            val clienteDelDoc = snap.getString("clienteId") ?: cidLocal

                            val puedeBorrar = when (rolLocal) {
                                "superuser" -> true
                                "admin" -> clienteDelDoc.equals(cidLocal, ignoreCase = true)
                                "invitado" -> creador == uid && tsCreado != null && sameDayInZone(
                                    tsCreado.toDate(), ahora, zone
                                )

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

                            // 2) Borrar
                            docRef.delete()
                                .addOnSuccessListener {
                                    // === Resolver nombre + email del usuario que elimina ===
                                    val auth = FirebaseAuth.getInstance().currentUser
                                    val usuarioUid =
                                        userViewModel.documentId.value ?: auth?.uid ?: ""
                                    val emailAuth = auth?.email

                                    Firebase.firestore.collection("usuarios").document(usuarioUid)
                                        .get()
                                        .addOnSuccessListener { udoc ->
                                            val nombreDoc =
                                                udoc.getString("nombre")?.takeIf { it.isNotBlank() }
                                            val emailDoc =
                                                udoc.getString("email")?.takeIf { it.isNotBlank() }

                                            val usuarioNombreFinal =
                                                nombreDoc ?: auth?.displayName
                                                ?: (emailAuth ?: emailDoc)?.substringBefore("@")
                                                ?: usuarioUid

                                            val usuarioEmailFinal = emailAuth ?: emailDoc

                                            registrarAuditoriaConteo(
                                                clienteId = cidLocal,              // tu cliente (en mayúsculas)
                                                registroId = item.documentId,       // id del doc borrado
                                                tipoAccion = "eliminar",
                                                usuarioNombre = usuarioNombreFinal,
                                                usuarioUid = usuarioUid,
                                                valoresAntes = mapOf(
                                                    "ubicacion" to item.location,
                                                    "lote" to item.lote,
                                                    "fechaVencimiento" to item.expirationDate,
                                                    "cantidad" to item.quantity
                                                ),
                                                valoresDespues = emptyMap(),
                                                usuarioEmail = usuarioEmailFinal
                                            )

                                            Toast.makeText(
                                                context,
                                                "Registro eliminado",
                                                Toast.LENGTH_SHORT
                                            )
                                                .show()
                                            showDialog = false
                                        }
                                }.addOnFailureListener { e ->
                                    Log.e("DeleteInv", "❌ Error al borrar", e)
                                    Toast.makeText(
                                        context, "No se pudo borrar (permisos).", Toast.LENGTH_LONG
                                    ).show()
                                }
                        }.addOnFailureListener { e ->
                            Log.e("DeleteInv", "❌ No se pudo leer doc antes de borrar", e)
                            Toast.makeText(
                                context, "No se pudo verificar permisos.", Toast.LENGTH_LONG
                            ).show()
                        }
                    }) { Text("Sí", color = Color(0xFF003366), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDialog = false }) {
                    Text(
                        "No",
                        color = Color(0xFF003366),
                        fontWeight = FontWeight.Bold
                    )
                }
            })
    }


    val isExpanded = expandedStates[item.documentId] ?: false
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f, label = "RotationAnimation"
    )

    // ✅ Hacemos scroll SOLO si expanded pasa a true
    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            delay(200)
            listState.animateScrollToItem(index)
        }
    }
    val backgroundColorCard = if (isExpanded) Color(0xFFE3F2FD) else Color.White


    // saber si la fecha vino del date picker
    var justPicked by remember { mutableStateOf(false) }

    // (opcional) normaliza la fecha inicial del item: si vino "yyyy-MM-dd" -> "dd/MM/yyyy"
    LaunchedEffect(isEditing, item.documentId) {
        if (isEditing) {
            val raw = item.expirationDate.trim()
            editedExpirationDate = when {
                raw.isBlank() || raw == "-" -> ""
                raw.matches(Regex("""^\d{2}/\d{2}/\d{4}$""")) -> raw
                raw.matches(Regex("""^\d{4}-\d{2}-\d{2}$""")) -> {
                    val y = raw.substring(0, 4)
                    val m = raw.substring(5, 7)
                    val d = raw.substring(8, 10)
                    "$d/$m/$y"
                }

                else -> raw // por si ya viene en otro formato conocido por ti
            }
            justPicked = false
        }
    }

    // construye el DatePicker con fecha inicial
    val cal = Calendar.getInstance().apply {
        val r = editedExpirationDate
        if (r.matches(Regex("""^\d{2}/\d{2}/\d{4}$"""))) {
            val d = r.substring(0, 2).toInt()
            val m = r.substring(3, 5).toInt() - 1
            val y = r.substring(6, 10).toInt()
            set(y, m, d)
        }
    }

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            editedExpirationDate = "%02d/%02d/%04d".format(dayOfMonth, month + 1, year)
            justPicked = true
        },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH)
    )

    // si el campo está deshabilitado por modo, fuerza "-"
    LaunchedEffect(canEditLoteYVenc) { if (!canEditLoteYVenc) editedExpirationDate = "-" }

    // si borran o queda vacío, quita el flag
    LaunchedEffect(editedExpirationDate) {
        if (editedExpirationDate.isBlank() || editedExpirationDate == "-") justPicked = false
    }

    val iconAlpha = if (canEditLoteYVenc) 1f else 0.3f


    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .border(
                width = 2.dp,
                color = if (isExpanded) Color(0xFF2196F3) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
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
                verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
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
                Column {
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
                                Text(item.expirationForUi, fontSize = 13.sp, color = Color.Black)
                            }
                            Row {
                                Text("Cantidad: ", fontSize = 13.sp, color = Color.Blue)
                                Text(
                                    item.quantity.toString(), fontSize = 13.sp, color = Color.Black
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
                                        })
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
                                        })
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
                                IconButton(onClick = {
                                    isEditing = true
                                    // ✅ Inicializa el campo una sola vez al abrir el diálogo
                                    editedLocationState.value = item.location.trim().uppercase()
                                    showErrorLocation.value = false
                                }) {
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
                            editedExpirationDate = item.expirationForUi.ifBlank { "-" }
                            editedQuantity = item.quantity.toString()
                            editedLocationState.value = item.location.uppercase()
                            showErrorLocation.value = false
                        }, title = {
                            Text("Editar Registro", fontWeight = FontWeight.Bold)
                        }, text = {
                            Column {
                                // === Ubicación (con validación al perder foco y con IME Next) ===
                                OutlinedTextField(
                                    value = editedLocationState.value,
                                    onValueChange = {
                                        editedLocationState.value = it.trim().uppercase()
                                        showErrorLocation.value = false
                                    },
                                    label = { Text("Editar Ubicación") },
                                    singleLine = true,
                                    isError = showErrorLocation.value && (editedLocationState.value.isBlank() || editedLocationState.value == "UBICACIÓN NO EXISTE"),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(focusLoc),
                                    // ✅ No dispares validaciones al cambiar de foco ni al presionar Next
                                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions.Default
                                )


                                Spacer(modifier = Modifier.height(8.dp))
                                SnackbarHost(hostState = snackbarHostState)

                                // === Lote ===
                                OutlinedTextField(
                                    value = editedLote,
                                    onValueChange = { editedLote = it.uppercase().trim() },
                                    label = { Text("Editar Lote") },
                                    enabled = canEditLoteYVenc,
                                    readOnly = !canEditLoteYVenc,
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // === Fecha de Vencimiento ===
                                OutlinedTextField(
                                    value = editedExpirationDate.ifBlank { "-" },        // ← no uses ifBlank aquí; ya viene “-”
                                    onValueChange = { editedExpirationDate = it },
                                    label = { Text("Editar Fecha Vencimiento") },
                                    enabled = canEditLoteYVenc,
                                    readOnly = !canEditLoteYVenc,
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    trailingIcon = {
                                        IconButton(
                                            onClick = { if (canEditLoteYVenc) datePickerDialog.show() },
                                            enabled = canEditLoteYVenc,
                                            modifier = Modifier.alpha(if (canEditLoteYVenc) 1f else 0.3f)
                                        ) {
                                            Icon(
                                                Icons.Default.CalendarMonth,
                                                contentDescription = "Seleccionar fecha"
                                            )
                                        }
                                    }
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // === Cantidad ===
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
                            // ⚠️ Antes el enabled dependía de 'nuevaUbi' declarada más abajo → crash.
                            // Ahora usamos directamente el valor del estado ya disponible.
                            Button(
                                enabled = editedLocationState.value.trim()
                                    .isNotBlank() && !showErrorLocation.value, onClick = {
                                    // === 0) Datos base ===
                                    val rolLocal = (userViewModel.tipo.value ?: "").lowercase()
                                    val isInv = (rolLocal == "invitado")
                                    val cidLocal =
                                        (userViewModel.clienteId.value ?: "").trim().uppercase()

                                    if (cidLocal.isBlank()) {
                                        Toast.makeText(
                                            context,
                                            "Falta clienteId (no se puede guardar).",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        return@Button
                                    }

                                    // ✅ Normalizaciones y detección de cambios (ambos lados)
                                    val originalUbi = item.location.trim().uppercase()
                                    val nuevaUbi = editedLocationState.value.trim().uppercase()


                                    val qtyParsed =
                                        editedQuantity.replace(",", ".").toDoubleOrNull()
                                    val qty = qtyParsed ?: item.quantity

                                    val locationChanged = (nuevaUbi != originalUbi)
                                    val quantityChanged = (qty != item.quantity)
                                    val loteChanged =
                                        (editedLote.trim().uppercase() != (item.lote.trim()
                                            .uppercase()))
                                    val fechaChanged =
                                        (editedExpirationDate.trim() != item.expirationDate.trim())

                                    // === 1) Validaciones ===
                                    if (isInv) {
                                        if (!locationChanged && !quantityChanged) {
                                            Toast.makeText(
                                                context,
                                                "No hay cambios para guardar.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            return@Button
                                        }
                                    } else {
                                        if (nuevaUbi.isBlank() || editedQuantity.isBlank()) {
                                            Toast.makeText(
                                                context,
                                                "Todos los campos deben estar completos",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            return@Button
                                        }
                                    }

                                    // === 2) Auditoría (capturas ANTES y DESPUÉS) ===
                                    val valoresAntes: Map<String, Any?> = mapOf(
                                        "ubicacion" to item.location,
                                        "lote" to item.lote,
                                        "fechaVencimiento" to item.expirationDate,
                                        "cantidad" to item.quantity
                                    )
                                    val valoresDespues: Map<String, Any?> = mapOf(
                                        "ubicacion" to nuevaUbi,
                                        "lote" to editedLote.trim().ifBlank { "-" }.uppercase(),
                                        "fechaVencimiento" to editedExpirationDate.trim()
                                            .ifBlank { "-" },
                                        "cantidad" to qty
                                    )
                                    val cambiosClaves =
                                        valoresAntes.keys.filter { k -> valoresAntes[k] != valoresDespues[k] }
                                    val auditAntes = valoresAntes.filterKeys { it in cambiosClaves }
                                    val auditDespues =
                                        valoresDespues.filterKeys { it in cambiosClaves }
                                    val huboCambios = cambiosClaves.isNotEmpty()

                                    // === 3) Update Firestore (según rol) ===
                                    fun continuarConUpdate() {
                                        val docRef = Firebase.firestore.collection("clientes")
                                            .document(cidLocal).collection("inventario")
                                            .document(item.documentId)

                                        // ✅ Siempre enviamos la nueva ubicación (aunque sea igual) para evitar “no-op”
                                        if (isInv) {
                                            val updatesInvitado = mutableMapOf<String, Any>(
                                                "ubicacion" to nuevaUbi  // <<— siempre
                                            )
                                            if (quantityChanged) updatesInvitado["cantidad"] = qty

                                            Log.d(
                                                "EditarInv", "Invitado UPDATE -> $updatesInvitado"
                                            )
                                            docRef.update(updatesInvitado).addOnSuccessListener {
                                                // === 4) Optimistic UI ===
                                                val idx =
                                                    allData.indexOfFirst { it.documentId == item.documentId }
                                                if (idx >= 0) {
                                                    val updatedItem = item.copy(
                                                        quantity = if (quantityChanged) qty else item.quantity,
                                                        location = nuevaUbi,     // <<— forzamos nueva ubicación en UI
                                                        lote = item.lote,        // invitados no cambian
                                                        expirationDate = item.expirationDate
                                                    )
                                                    allData[idx] = updatedItem
                                                }

                                                // === 5) Auditoría (solo si hubo cambios reales) ===
                                                val huboCambios =
                                                    (locationChanged || quantityChanged)
                                                if (huboCambios) {
                                                    val auth =
                                                        FirebaseAuth.getInstance().currentUser
                                                    val usuarioUid =
                                                        userViewModel.documentId.value ?: auth?.uid
                                                        ?: ""
                                                    val emailAuth = auth?.email

                                                    Firebase.firestore.collection("usuarios")
                                                        .document(usuarioUid).get()
                                                        .addOnSuccessListener { udoc ->
                                                            val nombreDoc = udoc.getString("nombre")
                                                                ?.takeIf { it.isNotBlank() }
                                                            val emailDoc = udoc.getString("email")
                                                                ?.takeIf { it.isNotBlank() }
                                                            val usuarioNombreFinal =
                                                                nombreDoc ?: auth?.displayName
                                                                ?: (emailAuth
                                                                    ?: emailDoc)?.substringBefore(
                                                                    "@"
                                                                ) ?: usuarioUid
                                                            val usuarioEmailFinal =
                                                                emailAuth ?: emailDoc

                                                            registrarAuditoriaConteo(
                                                                clienteId = cidLocal,
                                                                registroId = item.documentId,
                                                                tipoAccion = "editar",
                                                                usuarioNombre = usuarioNombreFinal,
                                                                usuarioUid = usuarioUid,
                                                                valoresAntes = mapOf(
                                                                    "ubicacion" to item.location,
                                                                    "cantidad" to item.quantity
                                                                ),
                                                                valoresDespues = mapOf(
                                                                    "ubicacion" to nuevaUbi,
                                                                    "cantidad" to (if (quantityChanged) qty else item.quantity)
                                                                ),
                                                                usuarioEmail = usuarioEmailFinal
                                                            )
                                                        }
                                                }

                                                // === 6) Feedback y cierre
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        "Registro modificado exitoso"
                                                    )
                                                }
                                                isEditing = false
                                                onSuccess()
                                            }.addOnFailureListener { e ->
                                                Log.e(
                                                    "EditarInv", "❌ Error update (invitado)", e
                                                )
                                                Toast.makeText(
                                                    context,
                                                    "No se pudo guardar (permisos).",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }

                                        } else {
                                            // admin / superuser: siempre enviamos ubicacion + demás campos
                                            val updatesFull =
                                                mutableMapOf<String, Any>(
                                                    "ubicacion" to nuevaUbi,  // <<— siempre
                                                    "cantidad" to qty,
                                                    "lote" to editedLote.trim().ifBlank { "-" }
                                                        .uppercase(),
                                                    "fechaVencimiento" to editedExpirationDate.trim(),
                                                    "updatedAt" to FieldValue.serverTimestamp(),
                                                    "updatedBy" to (userViewModel.documentId.value
                                                        ?: ""))

                                            Log.d(
                                                "EditarInv", "Admin/SU UPDATE -> $updatesFull"
                                            )
                                            docRef.update(updatesFull).addOnSuccessListener {
                                                // === 4) Optimistic UI ===
                                                val idx =
                                                    allData.indexOfFirst { it.documentId == item.documentId }
                                                if (idx >= 0) {
                                                    val updatedItem = item.copy(
                                                        quantity = qty,
                                                        location = nuevaUbi,  // <<— forzamos nueva ubicación en UI
                                                        lote = editedLote.trim().ifBlank { "-" }
                                                            .uppercase(),
                                                        expirationDate = editedExpirationDate.trim())
                                                    allData[idx] = updatedItem
                                                }

                                                // === 5) Auditoría (solo si hubo cambios reales)
                                                val huboCambios =
                                                    (locationChanged || quantityChanged || loteChanged || fechaChanged)
                                                if (huboCambios) {
                                                    val auth =
                                                        FirebaseAuth.getInstance().currentUser
                                                    val usuarioUid =
                                                        userViewModel.documentId.value ?: auth?.uid
                                                        ?: ""
                                                    val emailAuth = auth?.email

                                                    Firebase.firestore.collection("usuarios")
                                                        .document(usuarioUid).get()
                                                        .addOnSuccessListener { udoc ->
                                                            val nombreDoc = udoc.getString("nombre")
                                                                ?.takeIf { it.isNotBlank() }
                                                            val emailDoc = udoc.getString("email")
                                                                ?.takeIf { it.isNotBlank() }
                                                            val usuarioNombreFinal =
                                                                nombreDoc ?: auth?.displayName
                                                                ?: (emailAuth
                                                                    ?: emailDoc)?.substringBefore(
                                                                    "@"
                                                                ) ?: usuarioUid
                                                            val usuarioEmailFinal =
                                                                emailAuth ?: emailDoc

                                                            registrarAuditoriaConteo(
                                                                clienteId = cidLocal,
                                                                registroId = item.documentId,
                                                                tipoAccion = "editar",
                                                                usuarioNombre = usuarioNombreFinal,
                                                                usuarioUid = usuarioUid,
                                                                valoresAntes = mapOf(
                                                                    "ubicacion" to item.location,
                                                                    "lote" to item.lote,
                                                                    "fechaVencimiento" to item.expirationDate,
                                                                    "cantidad" to item.quantity
                                                                ),
                                                                valoresDespues = mapOf(
                                                                    "ubicacion" to nuevaUbi,
                                                                    "lote" to editedLote.trim()
                                                                        .ifBlank { "-" }
                                                                        .uppercase(),
                                                                    "fechaVencimiento" to editedExpirationDate.trim(),
                                                                    "cantidad" to qty),
                                                                usuarioEmail = usuarioEmailFinal)
                                                        }
                                                }

                                                // === 6) Feedback y cierre
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        "Registro modificado exitoso"
                                                    )
                                                }
                                                isEditing = false
                                                onSuccess()
                                            }.addOnFailureListener { e ->
                                                Log.e(
                                                    "EditarInv", "❌ Error update (admin/SU)", e
                                                )
                                                Toast.makeText(
                                                    context,
                                                    "No se pudo guardar (permisos).",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                    }
                                    // === 2.5) Validación en app (helper compartido) ===
                                    val locCodigo = item.localidad.trim().uppercase().orEmpty()

                                    if (!locationChanged) {
                                        // No cambió la ubicación → no validar maestro, guardar directo
                                        continuarConUpdate()
                                    } else {
                                        // Sí cambió → validar en maestro (nueva ruta + fallback legacy)
                                        val ubiNormalizada = normalizeUbi(nuevaUbi)
                                        validarUbicacionEnMaestro(
                                            clienteId = cidLocal,
                                            localidadCodigo = locCodigo,
                                            codigoUbi = ubiNormalizada,
                                            onResult = { existe ->
                                                if (existe) {
                                                    // Asegura que guardamos la versión normalizada
                                                    editedLocationState.value = ubiNormalizada
                                                    continuarConUpdate()
                                                } else {
                                                    showErrorLocation.value = true
                                                    showUbicacionNoExisteDialog.value = true
                                                }
                                            },
                                            onError = { e ->
                                                // Red / permisos: informa y no guardes
                                                showErrorLocation.value = true
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar("No se pudo validar ubicación (${e.message ?: "error"}).")
                                                }
                                            }
                                        )
                                    }

                                }, colors = ButtonDefaults.buttonColors(
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
                                    editedExpirationDate = item.expirationForUi.ifBlank { "-" }
                                    editedQuantity = item.quantity.toString()
                                    editedLocationState.value = item.location.uppercase()
                                    showErrorLocation.value = false
                                }, colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xD8692121), contentColor = Color.White
                                )
                            ) {
                                Text("Cancelar")
                            }
                        })
                    }
                }

                if (showUbicacionNoExisteDialog.value) {
                    AlertDialog(
                        onDismissRequest = { showUbicacionNoExisteDialog.value = false },
                        title = { Text("Ubicación inválida") },
                        text = { Text("La ubicación ingresada no existe en el maestro.") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showUbicacionNoExisteDialog.value = false
                                    focusLoc.requestFocus()
                                }) { Text("Aceptar") }
                        })
                }
            }
        }
    }
}
