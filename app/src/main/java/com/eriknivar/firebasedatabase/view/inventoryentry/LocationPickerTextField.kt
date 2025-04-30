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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.coroutines.delay

@Composable
fun OutlinedTextFieldsInputsLocation(
    location: MutableState<String>,
    showErrorLocation: MutableState<Boolean>,
    focusRequester: FocusRequester,
    nextFocusRequester: FocusRequester,
    onUserInteraction: () -> Unit = {},
    shouldRequestFocusAfterClear: MutableState<Boolean>
) {
    val qrCodeContentLocation = remember { mutableStateOf("") }
    val wasScanned = remember { mutableStateOf(false) }
    val isZebraScan = remember { mutableStateOf(false) }

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
            location.value = qrCodeContentLocation.value.uppercase()
            if (location.value.isNotEmpty() && location.value != "CÓDIGO NO ENCONTRADO") {
                delay(150)
                try {
                    keyboardController?.hide()
                    nextFocusRequester.requestFocus()
                    Log.d("FocusDebug", "Foco pasado al siguiente campo (QR launcher)")
                } catch (e: Exception) {
                    Log.e("FocusError", "Error al mover el foco: ${e.message}")
                }
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
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            modifier = Modifier
                .width(275.dp)
                .height(64.dp)
                .padding(4.dp)
                .focusRequester(focusRequester),
            singleLine = true,
            label = { Text(text = "Ubicación") },
            value = location.value,
            onValueChange = { newValue ->
                val clean = newValue.trim().uppercase()

                // 🟡 Detectar entrada rápida de Zebra (más de 4 caracteres inyectados de golpe)
                if (clean.length > 4 && clean.length - location.value.length > 2) {
                    isZebraScan.value = true
                    Log.d("ZebraScan", "Entrada tipo Zebra detectada: $clean")
                }

                onUserInteraction()

                location.value = clean
                qrCodeContentLocation.value = clean
                wasScanned.value = false
                showErrorLocation.value = false
            },
            isError = showErrorLocation.value &&
                    (location.value.isEmpty() || location.value == "CÓDIGO NO ENCONTRADO"),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = {
                try {
                    keyboardController?.hide()
                    nextFocusRequester.requestFocus()
                } catch (e: Exception) {
                    Log.e("KeyboardFocus", "Error pasando foco desde Ubicación: ${e.message}")
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
            }
        )

        // 🔵 Ícono de borrar separado (afuera del campo)
        if (location.value.isNotEmpty()) {
            IconButton(
                onClick = {
                    location.value = ""
                    qrCodeContentLocation.value = ""
                },
                modifier = Modifier
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
    }
}

