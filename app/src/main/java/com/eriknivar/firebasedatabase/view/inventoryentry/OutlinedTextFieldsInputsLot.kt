package com.eriknivar.firebasedatabase.view.inventoryentry

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.eriknivar.firebasedatabase.scan.CapturePortraitActivity


@Composable
fun OutlinedTextFieldsInputsLot(
    lot: MutableState<String>,
    focusRequester: FocusRequester,
    nextFocusRequester: FocusRequester,
    keyboardController: SoftwareKeyboardController?,
    onUserInteraction: () -> Unit = {},
    shouldRequestFocusAfterClear: MutableState<Boolean>, // 👈 Nueva bandera
    enable: Boolean = true

) {
    val zebraScanned = remember { mutableStateOf(false) }

    val scanLauncherLot = rememberLauncherForActivityResult(ScanContract()) { result ->
        val contents = result.contents
        if (contents != null) {
            val scanned = contents.trim().uppercase()
            lot.value = scanned

            try {
                keyboardController?.hide()
                nextFocusRequester.requestFocus()
            } catch (_: Exception) {
            }

            Log.d("ScanLot", "Escaneo Lote: $scanned")
        } else {
            Log.d("ScanLot", "Escaneo cancelado / sin contenido")
        }
    }


    // fuerza "-" cuando está deshabilitado
    LaunchedEffect(enable) { if (!enable) lot.value = "-" }

    // ✅ Foco automático después de limpiar
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

    LaunchedEffect(zebraScanned.value) {
        if (zebraScanned.value) {
            delay(150)
            if (lot.value.isNotEmpty() && lot.value != "CODIGO NO ENCONTRADO") {
                try {
                    keyboardController?.hide()
                    nextFocusRequester.requestFocus()
                    Log.d("ZebraFocus", "Foco pasado tras escaneo Zebra")
                } catch (e: Exception) {
                    Log.e("ZebraFocus", "Error al pasar foco Zebra: ${e.message}")
                }
            }
            if (lot.value.isNotEmpty() && lot.value != "CODIGO NO ENCONTRADO") {
                keyboardController?.hide()
                nextFocusRequester.requestFocus()
            }

            zebraScanned.value = false
        }
    }

    val context = LocalContext.current

    // detecta foco como en Location (si allí usas temp*, aquí no hace falta uno nuevo)
    val interaction = remember { MutableInteractionSource() }
    val isFocused by interaction.collectIsFocusedAsState()

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
            interactionSource = interaction,
            onValueChange = {
                val upper = it.trim().uppercase()
                val prev = lot.value
                val wasEmpty = prev.isBlank() || prev == "-"
                val isZebra = wasEmpty && upper.length >= 5 && (upper.length - prev.length) >= 5

                if (isZebra) {
                    zebraScanned.value = true
                }
                lot.value = upper
                onUserInteraction()
            },
            // ⬇️ NUEVO
            enabled = enable,
            readOnly = !enable,
            trailingIcon = {
                if (enable) {
                    IconButton(
                        onClick = {
                            val options = ScanOptions().apply {
                                setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES)
                                setPrompt("Escanea el código")
                                setBeepEnabled(false)
                                setOrientationLocked(true)                         // 👈 bloquea rotación
                                setCaptureActivity(CapturePortraitActivity::class.java) // 👈 portrait
                            }
                            scanLauncherLot.launch(options)
                        },
                        modifier = Modifier.size(60.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.QrCodeScanner,
                            contentDescription = "Escanear Código"
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = {
                nextFocusRequester.requestFocus()
            })
        )

        // ⬇️ NUEVO: solo mostrar borrar si está habilitado

        // Mostrar zafacón cuando hay texto (y no es "-") y el campo está habilitado
        if (enable && lot.value.isNotBlank() && lot.value != "-") {
            IconButton(
                onClick = {
                    lot.value = ""
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
    }
}
