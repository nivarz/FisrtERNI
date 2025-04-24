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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.google.zxing.integration.android.IntentIntegrator

@Composable
fun OutlinedTextFieldsInputsLot(
    lot: MutableState<String>,
    focusRequester: FocusRequester,
    nextFocusRequester: FocusRequester
) {
    val qrCodeContentLot = remember { mutableStateOf("") }
    val wasScanned = remember { mutableStateOf(false) }

    val qrScanLauncherLot =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            val intentResult = IntentIntegrator.parseActivityResult(result.resultCode, data)
            val scanned = intentResult?.contents?.trim()?.uppercase()

            if (!scanned.isNullOrEmpty() && scanned != "CÓDIGO NO ENCONTRADO") {
                qrCodeContentLot.value = scanned
                wasScanned.value = true // ✅ Indicamos que vino del escáner
            }
        }

    val qrCodeScannerLot = remember { QRCodeScanner(qrScanLauncherLot) }
    val context = LocalContext.current

    LaunchedEffect(qrCodeContentLot.value, wasScanned.value) {
        if (wasScanned.value && qrCodeContentLot.value.isNotBlank()) {
            lot.value = qrCodeContentLot.value

            try {
                nextFocusRequester.requestFocus()
            } catch (e: Exception) {
                Log.e("FocusError", "Error al pasar foco a fecha: ${e.message}")
            }

            wasScanned.value = false // ✅ Reseteamos la bandera
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
            label = { Text(text = "Lote") },
            value = lot.value,
            onValueChange = {
                val upper = it.trim().uppercase()
                lot.value = upper
            },
            trailingIcon = {
                IconButton(
                    onClick = {
                        qrCodeScannerLot.startQRCodeScanner(context as android.app.Activity)
                    },
                    modifier = Modifier.size(60.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.QrCodeScanner,
                        contentDescription = "Escanear Código"
                    )
                }
            },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = {
                nextFocusRequester.requestFocus()
            })
        )
    }
}
