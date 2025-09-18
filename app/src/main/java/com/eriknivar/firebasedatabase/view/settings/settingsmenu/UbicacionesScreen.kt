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
    // antes: Pair<String, String?>
    val ubicaciones = remember { mutableStateListOf<Pair<String, String>>() }


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

    val clienteIdAct = userViewModel.clienteId.observeAsState("").value.trim().uppercase()

    val context = LocalContext.current

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showErrorLocalidad by remember { mutableStateOf(false) }


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

    LaunchedEffect(clienteIdAct) {
        localidadesList.clear()
        if (clienteIdAct.isNotBlank()) {
            LocalidadesRepo.listen(clienteId = clienteIdAct, onData = { lista ->
                localidadesList.clear()
                localidadesList.addAll(lista) // ← lista de CÓDIGOS (ALM_REP, ALM_SUM, etc.)
                // Si no hay selección, toma la primera
                if (selectedLocalidad.value.isBlank() && lista.isNotEmpty()) {
                    selectedLocalidad.value = lista.first()
                }
            }, onErr = { e ->
                localidadesList.clear()
            })
        }
    }

    // items = Pair<CODIGO, NOMBRE>
    LaunchedEffect(clienteIdAct, selectedLocalidad.value) {
        ubicaciones.clear()
        if (clienteIdAct.isBlank() || selectedLocalidad.value.isBlank()) return@LaunchedEffect

        UbicacionesRepo.listen(
            clienteId = clienteIdAct,
            localidadCodigo = selectedLocalidad.value,
            onData = { lista ->
                ubicaciones.clear()
                ubicaciones.addAll(lista) // pares (codigo, nombre)
            },
            onErr = { _ ->
                ubicaciones.clear()
            })
    }


    val dummyLocation = remember { mutableStateOf("") }
    val dummySku = remember { mutableStateOf("") }
    val dummyQuantity = remember { mutableStateOf("") }
    val dummyLot = remember { mutableStateOf("") }
    val dummyDateText = remember { mutableStateOf("") }

    val qrCodeContent = remember { mutableStateOf("") }
    val wasScanned = remember { mutableStateOf(false) }

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
            ElevatedButton(
                colors = ButtonDefaults.buttonColors(
                    containerColor = navyBlue, contentColor = Color.White
                ), onClick = {
                    actualizarActividad(context)
                    codigoInput = ""
                    zonaInput = ""
                    docIdToEdit = ""
                    isEditing = false
                    showDialog.value = true
                }, modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("+ ", fontWeight = FontWeight.Bold, fontSize = 30.sp, color = Color.Green)

                Text("Crear Ubicación")
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
                                    IconButton(onClick = {
                                        isEditing = true
                                        docIdToEdit = codigo
                                        codigoInput = codigo
                                        zonaInput =
                                            nombre                     // usamos 'zona' como 'nombre'
                                        showDialog.value =
                                            true                // 👈 era showDialog = true
                                    }) { Icon(Icons.Filled.Edit, contentDescription = "Editar") }

                                    IconButton(onClick = {
                                        UbicacionesRepo.borrarUbicacion(
                                            codigo = codigo,
                                            clienteIdDestino = clienteIdAct,
                                            localidadCodigoDestino = selectedLocalidad.value
                                        ) { ok, msg ->
                                            if (!ok) Toast.makeText(
                                                ctx, "Error: $msg", Toast.LENGTH_LONG
                                            ).show()
                                            // el listener refresca solo
                                        }
                                    }) {
                                        Icon(
                                            Icons.Default.Delete, contentDescription = "Eliminar"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
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
                            trailingIcon = {
                                IconButton(
                                    onClick = { qrCodeScanner.startQRCodeScanner(context as Activity) },
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

                                .clickable(onClick = { expandedLocalidad.value = true })
                                .padding(12.dp)
                        ) {

                            if (!showErrorLocalidad) {
                                Text(
                                    text = selectedLocalidad.value.ifEmpty { "Seleccionar una localidad" },
                                    color = if (selectedLocalidad.value.isNotEmpty()) Color.Black else Color.Gray
                                )
                            }

                            if (showErrorLocalidad) {
                                Text(
                                    text = "Debes seleccionar una localidad",
                                    color = Color.Red,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                                )
                                selectedLocalidad.value = ""
                            }

                            // texto mostrado
                            Text(
                                text = selectedLocalidad.value.ifEmpty { "Seleccionar una localidad" },
                                color = if (selectedLocalidad.value.isNotEmpty()) Color.Black else Color.Gray
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
                            UbicacionesRepo.updateUbicacion(
                                codigo = docIdToEdit.ifBlank { codigoInput },
                                nuevoNombre = zonaInput.ifBlank { null },   // 'zona' → 'nombre'
                                nuevoActivo = null,
                                clienteIdDestino = cid,
                                localidadCodigoDestino = loc
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
                                nombreRaw = zonaInput,                      // 'zona' → 'nombre'
                                clienteIdDestino = cid,
                                localidadCodigoDestino = loc
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


