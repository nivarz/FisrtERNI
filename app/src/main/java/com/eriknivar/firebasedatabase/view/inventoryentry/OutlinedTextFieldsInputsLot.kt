package com.eriknivar.firebasedatabase.view.inventoryentry

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.zxing.integration.android.IntentIntegrator

@Composable
fun OutlinedTextFieldsInputsLot(
    lot: MutableState<String>,
) {
    val qrCodeContentLot = remember { mutableStateOf("") }
    val qrScanLauncherLot =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            val intentResult = IntentIntegrator.parseActivityResult(result.resultCode, data)
            if (intentResult != null) {
                qrCodeContentLot.value = intentResult.contents ?: "Código No Encontrado"
            }
        }

    val qrCodeScannerLot = remember { QRCodeScanner(qrScanLauncherLot) }
    val context = LocalContext.current


    LaunchedEffect(qrCodeContentLot.value) {
        lot.value = qrCodeContentLot.value.uppercase()
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically // 📌 Asegura alineación vertical
    ) {
        OutlinedTextField(
            modifier = Modifier
                .weight(2f) // 📌 Hace que el campo de texto ocupe el espacio disponible
                .padding(2.dp),
            singleLine = true,
            label = { Text(text = "Lote") },
            value = lot.value,
            onValueChange = { newValue ->
                lot.value = newValue.uppercase()
                qrCodeContentLot.value = newValue.uppercase()
            },

            trailingIcon = {
                Row {
                    IconButton(
                        onClick = { qrCodeScannerLot.startQRCodeScanner(context as android.app.Activity) },
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