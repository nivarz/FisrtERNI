package com.eriknivar.firebasedatabase.view.inventoryreports

import android.app.DatePickerDialog
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.eriknivar.firebasedatabase.view.inventoryentry.updateFirestore
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
import com.google.firebase.firestore.FirebaseFirestore
import com.eriknivar.firebasedatabase.data.Refs
import com.eriknivar.firebasedatabase.data.ReportesRepo
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.DocumentSnapshot
import com.eriknivar.firebasedatabase.view.storagetype.DataFields
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.toObject


private fun DocumentSnapshot.toDataFieldsUi(): DataFields {
    val base = this.toObject(DataFields::class.java) ?: DataFields()

    // Tomamos usuarioNombre si existe; si no, dejamos el que haya en ES
    val usuarioUi = this.getString("usuarioNombre") ?: base.usuario

    // unidadMedida puede venir como "unidad" en algunos docs
    val unidad = if (base.unidadMedida.isNotBlank())
        base.unidadMedida
    else
        (this.getString("unidad") ?: "")

    return base.copy(
        documentId    = this.id,                 // ← imprescindible para editar/eliminar
        // Aliases UI ← campos ES reales
        sku           = base.codigoProducto,
        description   = base.descripcion,
        location      = base.ubicacion,
        quantity      = base.cantidad,
        expirationDate= base.fechaVencimiento,
        // Overrides extra para cubrir variantes
        usuario       = usuarioUi,
        unidadMedida  = unidad
    )
}

@Composable
fun InventoryReportFiltersScreen(
    userViewModel: UserViewModel,
    allData: List<DataFields>,
    tipoUsuario: String,
    puedeModificarRegistro: (String, String) -> Boolean,
    onUserInteraction: () -> Unit,
    onSuccess: () -> Unit // 👈 este es nuevo

) {
    val sku = remember { mutableStateOf("") }
    val location = remember { mutableStateOf("") }
    val startDate = remember { mutableStateOf("") }
    val endDate = remember { mutableStateOf("") }
    val filtrosExpandido = remember { mutableStateOf(false) }

    val usuarioFiltro = remember { mutableStateOf("") }

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
    val cidRaw by userViewModel.clienteId.observeAsState("")

    // La lista que la UI pinta
    val filteredData = remember { mutableStateListOf<DataFields>() }

    // Cambia estos nombres si tus estados se llaman distinto
    val selectedLocalidadState = remember { mutableStateOf("") }   // o tu estado real
    val selectedUsuarioState = remember { mutableStateOf("") }   // o tu estado real



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


    fun cargarReportes() {
        val filtros = buildMap<String, String> {
            localidadSeleccionada.value.trim().takeIf { it.isNotBlank() }?.let {
                put("localidad", it.uppercase())
            }

            val tipo = tipoUsuario.lowercase().trim()
            val usr = if (tipo == "invitado") {
                userViewModel.nombre.value.orEmpty()   // 🔒 fuerza su propio nombre
            } else {
                usuarioFiltro.value.trim()
            }
            if (usr.isNotBlank()) put("usuario", usr) // 👈 importante: sin uppercase
        }

        // ⬇️ Reemplazo de fetchFilteredInventoryFromFirestore(...)
        val q = ReportesRepo.buildReportQueryForRole(
            db = firestore,
            clienteId = cid,
            tipoUsuario = tipoUsuario,
            uidActual = userViewModel.documentId.value,
            filters = filtros
        )

        q.get()
            .addOnSuccessListener { snap ->
                val nuevos = snap.documents.map { doc -> doc.toDataFieldsUi() }
                filteredData.clear()
                filteredData.addAll(nuevos.sortedByDescending { it.fechaRegistro?.toDate() })
                filtrosExpandido.value = false
                isLoading.value = false
            }
            .addOnFailureListener {
                isLoading.value = false
                Toast.makeText(context, "Error al consultar Firestore", Toast.LENGTH_SHORT).show()
            }

    }

    fun recargarFiltrosDelCliente(cid: String) {
        if (cid.isBlank()) return

        // Localidades
        Refs.ubic(db, cid)
            .orderBy("codigo_ubi")
            .get()
            .addOnSuccessListener { snap ->
                val data = snap.documents.mapNotNull { it.getString("codigo_ubi") }
                // TODO: sustituye por tu estado real de localidades:
                // localidadesOptions.clear(); localidadesOptions.addAll(data)
            }

        // Productos/SKUs
        Refs.prod(db, cid)
            .orderBy("codigo")      // o el campo que uses para listar
            .get()
            .addOnSuccessListener { snap ->
                val data = snap.documents.mapNotNull { it.getString("codigo") }
                // TODO: sustituye por tu estado real de SKUs:
                // skuOptions.clear(); skuOptions.addAll(data)
            }

        // (Opcional) Usuarios del cliente para filtros por usuario
        db.collection("usuarios")
            .whereEqualTo("clienteId", cid)
            .get()
            .addOnSuccessListener { snap ->
                val data = snap.documents.mapNotNull { it.getString("nombre") }
                // TODO: sustituye por tu estado real de usuarios:
                // usuariosOptions.clear(); usuariosOptions.addAll(data)
            }
    }

    LaunchedEffect(cid) {
        if (cid.isNotBlank()) {
            recargarFiltrosDelCliente(cid)
        } else {
            // Si quieres, limpia listas cuando no hay cliente
            // localidadesOptions.clear()
            // skuOptions.clear()
            // usuariosOptions.clear()
        }
    }

    // Carga inicial (y cuando cambie el cliente)
    LaunchedEffect(cid) { cargarReportes() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

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
                    filteredData.clear()
                    filteredData.addAll(allData.sortedByDescending { it.fechaRegistro?.toDate() })
                }

                OutlinedTextField(
                    value = usuarioFiltro.value.uppercase(),
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

                            onUserInteraction()

                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            isLoading.value = true

                            // === SÓLO sustituye el bloque buildMap actual por este ===
                            val filtros = buildMap<String, String> {
                                val tipo = tipoUsuario.lowercase().trim()
                                val usr = if (tipo == "invitado") {
                                    userViewModel.nombre.value.orEmpty()      // 🔒 fuerza su propio nombre
                                } else {
                                    usuarioFiltro.value.trim()
                                }
                                if (usr.isNotBlank()) put("usuario", usr)
                                if (localidadSeleccionada.value.isNotBlank())
                                    put("localidad", localidadSeleccionada.value.trim().uppercase())
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
                                        android.util.Log.d("DBG", "DocId=${first.id} data=${first.data}")
                                        android.util.Log.d("DBG", "sku=${first.getString("sku")} | SKU_alt=${first.getString("SKU")}")
                                        android.util.Log.d("DBG", "ubicacion=${first.getString("ubicacion")} | location=${first.getString("location")}")
                                        android.util.Log.d("DBG", "usuario=${first.getString("usuario")} | usuarioUid=${first.getString("usuarioUid")}")
                                        android.util.Log.d("DBG", "cantidad=${first.getDouble("cantidad")} | qty=${first.getDouble("qty")}")
                                    }
                                    // ⬆️ hasta aquí el diagnóstico

                                    val nuevosDatos = snap.documents.map { doc -> doc.toDataFieldsUi() }

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

                                            val dateFormatted = item.fechaRegistro?.toDate()?.let { sdf.format(it) } ?: ""
                                            val matchesDate = try {
                                                (startDate.value.isBlank() || dateFormatted >= startDate.value) &&
                                                        (endDate.value.isBlank()   || dateFormatted <= endDate.value)
                                            } catch (_: Exception) { true }

                                            val matchesLocalidad =
                                                localidadSeleccionada.value.isBlank() ||
                                                        item.localidad.equals(localidadSeleccionada.value, ignoreCase = true)

                                            matchesSku && matchesLocation && matchesDate && matchesLocalidad
                                        }.sortedByDescending { it.fechaRegistro?.toDate() }
                                    )

                                    filtrosExpandido.value = false
                                    isLoading.value = false
                                }
                                .addOnFailureListener {
                                    isLoading.value = false
                                    Toast.makeText(context, "Error al consultar Firestore", Toast.LENGTH_SHORT).show()
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
                            onUserInteraction()

                            coroutineScope.launch {
                                Log.d("EXPORT_DEBUG", "Ejecutando exportación y share...")

                                try {
                                    val file = exportToExcel(context, filteredData)

                                    delay(800) // ⏱️ Retardo antes de ejecutar share

                                    file?.let { shareExcelFile(context, it) }

                                } catch (_: Exception) {
                                    Toast.makeText(context, "Error al exportar", Toast.LENGTH_SHORT)
                                        .show()
                                } finally {
                                    delay(800) // 🔐 Tiempo extra antes de reactivar
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

        // 🧩 Lista de resultados (siempre visible)
        if (filteredData.isEmpty()) {
            Text("No hay resultados", modifier = Modifier.padding(top = 8.dp))
        } else {

            LazyColumn {
                items(filteredData) { item ->
                    InventoryReportItem(
                        item = item,
                        puedeModificarRegistro = puedeModificarRegistro,
                        tipoUsuarioActual = userViewModel.tipo.value
                            ?: "", // ✅ Aquí el nuevo parámetro
                        onDelete = { documentId ->
                            Firebase.firestore
                                .collection("clientes").document(cid)
                                .collection("inventario")
                                .document(documentId)
                                .delete()
                                .addOnSuccessListener {
                                    Toast.makeText(
                                        context,
                                        "Registro eliminado",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    filteredData.removeIf { it.documentId == documentId }
                                }
                                .addOnFailureListener {
                                    Toast.makeText(context, "Error al eliminar", Toast.LENGTH_SHORT)
                                        .show()
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
                                onSuccess = onSuccess
                            )
                        }
                    )
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
            label = { Text("Localidad", color = Color.Black) },
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
    sku: String,               // opcional (mapéalo a codigoProducto si deseas permitir editarlo)
    lote: String,
    expirationDate: String?,
    quantity: Double,
    uiList: SnapshotStateList<DataFields>,
    onSuccess: () -> Unit = {}
) {
    val docRef = db.collection("clientes")
        .document(cid.trim().uppercase())
        .collection("inventario")
        .document(docId)

    // ⚠️ Campos REALES en Firestore (ES)
    val payload = hashMapOf<String, Any>(
        "ubicacion" to location.trim().uppercase(),
        "lote" to lote.trim().uppercase(),
        "cantidad" to quantity
    )

    // Si vas a permitir editar el SKU desde el reporte:
    if (sku.isNotBlank()) {
        payload["codigoProducto"] = sku.trim().uppercase()
    }

    // Vencimiento solo si viene (ajusta si usas Timestamp)
    if (!expirationDate.isNullOrBlank()) {
        payload["fechaVencimiento"] = expirationDate
    }

    // Auditoría
    payload["updatedAt"] = Timestamp.now()
    payload["updatedBy"] = (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "")

    docRef.update(payload)
        .addOnSuccessListener {
            Log.d("UpdateInv", "✅ Actualizado OK $docId")

            // Sin listener: refleja en la lista de UI
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

            onSuccess()
        }
        .addOnFailureListener { e ->
            Log.e("UpdateInv", "❌ Error al actualizar", e)
            // Muestra mensaje claro (útil cuando son reglas)
            try {
                Toast.makeText(
                    (uiList as? Any)?.let { null }, // ignora contexto si no lo tienes aquí
                    "No se pudo actualizar: ${e.message ?: "PERMISSION_DENIED"}",
                    Toast.LENGTH_LONG
                ).show()
            } catch (_: Exception) {}
        }
}




