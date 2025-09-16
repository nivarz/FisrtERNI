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
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.coroutines.delay
import com.google.firebase.firestore.ktx.firestore

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
    val qrCodeContentLocation = remember { mutableStateOf("") }
    val wasScanned = remember { mutableStateOf(false) }
    val isZebraScan = remember { mutableStateOf(false) }
    val showUbicacionNoExisteDialog = remember { mutableStateOf(false) }
    val focusRequesterLocation = remember { FocusRequester() }

    val qrScanLauncherLocation =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            val intentResult = IntentIntegrator.parseActivityResult(result.resultCode, data)
            if (intentResult != null) {
                qrCodeContentLocation.value = intentResult.contents ?: "Código No Encontrado"
                wasScanned.value = true
                Log.d("ScanDebug", "Escaneo recibido: ${qrCodeContentLocation.value}")
            }
        }

    val qrCodeScannerLocation = remember { QRCodeScanner(qrScanLauncherLocation) }
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // ✅ Escaneo desde launcher (cámara)
    LaunchedEffect(qrCodeContentLocation.value, wasScanned.value) {
        if (wasScanned.value) {
            val scannedValue = qrCodeContentLocation.value.trim().uppercase()
            if (scannedValue.isNotBlank()) {
                tempLocationInput.value = scannedValue
                validarUbicacionEnMaestro(
                    codigo = scannedValue,
                    clienteIdActual = clienteIdActual,
                    localidadActual = localidadActual,
                    location = location,
                    showErrorLocation = showErrorLocation,
                    showUbicacionNoExisteDialog = showUbicacionNoExisteDialog,
                    nextFocusRequester = nextFocusRequester,
                    keyboardController = keyboardController
                )
            }
            wasScanned.value = false
        }
    }

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

                wasScanned.value = false
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
                    onClick = { qrCodeScannerLocation.startQRCodeScanner(context as android.app.Activity) },
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
                    qrCodeContentLocation.value = ""
                    tempLocationInput.value = ""
                },
                modifier = Modifier
                    .size(48.dp)
                    .padding(start = 4.dp)
            ) {
                Icon(
                    modifier = Modifier.size(48.dp),
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
    localidadActual: String?, // se ignora por ahora
    location: MutableState<String>,
    showErrorLocation: MutableState<Boolean>,
    showUbicacionNoExisteDialog: MutableState<Boolean>,
    nextFocusRequester: FocusRequester,
    keyboardController: SoftwareKeyboardController?,
    ocultarTeclado: Boolean = false
) {
    val codigoLimpio = codigo.uppercase().replace(Regex("[^A-Z0-9]"), "")
    val cid = clienteIdActual?.trim()?.uppercase()

    Log.d("UBI_VALID", "-> cid=$cid  code(raw)='$codigo'  code(clean)='$codigoLimpio'")

    if (cid.isNullOrBlank() || codigoLimpio.isBlank()) {
        location.value = ""
        showErrorLocation.value = true
        showUbicacionNoExisteDialog.value = true
        return
    }

    val col = Firebase.firestore
        .collection("clientes").document(cid)
        .collection("ubicaciones")

    // ÚNICO MATCH por codigo_ubi (ruta ya restringe por cliente)
    col.whereEqualTo("codigo_ubi", codigoLimpio)
        .limit(1)
        .get()
        .addOnSuccessListener { snap ->
            Log.d("UBI_VALID", "size=${snap.size()} path=clientes/$cid/ubicaciones")
            if (!snap.isEmpty) {
                location.value = codigoLimpio
                showErrorLocation.value = false
                if (ocultarTeclado) keyboardController?.hide()
                try { nextFocusRequester.requestFocus() } catch (_: Exception) {}
            } else {
                location.value = ""
                showErrorLocation.value = true
                showUbicacionNoExisteDialog.value = true
            }
        }
        .addOnFailureListener { e ->
            Log.e("UBI_VALID", "error: ${e.message}", e)
            location.value = ""
            showErrorLocation.value = true
            showUbicacionNoExisteDialog.value = true
        }
}
