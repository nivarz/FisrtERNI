package com.eriknivar.firebasedatabase.view.inventoryentry

import android.app.Activity
import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.eriknivar.firebasedatabase.view.storagetype.DataFields
import com.eriknivar.firebasedatabase.viewmodel.UserViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineScope
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.eriknivar.firebasedatabase.view.utility.validarRegistroDuplicado
import com.google.firebase.storage.storage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.UUID
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import com.eriknivar.firebasedatabase.network.CatalogoRepository
import com.eriknivar.firebasedatabase.network.SelectedClientStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eriknivar.firebasedatabase.data.UbicacionesRepo
import com.eriknivar.firebasedatabase.view.common.ConteoMode
import com.google.firebase.auth.FirebaseAuth
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import kotlinx.coroutines.withContext


@Composable
fun FormEntradaDeInventario(
    location: MutableState<String>,
    sku: MutableState<String>,
    lot: MutableState<String>,
    dateText: MutableState<String>,
    quantity: MutableState<String>,
    productoDescripcion: MutableState<String>,
    unidadMedida: MutableState<String>,
    coroutineScope: CoroutineScope,
    userViewModel: UserViewModel,
    localidad: String,
    allData: SnapshotStateList<DataFields>,
    listState: LazyListState,
    isVisible: Boolean,
    conteoMode: ConteoMode

) {

    val qrCodeContentSku = remember { mutableStateOf("") } //esto es para el scanner de QRCode
    val qrCodeContentLot = remember { mutableStateOf("") } //esto es para el scanner de QRCode

    val showErrorQuantity = remember { mutableStateOf(false) }
    val showErrorLocation = remember { mutableStateOf(false) }
    val showErrorSku = remember { mutableStateOf(false) }

    val errorMessageQuantity = remember { mutableStateOf("") }
    var showDialogValueQuantityCero by remember { mutableStateOf(false) }
    val showDialogRegistroDuplicado = remember { mutableStateOf(false) }
    val showConfirmDialog = remember { mutableStateOf(false) }

    val catalogRepo = remember { CatalogoRepository() }

    val showProductDialog = remember { mutableStateOf(false) } // 🔥 Para la lista de productos
    val productList = remember { mutableStateOf(emptyList<String>()) }
    val productMap = remember { mutableStateOf(emptyMap<String, Pair<String, String>>()) }

    val shouldRequestFocus = remember { mutableStateOf(false) }
    val focusRequesterSku = remember { FocusRequester() }
    val focusRequesterLot = remember { FocusRequester() }
    val focusRequesterFecha = remember { FocusRequester() }
    val focusRequesterCantidad = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequesterLocation = remember { FocusRequester() }
    val openUbicacionInvalidaDialog = remember { mutableStateOf(false) }
    val tempLocationInput = remember { mutableStateOf("") }
    val tempLotInput = remember { mutableStateOf("") }
    val showSavingDialog = remember { mutableStateOf(false) }

    var isSaving by remember { mutableStateOf(false) }
    // El botón “Grabar” solo se habilita si NO está guardando y hay datos mínimos válidos
    val canSave =
        !isSaving && location.value.isNotBlank() && sku.value.isNotBlank() && (quantity.value.replace(
            ",", "."
        ).toDoubleOrNull()?.let { it > 0 } == true)

    val context = LocalContext.current
    val firestore = Firebase.firestore

    var showError1 by remember { mutableStateOf(false) }
    var showError2 by remember { mutableStateOf(false) }
    var showError3 by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) } // Estado para mostrar el cuadro de diálogo
    var showDialog1 by remember { mutableStateOf(false) }
    var showDialog2 by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") } // Mensaje de error para el cuadro de diálogo
    var errorMessage1 by remember { mutableStateOf("") }
    var errorMessage2 by remember { mutableStateOf("") }
    var errorMessage3 by remember { mutableStateOf("") }

    val focusRequester = remember { FocusRequester() }
    val shouldRequestFocusAfterClear = remember { mutableStateOf(false) }
    val usuario by userViewModel.nombre.observeAsState("")
    val restored = remember { mutableStateOf(false) }
    val showSuccessDialog = remember { mutableStateOf(false) }
    var usuarioDuplicado by remember { mutableStateOf("Desconocido") }

    val uidActual = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    val tipoActual = userViewModel.tipo.value?.lowercase().orEmpty()

    var showExitDialog by remember { mutableStateOf(false) }
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val componentActivity = LocalContext.current as? ComponentActivity
    val backOwner = LocalOnBackPressedDispatcherOwner.current
    var pendingExit by remember { mutableStateOf(false) }

    val conLote = (conteoMode == ConteoMode.CON_LOTE)

    // 👇 Centraliza si hay algún diálogo modal abierto que deba cerrar primero con Back
    val anyBlockingDialogOpen =
        showConfirmDialog.value || showDialog || showDialog1 || showDialog2 || showDialogValueQuantityCero || openUbicacionInvalidaDialog.value || showDialogRegistroDuplicado.value || showSavingDialog.value
    // Nota: normalmente NO cierres showSuccessDialog con back;
    // si quieres que Back lo cierre, añádelo aquí: || showSuccessDialog.value

    // 👇 Detecta si hay cambios que NO se han grabado
    val hasDirtyForm: Boolean = run {
        val locDirty = location.value.trim().isNotBlank()
        val skuDirty = sku.value.trim().isNotBlank()
        val qtyDirty = quantity.value.trim().isNotBlank()

        // En SIN_LOTE: ignora los "-" automáticos
        val loteVal = lot.value.trim()
        val vencVal = dateText.value.trim()

        val loteDirty = if (conLote) loteVal.isNotBlank() else false
        val vencDirty = if (conLote) (vencVal.isNotBlank() && vencVal != "-") else false

        locDirty || skuDirty || qtyDirty || loteDirty || vencDirty
    }

    // ¿hay datos sin grabar?
    BackHandler(enabled = !isSaving && !pendingExit) {
        when {
            anyBlockingDialogOpen -> {
                showConfirmDialog.value = false
                showDialog = false
                showDialog1 = false
                showDialog2 = false
                showDialogValueQuantityCero = false
                openUbicacionInvalidaDialog.value = false
                showDialogRegistroDuplicado.value = false

            }

            hasDirtyForm -> {
                showExitDialog = true
            }

            else -> {
                pendingExit = true
            }
        }
    }

    val clienteIdFromUser by userViewModel.clienteId.observeAsState()
    val clienteIdActual: String? =
        if (SelectedClientStore.isSuperuser) SelectedClientStore.selectedClienteId?.takeIf {
            it.isNullOrBlank().not()
        } ?: clienteIdFromUser
        else clienteIdFromUser

    val fotoBytes = remember { mutableStateOf<ByteArray?>(null) }
    val tieneFoto = remember { mutableStateOf(false) }
    val photoUri = remember { mutableStateOf<android.net.Uri?>(null) }

    val tomarFotoLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && photoUri.value != null) {
                // No guardamos Bitmap ni bytes aquí: solo marcamos el indicador
                tieneFoto.value = true
                // Si venías de una versión con bytes, los limpiamos para no duplicar memoria
                fotoBytes.value = null
            } else {
                // Cancelado o fallo: limpiamos indicador/uri
                tieneFoto.value = false
                photoUri.value = null
            }
        }



    LaunchedEffect(Unit) {
        userViewModel.nombre.observeForever { nuevoNombre ->
            if (nuevoNombre.isEmpty()) {
                if (sku.value.isNotBlank() || lot.value.isNotBlank() || quantity.value.isNotBlank() || location.value.isNotBlank() || dateText.value.isNotBlank()) {
                    userViewModel.guardarValoresTemporalmente(
                        sku.value, lot.value, quantity.value, location.value, dateText.value
                    )

                    Log.d("TEMPORAL", "✅ Guardado CORRECTO antes de logout")
                } else {
                    Log.d("TEMPORAL", "⚠️ Evitado guardado de campos vacíos")
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!restored.value) {
            sku.value = userViewModel.tempSku
            lot.value = userViewModel.tempLote
            quantity.value = userViewModel.tempCantidad
            location.value = userViewModel.tempUbicacion
            dateText.value = userViewModel.tempFecha
            restored.value = true

            Log.d("TEMPORAL", "✅ Restauración visual aplicada")
        }
    }

    LaunchedEffect(shouldRequestFocus.value) {
        if (shouldRequestFocus.value) {
            try {
                focusRequester.requestFocus()
            } catch (e: Exception) {
                Log.e("FocusError", "Focus no disponible: ${e.message}")
            }
            shouldRequestFocus.value = false
        }
    }

    LaunchedEffect(usuario) {
        if (usuario.isNotEmpty()) {
            fetchDataFromFirestore(
                db = Firebase.firestore,
                allData = allData,
                usuario = usuario,
                listState = listState,
                localidad = localidad,
                clienteId = clienteIdActual.orEmpty(),
                tipo = tipoActual,
                uid = uidActual
            )
        }
    }

    fun enfocarSkuDespuesDeGrabar() {
        // Limpia cualquier foco previo y muestra el teclado ya en el campo SKU
        focusManager.clearFocus(force = true)
        // pequeño respiro para que Compose cierre diálogos/animaciones
        coroutineScope.launch {
            kotlinx.coroutines.delay(120)
            try {
                focusRequesterSku.requestFocus()
                keyboardController?.show()
            } catch (_: Exception) {
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isVisible) Dp.Unspecified else 0.dp) // 🔥 Oculta visualmente
            .padding(horizontal = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .imePadding()                     // ⬅️ que se mueva con el teclado
                .navigationBarsPadding()
                .padding(bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            val userVM: UserViewModel = viewModel()

            // Location

            OutlinedTextFieldsInputsLocation(
                location,
                showErrorLocation,
                focusRequester = focusRequesterLocation,
                nextFocusRequester = focusRequesterSku,
                shouldRequestFocusAfterClear = shouldRequestFocusAfterClear,
                tempLocationInput = tempLocationInput,
                clienteIdActual = clienteIdActual,        // ← antes: userViewModel.clienteId.value
                localidadActual = localidad
            )

            // SKU

            OutlinedTextFieldsInputsSku(
                sku,
                showErrorSku,
                productoDescripcion,
                productList,
                productMap,
                showProductDialog,
                unidadMedida,
                focusRequester = focusRequesterSku,
                nextFocusRequester = focusRequesterLot,
                shouldRequestFocusAfterClear = shouldRequestFocusAfterClear,
                keyboardController = keyboardController,
                clienteIdActual = clienteIdActual         // ← antes: userViewModel.clienteId.value
            )

            // 📌 FUNCION PARA EL DIALOGO DE PRODUCTOS, DIGASE EL LISTADO DE PRODUCTOS(DESCRIPCIONES)

            ProductSelectionDialog(
                productList = productList,
                productMap = productMap,
                showProductDialog = showProductDialog,
                sku = sku,
                qrCodeContentSku = qrCodeContentSku,
                productoDescripcion = productoDescripcion,
                unidadMedida = unidadMedida,
                focusRequesterLote = focusRequesterLot,
                clienteIdActual = clienteIdActual

            )

            // 📌 CAMPO DE TEXTO PARA EL LOTE
            if (conLote) {
                OutlinedTextFieldsInputsLot(
                    lot,
                    focusRequester = focusRequesterLot,
                    nextFocusRequester = focusRequesterFecha,
                    keyboardController = keyboardController,
                    shouldRequestFocusAfterClear = shouldRequestFocusAfterClear,
                    enable = conLote

                )

                // 📌 CAMPO DE TEXTO PARA LA FECHA

                DatePickerTextField(
                    dateText,
                    focusRequester = focusRequesterFecha,
                    nextFocusRequester = focusRequesterCantidad,
                    enable = conLote

                )

            } else {
                // 🔹 Si no usa lote, asigna los valores por defecto
                LaunchedEffect(Unit) {
                    lot.value = "-"
                    dateText.value = "-"
                }
            }

            // Opcional: forzar “-” cuando se cambia a SIN_LOTE
            LaunchedEffect(conLote) {
                if (!conLote) {
                    lot.value = "-"
                    dateText.value = "-"
                }
            }

            LaunchedEffect(Unit) {
                android.util.Log.d(
                    "CONTEO_MODE_FORM", "conteoMode=${conteoMode.name}, conLote=$conLote"
                )
            }


            // 📌 CAMPO DE TEXTO PARA LA CANTIDAD

            OutlinedTextFieldsInputsQuantity(
                quantity,
                showErrorQuantity,
                errorMessageQuantity,
                lot,
                dateText,
                focusRequester = focusRequesterCantidad,
                keyboardController = LocalSoftwareKeyboardController.current
            )

            fun continuarGuardadoConFoto(fotoUrl: String?) {
                validarRegistroDuplicado(
                    db = firestore,
                    ubicacion = location.value,
                    sku = sku.value,
                    lote = lot.value,
                    cantidad = quantity.value.toDoubleOrNull() ?: 0.0,
                    localidad = localidad,
                    clienteId = clienteIdActual.orEmpty(),
                    onResult = { existeDuplicado, usuarioEncontrado ->
                        if (existeDuplicado) {
                            usuarioDuplicado = usuarioEncontrado ?: "Desconocido"
                            showDialogRegistroDuplicado.value = true
                            isSaving = false
                        } else {
                            Log.d("FotoDebug", "📤 Enviando a Firestore: $fotoUrl")

                            saveToFirestore(
                                firestore,
                                location.value,
                                sku.value,
                                productoDescripcion.value,
                                lot.value,
                                dateText.value,
                                quantity.value.toDoubleOrNull() ?: 0.0,
                                unidadMedida.value,
                                allData,
                                usuario = userViewModel.nombre.value ?: "",
                                coroutineScope,
                                localidad = localidad,
                                userViewModel,
                                showSuccessDialog,
                                listState,
                                fotoUrl = fotoUrl
                            )

                            fetchDataFromFirestore(
                                db = firestore,
                                allData = allData,
                                usuario = usuario,
                                listState = listState,
                                localidad = localidad,
                                clienteId = clienteIdActual.orEmpty(),
                                tipo = tipoActual,
                                uid = uidActual
                            )

                            // limpiar campos
                            sku.value = ""
                            lot.value = ""
                            dateText.value = ""
                            quantity.value = ""
                            productoDescripcion.value = ""
                            unidadMedida.value = ""
                            qrCodeContentSku.value = ""
                            qrCodeContentLot.value = ""
                            //imagenBitmap.value = null
                            userViewModel.limpiarValoresTemporales()
                        }
                        isSaving = false
                        enfocarSkuDespuesDeGrabar()
                    },
                    onError = { e ->                                     // 👈 MEJORADO
                        Log.e("DupCheck", "Error al validar duplicados: ${e.message}", e)
                        Toast.makeText(
                            context,
                            "Error al validar duplicados: ${e.message ?: "ver Logcat"}",
                            Toast.LENGTH_LONG
                        ).show()
                        isSaving = false
                    })
            }

            fun subirImagenAFirebase(
                bytes: ByteArray?,
                uri: android.net.Uri?,
                onUrlLista: (String) -> Unit
            ) {
                val storage = com.google.firebase.storage.FirebaseStorage.getInstance()
                val storageRef = storage.reference
                    .child("fotos_registro/${java.util.UUID.randomUUID()}.jpg")

                val metadata = com.google.firebase.storage.storageMetadata {
                    contentType = "image/jpeg"
                }

                // 🔹 Función interna: borra el archivo temporal del FileProvider
                fun borrarTemporal(u: android.net.Uri) {
                    try {
                        if (u.scheme == "content") {
                            val rows = context.contentResolver.delete(u, null, null)
                            android.util.Log.d("FotoDebug", "Tmp borrado via resolver: rows=$rows")
                        } else {
                            val f = java.io.File(u.path ?: "")
                            if (f.exists()) {
                                val ok = f.delete()
                                android.util.Log.d("FotoDebug", "Tmp borrado via File.delete(): $ok")
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("FotoDebug", "No se pudo borrar tmp: ${e.message}", e)
                    }
                }

                // ✅ Preferir URI: usar putFile
                if (uri != null) {
                    storageRef.putFile(uri, metadata)
                        .addOnSuccessListener {
                            storageRef.downloadUrl.addOnSuccessListener { dl ->
                                android.util.Log.d("FotoDebug", "✅ Subida OK (putFile). URL=$dl")

                                // 🧹 borra el archivo temporal en cache
                                borrarTemporal(uri)

                                onUrlLista(dl.toString())
                            }.addOnFailureListener { e ->
                                android.util.Log.e("FotoDebug", "Error URL: ${e.message}", e)
                                onUrlLista("")
                            }
                        }
                        .addOnFailureListener { e ->
                            val se = e as? com.google.firebase.storage.StorageException
                            android.util.Log.e(
                                "FotoDebug",
                                "❌ putFile falló. code=${se?.errorCode} http=${se?.httpResultCode} msg=${e.message}",
                                e
                            )
                            onUrlLista("")
                        }
                    return
                }

                // Fallback: bytes (por compatibilidad con tu versión anterior)
                if (bytes != null && bytes.isNotEmpty()) {
                    storageRef.putBytes(bytes, metadata)
                        .addOnSuccessListener {
                            storageRef.downloadUrl.addOnSuccessListener { dl ->
                                android.util.Log.d("FotoDebug", "✅ Subida OK (bytes). URL=$dl")
                                onUrlLista(dl.toString())
                            }.addOnFailureListener { e ->
                                android.util.Log.e("FotoDebug", "Error URL: ${e.message}", e)
                                onUrlLista("")
                            }
                        }
                        .addOnFailureListener { e ->
                            val se = e as? com.google.firebase.storage.StorageException
                            android.util.Log.e(
                                "FotoDebug",
                                "❌ putBytes falló. code=${se?.errorCode} http=${se?.httpResultCode} msg=${e.message}",
                                e
                            )
                            onUrlLista("")
                        }
                    return
                }

                android.util.Log.d("FotoDebug", "Sin foto (ni Uri ni bytes)")
                onUrlLista("")
            }


            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val ctx = context
                        val imagesDir = java.io.File(ctx.cacheDir, "images").apply { mkdirs() }
                        val imageFile = java.io.File.createTempFile("foto_", ".jpg", imagesDir)

                        val uri = androidx.core.content.FileProvider.getUriForFile(
                            ctx,
                            ctx.packageName + ".fileprovider",
                            imageFile
                        )
                        photoUri.value = uri

                        tomarFotoLauncher.launch(uri)
                    },
                    enabled = !isSaving && (tieneFoto.value == false),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                    modifier = Modifier.weight(1f).height(40.dp)
                ) {
                    Text("📷 Foto", fontSize = 13.sp, color = Color.White)
                }

                Button(
                    onClick = {
                        if (isSaving) return@Button     // doble-tap guard
                        isSaving = true

                        keyboardController?.hide()

                        coroutineScope.launch {

                            // 🟥 1. Validación: ubicación contra maestro por cliente/localidad
                            val cid = clienteIdActual.orEmpty()
                            val okUbic = UbicacionesRepo.existeUbicacion(
                                clienteId = cid,
                                codigoIngresado = location.value,
                                localidad = localidad
                            )

                            if (!okUbic) {
                                showErrorLocation.value = true
                                delay(150)
                                openUbicacionInvalidaDialog.value = true
                                isSaving = false
                                return@launch
                            }

                            // 🟥 2. Validación general de campos vacíos
                            if (location.value.isEmpty() || sku.value.isEmpty() || quantity.value.isEmpty()) {
                                showErrorLocation.value = location.value.isEmpty()
                                showErrorSku.value = sku.value.isEmpty()
                                showErrorQuantity.value = quantity.value.isEmpty()
                                delay(150)
                                showDialog = true
                                isSaving = false
                                return@launch
                            }

                            // 🟥 3. Validación: "CÓDIGO NO ENCONTRADO"
                            if (location.value == "CÓDIGO NO ENCONTRADO" || sku.value == "CÓDIGO NO ENCONTRADO") {
                                showErrorLocation.value = true
                                showErrorSku.value = true
                                delay(150)
                                showDialog1 = true
                                isSaving = false
                                return@launch
                            }

                            // 🟡 4. Lote vacío o no encontrado → colocar -
                            if (lot.value == "CÓDIGO NO ENCONTRADO" || lot.value.isEmpty()) {
                                lot.value = "-"
                            }

                            // 🟡 5. Fecha vacía → colocar -
                            if (dateText.value.isEmpty()) {
                                dateText.value = "-"
                            }

                            // 🟥 6. Validación: producto no existe o sin descripción válida
                            if (productoDescripcion.value == "Sin descripción" || productoDescripcion.value.isEmpty() || productoDescripcion.value == "Error al obtener datos") {
                                errorMessage2 = "Código No Existe"
                                delay(150)
                                showDialog2 = true
                                isSaving = false
                                return@launch
                            }

                            // 🟥 7. Validación: cantidad igual a 0
                            if (quantity.value == "0" || quantity.value.isEmpty()) {
                                errorMessage = "No Admite cantidades 0"
                                showDialogValueQuantityCero = true
                                showErrorQuantity.value = true
                                isSaving = false
                                return@launch
                            }

                            // ✅ 8. Si todas las validaciones pasaron, mostrar AlertDialog de confirmación
                            delay(150)
                            showConfirmDialog.value = true

                            showErrorLocation.value = false
                            showErrorSku.value = false
                            errorMessage = ""
                            showError1 = false
                            errorMessage1 = ""
                            showError2 = false
                            errorMessage2 = ""
                            showError3 = false
                            errorMessage3 = ""

                        }
                    },

                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF003366), contentColor = Color.White
                    ), modifier = Modifier
                        .weight(1f)
                        .height(40.dp), enabled = canSave
                ) {
                    // 👇 ESTE ES EL CONTENIDO DEL BOTÓN
                    if (isSaving) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp, modifier = Modifier
                                .height(18.dp)
                                .width(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Procesando…", fontSize = 13.sp, color = Color.White)
                    } else {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Grabar", fontSize = 13.sp, color = Color.White)
                    }
                }

                // 🔘 Botón Limpiar
                Button(
                    onClick = {
                        //focusManager.clearFocus()

                        location.value = ""
                        tempLocationInput.value = ""
                        sku.value = ""
                        lot.value = ""
                        dateText.value = ""
                        quantity.value = ""
                        productoDescripcion.value = ""
                        unidadMedida.value = ""
                        qrCodeContentSku.value = ""
                        qrCodeContentLot.value = ""

                        showErrorLocation.value = false
                        showErrorSku.value = false
                        showErrorQuantity.value = false

                        enfocarSkuDespuesDeGrabar()

                    }, //enabled = isSaving,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50), contentColor = Color.White
                    ), modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Limpiar", fontSize = 13.sp, color = Color.White)
                }
            }

            if (tieneFoto.value) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("✅ Foto lista para subir", fontSize = 12.sp, color = Color(0xFF2E7D32))

                    TextButton(onClick = {
                        fotoBytes.value = null
                        tieneFoto.value = false
                        photoUri.value = null
                    }) { Text("Quitar foto") }

                }
            }


            HorizontalDivider(
                thickness = 2.dp,
                color = Color.Gray,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )

            if (showConfirmDialog.value) {
                AlertDialog(
                    onDismissRequest = { showConfirmDialog.value = false },
                    title = { Text("Confirmar Registro") },
                    text = { Text("¿Estás seguro de que deseas grabar este registro?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showConfirmDialog.value = false
                                showSavingDialog.value = true

                                // ✅ ÚNICA llamada de subida (usa Uri si hay, si no usa bytes; si no hay nada, devuelve "")
                                subirImagenAFirebase(
                                    bytes = fotoBytes.value,
                                    uri = photoUri.value
                                ) { urlFoto ->
                                    showSavingDialog.value = false
                                    val finalUrl = urlFoto.takeIf { it.isNotBlank() } // "" -> null
                                    continuarGuardadoConFoto(finalUrl)

                                    // Limpieza tras guardar
                                    fotoBytes.value = null
                                    photoUri.value = null
                                    tieneFoto.value = false
                                }
                            }
                        ) {
                            Text("Sí, grabar", color = Color(0xFF003366))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showConfirmDialog.value = false
                            isSaving = false
                        }) {
                            Text("Cancelar", color = Color.Red)
                        }
                    }
                )
            }


            if (showDialog) {
                AlertDialog(
                    onDismissRequest = {
                        showDialog = true
                    }, // No se cierra al tocar fuera del cuadro
                    title = { Text("Campos Obligatorios Vacios") },
                    text = { Text("Por favor, completa todos los campos requeridos antes de continuar.") },
                    confirmButton = {
                        Button(onClick = { showDialog = false }) {
                            Text("Aceptar")
                        }
                    })
            }
            if (showDialog1) {
                AlertDialog(
                    onDismissRequest = {
                        showDialog1 = true
                    }, // No se cierra al tocar fuera del cuadro
                    title = { Text("Codigo No Encontrado") },
                    text = { Text("Por favor, completa todos los campos requeridos antes de continuar.") },
                    confirmButton = {
                        Button(onClick = { showDialog1 = false }) {
                            Text("Aceptar")
                        }
                    })
            }
            if (showDialog2) {
                AlertDialog(
                    onDismissRequest = {
                        showDialog2 = true
                    },
                    title = { Text("Codigo No Existe") },
                    text = { Text("Por favor, completa todos los campos requeridos antes de continuar.") },
                    confirmButton = {
                        Button(onClick = { showDialog2 = false }) {
                            Text("Aceptar")
                        }
                    })
            }
            if (showDialogValueQuantityCero) {
                AlertDialog(
                    onDismissRequest = {
                        showDialogValueQuantityCero = true
                    }, // No se cierra al tocar fuera del cuadro
                    title = { Text("No Admite cantidades 0") },
                    text = { Text("Por favor, completa todos los campos requeridos antes de continuar.") },
                    confirmButton = {
                        Button(onClick = { showDialogValueQuantityCero = false }) {
                            Text("Aceptar")
                        }
                    })
            }

            if (showDialogRegistroDuplicado.value) {
                AlertDialog(
                    onDismissRequest = { showDialogRegistroDuplicado.value = false },
                    title = { Text("Registro Duplicado") },
                    text = {
                        Text(
                            buildAnnotatedString {
                                append("Ya existe un registro con los mismos datos realizado por: ")
                                withStyle(
                                    style = SpanStyle(
                                        fontWeight = FontWeight.Bold,
                                        fontStyle = FontStyle.Italic,
                                        color = Color.DarkGray,
                                        fontFamily = FontFamily.SansSerif
                                    )
                                ) {
                                    append("\"$usuarioDuplicado\"")
                                }
                                append(". Verifica antes de grabar nuevamente.")
                            }, fontSize = 14.sp // opcional, ajusta tamaño a gusto
                        )
                    },
                    confirmButton = {
                        Button(onClick = { showDialogRegistroDuplicado.value = false }) {
                            Text("Aceptar")
                        }
                    })
            }

            if (showSuccessDialog.value) {
                AlertDialog(
                    onDismissRequest = { showSuccessDialog.value = false },
                    confirmButton = {},
                    title = { Text("✔️ Registro exitoso") },
                    text = { Text("El registro se guardó correctamente.") },
                    properties = DialogProperties(
                        dismissOnBackPress = false, dismissOnClickOutside = false
                    )
                )

                LaunchedEffect(showSuccessDialog.value) {
                    delay(1000) // ✅ o el tiempo que prefieras
                    showSuccessDialog.value = false
                }
            }

            if (showSavingDialog.value) {
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
                                color = Color(0xFF003366), strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Guardando registro...",
                                fontSize = 16.sp,
                                color = Color.Black,
                                fontStyle = FontStyle.Italic
                            )
                        }
                    },
                    containerColor = Color.Black.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(16.dp),
                    tonalElevation = 4.dp
                )
            }

            if (openUbicacionInvalidaDialog.value) {
                AlertDialog(
                    onDismissRequest = { openUbicacionInvalidaDialog.value = false },
                    title = { Text("Ubicación no válida") },
                    text = { Text("La ubicación ingresada no existe en el maestro. Verifícala antes de continuar.") },
                    confirmButton = {
                        TextButton(onClick = { openUbicacionInvalidaDialog.value = false }) {
                            Text("Aceptar")
                        }
                    })
            }
            if (showExitDialog) {
                AlertDialog(
                    onDismissRequest = { showExitDialog = false },
                    title = { Text("Salir del conteo") },
                    text = { Text("Tienes datos sin grabar. ¿Seguro que deseas salir?") },
                    confirmButton = {
                        TextButton(onClick = {
                            showExitDialog = false
                            // Limpieza visual mínima (opcional)
                            focusManager.clearFocus(force = true)
                            keyboardController?.hide()
                            // Dispara la salida (BackHandler ya está deshabilitado por pendingExit)
                            pendingExit = true
                        }) { Text("Salir", color = Color.Red, fontWeight = FontWeight.Bold) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showExitDialog = false }) { Text("Cancelar",color = Color(0xFF003366), fontWeight = FontWeight.Bold) }
                    })
            }
        }
    }
    LaunchedEffect(pendingExit) {
        if (pendingExit) {
            backDispatcher?.onBackPressed()
            pendingExit = false
        }
    }
}

private fun prepararFotoParaSubir(
    context: android.content.Context,
    sourceUri: android.net.Uri,
    maxDimPx: Int = 1600,   // lado mayor máx.
    qualityJpeg: Int = 80   // calidad de compresión
): android.net.Uri? {
    try {
        // 1) Leer solo dimensiones
        val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            android.graphics.BitmapFactory.decodeStream(input, null, bounds)
        }

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return sourceUri

        // 2) Calcular inSampleSize para bajar memoria
        fun calcInSampleSize(w: Int, h: Int, maxDim: Int): Int {
            var sample = 1
            var cw = w
            var ch = h
            while (cw > maxDim || ch > maxDim) {
                cw /= 2; ch /= 2; sample *= 2
            }
            return sample.coerceAtLeast(1)
        }
        val inSample = calcInSampleSize(bounds.outWidth, bounds.outHeight, maxDimPx)

        // 3) Decodificar ya con sample
        val opts = android.graphics.BitmapFactory.Options().apply { inSampleSize = inSample }
        val sampled = context.contentResolver.openInputStream(sourceUri)?.use { input ->
            android.graphics.BitmapFactory.decodeStream(input, null, opts)
        } ?: return sourceUri

        // 4) Asegurar que el lado mayor no pase maxDimPx (por si quedó un poco >)
        val w = sampled.width
        val h = sampled.height
        val maxActual = maxOf(w, h)
        val finalBitmap = if (maxActual > maxDimPx) {
            val scale = maxDimPx.toFloat() / maxActual.toFloat()
            val nw = (w * scale).toInt().coerceAtLeast(1)
            val nh = (h * scale).toInt().coerceAtLeast(1)
            android.graphics.Bitmap.createScaledBitmap(sampled, nw, nh, true).also {
                if (it !== sampled) sampled.recycle()
            }
        } else sampled

        // 5) Escribir JPEG comprimido en un archivo temporal nuevo
        val outDir = java.io.File(context.cacheDir, "images_up").apply { mkdirs() }
        val outFile = java.io.File.createTempFile("up_", ".jpg", outDir)
        java.io.FileOutputStream(outFile).use { fos ->
            finalBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, qualityJpeg, fos)
        }
        finalBitmap.recycle()

        // 6) Devolver un Uri FileProvider para ese archivo
        return androidx.core.content.FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            outFile
        )
    } catch (e: Exception) {
        android.util.Log.w("FotoDebug", "prepararFotoParaSubir falló: ${e.message}", e)
        // Ante cualquier problema, seguimos con el original
        return sourceUri
    }
}
