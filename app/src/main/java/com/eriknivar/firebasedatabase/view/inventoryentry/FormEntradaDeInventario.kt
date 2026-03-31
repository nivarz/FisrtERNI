package com.eriknivar.firebasedatabase.view.inventoryentry

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
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateListOf
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await
import com.eriknivar.firebasedatabase.view.utility.ImageUtils
import androidx.compose.runtime.saveable.rememberSaveable

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

    // 🧩 Estados para manejar acción sobre registro duplicado
    var showDuplicateActionDialog by remember { mutableStateOf(false) }

    // Guardaremos aquí el doc a actualizar y la cantidad existente
    var duplicateDocRef by remember {
        mutableStateOf<com.google.firebase.firestore.DocumentReference?>(
            null
        )
    }
    var duplicateCantidadActual by remember { mutableDoubleStateOf(0.0) }

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

    // states
    // states
    val photoUri = userViewModel.photoUriTemporal
    val tieneFoto = userViewModel.tieneFotoTemporal

    // launcher
    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { ok ->
        if (ok && !photoUri.isNullOrBlank()) {
            userViewModel.setPhotoTemporal(photoUri)
            Log.d("FotoDebug", "✅ TakePicture OK: $photoUri")
        } else {
            Log.d("FotoDebug", "❌ TakePicture cancelada/falló, limpiando Uri temporal")
            userViewModel.clearPhotoTemporal()
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

    // ANCLA: // estados de UI (debajo de tempLocationInput)
    val showLocationDialog = remember { mutableStateOf(false) }
    val ubicacionesLista = remember { mutableStateListOf<String>() }

    // 🔁 Función reutilizable para recargar las ubicaciones desde Firestore
    suspend fun recargarUbicacionesLista(cid: String, loc: String) {
        ubicacionesLista.clear()
        if (cid.isBlank() || loc.isBlank()) return

        val db = FirebaseFirestore.getInstance()
        val codigos = mutableSetOf<String>()

        fun extraerCodigosFromSnap(snap: QuerySnapshot) {
            snap.documents.forEach { d ->
                val byField = d.getString("codigo_ubi") ?: d.getString("codigoUbicacion")
                ?: d.getString("codigo") ?: d.getString("code") ?: d.getString("ubicacion")
                ?: d.getString("location")

                val byId = d.id.substringAfterLast('_').ifBlank { d.id }

                val code = (byField ?: byId).trim().uppercase()
                if (code.isNotEmpty()) codigos += code
            }
        }

        try {
            // 1) Ruta nueva: clientes/{cid}/localidades/{loc}/ubicaciones/*
            extraerCodigosFromSnap(
                db.collection("clientes").document(cid).collection("localidades").document(loc)
                    .collection("ubicaciones").get().await()
            )

            // 2) Ruta nueva sin subcolección por loc: clientes/{cid}/ubicaciones where localidad==loc
            extraerCodigosFromSnap(
                db.collection("clientes").document(cid).collection("ubicaciones")
                    .whereEqualTo("localidad", loc).get().await()
            )

            // 3) Legacy global: ubicaciones where clienteId==cid and localidad==loc
            extraerCodigosFromSnap(
                db.collection("ubicaciones").whereEqualTo("clienteId", cid)
                    .whereEqualTo("localidad", loc).get().await()
            )
        } catch (e: Exception) {
            Log.e("UbicDlg", "Error cargando ubicaciones ($cid/$loc)", e)
        }

        ubicacionesLista.addAll(codigos.sorted())
    }

    // Carga inicial al entrar / cambiar cliente o localidad
    LaunchedEffect(clienteIdActual, localidad) {
        val cid = clienteIdActual.orEmpty()
        val loc = localidad
        recargarUbicacionesLista(cid, loc)
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

            /*
            // IMPORTANTE arriba del archivo:
            // import com.google.firebase.crashlytics.FirebaseCrashlytics

            Button(
                onClick = {
                    FirebaseCrashlytics.getInstance()
                        .log("Prueba de Crashlytics desde el botón secreto")
                    throw RuntimeException("Test Crashlytics Nivar - botón secreto")
                }
            ) {
                Text("Forzar Crash")
            }
            */

            // Location

            OutlinedTextFieldsInputsLocation(
                location,
                showErrorLocation,
                focusRequester = focusRequesterLocation,
                nextFocusRequester = focusRequesterSku,
                shouldRequestFocusAfterClear = shouldRequestFocusAfterClear,
                tempLocationInput = tempLocationInput,
                clienteIdActual = clienteIdActual,        // ← antes: userViewModel.clienteId.value
                localidadActual = localidad,
                onSearchClick = {
                    val cid = clienteIdActual.orEmpty()
                    val loc = localidad
                    coroutineScope.launch {
                        recargarUbicacionesLista(cid, loc)   // 🔄 refresca maestro
                        showLocationDialog.value = true      // y luego abre el diálogo
                    }
                })

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

            // ANCLA: reemplazo completo de continuarGuardadoConFoto(...)
            fun continuarGuardadoConFoto(hadPhoto: Boolean, uriLocal: String?, fotoUrl: String?) {

                // 1) Forzar estado real de foto según la URI local
                val hadPhotoFinal = !uriLocal.isNullOrBlank()
                val uriLog = uriLocal ?: photoUri

                validarRegistroDuplicado(
                    db = firestore,
                    ubicacion = location.value,
                    sku = sku.value,
                    lote = lot.value,
                    cantidad = quantity.value.toDoubleOrNull() ?: 0.0,
                    localidad = localidad,
                    clienteId = clienteIdActual.orEmpty(),
                    onResult = { existeDuplicado, usuarioEncontrado, docRef, cantidadExistente ->

                        if (existeDuplicado && docRef != null && cantidadExistente != null) {
                            // 👉 Hay duplicado y sabemos cuál doc actualizar
                            usuarioDuplicado = usuarioEncontrado ?: "Desconocido"

                            // Usuario actualmente logueado
                            val nombreActual = userViewModel.nombre.value ?: ""
                            val esMismoUsuario = usuarioDuplicado == nombreActual

                            if (esMismoUsuario) {
                                // ✅ Mismo usuario → mostrar opciones Sumar / Reemplazar / Cancelar
                                duplicateDocRef = docRef
                                duplicateCantidadActual = cantidadExistente
                                showDuplicateActionDialog = true   // abrimos el diálogo con opciones
                                // NO grabamos todavía, esperamos la decisión del usuario
                            } else {
                                // 🚫 Otro usuario hizo ese conteo → no permitimos grabar
                                showDialogRegistroDuplicado.value = true
                                isSaving = false
                                // aquí simplemente salimos del callback sin llamar a saveToFirestore
                            }

                        } else if (!existeDuplicado) {
                            // 👉 Sin duplicado: comportamiento normal (tu código tal cual)
                            Log.d("FotoDebug", "📤 Enviando a Firestore: fotoUrlRemota=$fotoUrl")
                            Log.d(
                                "FotoDebug",
                                "🚀 Llamando saveToFirestore hadPhoto=$hadPhotoFinal, uriLocal=$uriLog"
                            )

                            saveToFirestore(
                                db = firestore,
                                location = location.value,
                                sku = sku.value,
                                description = productoDescripcion.value,
                                lote = lot.value,
                                expirationDate = dateText.value,
                                quantity = quantity.value.toDoubleOrNull() ?: 0.0,
                                unidadMedida = unidadMedida.value,
                                allData = allData,
                                usuario = userViewModel.nombre.value ?: "",
                                coroutineScope = coroutineScope,
                                localidad = localidad,
                                userViewModel = userViewModel,
                                showSuccessDialog = showSuccessDialog,
                                listState = listState,
                                fotoUrl = null,                          // la sube el worker
                                hadPhoto = hadPhotoFinal,
                                fotoUriLocal = uriLocal?.trim(),
                                appContext = context.applicationContext
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

                            isSaving = false
                            enfocarSkuDespuesDeGrabar()
                        } else {
                            // Caso raro: dijo que hay duplicado, pero no tenemos docRef/cantidad
                            usuarioDuplicado = usuarioEncontrado ?: "Desconocido"
                            showDialogRegistroDuplicado.value = true
                            isSaving = false
                        }
                    },
                    onError = { e ->
                        Log.e("DupCheck", "Error al validar duplicados: ${e.message}", e)
                        Toast.makeText(
                            context,
                            "Error al validar duplicados: ${e.message ?: "ver Logcat"}",
                            Toast.LENGTH_LONG
                        ).show()
                        isSaving = false
                    })
            }

            // Libera loading y continúa con o sin URL
            fun finishSaving(hadPhoto: Boolean, uriLocal: String?) {
                showSavingDialog.value = false
                isSaving = false
                continuarGuardadoConFoto(hadPhoto, uriLocal, fotoUrl = null)
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
                        //val ctx = context
                        // val imagesDir = File(ctx.cacheDir, "images").apply { mkdirs() }
                        //val imageFile = File.createTempFile("foto_", ".jpg", imagesDir)

                        // ANCLA BOTON-FOTO (onClick)
                        val tmpUri = ImageUtils.createTempImageUri(context)
                        userViewModel.setPhotoTemporal(tmpUri.toString())
                        takePictureLauncher.launch(tmpUri)

                    },
                    enabled = !isSaving && !tieneFoto,
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

                            // 🟥 1) Validación de ubicación (OFFLINE-FIRST con tri-estado)
                            val cid = clienteIdActual.orEmpty()
                            val ubicCheck: Boolean? = UbicacionesRepo.existeUbicacionOfflineFirst(
                                clienteId = cid,
                                localidad = localidad,
                                codigoIngresado = location.value
                            )

                            when (ubicCheck) {
                                true -> { /* OK, sigue */ }

                                false -> {
                                    showErrorLocation.value = true
                                    openUbicacionInvalidaDialog.value = true
                                    isSaving = false
                                    return@launch
                                }

                                null -> {
                                    Toast.makeText(
                                        context,
                                        "Sin conexión. Se validará al sincronizar.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    showErrorLocation.value = false
                                }
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

                            // 🔐 6. Refrescar SIEMPRE la descripción desde el maestro según el SKU actual
                            try {
                                val cidMaestro = clienteIdActual.orEmpty()
                                val codigoActual = sku.value.trim()

                                if (cidMaestro.isNotEmpty() && codigoActual.isNotEmpty()) {
                                    val doc = firestore.collection("clientes")
                                        .document(cidMaestro)
                                        .collection("productos")
                                        .document(codigoActual)
                                        .get()
                                        .await()

                                    if (!doc.exists()) {
                                        // SKU no existe en el maestro → marcamos como sin descripción
                                        productoDescripcion.value = "Sin descripción"
                                    } else {
                                        val descMaestro = (doc.getString("descripcion") ?: "").trim()
                                        productoDescripcion.value =
                                            descMaestro.ifBlank { "Sin descripción" }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("FormEntrada", "Error refrescando descripción desde maestro", e)
                                // si falla, seguimos con el valor que ya tuviera productoDescripcion
                            }

                            // 🟥 7. Validación: producto no existe o sin descripción válida (ya después de refrescar)
                            if (
                                productoDescripcion.value == "Sin descripción" ||
                                productoDescripcion.value.isEmpty() ||
                                productoDescripcion.value == "Error al obtener datos"
                            ) {
                                delay(150)
                                showDialog2 = true
                                isSaving = false
                                return@launch
                            }

                            // 🟥 8. Validación: cantidad igual a 0
                            if (quantity.value == "0" || quantity.value.isEmpty()) {
                                showDialogValueQuantityCero = true
                                showErrorQuantity.value = true
                                isSaving = false
                                return@launch
                            }

                            // ✅ 9. Si todas las validaciones pasaron, mostrar AlertDialog de confirmación
                            delay(150)
                            showConfirmDialog.value = true

                            showErrorLocation.value = false
                            showErrorSku.value = false

                        }
                    }, colors = ButtonDefaults.buttonColors(
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

            if (tieneFoto) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("✅ Foto lista para subir", fontSize = 12.sp, color = Color(0xFF2E7D32))

                    TextButton(onClick = {
                        userViewModel.clearPhotoTemporal()
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

                                // ANCLA donde falla ahora
                                val uriLocal = photoUri
                                val hadPhoto = !uriLocal.isNullOrBlank()
                                finishSaving(hadPhoto, uriLocal)   // <- pasamos el String? correcto

                                // Limpieza y aviso (igual que ya tenías)
                                if (hadPhoto) {
                                    userViewModel.clearPhotoTemporal()
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

            // 🔁 Nuevo diálogo para decidir qué hacer con el duplicado
            if (showDuplicateActionDialog && duplicateDocRef != null) {
                AlertDialog(onDismissRequest = {
                    // No cerramos solo tocando fuera
                }, title = { Text("Registro duplicado") }, text = {
                    Text(
                        "Ya existe un registro con la misma Localidad + Ubicación + SKU + Lote.\n\n" + "¿Qué deseas hacer con la cantidad?"
                    )
                },
                    confirmButton = {
                        // 🔒 Flag local para evitar múltiples clics mientras se actualiza
                        var isUpdatingDuplicate by remember { mutableStateOf(false) }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 🧮 SUMAR CANTIDADES
                            TextButton(
                                onClick = {
                                    if (isUpdatingDuplicate) return@TextButton   // evita doble tap

                                    val cantidadNueva =
                                        quantity.value.replace(",", ".").toDoubleOrNull() ?: 0.0
                                    val total = duplicateCantidadActual + cantidadNueva

                                    coroutineScope.launch {
                                        isUpdatingDuplicate = true
                                        try {
                                            duplicateDocRef?.update("cantidad", total)?.await()

                                            Toast.makeText(
                                                context,
                                                "Cantidad actualizada (sumada).",
                                                Toast.LENGTH_SHORT
                                            ).show()

                                            // refrescamos lista
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

                                            // limpiamos campos como en el grabado normal
                                            sku.value = ""
                                            lot.value = ""
                                            dateText.value = ""
                                            quantity.value = ""
                                            productoDescripcion.value = ""
                                            unidadMedida.value = ""
                                            qrCodeContentSku.value = ""
                                            qrCodeContentLot.value = ""
                                            userViewModel.limpiarValoresTemporales()

                                        } catch (e: Exception) {
                                            Toast.makeText(
                                                context,
                                                "Error al actualizar: ${e.message}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        } finally {
                                            isUpdatingDuplicate = false
                                            showDuplicateActionDialog = false
                                            isSaving = false
                                            enfocarSkuDespuesDeGrabar()
                                        }
                                    }
                                },
                                enabled = !isUpdatingDuplicate,
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = Color(0xFF2E7D32) // verde
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AddCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sumar")
                            }

                            // 🔁 REEMPLAZAR CANTIDAD
                            TextButton(
                                onClick = {
                                    if (isUpdatingDuplicate) return@TextButton   // evita doble tap

                                    val cantidadNueva =
                                        quantity.value.replace(",", ".").toDoubleOrNull() ?: 0.0

                                    coroutineScope.launch {
                                        isUpdatingDuplicate = true
                                        try {
                                            duplicateDocRef?.update("cantidad", cantidadNueva)?.await()

                                            Toast.makeText(
                                                context,
                                                "Cantidad reemplazada.",
                                                Toast.LENGTH_SHORT
                                            ).show()

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

                                            sku.value = ""
                                            lot.value = ""
                                            dateText.value = ""
                                            quantity.value = ""
                                            productoDescripcion.value = ""
                                            unidadMedida.value = ""
                                            qrCodeContentSku.value = ""
                                            qrCodeContentLot.value = ""
                                            userViewModel.limpiarValoresTemporales()

                                        } catch (e: Exception) {
                                            Toast.makeText(
                                                context,
                                                "Error al actualizar: ${e.message}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        } finally {
                                            isUpdatingDuplicate = false
                                            showDuplicateActionDialog = false
                                            isSaving = false
                                            enfocarSkuDespuesDeGrabar()
                                        }
                                    }
                                },
                                enabled = !isUpdatingDuplicate,
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = Color(0xFF1565C0) // azulito para diferenciar
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reemplazar")
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showDuplicateActionDialog = false
                                isSaving = false
                            }
                        ) {
                            Text("Cancelar", color = Color.Red)
                        }
                    }
                )
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

            // ANCLA: // diálogos al final
            if (showLocationDialog.value) {
                LocationSelectionDialog(locations = ubicacionesLista, onSelect = { code ->
                    val limpio = code.trim().uppercase()

                    // 👇 lo que ve el TextField
                    tempLocationInput.value = limpio

                    // 👇 tu valor “oficial” validado
                    location.value = limpio

                    showErrorLocation.value = false
                    shouldRequestFocusAfterClear.value = false
                    focusRequesterSku.requestFocus()   // pasa al SKU
                    showLocationDialog.value = false
                }, onDismiss = { showLocationDialog.value = false })
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