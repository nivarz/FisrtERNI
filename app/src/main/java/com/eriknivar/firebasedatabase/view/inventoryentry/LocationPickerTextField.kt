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
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
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
    nextFocusRequester: FocusRequester
) {
    val qrCodeContentLocation = remember { mutableStateOf("") }
    val wasScanned = remember { mutableStateOf(false) }

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

    // ✅ Controla el paso de foco solamente después del escaneo
    LaunchedEffect(qrCodeContentLocation.value, wasScanned.value) {
        Log.d("FocusDebug", "wasScanned: ${wasScanned.value}, value: ${qrCodeContentLocation.value}")
        if (wasScanned.value) {
            location.value = qrCodeContentLocation.value.uppercase()
            if (location.value.isNotEmpty() && location.value != "CÓDIGO NO ENCONTRADO") {
                delay(150)
                try {
                    keyboardController?.hide()
                    nextFocusRequester.requestFocus()
                    Log.d("FocusDebug", "Foco pasado al siguiente campo")
                } catch (e: Exception) {
                    Log.e("FocusError", "Error al mover el foco: ${e.message}")
                }
            }
            wasScanned.value = false
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
                .padding(4.dp),
            singleLine = true,
            label = { Text(text = "Ubicación") },
            value = location.value,
            onValueChange = { newValue ->
                location.value = newValue.uppercase()
                qrCodeContentLocation.value = newValue.uppercase()
                wasScanned.value = false
                showErrorLocation.value = false
            },
            isError = showErrorLocation.value && (location.value.isEmpty() || location.value == "CODIGO NO ENCONTRADO"),
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
                        contentDescription = "Escanear Código",
                    )
                }
            }
        )
    }
}
