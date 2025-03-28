package com.eriknivar.firebasedatabase.view.inventoryentry

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.coroutines.delay

@Composable
fun OutlinedTextFieldsInputsSku(
    sku: MutableState<String>,
    showErrorSku: MutableState<Boolean>,// 🔥 Ahora recibimos un MutableState
    productoDescripcion: MutableState<String>,
    productList: MutableState<List<String>>,
    productMap: MutableState<Map<String, Pair<String, String>>>,
    showProductDialog: MutableState<Boolean>,
    unidadMedida: MutableState<String>,
    focusRequester: FocusRequester, // 👈 NUEVO parámetro


) {
    val qrCodeContentSku = remember { mutableStateOf("") }
    val qrScanLauncherSku =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            val intentResult = IntentIntegrator.parseActivityResult(result.resultCode, data)
            if (intentResult != null) {
                qrCodeContentSku.value = intentResult.contents ?: "Código No Encontrado"
            }
        }

    val qrCodeScannerSku = remember { QRCodeScanner(qrScanLauncherSku) }
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()



    LaunchedEffect(qrCodeContentSku.value) {
        sku.value = qrCodeContentSku.value.uppercase()

        if (sku.value.isNotEmpty() && sku.value != "CODIGO NO ENCONTRADO") {
            findProductDescription(db, sku.value) { descripcion, unidadMedidaObtenida ->
                productoDescripcion.value = descripcion // ✅ Actualiza la descripción
                unidadMedida.value = unidadMedidaObtenida // ✅ Actualiza la unidad de medida
            }
            delay(200) // 🔁 Breve espera para estabilidad visual

        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(modifier = Modifier
            .width(275.dp)
            .height(64.dp)
            .padding(4.dp)
            .focusRequester(focusRequester) // 👈 Aquí aplicas el focus
            .onFocusChanged { focusState ->
                if (!focusState.isFocused && sku.value.isNotEmpty() && sku.value != "CODIGO NO ENCONTRADO") {
                    findProductDescription(db, sku.value) { descripcion ->
                        productoDescripcion.value = descripcion // 🔥 Actualiza la descripción
                    }
                }
            },
            singleLine = true,
            label = { Text(text = "Código Producto") },
            value = sku.value,
            onValueChange = { newValue ->
                sku.value = newValue.uppercase()
                qrCodeContentSku.value = newValue.uppercase()
                showErrorSku.value = false
            },
            isError = showErrorSku.value && (sku.value.isEmpty() || sku.value == "CODIGO NO ENCONTRADO"),

            trailingIcon = {


                Row {
                    // 📌 Botón para abrir la lista de productos
                    IconButton(
                        onClick = {
                            findProducts(db) { lista, mapa ->
                                productList.value = lista
                                productMap.value = mapa
                                showProductDialog.value = true // 🔥 Abre el diálogo de productos

                            }

                        }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Buscar productos"
                        )

                    }


                    // 📌 Botón para escanear código QR
                    IconButton(onClick = { qrCodeScannerSku.startQRCodeScanner(context as android.app.Activity) }) {
                        Icon(
                            imageVector = Icons.Filled.QrCodeScanner,
                            contentDescription = "Escanear Código"
                        )
                    }
                }
            },
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onNext = {
                    // Aquí puedes mover el foco a otro campo si lo deseas
                }
            ),

        )

        Spacer(modifier = Modifier.width(4.dp)) // 🔥 Espacio entre el campo y la UM

        // 📌 Texto para mostrar la unidad de medida
        Text(
            text = unidadMedida.value, // 🔥 Aquí se muestra la UM
            fontSize = 22.sp,
            color = Color.Black,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            modifier = Modifier.padding(8.dp).background(color = Color.Red)
        )
    }

}
