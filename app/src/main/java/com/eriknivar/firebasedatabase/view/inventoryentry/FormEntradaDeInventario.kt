package com.eriknivar.firebasedatabase.view.inventoryentry

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import com.eriknivar.firebasedatabase.network.SelectedClientStore
import com.eriknivar.firebasedatabase.data.UbicacionesRepo
import com.eriknivar.firebasedatabase.view.common.ConteoMode
import com.google.firebase.auth.FirebaseAuth
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.core.content.FileProvider.getUriForFile
import com.google.firebase.storage.StorageReference
import java.io.File
import java.io.FileOutputStream

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
    val showSavingDialog = remember { mutableStateOf(false) }

    var isSaving by remember { mutableStateOf(false) }
    // El botón “Grabar” solo se habilita si NO está guardando y hay datos mínimos válidos
    val canSave =
        !isSaving && location.value.isNotBlank() && sku.value.isNotBlank() && (quantity.value.replace(
            ",", "."
        ).toDoubleOrNull()?.let { it > 0 } == true)

    val context = LocalContext.current
    val firestore = Firebase.firestore

    var showDialog by remember { mutableStateOf(false) } // Estado para mostrar el cuadro de diálogo
    var showDialog1 by remember { mutableStateOf(false) }
    var showDialog2 by remember { mutableStateOf(false) }


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
            it.isBlank().not()
        } ?: clienteIdFromUser
        else clienteIdFromUser

    val fotoBytes = remember { mutableStateOf<ByteArray?>(null) }
    val tieneFoto = remember { mutableStateOf(false) }
    val photoUri = remember { mutableStateOf<Uri?>(null) }

    val tomarFotoLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && photoUri.value != null) {
                tieneFoto.value = true
                fotoBytes.value = null // por si quedó algo viejo
            } else {
                tieneFoto.value = false
                photoUri.value = null
                Toast.makeText(context, "Captura cancelada", Toast.LENGTH_SHORT).show()
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
        if (usuario.isEmpty()) {
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

    fun enfocarSkuDespuesDeGrabar() {
        // Limpia cualquier foco previo y muestra el teclado ya en el campo SKU
        focusManager.clearFocus(force = true)
        // pequeño respiro para que Compose cierre diálogos/animaciones
        coroutineScope.launch {
            delay(120)
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
                    enable = true

                )

                // 📌 CAMPO DE TEXTO PARA LA FECHA

                DatePickerTextField(
                    dateText,
                    focusRequester = focusRequesterFecha,
                    nextFocusRequester = focusRequesterCantidad,
                    enable = true

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
                Log.d(
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
                bytes: ByteArray?, uri: Uri?, onUrlLista: (String) -> Unit
            ) {
                val storage = com.google.firebase.storage.FirebaseStorage.getInstance()
                val storageRef =
                    storage.reference.child("fotos_registro/${java.util.UUID.randomUUID()}.jpg")

                // Metadatos con cacheControl (mejora muchísimo la carga posterior)
                val metadata = com.google.firebase.storage.StorageMetadata.Builder()
                    .setContentType("image/jpeg")
                    .setCacheControl("public, max-age=31536000, immutable").build()

                // 🔹 Borra el archivo temporal (si es del FileProvider o un File path)
                fun borrarTemporal(u: Uri) {
                    try {
                        if (u.scheme == "content") {
                            val rows = context.contentResolver.delete(u, null, null)
                            Log.d("FotoDebug", "Tmp borrado via resolver: rows=$rows")
                        } else {
                            val f = File(u.path ?: "")
                            if (f.exists()) {
                                val ok = f.delete()
                                Log.d("FotoDebug", "Tmp borrado via File.delete(): $ok")
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("FotoDebug", "No se pudo borrar tmp: ${e.message}", e)
                    }
                }

                if (uri != null) {
                    // ⬇️ Optimiza (1600px máx., 80% JPEG) y sube con putFile
                    val optimizedUri: Uri = prepararFotoParaSubir(context, uri)

                    storageRef.putFile(optimizedUri, metadata).addOnSuccessListener {
                            getDownloadUrlWithRetry(
                                ref = storageRef,
                                attempts = 2,
                                delayMs = 300,
                                onSuccess = { dl ->
                                    Log.d("FotoDebug", "✅ Subida OK (putFile). URL=$dl")

                                    // 🧹 limpieza de temporales
                                    borrarTemporal(optimizedUri)
                                    if (optimizedUri != uri) borrarTemporal(uri)

                                    onUrlLista(dl.toString())
                                },
                                onError = { e ->
                                    Log.e(
                                        "FotoDebug",
                                        "❌ downloadUrl falló tras reintentos: ${e.message}",
                                        e
                                    )

                                    // 🧹 igual limpiamos temporales
                                    borrarTemporal(optimizedUri)
                                    if (optimizedUri != uri) borrarTemporal(uri)

                                    onUrlLista("")
                                })
                        }.addOnFailureListener { e ->
                            val se = e as? com.google.firebase.storage.StorageException
                            Log.e(
                                "FotoDebug",
                                "❌ putFile falló. code=${se?.errorCode} http=${se?.httpResultCode} msg=${e.message}",
                                e
                            )

                            // 🧹 limpieza de temporales también en fallo
                            borrarTemporal(optimizedUri)
                            if (optimizedUri != uri) borrarTemporal(uri)

                            onUrlLista("")
                        }
                    return
                }

                // Fallback: subir desde bytes (compatibilidad)
                if (bytes != null && bytes.isNotEmpty()) {
                    storageRef.putBytes(bytes, metadata).addOnSuccessListener {
                            getDownloadUrlWithRetry(
                                ref = storageRef,
                                attempts = 2,
                                delayMs = 300,
                                onSuccess = { dl ->
                                    Log.d("FotoDebug", "✅ Subida OK (bytes). URL=$dl")
                                    onUrlLista(dl.toString())
                                },
                                onError = { e ->
                                    Log.e(
                                        "FotoDebug",
                                        "❌ downloadUrl (bytes) tras reintentos: ${e.message}",
                                        e
                                    )
                                    onUrlLista("")
                                })
                        }.addOnFailureListener { e ->
                            val se = e as? com.google.firebase.storage.StorageException
                            Log.e(
                                "FotoDebug",
                                "❌ putBytes falló. code=${se?.errorCode} http=${se?.httpResultCode} msg=${e.message}",
                                e
                            )
                            onUrlLista("")
                        }
                    return
                }

                Log.d("FotoDebug", "Sin foto (ni Uri ni bytes)")
                onUrlLista("")
            }

            // Libera loading y continúa con o sin URL
            fun finishSaving(urlFoto: String?) {
                showSavingDialog.value = false
                isSaving = false
                continuarGuardadoConFoto(urlFoto)
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
                        val imagesDir = File(ctx.cacheDir, "images").apply { mkdirs() }
                        val imageFile = File.createTempFile("foto_", ".jpg", imagesDir)

                        val uri = getUriForFile(
                            ctx, ctx.packageName + ".fileprovider", imageFile
                        )
                        photoUri.value = uri

                        tomarFotoLauncher.launch(uri)
                    },
                    enabled = !isSaving && !tieneFoto.value,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
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
                                delay(150)
                                showDialog2 = true
                                isSaving = false
                                return@launch
                            }

                            // 🟥 7. Validación: cantidad igual a 0
                            if (quantity.value == "0" || quantity.value.isEmpty()) {
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
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

                                val uri = photoUri.value
                                val hadPhoto = tieneFoto.value

                                if (hadPhoto && uri != null) {
                                    subirImagenAFirebase(
                                        bytes = fotoBytes.value, uri = uri
                                    ) { url ->
                                        // Limpieza SIEMPRE
                                        fotoBytes.value = null
                                        photoUri.value = null
                                        tieneFoto.value = false

                                        // si subió ok → url no vacía; si falló → null
                                        finishSaving(url.ifEmpty { null })
                                    }
                                } else {
                                    // no hay foto → guardado normal
                                    finishSaving(null)
                                }
                            }) {
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
                    })
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
                        TextButton(onClick = { showExitDialog = false }) {
                            Text(
                                "Cancelar", color = Color(0xFF003366), fontWeight = FontWeight.Bold
                            )
                        }
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


private fun applyExifRotation(
    context: Context, sourceUri: Uri, bmp: Bitmap
): Bitmap {
    return try {
        context.contentResolver.openInputStream(sourceUri)?.use { ins ->
            val exif = ExifInterface(ins)
            val rotation = when (exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
            )) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            if (rotation != 0f) {
                val m = Matrix().apply { postRotate(rotation) }
                Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
            } else bmp
        } ?: bmp
    } catch (_: Exception) {
        bmp
    }
}

private fun prepararFotoParaSubir(
    context: Context,
    sourceUri: Uri,
    maxDimPx: Int = 1600,
    qualityStart: Int = 85,
    targetMaxBytes: Long = 1_200_000L, // ~1.2 MB
    qualityFloor: Int = 60
): Uri {
    var decoded: Bitmap? = null
    var oriented: Bitmap? = null
    var scaled: Bitmap? = null

    try {
        val resolver = context.contentResolver

        // 1) Sólo bounds
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(sourceUri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

        val w = bounds.outWidth
        val h = bounds.outHeight
        if (w <= 0 || h <= 0) return sourceUri

        // 2) Sample + config ahorradora
        var inSample = 1
        run {
            val halfW = w / 2
            val halfH = h / 2
            while ((halfW / inSample) >= maxDimPx || (halfH / inSample) >= maxDimPx) {
                inSample *= 2
            }
        }
        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = inSample.coerceAtLeast(1)
            inPreferredConfig = Bitmap.Config.RGB_565
            inDither = true
        }

        decoded = resolver.openInputStream(sourceUri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOpts)
        } ?: return sourceUri

        // 3) Rotación EXIF
        oriented = applyExifRotation(context, sourceUri, decoded)
        if (oriented !== decoded) {
            decoded.recycle(); decoded = null
        }

        // 4) Escalado final
        val cur = oriented
        val maxCur = maxOf(cur.width, cur.height)
        scaled = if (maxCur > maxDimPx) {
            val f = maxDimPx.toFloat() / maxCur.toFloat()
            val m = Matrix().apply { postScale(f, f) }
            Bitmap.createBitmap(cur, 0, 0, cur.width, cur.height, m, true)
        } else {
            cur
        }
        if (scaled !== oriented) {
            oriented.recycle(); oriented = null
        }

        // 5) Escribir JPEG con control de tamaño (un solo archivo)
        val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
        val outFile = File.createTempFile("upload_", ".jpg", imagesDir)

        var q = qualityStart.coerceIn(qualityFloor, 100)
        fun write(qty: Int) {
            FileOutputStream(outFile, false).use { fos ->
                scaled.compress(Bitmap.CompressFormat.JPEG, qty, fos)
                fos.flush()
            }
        }

        write(q)
        // Si quedó grande, bajamos calidad en pasos
        while (outFile.length() > targetMaxBytes && q > qualityFloor) {
            q = (q - 10).coerceAtLeast(qualityFloor)
            write(q)
        }

        // 6) Devolver Uri del FileProvider
        return getUriForFile(context, "${context.packageName}.fileprovider", outFile)
    } catch (oom: OutOfMemoryError) {
        Log.e("FotoDebug", "OOM al preparar foto: ${oom.message}")
        Toast.makeText(context, "Memoria insuficiente al procesar la foto", Toast.LENGTH_LONG).show()
        return sourceUri // seguimos sin bloquear el flujo
    } catch (e: Exception) {
        Log.w("FotoDebug", "prepararFotoParaSubir falló: ${e.message}", e)
        return sourceUri
    } finally {
        try { scaled?.recycle() } catch (_: Exception) {}
        try { oriented?.recycle() } catch (_: Exception) {}
        try { decoded?.recycle() } catch (_: Exception) {}
    }
}


// Helper: obtiene la URL con reintentos suaves y backoff.
private fun getDownloadUrlWithRetry(
    ref: StorageReference, attempts: Int = 2,          // reintentos extra (total = 1 + attempts)
    delayMs: Long = 300,        // backoff inicial
    onSuccess: (Uri) -> Unit, onError: (Exception) -> Unit
) {
    ref.downloadUrl.addOnSuccessListener { onSuccess(it) }.addOnFailureListener { e ->
            if (attempts > 0) {
                Handler(Looper.getMainLooper()).postDelayed(
                    { getDownloadUrlWithRetry(ref, attempts - 1, delayMs * 2, onSuccess, onError) },
                    delayMs
                )
            } else {
                onError(e)
            }
        }
}