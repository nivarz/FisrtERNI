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
import androidx.compose.material.icons.filled.Clear
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
import com.google.firebase.firestore.firestore
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun OutlinedTextFieldsInputsLocation(
    location: MutableState<String>,
    showErrorLocation: MutableState<Boolean>,
    focusRequester: FocusRequester,
    nextFocusRequester: FocusRequester,
    onUserInteraction: () -> Unit = {},
    shouldRequestFocusAfterClear: MutableState<Boolean>,
    tempLocationInput: MutableState<String>
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
            val scannedValue = qrCodeContentLocation.value.uppercase()

            if (scannedValue.isNotBlank()) {
                tempLocationInput.value = scannedValue

                validarUbicacionEnMaestro(
                    scannedValue, location, showErrorLocation, showUbicacionNoExisteDialog, nextFocusRequester, keyboardController
                )
            }

            wasScanned.value = false
        }
    }

    // ✅ Escaneo directo tipo Zebra (entrada rápida)
    LaunchedEffect(isZebraScan.value) {
        if (isZebraScan.value) {
            delay(100) // Pequeña pausa para asegurar estabilidad
            try {
                keyboardController?.hide()
                nextFocusRequester.requestFocus()
                Log.d("ZebraScan", "Foco pasado tras escaneo Zebra")
            } catch (e: Exception) {
                Log.e("ZebraFocus", "Error al pasar foco con Zebra: ${e.message}")
            }
            isZebraScan.value = false
        }
    }

    // val focusRequester = remember { FocusRequester() }
    LaunchedEffect(shouldRequestFocusAfterClear.value) {
        if (shouldRequestFocusAfterClear.value) {
            delay(100) // ⏳ Esperar a que Compose termine de recomponer
            try {
                focusRequester.requestFocus()
            } catch (e: Exception) {
                Log.e("FocusClear", "Error al pasar foco tras limpiar: ${e.message}")
            }
            shouldRequestFocusAfterClear.value = false
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(modifier = Modifier
            .width(275.dp)
            .height(64.dp)
            .padding(4.dp)
            .focusRequester(focusRequesterLocation)
            .onFocusChanged { focusState ->
                if (!focusState.isFocused && tempLocationInput.value.length >= 4) {
                    validarUbicacionEnMaestro(
                        tempLocationInput.value,
                        location,
                        showErrorLocation,
                        showUbicacionNoExisteDialog,
                        nextFocusRequester,
                        keyboardController
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

                // Detectar entrada rápida tipo Zebra
                if (clean.length > 4 && clean.length - location.value.length > 2) {
                    isZebraScan.value = true
                    Log.d("ZebraScan", "Entrada tipo Zebra detectada: $clean")
                }

                wasScanned.value = false
                showErrorLocation.value = false
            },
            isError = showErrorLocation.value && (location.value.isEmpty() || location.value == "UBICACIÓN NO EXISTE"),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = {
                if (tempLocationInput.value.length >= 4) {
                    validarUbicacionEnMaestro(
                        tempLocationInput.value,
                        location,
                        showErrorLocation,
                        showUbicacionNoExisteDialog,
                        nextFocusRequester,
                        keyboardController,
                        ocultarTeclado = false
                    )
                }

                if (!showErrorLocation.value) {
                    nextFocusRequester.requestFocus()
                } else {
                    showUbicacionNoExisteDialog.value = true
                }
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
            })


        // 🔵 Ícono de borrar separado (afuera del campo)
        if (tempLocationInput.value.isNotEmpty()) {
            IconButton(
                onClick = {
                    location.value = ""
                    qrCodeContentLocation.value = ""
                    tempLocationInput.value = ""

                }, modifier = Modifier
                    .size(48.dp)
                    .padding(start = 4.dp)

            ) {
                Icon(
                    modifier = Modifier.size(48.dp),
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Borrar texto",
                    tint = Color.Red
                )
            }
        }


        if (showUbicacionNoExisteDialog.value) {
            AlertDialog(onDismissRequest = { showUbicacionNoExisteDialog.value = false },
                title = { Text("Ubicación inválida") },
                text = { Text("La ubicación ingresada no existe en el maestro de ubicaciones.") },
                confirmButton = {
                    TextButton(onClick = {
                        showUbicacionNoExisteDialog.value = false
                        focusRequesterLocation.requestFocus() // ✅ vuelve al campo ubicación

                    }) {
                        Text("Aceptar")
                    }
                })
        }

    }

}

fun validarUbicacionEnMaestro(
    codigo: String,
    location: MutableState<String>,
    showErrorLocation: MutableState<Boolean>,
    showUbicacionNoExisteDialog: MutableState<Boolean>,
    nextFocusRequester: FocusRequester,
    keyboardController: SoftwareKeyboardController?,
    ocultarTeclado: Boolean = false
) {
    Firebase.firestore.collection("ubicaciones")
        .whereEqualTo("codigo_ubi", codigo)
        .get()
        .addOnSuccessListener { documents ->
            if (!documents.isEmpty) {
                location.value = codigo
                showErrorLocation.value = false

                if (ocultarTeclado) {
                    keyboardController?.hide()
                }

                try {
                    nextFocusRequester.requestFocus()
                } catch (e: Exception) {
                    Log.e("FocusError", "Error al solicitar foco: ${e.message}")
                }
            } else {
                location.value = ""
                showErrorLocation.value = true
                showUbicacionNoExisteDialog.value = true
            }
        }
        .addOnFailureListener {
            Log.e("Firestore", "Error al validar ubicación", it)
        }
}



