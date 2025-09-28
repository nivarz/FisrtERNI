package com.eriknivar.firebasedatabase.view.settings.settingsmenu

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.eriknivar.firebasedatabase.data.LocalidadesRepo
import com.eriknivar.firebasedatabase.data.UbicacionesRepo
import com.eriknivar.firebasedatabase.navigation.NavigationDrawer
import com.eriknivar.firebasedatabase.view.inventoryentry.QRCodeScanner
import com.eriknivar.firebasedatabase.view.utility.SessionUtils
import com.eriknivar.firebasedatabase.viewmodel.UserViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.saveable.rememberSaveable
import com.eriknivar.firebasedatabase.data.ClientesRepo
import com.eriknivar.firebasedatabase.data.Ubicacion
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme


@Composable
fun UbicacionesScreen(navController: NavHostController, userViewModel: UserViewModel) {

    val isLoggedOut = userViewModel.nombre.observeAsState("").value.isEmpty()
    val isInitialized = userViewModel.isInitialized.observeAsState(false).value

    if (isInitialized && isLoggedOut) {
        // 🔴 No muestres nada, Compose lo ignora y se cerrará la app correctamente
        return
    }

    val tipo = userViewModel.tipo.value ?: ""

    if (tipo.lowercase() != "admin" && tipo.lowercase() != "superuser") {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.TopCenter
        ) {
            Text(
                text = "⛔ Acceso restringido",
                color = Color.Red,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(32.dp)
            )
        }
        return
    }

    val ctx = LocalContext.current

    val firestore = Firebase.firestore
    val ubicaciones = remember { mutableStateListOf<Pair<String, String>>() }
    val clienteId by userViewModel.clienteId.observeAsState("")

    val showDialog = remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }

    var docIdToEdit by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var codigoInput by remember { mutableStateOf("") }
    var zonaInput by remember { mutableStateOf("") }

    val navyBlue = Color(0xFF001F5B)

    val showSuccessDialog = remember { mutableStateOf(false) }
    val showSuccessDeleteDialog = remember { mutableStateOf(false) }
    val successMessage = remember { mutableStateOf("") }
    var ubicacionAEliminar by remember { mutableStateOf<Pair<String, String?>?>(null) }

    // Estados para el Dropdown de Localidades
    // Guardamos PAIR(codigo, nombre) para mostrar bonito en el menú
    val localidadesList =
        remember { mutableStateListOf<Pair<String, String>>() } // (codigo to nombre)
    val selectedLocalidad = remember { mutableStateOf("") } // aquí solo el CÓDIGO seleccionado
    val expandedLocalidad = remember { mutableStateOf(false) }

    val context = LocalContext.current

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showErrorLocalidad by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Triple<String, String, String>?>(null) }

    val selectedCid by userViewModel.clienteId.observeAsState("")
    //val clienteNombre by userViewModel.nombre.observeAsState("") // usa el nombre que guardes en tu VM
    var showClientePicker by remember { mutableStateOf(false) }
    val clientesActivos =
        remember { mutableStateListOf<Pair<String, String>>() } // (clienteId, nombre)


    val lastInteractionTime =
        remember { mutableLongStateOf(SessionUtils.obtenerUltimaInteraccion(context)) }

    fun actualizarActividad(context: Context) {
        val tiempoActual = System.currentTimeMillis()
        lastInteractionTime.longValue = tiempoActual
        SessionUtils.guardarUltimaInteraccion(context, tiempoActual)
    }

    LaunchedEffect(lastInteractionTime.longValue) {
        while (true) {
            delay(600_000)
            val tiempoActual = System.currentTimeMillis()
            val tiempoInactivo = tiempoActual - lastInteractionTime.longValue

            if (tiempoInactivo >= 30 * 600_000) {
                val documentId = userViewModel.documentId.value ?: ""
                Firebase.firestore.collection("usuarios").document(documentId)
                    .update("sessionId", "")
                Toast.makeText(context, "Sesión finalizada por inactividad", Toast.LENGTH_LONG)
                    .show()

                userViewModel.clearUser()

                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                }

                break
            }
        }
    }

    val clienteIdAct by userViewModel.clienteId.observeAsState("")
    var clienteNombre by rememberSaveable { mutableStateOf("") }

    /*
    // 1) Cargar nombre del cliente + localidades del cliente activo
    DisposableEffect(clienteIdAct) {
        // Nombre del cliente
        clienteNombre = ""
        if (clienteIdAct.isBlank()) {
            // No hay cliente: no registres listeners y devuelve un onDispose vacío
            return@DisposableEffect onDispose { }
        }

        // Nombre del cliente
        ClientesRepo.getNombreCliente(clienteIdAct) { nombre -> clienteNombre = nombre }

        // Localidades del cliente
        localidadesList.clear()
        val removeLoc = LocalidadesRepo.listen(
            clienteId = clienteIdAct,
            onData = { lista ->
                localidadesList.clear()
                localidadesList.addAll(lista)
                if (selectedLocalidad.value.isBlank() && lista.isNotEmpty()) {
                    selectedLocalidad.value = lista.first()
                }
            },
            onErr = { localidadesList.clear() }
        )

        // <- El último statement debe ser un onDispose { … }
        onDispose {
            // si tu repo devuelve algo removible, úsalo
            LocalidadesRepo.stop()
        }
    }

    */

    val ubisRaw = remember { mutableStateListOf<Ubicacion>() }

    // CARGAR LOCALIDADES CUANDO SE ABRE EL DIÁLOGO "CREAR/EDITAR UBICACIÓN"
    DisposableEffect(showDialog.value, clienteIdAct) {
        var removeLocListener: com.google.firebase.firestore.ListenerRegistration? = null

        if (showDialog.value && clienteIdAct.isNotBlank()) {
            localidadesList.clear()
            selectedLocalidad.value = ""     // resetea selección al abrir

            removeLocListener =
                firestore.collection("clientes").document(clienteIdAct).collection("localidades")
                    .orderBy("codigo").addSnapshotListener { snap, e ->
                        if (e != null) {
                            localidadesList.clear()
                            return@addSnapshotListener
                        }
                        val items = snap?.documents?.map { d ->
                            val cod = d.getString("codigo") ?: d.id
                            val nom = d.getString("nombre") ?: ""
                            cod to nom
                        }.orEmpty()

                        localidadesList.clear()
                        localidadesList.addAll(items)

                        // Si no hay selección aún, toma la primera disponible
                        if (selectedLocalidad.value.isBlank() && localidadesList.isNotEmpty()) {
                            selectedLocalidad.value = localidadesList.first().first
                        }
                    }
        }

        onDispose { removeLocListener?.remove() }
    }


    // 2) Escuchar TODAS las ubicaciones del cliente (todas las localidades)
    DisposableEffect(clienteIdAct) {
        ubicaciones.clear()

        if (clienteIdAct.isBlank()) {
            return@DisposableEffect onDispose { }
        }

        // Importa Ubicacion si no lo tienes:
        // import com.eriknivar.firebasedatabase.data.Ubicacion

        val removeUbi =
            UbicacionesRepo.listenAll(clienteId = clienteIdAct, onData = { lista: List<Ubicacion> ->
                ubisRaw.clear()
                ubisRaw.addAll(
                    lista.sortedWith(
                        compareBy<Ubicacion>(
                            { (it.localidadCodigo.ifBlank { "—" }).uppercase() },
                            { (it.codigo.ifBlank { it.id }).uppercase() })
                    )
                )

                // (opcional) si aún usas el estado antiguo para otro componente:
                ubicaciones.clear()
                ubicaciones.addAll(
                    ubisRaw.map { u ->
                        val titulo =
                            "${u.localidadCodigo.ifBlank { "—" }} · ${u.codigo.ifBlank { u.id }}"
                        val subtitulo = u.nombre
                        titulo to subtitulo
                    })
            }, onErr = {
                ubicaciones.clear()
                Log.e("UbicacionesScreen", "Error cargando ubicaciones (listenAll).")
            })

        onDispose {
            removeUbi.remove()
            UbicacionesRepo.stop()
        }
    }

    val dummyLocation = remember { mutableStateOf("") }
    val dummySku = remember { mutableStateOf("") }
    val dummyQuantity = remember { mutableStateOf("") }
    val dummyLot = remember { mutableStateOf("") }
    val dummyDateText = remember { mutableStateOf("") }

    val qrCodeContent = remember { mutableStateOf("") }
    val wasScanned = remember { mutableStateOf(false) }

    val codigoAEliminar = remember { mutableStateOf<String?>(null) }
    val isDeleting = remember { mutableStateOf(false) }

    val qrScanLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            val intentResult = IntentIntegrator.parseActivityResult(result.resultCode, data)
            if (intentResult != null) {
                qrCodeContent.value = intentResult.contents ?: ""
                wasScanned.value = true
            }
        }

    val qrCodeScanner = remember { QRCodeScanner(qrScanLauncher) }

    // Esto dentro de LaunchedEffect
    LaunchedEffect(wasScanned.value) {
        if (wasScanned.value && qrCodeContent.value.isNotBlank()) {
            codigoInput = qrCodeContent.value.trim().uppercase()
            wasScanned.value = false
        }
    }

    NavigationDrawer(
        navController = navController,
        storageType = "Ubicaciones",
        userViewModel = userViewModel,
        location = dummyLocation,
        sku = dummySku,
        quantity = dummyQuantity,
        lot = dummyLot,
        expirationDate = dummyDateText
    ) {

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Chip que muestra el cliente activo
                Box(
                    modifier = Modifier
                        .border(1.dp, Color.Gray, RoundedCornerShape(12.dp))
                        .clickable {
                            // Solo superuser puede cambiarlo
                            if (tipo.equals("superuser", true)) {
                                showClientePicker = true
                            } else {
                                Toast.makeText(ctx, "Cliente fijo para admin", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)) {
                    val label = when {
                        clienteId.isBlank() -> "Selecciona un cliente"
                        clienteNombre.isNotBlank() -> "$clienteNombre ($clienteId)"
                        else -> clienteId
                    }
                    Text(label, fontSize = 14.sp)
                }

                // Botón crear (lo deshabilitamos si no hay cliente activo)
                ElevatedButton(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = navyBlue, contentColor = Color.White
                    ), enabled = selectedCid.isNotBlank(), onClick = {
                        actualizarActividad(context)
                        if (selectedCid.isBlank()) {
                            showClientePicker = true
                            return@ElevatedButton
                        }
                        codigoInput = ""
                        zonaInput = ""
                        docIdToEdit = ""
                        isEditing = false
                        showDialog.value = true
                    }) {
                    Text("+ ", fontWeight = FontWeight.Bold, fontSize = 30.sp, color = Color.Green)
                    Text("Crear Ubicación")
                }
            }

            if (ubicaciones.isEmpty()) {
                Text(
                    "Sin ubicaciones para este cliente",
                    color = Color.Gray,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                @OptIn(ExperimentalFoundationApi::class) LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (ubisRaw.isEmpty()) {
                        item {
                            Text(
                                "Sin ubicaciones para este cliente.",
                                color = Color.Gray,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        val grupos = ubisRaw.groupBy { it.localidadCodigo.ifBlank { "—" } }
                            .toSortedMap(String.CASE_INSENSITIVE_ORDER)

                        grupos.forEach { (loc, itemsDeLoc) ->
                            stickyHeader {
                                Text(
                                    text = loc,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFEFEFEF))
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }

                            items(itemsDeLoc, key = { it.codigo.ifBlank { it.id } }) { u ->
                                // Tu fila existente: muestra código y nombre
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        "${u.codigo.ifBlank { u.id }}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (u.nombre.isNotBlank()) {
                                        Text(
                                            u.nombre,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray
                                        )
                                    }
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }


            }

            if (showClientePicker && tipo.equals("superuser", true)) {
                // Carga de clientes activos (solo la 1ra vez que se abre)
                LaunchedEffect(Unit) {
                    clientesActivos.clear()
                    com.eriknivar.firebasedatabase.data.ClientesRepo.listarActivos { ok, items, msg ->
                        if (ok) clientesActivos.addAll(items) else Toast.makeText(
                            ctx, msg, Toast.LENGTH_LONG
                        ).show()
                    }
                }

                AlertDialog(
                    onDismissRequest = { showClientePicker = false },
                    title = { Text("Selecciona un cliente") },
                    text = {
                        LazyColumn(modifier = Modifier.height(300.dp)) {
                            items(clientesActivos) { (cid, nombre) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            // Cambiar cliente activo en el VM
                                            userViewModel.setClienteId(cid)
                                            // Resetear localidad para forzar recarga
                                            selectedLocalidad.value = ""
                                            showClientePicker = false
                                        }
                                        .padding(vertical = 10.dp, horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(nombre.ifBlank { cid })
                                    Text(cid, color = Color.Gray)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            showClientePicker = false
                        }) { Text("Cerrar") }
                    })
            }

            pendingDelete?.let { (codigoUbi, locCodigo, cid) ->
                AlertDialog(
                    onDismissRequest = { pendingDelete = null },
                    title = { Text("¿Eliminar $codigoUbi?") },
                    text = { Text("Esta acción no se puede deshacer.") },
                    confirmButton = {
                        TextButton(onClick = {
                            UbicacionesRepo.borrarUbicacion(
                                codigo = codigoUbi,
                                clienteIdDestino = cid,
                                localidadCodigoDestino = locCodigo
                            ) { ok, msg ->
                                pendingDelete = null
                                if (ok) {
                                    Toast.makeText(ctx, "Eliminada $codigoUbi", Toast.LENGTH_SHORT)
                                        .show()
                                    // si no usas realtime, aquí recarga la lista
                                } else {
                                    Toast.makeText(ctx, "Error: $msg", Toast.LENGTH_LONG).show()
                                }
                            }
                        }) { Text("Eliminar") }
                    },
                    dismissButton = {
                        TextButton(onClick = { pendingDelete = null }) { Text("Cancelar") }
                    })
            }


        }


        var showUbicacionExistenteDialog by remember { mutableStateOf(false) }

        if (showDialog.value) {
            AlertDialog(
                onDismissRequest = { showDialog.value = false }, // ✅
                title = { Text(if (isEditing) "Editar Ubicación" else "Nueva Ubicación") }, text = {
                    Column {
                        OutlinedTextField(
                            value = codigoInput,
                            onValueChange = { codigoInput = it.uppercase().trim() },
                            label = { Text("Código de Ubicación*") },
                            singleLine = true,
                            enabled = !isEditing,
                            trailingIcon = {
                                IconButton(
                                    onClick = { qrCodeScanner.startQRCodeScanner(context as Activity) },
                                    enabled = !isEditing,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.QrCodeScanner,
                                        contentDescription = "Escanear Código"
                                    )
                                }
                            })
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = zonaInput,
                            singleLine = true,
                            onValueChange = { zonaInput = it.uppercase() },
                            label = { Text("Zona (opcional)") })



                        Spacer(Modifier.height(8.dp))
                        Text("Almacén*", fontWeight = FontWeight.Bold)

                        var expanded by remember { mutableStateOf(false) }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    color = if (showErrorLocalidad) Color.Red else Color.Gray,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .clickable(enabled = !isEditing) { expanded = true }
                                .padding(12.dp)) {
                            val etiqueta = when {
                                selectedLocalidad.value.isNotBlank() -> {
                                    val par =
                                        localidadesList.firstOrNull { it.first == selectedLocalidad.value }
                                    if (par != null && par.second.isNotBlank()) "${par.first} · ${par.second}" else par?.first
                                        ?: ""
                                }

                                else -> "Seleccionar un Almacén"
                            }

                            Text(
                                text = if (showErrorLocalidad) "Debes seleccionar un Almacén" else etiqueta,
                                color = when {
                                    showErrorLocalidad -> Color.Red
                                    selectedLocalidad.value.isNotEmpty() -> Color.Black
                                    else -> Color.Gray
                                },
                                fontSize = if (showErrorLocalidad) 12.sp else 16.sp
                            )

                            DropdownMenu(
                                expanded = expanded, onDismissRequest = { expanded = false }) {
                                if (localidadesList.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("Sin localidades") },
                                        onClick = { expanded = false },
                                        enabled = false
                                    )
                                } else {
                                    localidadesList.forEach { (cod, nom) ->
                                        DropdownMenuItem(
                                            text = { Text(if (nom.isBlank()) cod else "$cod · $nom") },
                                            onClick = {
                                                selectedLocalidad.value = cod
                                                showErrorLocalidad = false
                                                expanded = false
                                            })
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (codigoInput.isBlank()) return@Button

                        if (selectedLocalidad.value.isBlank()) {
                            showErrorLocalidad = true
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Debes seleccionar un Almacén")
                            }
                            return@Button
                        } else {
                            showErrorLocalidad = false
                        }

                        if (isEditing) {
                            val nuevoNombre: String? =
                                zonaInput.takeIf { it.isNotBlank() }?.uppercase()

                            UbicacionesRepo.updateUbicacion(
                                codigo = docIdToEdit,
                                nuevoNombre = nuevoNombre,
                                nuevoActivo = null,
                                clienteIdDestino = clienteIdAct,
                                localidadCodigoDestino = selectedLocalidad.value
                            ) { ok, msg ->
                                if (ok) {
                                    successMessage.value = "Ubicación actualizada exitosamente"
                                    showSuccessDialog.value = true
                                    showDialog.value = false
                                } else {
                                    Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
                                }
                            }
                        } else {
                            UbicacionesRepo.crearUbicacion(
                                codigoRaw = codigoInput,
                                nombreRaw = zonaInput,
                                clienteIdDestino = clienteIdAct,
                                localidadCodigoDestino = selectedLocalidad.value
                            ) { ok, msg ->
                                if (ok) {
                                    successMessage.value = "Ubicación agregada exitosamente"
                                    showSuccessDialog.value = true
                                    showDialog.value = false
                                } else {
                                    Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }) {
                        Text(if (isEditing) "Actualizar" else "Guardar")
                    }
                }, dismissButton = {
                    TextButton(onClick = { showDialog.value = false }) {
                        Text("Cancelar")
                    }
                })
        }

        if (showUbicacionExistenteDialog) {
            AlertDialog(
                onDismissRequest = { showUbicacionExistenteDialog = false },
                title = { Text("Ubicación existente") },
                text = { Text("Ya existe una ubicación con ese mismo código y zona.") },
                confirmButton = {
                    TextButton(onClick = { showUbicacionExistenteDialog = false }) {
                        Text("Aceptar")
                    }
                })
        }
    }

    if (showSuccessDialog.value) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog.value = false },
            title = { Text("Creada/Actualizada") },
            text = { Text(successMessage.value) },
            confirmButton = {
                TextButton(onClick = { showSuccessDialog.value = false }) {
                    Text("Aceptar")
                }
            })
    }
}