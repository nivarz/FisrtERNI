package com.eriknivar.firebasedatabase.view.settings.settingsmenu

import android.app.Activity
import android.content.Context
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
    val localidadesList = remember { mutableStateListOf<String>() }
    val selectedLocalidad = remember { mutableStateOf("") }
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
            delay(60_000)
            val tiempoActual = System.currentTimeMillis()
            val tiempoInactivo = tiempoActual - lastInteractionTime.longValue

            if (tiempoInactivo >= 30 * 60_000) {
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


    // 2) Escuchar ubicaciones de la localidad seleccionada
    DisposableEffect(clienteIdAct, selectedLocalidad.value) {
        ubicaciones.clear()

        if (clienteIdAct.isBlank() || selectedLocalidad.value.isBlank()) {
            // Sin cliente o sin localidad -> nada que escuchar
            return@DisposableEffect onDispose { }
        }

        val removeUbi = UbicacionesRepo.listen(
            clienteId = clienteIdAct,
            localidadCodigo = selectedLocalidad.value,
            onData = { lista ->
                ubicaciones.clear()
                ubicaciones.addAll(lista)
            },
            onErr = { ubicaciones.clear() }
        )

        onDispose {
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

            LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
                items(ubicaciones, key = { it.first }) { (codigo, nombre) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Ubicación: $codigo", fontWeight = FontWeight.Bold)
                                    if (nombre.isNotBlank()) Text("Nombre: $nombre")
                                    Text("Localidad: ${selectedLocalidad.value}")
                                }
                                Row {
                                    IconButton(
                                        onClick = {
                                            isEditing = true
                                            docIdToEdit = codigo
                                            codigoInput = codigo
                                            zonaInput =
                                                nombre                     // usamos 'zona' como 'nombre'
                                            showDialog.value =
                                                true                // 👈 era showDialog = true
                                        }) {
                                        Icon(
                                            Icons.Filled.Edit, contentDescription = "Editar"
                                        )
                                    }

                                    IconButton(onClick = {
                                        val cid = selectedCid.trim()
                                        if (cid.isBlank()) {
                                            Toast.makeText(
                                                ctx, "Selecciona un cliente", Toast.LENGTH_SHORT
                                            ).show()
                                            return@IconButton
                                        }
                                        // 'codigo' es el de la ubicación; 'locCodigo' es la localidad del item
                                        pendingDelete = Triple(codigo, selectedLocalidad.value, cid)
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                                    }
                                }
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
                        Text("Localidad*", fontWeight = FontWeight.Bold)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    color = if (showErrorLocalidad) Color.Red else Color.Gray,
                                    shape = RoundedCornerShape(4.dp)
                                )

                                .clickable(
                                    enabled = !isEditing,
                                    onClick = { expandedLocalidad.value = true })
                                .padding(12.dp)
                        ) {

                            // Dentro del Box del selector de localidad
                            val labelLocalidad =
                                selectedLocalidad.value.ifEmpty { "Seleccionar una localidad" }

                            Text(
                                text = if (showErrorLocalidad) "Debes seleccionar una localidad" else labelLocalidad,
                                color = if (showErrorLocalidad) Color.Red else if (selectedLocalidad.value.isNotEmpty()) Color.Black else Color.Gray,
                                fontSize = if (showErrorLocalidad) 12.sp else 16.sp,
                                modifier = if (showErrorLocalidad) Modifier.padding(
                                    start = 4.dp,
                                    top = 4.dp
                                ) else Modifier
                            )


                            // menú
                            DropdownMenu(
                                expanded = expandedLocalidad.value,
                                onDismissRequest = { expandedLocalidad.value = false }) {
                                localidadesList.forEach { locCode ->
                                    DropdownMenuItem(text = { Text(locCode) }, onClick = {
                                        selectedLocalidad.value = locCode
                                        expandedLocalidad.value = false
                                    })
                                }
                            }
                        }
                    }
                }, confirmButton = {
                    Button(onClick = {
                        if (codigoInput.isBlank()) return@Button

                        if (selectedLocalidad.value.isBlank()) {
                            showErrorLocalidad = true
                            coroutineScope.launch { snackbarHostState.showSnackbar("Debes seleccionar una localidad") }
                            return@Button
                        } else showErrorLocalidad = false

                        val cid = clienteIdAct
                        val loc = selectedLocalidad.value

                        if (isEditing) {
                            val nuevoNombre: String? = zonaInput
                                .takeIf { it.isNotBlank() }     // -> String? (null si está en blanco)
                                ?.uppercase()

                            UbicacionesRepo.updateUbicacion(
                                codigo = docIdToEdit,                    // usa SIEMPRE el doc original
                                nuevoNombre = nuevoNombre,               // String?
                                nuevoActivo = null,                      // o true/false si quieres
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


