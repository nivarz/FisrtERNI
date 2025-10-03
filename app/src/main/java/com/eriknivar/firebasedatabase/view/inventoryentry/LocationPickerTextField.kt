package com.eriknivar.firebasedatabase.view.inventoryentry

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.google.firebase.Firebase
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.delay
import androidx.compose.runtime.MutableState
import com.google.firebase.firestore.ktx.firestore
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import androidx.compose.ui.platform.LocalSoftwareKeyboardController


@Composable
fun OutlinedTextFieldsInputsLocation(
    location: MutableState<String>,
    showErrorLocation: MutableState<Boolean>,
    focusRequester: FocusRequester,
    nextFocusRequester: FocusRequester,
    onUserInteraction: () -> Unit = {},
    shouldRequestFocusAfterClear: MutableState<Boolean>,
    tempLocationInput: MutableState<String>,
    clienteIdActual: String?,            // 👈 NUEVO: quién es el cliente
    localidadActual: String?             // 👈 NUEVO: (opcional) filtrar por localidad
) {

    val isZebraScan = remember { mutableStateOf(false) }
    val showUbicacionNoExisteDialog = remember { mutableStateOf(false) }
    val focusRequesterLocation = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val contents = result.contents
        if (contents != null) {
            val scanned = contents.trim().uppercase()

            // 1) Mostrar el código en el campo
            tempLocationInput.value = scanned

            // 2) Validar inmediatamente contra el maestro
            validarUbicacionEnMaestro(
                codigo = scanned,
                clienteIdActual = clienteIdActual,
                localidadActual = localidadActual,
                location = location,
                showErrorLocation = showErrorLocation,
                showUbicacionNoExisteDialog = showUbicacionNoExisteDialog,
                nextFocusRequester = nextFocusRequester,
                keyboardController = keyboardController,
                ocultarTeclado = true
            )

            Log.d("ScanDebug", "Escaneo recibido: $scanned")
        } else {
            Log.d("ScanDebug", "Escaneo cancelado / sin contenido")
        }
    }


    //val qrCodeScannerLocation = remember { QRCodeScanner(qrScanLauncherLocation) }
    val context = LocalContext.current

    // 2) Zebra: además de pasar foco, valida ANTES
    LaunchedEffect(isZebraScan.value) {
        if (isZebraScan.value) {
            val code = tempLocationInput.value
            if (code.length >= 5) {
                validarUbicacionEnMaestro(
                    codigo = code,
                    clienteIdActual = clienteIdActual,
                    localidadActual = localidadActual,
                    location = location,
                    showErrorLocation = showErrorLocation,
                    showUbicacionNoExisteDialog = showUbicacionNoExisteDialog,
                    nextFocusRequester = nextFocusRequester,
                    keyboardController = keyboardController
                )
            }
            try {
                keyboardController?.hide()
                nextFocusRequester.requestFocus()
            } catch (_: Exception) {
            }
            isZebraScan.value = false
        }
    }

    LaunchedEffect(shouldRequestFocusAfterClear.value) {
        if (shouldRequestFocusAfterClear.value) {
            delay(100)
            try {
                focusRequester.requestFocus()
            } catch (e: Exception) {
                Log.e("FocusClear", "Error al pasar foco tras limpiar: ${e.message}")
            }
            shouldRequestFocusAfterClear.value = false
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            modifier = Modifier
                .width(275.dp)
                .height(64.dp)
                .padding(4.dp)
                .focusRequester(focusRequesterLocation)
                // 1) onFocusChanged: baja el umbral
                .onFocusChanged { focusState ->
                    if (!focusState.isFocused && tempLocationInput.value.length >= 5) {
                        validarUbicacionEnMaestro(
                            codigo = tempLocationInput.value,
                            clienteIdActual = clienteIdActual,
                            localidadActual = localidadActual,
                            location = location,
                            showErrorLocation = showErrorLocation,
                            showUbicacionNoExisteDialog = showUbicacionNoExisteDialog,
                            nextFocusRequester = nextFocusRequester,
                            keyboardController = keyboardController
                        )
                    }
                },
            singleLine = true,
            label = { Text(text = "Ubicación") },
            value = tempLocationInput.value,
            onValueChange = { newValue ->
                val clean = newValue.trim().uppercase()
                tempLocationInput.value = clean
                onUserInteraction()

                // 3) onValueChange: detección Zebra más realista
                if (clean.length >= 5 && clean.length - location.value.length >= 3) {
                    isZebraScan.value = true
                }

                showErrorLocation.value = false
            },
            isError = showErrorLocation.value && (location.value.isEmpty() || location.value == "UBICACIÓN NO EXISTE"),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            // 4) KeyboardActions (onNext): baja el umbral
            keyboardActions = KeyboardActions(onNext = {
                if (tempLocationInput.value.length >= 5) {
                    validarUbicacionEnMaestro(
                        codigo = tempLocationInput.value,
                        clienteIdActual = clienteIdActual,
                        localidadActual = localidadActual,
                        location = location,
                        showErrorLocation = showErrorLocation,
                        showUbicacionNoExisteDialog = showUbicacionNoExisteDialog,
                        nextFocusRequester = nextFocusRequester,
                        keyboardController = keyboardController,
                        ocultarTeclado = false
                    )
                }
                if (!showErrorLocation.value) nextFocusRequester.requestFocus()
                else showUbicacionNoExisteDialog.value = true
            }),
            trailingIcon = {
                IconButton(
                    onClick = {
                        val options = ScanOptions().apply {
                            setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES)
                            setPrompt("Escanea el código")
                            setBeepEnabled(false)
                            setOrientationLocked(true) // 👈 bloquea rotación
                            setCaptureActivity(com.eriknivar.firebasedatabase.scan.CapturePortraitActivity::class.java) // 👈 portrait
                        }
                        scanLauncher.launch(options)
                    },
                    modifier = Modifier.size(60.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.QrCodeScanner,
                        contentDescription = "Escanear Código"
                    )
                }
            }
        )

        // 🔵 Ícono de borrar separado (afuera del campo)
        if (tempLocationInput.value.isNotEmpty()) {
            IconButton(
                onClick = {
                    location.value = ""
                    tempLocationInput.value = ""
                },
                modifier = Modifier
                    .size(48.dp)
                    .padding(start = 4.dp)
            ) {
                Icon(
                    modifier = Modifier.size(32.dp),
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = "Borrar texto",
                    tint = Color.Red
                )
            }
        }

        if (showUbicacionNoExisteDialog.value) {
            AlertDialog(
                onDismissRequest = { showUbicacionNoExisteDialog.value = false },
                title = { Text("Ubicación inválida") },
                text = { Text("La ubicación ingresada no existe en el maestro de ubicaciones.") },
                confirmButton = {
                    TextButton(onClick = {
                        showUbicacionNoExisteDialog.value = false
                        focusRequesterLocation.requestFocus()
                    }) { Text("Aceptar") }
                }
            )
        }
    }
}


fun validarUbicacionEnMaestro(
    codigo: String,
    clienteIdActual: String?,
    localidadActual: String?,
    location: MutableState<String>,
    showErrorLocation: MutableState<Boolean>,
    showUbicacionNoExisteDialog: MutableState<Boolean>,
    nextFocusRequester: FocusRequester?,
    keyboardController: SoftwareKeyboardController?,
    ocultarTeclado: Boolean = false,
    onValid: (() -> Unit)? = null,      // ⬅️ se usará para “guardar”
    onInvalid: (() -> Unit)? = null     // ⬅️ feedback adicional
) {
    val TAG = "UBI_VALID"
    val code = codigo.trim().uppercase().replace(Regex("[^A-Z0-9]"), "")
    val cid = clienteIdActual?.trim()?.uppercase()
    val loc = localidadActual?.trim()?.uppercase()

    Log.d(TAG, "-> cid=$cid  loc=$loc  code='$code'")

    // Validaciones rápidas
    if (cid.isNullOrBlank() || code.isBlank() || loc.isNullOrBlank()) {
        showErrorLocation.value = true
        showUbicacionNoExisteDialog.value = true
        onInvalid?.invoke()
        return
    }

    val db = Firebase.firestore

    // Ruta NUEVA
    val docNueva = db.collection("clientes").document(cid)
        .collection("localidades").document(loc)
        .collection("ubicaciones").document(code)

    docNueva.get()
        .addOnSuccessListener { snap ->
            Log.d(
                TAG,
                "exists=${snap.exists()} path=clientes/$cid/localidades/$loc/ubicaciones/$code"
            )
            if (snap.exists()) {
                // ✅ éxito (nueva)
                location.value = code
                showErrorLocation.value = false
                if (ocultarTeclado) keyboardController?.hide()
                try {
                    nextFocusRequester?.requestFocus()
                } catch (_: Exception) {
                }
                onValid?.invoke()                                  // ⬅️ DISPARA GUARDADO
            } else {
                // LEGACY por id
                val docLegacy = db.collection("clientes").document(cid)
                    .collection("ubicaciones").document(code)

                docLegacy.get()
                    .addOnSuccessListener { legacySnap ->
                        Log.d(
                            TAG,
                            "LEGACY exists=${legacySnap.exists()} path=clientes/$cid/ubicaciones/$code"
                        )
                        if (legacySnap.exists()) {
                            // ✅ éxito (legacy)
                            location.value = code
                            showErrorLocation.value = false
                            if (ocultarTeclado) keyboardController?.hide()
                            try {
                                nextFocusRequester?.requestFocus()
                            } catch (_: Exception) {
                            }
                            onValid?.invoke()                      // ⬅️ DISPARA GUARDADO
                        } else {
                            // LEGACY por campo alterno (si lo usaste)
                            db.collection("clientes").document(cid)
                                .collection("ubicaciones")
                                .whereEqualTo("codigo_ubi", code)
                                .limit(1)
                                .get()
                                .addOnSuccessListener { q ->
                                    if (!q.isEmpty) {
                                        location.value = code
                                        showErrorLocation.value = false
                                        if (ocultarTeclado) keyboardController?.hide()
                                        try {
                                            nextFocusRequester?.requestFocus()
                                        } catch (_: Exception) {
                                        }
                                        onValid?.invoke()          // ⬅️ DISPARA GUARDADO
                                    } else {
                                        showErrorLocation.value = true
                                        showUbicacionNoExisteDialog.value = true
                                        onInvalid?.invoke()
                                    }
                                }
                                .addOnFailureListener {
                                    showErrorLocation.value = true
                                    showUbicacionNoExisteDialog.value = true
                                    onInvalid?.invoke()
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "LEGACY get() failed: ${e.message}")
                        showErrorLocation.value = true
                        showUbicacionNoExisteDialog.value = true
                        onInvalid?.invoke()
                    }
            }
        }
        .addOnFailureListener { e ->
            Log.e(TAG, "NUEVA get() failed: ${e.message}")
            showErrorLocation.value = true
            showUbicacionNoExisteDialog.value = true
            onInvalid?.invoke()
        }
}

