package com.eriknivar.firebasedatabase.view.inventoryentry

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.coroutines.delay

@Composable
fun OutlinedTextFieldsInputsSku(
    sku: MutableState<String>,
    showErrorSku: MutableState<Boolean>,
    productoDescripcion: MutableState<String>,
    productList: MutableState<List<String>>,
    productMap: MutableState<Map<String, Pair<String, String>>>,
    showProductDialog: MutableState<Boolean>,
    unidadMedida: MutableState<String>,
    focusRequester: FocusRequester,
    nextFocusRequester: FocusRequester,
    keyboardController: SoftwareKeyboardController?,
    onUserInteraction: () -> Unit = {},
    shouldRequestFocusAfterClear: MutableState<Boolean> // 👈 Nueva bandera

) {
    val qrCodeContentSku = remember { mutableStateOf("") }
    val wasScanned = remember { mutableStateOf(false) }
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val isLoadingProductos = remember { mutableStateOf(false) }
    val zebraScanned = remember { mutableStateOf(false) }


    val qrScanLauncherSku =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            val intentResult = IntentIntegrator.parseActivityResult(result.resultCode, data)
            if (intentResult != null) {
                qrCodeContentSku.value = intentResult.contents ?: "Código No Encontrado"
                wasScanned.value = true
            }
        }

    val qrCodeScannerSku = remember { QRCodeScanner(qrScanLauncherSku) }

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

    LaunchedEffect(qrCodeContentSku.value, wasScanned.value) {
        if (wasScanned.value) {
            val scanned = qrCodeContentSku.value.uppercase()
            sku.value = scanned

            if (scanned.isNotEmpty() && scanned != "CODIGO NO ENCONTRADO") {
                findProductDescription(db, scanned) { descripcion, unidadMedidaObtenida ->
                    productoDescripcion.value = descripcion
                    unidadMedida.value = unidadMedidaObtenida
                }
                delay(200)
                try {
                    keyboardController?.hide()
                    nextFocusRequester.requestFocus()
                } catch (e: Exception) {
                    Log.e("FocusError", "Error al mover el foco desde SKU: \${e.message}")
                }
            } else {
                productoDescripcion.value = ""
                unidadMedida.value = ""
            }
            wasScanned.value = false
        }
    }

    LaunchedEffect(zebraScanned.value) {
        if (zebraScanned.value) {
            delay(150)
            if (sku.value.isNotEmpty() && sku.value != "CODIGO NO ENCONTRADO") {
                try {
                    keyboardController?.hide()
                    nextFocusRequester.requestFocus()
                    Log.d("ZebraFocus", "Foco pasado tras escaneo Zebra")
                } catch (e: Exception) {
                    Log.e("ZebraFocus", "Error al pasar foco Zebra: ${e.message}")
                }
            }
            zebraScanned.value = false
        }
    }


    Row(
        modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            modifier = Modifier
                .width(275.dp)
                .height(64.dp)
                .padding(4.dp)
                .focusRequester(focusRequester),
            singleLine = true,
            label = { Text(text = "Código Producto") },
            value = sku.value,
            onValueChange = { newValue ->
                val cleanSku = newValue.replace("\\s".toRegex(), "").uppercase()

                val isZebra = cleanSku.length >= 5 && (cleanSku.length - sku.value.length > 2)

                if (isZebra) {
                    zebraScanned.value = true
                }

                sku.value = cleanSku
                qrCodeContentSku.value = cleanSku
                showErrorSku.value = false

                if (cleanSku.isEmpty()) {
                    productoDescripcion.value = ""
                    unidadMedida.value = ""
                }

                if (cleanSku.isBlank()) {
                    productoDescripcion.value = ""
                    unidadMedida.value = ""
                } else {
                    findProductDescription(db, cleanSku) { descripcion, unidadMedidaObtenida ->
                        productoDescripcion.value = descripcion
                        unidadMedida.value = unidadMedidaObtenida
                    }
                }

                onUserInteraction()

            },
            isError = showErrorSku.value && (sku.value.isEmpty() || sku.value == "CODIGO NO ENCONTRADO"),
            trailingIcon = {
                Row {
                    if (!isLoadingProductos.value) {
                        IconButton(onClick = {
                            isLoadingProductos.value = true
                            findProducts(db) { lista, mapa ->
                                productList.value = lista
                                productMap.value = mapa
                                isLoadingProductos.value = false
                                showProductDialog.value = true
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Buscar productos"
                            )
                        }
                    }

                    IconButton(onClick = {
                        qrCodeScannerSku.startQRCodeScanner(context as android.app.Activity)
                    }) {
                        Icon(
                            imageVector = Icons.Filled.QrCodeScanner,
                            contentDescription = "Escanear Código"
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = {
                try {
                    keyboardController?.hide()
                    nextFocusRequester.requestFocus()
                } catch (e: Exception) {
                    Log.e("KeyboardFocus", "Error pasando foco desde SKU: \${e.message}")
                }
            })
        )

        // 🔵 Ícono de borrar separado (afuera del campo)
        if (sku.value.isNotEmpty()) {
            IconButton(
                onClick = {
                    sku.value = ""
                    qrCodeContentSku.value = ""
                    productoDescripcion.value = ""
                    unidadMedida.value = ""
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
        if (isLoadingProductos.value) {
            Dialog(onDismissRequest = {},
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f))
                    )
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            elevation = CardDefaults.cardElevation(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp).width(200.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Cargando productos...",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

    }
}