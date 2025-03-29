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
import androidx.compose.ui.unit.dp
import com.google.zxing.integration.android.IntentIntegrator

@Composable
fun OutlinedTextFieldsInputsLocation(
    location: MutableState<String>,
    showErrorLocation: MutableState<Boolean>, // 🔥 Ahora recibimos un MutableState
    nextFocusRequester: FocusRequester
) {
    val qrCodeContentLocation = remember { mutableStateOf("") }
    val qrScanLauncherLocation =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            val intentResult = IntentIntegrator.parseActivityResult(result.resultCode, data)
            if (intentResult != null) {
                qrCodeContentLocation.value = intentResult.contents ?: "Código No Encontrado"
            }
        }

    val qrCodeScannerLocation = remember { QRCodeScanner(qrScanLauncherLocation) }
    val context = LocalContext.current


    LaunchedEffect(qrCodeContentLocation.value) {
        location.value = qrCodeContentLocation.value.uppercase()
        if (location.value.isNotEmpty() && location.value != "CÓDIGO NO ENCONTRADO") {
            try {
                nextFocusRequester.requestFocus()
            } catch (e: Exception) {
                Log.e("FocusError", "Error al mover el foco: ${e.message}")
            }
        }
    }


    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically // 📌 Asegura alineación vertical
    ) {
        OutlinedTextField(modifier = Modifier
            .width(275.dp)
            .height(64.dp)
            .padding(4.dp),
            singleLine = true,
            label = { Text(text = "Ubicación") },
            value = location.value,
            onValueChange = { newValue ->
                location.value = newValue.uppercase()
                qrCodeContentLocation.value = newValue.uppercase()
                if (newValue.isNotEmpty()) {
                    showErrorLocation.value // ✅ Si hay un valor, ocultar el error

                }
            },
            isError = showErrorLocation.value && (location.value.isEmpty() || location.value == "CODIGO NO ENCONTRADO"),

            trailingIcon = {
                Row {
                    IconButton(
                        onClick = { qrCodeScannerLocation.startQRCodeScanner(context as android.app.Activity) },
                        modifier = Modifier.size(60.dp) // 📌 Tamaño del botón
                    ) {
                        Icon(
                            imageVector = Icons.Filled.QrCodeScanner,
                            contentDescription = "Escanear Código",
                        )
                    }
                }
            })
    }
}
