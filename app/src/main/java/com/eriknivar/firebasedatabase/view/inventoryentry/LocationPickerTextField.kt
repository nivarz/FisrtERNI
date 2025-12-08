package com.eriknivar.firebasedatabase.view.inventoryentry

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.compose.runtime.MutableState
import androidx.compose.ui.platform.LocalContext
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import android.content.Context
import android.widget.Toast
import com.eriknivar.firebasedatabase.view.utility.normalizeUbi
import com.eriknivar.firebasedatabase.view.utility.validarUbicacionEnMaestro

private fun isOnline(ctx: Context): Boolean {
    val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val net = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(net) ?: return false
    return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
}

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
    localidadActual: String?,             // 👈 NUEVO: (opcional) filtrar por localidad
    onSearchClick: () -> Unit
) {

    val isZebraScan = remember { mutableStateOf(false) }
    val showUbicacionNoExisteDialog = remember { mutableStateOf(false) }
    val focusRequesterLocation = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val ctx = LocalContext.current

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

    // ANCLA: helper local para validar o aceptar offline
    fun validarOAceptarUbicacion(
        codigo: String,
        ocultarTeclado: Boolean
    ) {
        val code = normalizeUbi(codigo)

        // 🔌 Offline → aceptar sin error y avisar
        if (!isOnline(ctx)) {
            location.value = code
            showErrorLocation.value = false
            Toast.makeText(ctx, "Sin conexión. Se validará al enviar.", Toast.LENGTH_SHORT).show()
            if (ocultarTeclado) keyboardController?.hide()
            try {
                nextFocusRequester.requestFocus()
            } catch (_: Exception) {
            }
            return
        }

        // 🌐 Online → usar el validador central
        validarUbicacionEnMaestro(
            clienteId = clienteIdActual.orEmpty(),
            localidadCodigo = localidadActual.orEmpty(),
            codigoUbi = code,
            onResult = { existe ->
                if (existe) {
                    location.value = code
                    showErrorLocation.value = false
                    if (ocultarTeclado) keyboardController?.hide()
                    try {
                        nextFocusRequester.requestFocus()
                    } catch (_: Exception) {
                    }
                } else {
                    showErrorLocation.value = true
                    showUbicacionNoExisteDialog.value = true
                }
            },
            onError = {
                // Trátalo como red caída/transitorio
                location.value = code
                showErrorLocation.value = false
                Toast.makeText(
                    ctx,
                    "No se pudo validar (red). Se validará al enviar.",
                    Toast.LENGTH_SHORT
                ).show()
                if (ocultarTeclado) keyboardController?.hide()
                try {
                    nextFocusRequester.requestFocus()
                } catch (_: Exception) {
                }
            }
        )
    }

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val contents = result.contents
        if (contents != null) {
            val scanned = contents.trim().uppercase()

            // 1) Mostrar el código en el campo
            tempLocationInput.value = scanned

            // 2) Validar inmediatamente contra el maestro
            tempLocationInput.value = scanned
            validarOAceptarUbicacion(scanned, ocultarTeclado = true)

            Log.d("ScanDebug", "Escaneo recibido: $scanned")
        } else {
            Log.d("ScanDebug", "Escaneo cancelado / sin contenido")
        }
    }

    // 2) Zebra: además de pasar foco, valida ANTES
    LaunchedEffect(isZebraScan.value) {
        if (isZebraScan.value) {
            val code = tempLocationInput.value
            if (code.length >= 13) {
                validarOAceptarUbicacion(code, ocultarTeclado = true)
            }
            try {
                keyboardController?.hide()
                nextFocusRequester.requestFocus()
            } catch (_: Exception) {
            }
            isZebraScan.value = false
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
                    if (!focusState.isFocused && tempLocationInput.value.length >= 13) {

                        if (!isOnline(ctx)) {
                            // Offline: no marcar error; acepta la ubicación y sigue
                            val code = tempLocationInput.value.trim().uppercase()
                            location.value = code
                            showErrorLocation.value = false

                            // 👇 Aviso offline
                            Toast.makeText(
                                ctx,
                                "Sin conexión. Se validará al enviar.",
                                Toast.LENGTH_SHORT
                            ).show()

                            return@onFocusChanged
                        }

                        if (!focusState.isFocused && tempLocationInput.value.length >= 13) {
                            validarOAceptarUbicacion(
                                tempLocationInput.value,
                                ocultarTeclado = false
                            )
                        }
                    }
                },
            singleLine = true,
            label = { Text(text = "Ubicación", color = Color.Gray) },
            value = tempLocationInput.value,
            onValueChange = { newValue ->
                val clean = newValue.trim().uppercase()
                tempLocationInput.value = clean
                onUserInteraction()

                // 3) onValueChange: detección Zebra más realista
                if (clean.length >= 13 && clean.length - location.value.length >= 3) {
                    isZebraScan.value = true
                }
                showErrorLocation.value = false
            },
            isError = showErrorLocation.value && (location.value.isEmpty() || location.value == "UBICACIÓN NO EXISTE"),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            // 4) KeyboardActions (onNext): baja el umbral
            keyboardActions = KeyboardActions(onNext = {
                if (tempLocationInput.value.length >= 13) {

                    if (!isOnline(ctx)) {
                        val code = tempLocationInput.value.trim().uppercase()
                        location.value = code
                        showErrorLocation.value = false
                        nextFocusRequester.requestFocus()

                        Toast.makeText(
                            ctx,
                            "Sin conexión. Se validará al enviar.",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@KeyboardActions
                    }
                    if (tempLocationInput.value.length >= 13) {
                        validarOAceptarUbicacion(tempLocationInput.value, ocultarTeclado = false)
                    }
                }
                if (!showErrorLocation.value) nextFocusRequester.requestFocus()
                else showUbicacionNoExisteDialog.value = true
            }),
            // ANCLA: reemplazo de trailingIcon en el OutlinedTextField de Ubicación
            trailingIcon = {
                Row(
                    modifier = Modifier.padding(end = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Lupa → abre el diálogo de selección de ubicación
                    IconButton(
                        onClick = {
                            // 👇 Mensaje inmediato mientras carga el AlertDialog
                            Toast.makeText(
                                ctx,
                                "Cargando ubicaciones...",
                                Toast.LENGTH_LONG
                            ).show()

                            onSearchClick()
                        },           // ← callback que abre LocationSelectionDialog
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Buscar ubicación"
                        )
                    }

                    // QR → escanear código de ubicación (tu lógica actual)
                    IconButton(
                        onClick = {
                            val options = ScanOptions().apply {
                                setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES)
                                setPrompt("Escanea el código")
                                setBeepEnabled(false)
                                setOrientationLocked(true)
                                setCaptureActivity(
                                    com.eriknivar.firebasedatabase.scan.CapturePortraitActivity::class.java
                                )
                            }
                            scanLauncher.launch(options)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.QrCodeScanner,
                            contentDescription = "Escanear Código"
                        )
                    }
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