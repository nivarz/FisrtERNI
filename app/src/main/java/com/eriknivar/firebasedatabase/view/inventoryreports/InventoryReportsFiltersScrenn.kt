package com.eriknivar.firebasedatabase.view.inventoryreports

import android.app.DatePickerDialog
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eriknivar.firebasedatabase.viewmodel.UserViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import com.google.firebase.firestore.FirebaseFirestore
import com.eriknivar.firebasedatabase.data.Refs
import com.eriknivar.firebasedatabase.data.ReportesRepo
import com.google.firebase.firestore.DocumentSnapshot
import com.eriknivar.firebasedatabase.view.storagetype.DataFields
import com.eriknivar.firebasedatabase.view.utility.auditoria.registrarAuditoriaConteo
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.withContext


private fun DocumentSnapshot.toDataFieldsUi(): DataFields {
    val base = this.toObject(DataFields::class.java) ?: DataFields()

    // Tomamos usuarioNombre si existe; si no, dejamos el que haya en ES
    val usuarioUi = this.getString("usuarioNombre") ?: base.usuario

    // unidadMedida puede venir como "unidad" en algunos docs
    val unidad = base.unidadMedida.ifBlank { (this.getString("unidad") ?: "") }

    return base.copy(
        documentId = this.id,                 // ← imprescindible para editar/eliminar
        // Aliases UI ← campos ES real
        sku = base.codigoProducto,
        description = base.descripcion,
        location = base.ubicacion,
        quantity = base.cantidad,
        expirationDate = base.fechaVencimiento,
        // Overrides extra para cubrir variantes
        usuario = usuarioUi,
        unidadMedida = unidad
    )
}

@Composable
fun InventoryReportFiltersScreen(
    userViewModel: UserViewModel,
    allData: SnapshotStateList<DataFields>,
    tipoUsuario: String,
    onSuccess: () -> Unit,
    puedeModificarRegistro: (String, String) -> Boolean

) {
    val sku = remember { mutableStateOf("") }
    val location = remember { mutableStateOf("") }
    val startDate = remember { mutableStateOf("") }
    val endDate = remember { mutableStateOf("") }
    val filtrosExpandido = remember { mutableStateOf(false) }

    val usuarioFiltro = remember { mutableStateOf("") }
    val auditFilter =
        remember { mutableStateOf("Todos") } // "Todos", "Solo auditados", "Solo no auditados"

    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance() }
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    val listaLocalidades = remember { mutableStateListOf<String>() }
    val localidadSeleccionada = remember { mutableStateOf("") }

    val isLoading = remember { mutableStateOf(false) }

    // --- Carga para Reportes ---
    val firestore = Firebase.firestore
    val cid = userViewModel.clienteId.observeAsState("").value.trim().uppercase()
    val db = Firebase.firestore

    // La lista que la UI pinta
    val filteredData = remember { mutableStateListOf<DataFields>() }
    val cidLocal = (userViewModel.clienteId.value ?: "").trim().uppercase()

    LaunchedEffect(cid) {
        firestore.collection("clientes").document(cid)
            .collection("localidades")
            .get()
            .addOnSuccessListener { result ->
                listaLocalidades.clear()
                listaLocalidades.addAll(result.mapNotNull { it.getString("nombre") })
            }
    }

    LaunchedEffect(tipoUsuario, userViewModel.nombre) {
        if (tipoUsuario != "admin" && tipoUsuario != "superuser") {
            usuarioFiltro.value = userViewModel.nombre.value ?: ""
        }
    }

    val startDatePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, y, m, d ->
                calendar.set(y, m, d)
                startDate.value = dateFormatter.format(calendar.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    val endDatePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, y, m, d ->
                calendar.set(y, m, d)
                endDate.value = dateFormatter.format(calendar.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    var loading by remember { mutableStateOf(false) } // al entrar, nada cargando

    fun recargarFiltrosDelCliente(cid: String) {
        if (cid.isBlank()) return

        // Localidades
        Refs.ubic(db, cid)
            .orderBy("codigo_ubi")
            .get()
            .addOnSuccessListener { snap ->
                //val data = snap.documents.mapNotNull { it.getString("codigo_ubi") }
                // TODO: sustituye por tu estado real de localidades:
                // localidadesOptions.clear(); localidadesOptions.addAll(data)
            }

        // Productos/SKUs
        Refs.prod(db, cid)
            .orderBy("codigo")      // o el campo que uses para listar
            .get()
            .addOnSuccessListener { snap ->
                //val data = snap.documents.mapNotNull { it.getString("codigo") }
                // TODO: sustituye por tu estado real de SKUs:
                // skuOptions.clear(); skuOptions.addAll(data)
            }

        // (Opcional) Usuarios del cliente para filtros por usuario
        db.collection("usuarios")
            .whereEqualTo("clienteId", cid)
            .get()
            .addOnSuccessListener { snap ->
                //val data = snap.documents.mapNotNull { it.getString("nombre") }
                // TODO: sustituye por tu estado real de usuarios:
                // usuariosOptions.clear(); usuariosOptions.addAll(data)
            }
    }

    LaunchedEffect(cid) {
        if (cid.isNotBlank()) {
            recargarFiltrosDelCliente(cid)
        } else {
            // Si quieres, limpia lista cuando no hay cliente
            // localidadesOptions.clear()
            // skuOptions.clear()
            // usuariosOptions.clear()
        }
    }

    // Carga inicial (y cuando cambie el cliente)
   // LaunchedEffect(cid) { cargarReportes() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // 🔄 Overlay encima del contenido
        //LoadingOverlay(visible = loading)

        // 🔽 Encabezado expandible
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { filtrosExpandido.value = !filtrosExpandido.value }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (filtrosExpandido.value) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                contentDescription = null
            )
            Spacer(
                modifier = Modifier
                    .width(8.dp)
            )
            Text(
                text = "Filtros de búsqueda",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }

        // 🧩 Filtros dentro de AnimatedVisibility
        AnimatedVisibility(visible = filtrosExpandido.value) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                val tipo = tipoUsuario.lowercase().trim()

                fun limpiarFiltros() {
                    usuarioFiltro.value = if (tipo == "invitado") {
                        userViewModel.nombre.value ?: ""
                    } else {
                        ""
                    }

                    sku.value = ""
                    location.value = ""
                    startDate.value = ""
                    endDate.value = ""
                    localidadSeleccionada.value = ""
                    auditFilter.value = "Todos"

                    filteredData.clear()
                    filteredData.addAll(
                        allData.sortedByDescending { registro: DataFields ->
                            registro.fechaRegistro?.toDate()
                        }
                    )
                }

                OutlinedTextField(
                    value = usuarioFiltro.value,
                    onValueChange = { usuarioFiltro.value = it },
                    label = { Text("Nombre de Usuario") },
                    singleLine = true,
                    enabled = tipoUsuario.lowercase().trim() in listOf("admin", "superuser"),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = sku.value.uppercase(),
                    onValueChange = { sku.value = it },
                    label = { Text("SKU o palabra clave") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = location.value.uppercase(),
                    onValueChange = { location.value = it },
                    label = { Text("Ubicación") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                LocalidadDropdown(
                    localidadSeleccionada = localidadSeleccionada,
                    listaLocalidades = listaLocalidades
                )

                AuditFilterDropdown(auditFilter = auditFilter)

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startDate.value,
                        onValueChange = {},
                        label = { Text("Desde") },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { startDatePickerDialog.show() }) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .clickable { startDatePickerDialog.show() }
                    )

                    OutlinedTextField(
                        value = endDate.value,
                        onValueChange = {},
                        label = { Text("Hasta") },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { endDatePickerDialog.show() }) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .clickable { endDatePickerDialog.show() }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {

                    val azulMarino = Color(0xFF001F5B)

                    Button(
                        onClick = {

                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            isLoading.value = true

                            // === SÓLO sustituye el bloque buildMap actual por este ===
                            val filtros = buildMap {
                                val tipo = tipoUsuario.lowercase().trim()
                                val usrFinal = if (tipo == "invitado") {
                                    userViewModel.nombre.value.orEmpty()      // fuerza su propio nombre
                                } else {
                                    usuarioFiltro.value.trim()
                                }

                                if (usrFinal.isNotBlank()) {
                                    // Enviar ambas variantes para que el repo/consulta coincida
                                    put("usuarioNombre", usrFinal)
                                    put("usuario", usrFinal)
                                }

                                localidadSeleccionada.value.trim()
                                    .takeIf { it.isNotBlank() }
                                    ?.let { put("localidad", it.uppercase()) }
                            }


                            // ⬇️ Reemplazo de fetchFilteredInventoryFromFirestore(...)
                            val q = ReportesRepo.buildReportQueryForRole(
                                db = Firebase.firestore,
                                clienteId = cid,
                                tipoUsuario = tipoUsuario,
                                uidActual = userViewModel.documentId.value,
                                filters = filtros
                            )

                            q.get()
                                .addOnSuccessListener { snap ->
                                    // 🔎 DIAGNÓSTICO TEMPORAL (ponlo aquí mismo):
                                    val first = snap.documents.firstOrNull()
                                    if (first != null) {
                                        Log.d(
                                            "DBG",
                                            "DocId=${first.id} data=${first.data}"
                                        )
                                        Log.d(
                                            "DBG",
                                            "sku=${first.getString("sku")} | SKU_alt=${
                                                first.getString("SKU")
                                            }"
                                        )
                                        Log.d(
                                            "DBG",
                                            "ubicacion=${first.getString("ubicacion")} | location=${
                                                first.getString("location")
                                            }"
                                        )
                                        Log.d(
                                            "DBG",
                                            "usuario=${first.getString("usuario")} | usuarioUid=${
                                                first.getString("usuarioUid")
                                            }"
                                        )
                                        Log.d(
                                            "DBG",
                                            "cantidad=${first.getDouble("cantidad")} | qty=${
                                                first.getDouble("qty")
                                            }"
                                        )
                                    }
                                    // ⬆️ hasta aquí el diagnóstico

                                    val nuevosDatos =
                                        snap.documents.map { doc -> doc.toDataFieldsUi() }

                                    filteredData.clear()
                                    filteredData.addAll(
                                        nuevosDatos.filter { item ->
                                            val matchesSku =
                                                sku.value.isBlank() ||
                                                        item.sku.contains(sku.value, true) ||
                                                        item.description.contains(sku.value, true)

                                            val matchesLocation =
                                                location.value.isBlank() ||
                                                        item.location.equals(location.value, true)

                                            val dateFormatted =
                                                item.fechaRegistro?.toDate()?.let { sdf.format(it) }
                                                    ?: ""
                                            val matchesDate = try {
                                                (startDate.value.isBlank() || dateFormatted >= startDate.value) &&
                                                        (endDate.value.isBlank() || dateFormatted <= endDate.value)
                                            } catch (_: Exception) {
                                                true
                                            }

                                            val matchesLocalidad =
                                                localidadSeleccionada.value.isBlank() ||
                                                        item.localidad.equals(
                                                            localidadSeleccionada.value,
                                                            ignoreCase = true
                                                        )

                                            val usrFinalUi =
                                                if (tipoUsuario.lowercase().trim() == "invitado")
                                                    userViewModel.nombre.value.orEmpty()
                                                else
                                                    usuarioFiltro.value.trim()

                                            val matchesUsuario =
                                                usrFinalUi.isBlank() || item.usuario.equals(
                                                    usrFinalUi,
                                                    ignoreCase = true
                                                )

                                            val matchesAudit = when (auditFilter.value) {
                                                "Solo auditados" -> item.auditado
                                                "Solo no auditados" -> !item.auditado
                                                else -> true  // "Todos"
                                            }

                                            matchesSku &&
                                                    matchesLocation &&
                                                    matchesDate &&
                                                    matchesLocalidad &&
                                                    matchesUsuario &&
                                                    matchesAudit
                                        }.sortedByDescending { it.fechaRegistro?.toDate() }
                                    )

                                    filtrosExpandido.value = false
                                    isLoading.value = false
                                }
                                .addOnFailureListener {
                                    isLoading.value = false
                                    Toast.makeText(
                                        context,
                                        "Error al consultar Firestore",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = azulMarino,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Aplicar filtros")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    val coroutineScope = rememberCoroutineScope()
                    var isLoadingExport by remember { mutableStateOf(false) }
                    var isExportEnabled by remember { mutableStateOf(true) }

                    ElevatedButton(
                        onClick = {
                            Log.d(
                                "EXPORT_DEBUG",
                                "Botón presionado. Estado isExportEnabled = $isExportEnabled"
                            )
                            if (!isExportEnabled) return@ElevatedButton

                            isExportEnabled = false
                            isLoadingExport = true

                            coroutineScope.launch {
                                Log.d("EXPORT_DEBUG", "Ejecutando exportación y share...")

                                try {
                                    // 1) Exportar en hilo de I/O (evita bloquear la UI)
                                    val file = withContext(kotlinx.coroutines.Dispatchers.IO) {
                                        exportToExcel(context, filteredData)
                                    }

                                    // 2) Compartir en Main
                                    if (file != null) {
                                        shareExcelFile(context, file)
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "No se generó el archivo",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }

                                } catch (e: Throwable) {
                                    // 👇 Ver el error real en Logcat
                                    Log.e(
                                        "EXPORT_ERROR",
                                        "Fallo al exportar/compartir",
                                        e
                                    )
                                    Toast.makeText(
                                        context,
                                        "Error al exportar: ${e.message ?: ""}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } finally {
                                    // Pequeña pausa opcional para UX
                                    delay(300)
                                    isExportEnabled = true
                                    isLoadingExport = false
                                }
                            }
                        },
                        enabled = tipo != "invitado" && isExportEnabled,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = azulMarino,
                            contentColor = Color.White,
                            disabledContainerColor = Color.LightGray,
                            disabledContentColor = Color.DarkGray
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (isLoadingExport) "Exportando..." else "Exportar Excel")

                            if (isLoadingExport) {
                                Spacer(modifier = Modifier.width(8.dp))
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                val azulMarino = Color(0xFF001F5B)

                Button(
                    onClick = { limpiarFiltros() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = azulMarino,
                        contentColor = Color.White
                    )
                ) {
                    Text("Limpiar filtros")
                }
            }
        }

        AnimatedVisibility(
            visible = isLoading.value,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            AlertDialog(
                onDismissRequest = {}, // No se puede cerrar manualmente
                confirmButton = {},
                title = null,
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth()
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF001F5B),
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Cargando...",
                            fontSize = 16.sp,
                            color = Color.Black,
                            fontStyle = FontStyle.Italic
                        )
                    }
                },
                containerColor = Color.Black.copy(alpha = 0.3f), // ✅ Ligero blur
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 4.dp,

                )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 🧾 Total de resultados
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Total de Registros: ${filteredData.size}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when {
            loading -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(6) { ShimmerReportCard() } // 6 esqueletos mientras carga
                }
            }

            filteredData.isEmpty() -> {
                Text("No hay resultados", modifier = Modifier.padding(top = 8.dp))
            }

            else -> {
                LazyColumn {
                    items(filteredData, key = { it.documentId }) { item ->
                        InventoryReportItem(
                            item = item,
                            puedeModificarRegistro = puedeModificarRegistro,
                            tipoUsuarioActual = userViewModel.tipo.value
                                ?: "", // ✅ Aquí el nuevo parámetro
                            nombreUsuarioActual = userViewModel.nombre.value.orEmpty(),  
                            clienteIdActual = userViewModel.clienteId.value.orEmpty(),
                            onDelete = { documentId ->
                                val db = Firebase.firestore
                                val docRef = db.collection("clientes").document(cid)
                                    .collection("inventario").document(documentId)

                                // 1) Leer ANTES para auditar
                                docRef.delete()
                                    .addOnSuccessListener {
                                        // === Resolver nombre + email del usuario que elimina ===
                                        val auth = FirebaseAuth.getInstance().currentUser
                                        val usuarioUid =
                                            userViewModel.documentId.value ?: auth?.uid ?: ""
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
                                                ).show()
                                                filteredData.removeIf { it.documentId == documentId }
                                            }
                                            .addOnFailureListener {
                                                Toast.makeText(
                                                    context,
                                                    "Error al eliminar",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(
                                            context,
                                            "No se pudo leer el registro a eliminar",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            },
                            onEdit = { updatedItem ->
                                updateFirestore(
                                    Firebase.firestore,
                                    cid,
                                    updatedItem.documentId,
                                    updatedItem.location,
                                    updatedItem.sku,
                                    updatedItem.lote,
                                    updatedItem.expirationDate,
                                    updatedItem.quantity,
                                    filteredData,
                                    onSuccess = onSuccess,
                                    usuarioNombre = userViewModel.nombre.value,
                                    usuarioUid = userViewModel.documentId.value
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalidadDropdown(
    localidadSeleccionada: MutableState<String>,
    listaLocalidades: List<String>
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            readOnly = true,
            value = localidadSeleccionada.value,
            onValueChange = { },
            label = { Text("Almacen", color = Color.Black) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded)
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary, // ✅ Igual al resto
                unfocusedBorderColor = Color.Gray,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = Color.Gray,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            listaLocalidades.forEach { localidad ->
                DropdownMenuItem(
                    text = { Text(localidad) },
                    onClick = {
                        localidadSeleccionada.value = localidad
                        expanded = false
                    }
                )
            }
        }
    }
}


fun updateFirestore(
    db: FirebaseFirestore,
    cid: String,
    docId: String,
    location: String,
    sku: String,               // si lo editas, lo auditamos como codigoProducto
    lote: String,
    expirationDate: String?,
    quantity: Double,
    uiList: SnapshotStateList<DataFields>,
    onSuccess: () -> Unit = {},
    usuarioNombre: String? = null,
    usuarioUid: String? = null
) {
    val docRef = db.collection("clientes").document(cid.trim().uppercase())
        .collection("inventario").document(docId)

    // 1) Snapshot ANTES
    docRef.get().addOnSuccessListener { beforeSnap ->
        val antes = beforeSnap.data?.let { mapParaAuditoria(it) } ?: emptyMap()

        // 2) Payload del UPDATE (campos reales ES)
        val payload = hashMapOf<String, Any>(
            "ubicacion" to location.trim().uppercase(),
            "lote" to lote.trim().uppercase(),
            "cantidad" to quantity
        )
        if (sku.isNotBlank()) payload["codigoProducto"] = sku.trim().uppercase()
        if (!expirationDate.isNullOrBlank()) payload["fechaVencimiento"] = expirationDate
        payload["updatedAt"] = Timestamp.now()
        payload["updatedBy"] = (usuarioUid ?: "")

        // 3) Ejecutar UPDATE
        docRef.update(payload)
            .addOnSuccessListener {
                // Reflejar en UI (si no usas listener)
                val idx = uiList.indexOfFirst { it.documentId == docId }
                if (idx >= 0) {
                    uiList[idx] = uiList[idx].copy(
                        location = location.trim().uppercase(),
                        lote = lote.trim().uppercase(),
                        quantity = quantity,
                        sku = if (sku.isNotBlank()) sku.trim().uppercase() else uiList[idx].sku,
                        expirationDate = expirationDate ?: uiList[idx].expirationDate
                    )
                }

                // 4) Snapshot DESPUÉS (para auditar valores finales)
                docRef.get().addOnSuccessListener { afterSnap ->
                    val despues = afterSnap.data?.let { mapParaAuditoria(it) } ?: emptyMap()
                    registrarAuditoriaConteo(
                        clienteId = cid,
                        registroId = docId,
                        tipoAccion = "modificación",
                        usuarioNombre = usuarioNombre ?: "",
                        usuarioUid = usuarioUid,
                        valoresAntes = antes,
                        valoresDespues = despues
                    )
                    Log.d("Auditoría", "✓ registrada (update)")
                    onSuccess()
                }.addOnFailureListener { e ->
                    Log.e("Auditoría", "No se pudo leer 'después' (update)", e)
                    onSuccess()
                }
            }
            .addOnFailureListener { e ->
                Log.e("UpdateInv", "❌ Error al actualizar", e)
            }
    }.addOnFailureListener { e ->
        Log.e("Auditoría", "No se pudo leer 'antes' (update)", e)
    }
}

/** Solo conservamos campos auditables y de forma legible para tus tarjetas */
private fun mapParaAuditoria(full: Map<String, Any?>): Map<String, Any?> {
    val keep = setOf(
        "codigoProducto",
        "descripcion",
        "ubicacion",
        "lote",
        "cantidad",
        "unidadMedida",
        "fechaVencimiento",
        "localidad"
    )
    return full.filterKeys { it in keep }
}

@Composable
private fun rememberShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val anim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerAnim"
    )
    return Brush.linearGradient(
        colors = listOf(
            Color(0xFFEDEDED),
            Color(0xFFDCDCDC),
            Color(0xFFEDEDED)
        ),
        start = Offset.Zero,
        end = Offset(x = anim.value, y = anim.value)
    )
}

@Composable
private fun ShimmerLine(modifier: Modifier = Modifier) {
    val brush = rememberShimmerBrush()
    Box(
        modifier
            .background(brush, shape = RoundedCornerShape(6.dp))
            .height(14.dp)
    )
}

@Composable
private fun ShimmerChip(modifier: Modifier = Modifier) {
    val brush = rememberShimmerBrush()
    Box(
        modifier
            .background(brush, shape = RoundedCornerShape(50))
            .height(20.dp)
            .width(72.dp)
    )
}

@Composable
fun ShimmerReportCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            // Título
            ShimmerLine(Modifier.fillMaxWidth(0.6f))
            Spacer(Modifier.height(8.dp))
            // Subtítulo
            ShimmerLine(Modifier.fillMaxWidth(0.4f))
            Spacer(Modifier.height(12.dp))
            // Chips / contadores
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ShimmerChip()
                ShimmerChip()
                ShimmerChip()
            }
            Spacer(Modifier.height(12.dp))
            // 2 líneas de contenido
            ShimmerLine(Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            ShimmerLine(Modifier.fillMaxWidth(0.8f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditFilterDropdown(auditFilter: MutableState<String>) {
    var expanded by remember { mutableStateOf(false) }
    val opciones = listOf("Todos", "Solo auditados", "Solo no auditados")

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            readOnly = true,
            value = auditFilter.value,
            onValueChange = { },
            label = { Text("Estado de auditoría", color = Color.Black) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded)
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Gray,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = Color.Gray,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            opciones.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(opcion) },
                    onClick = {
                        auditFilter.value = opcion
                        expanded = false
                    }
                )
            }
        }
    }
}