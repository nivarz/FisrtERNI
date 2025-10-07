package com.eriknivar.firebasedatabase.view.inventoryentry

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eriknivar.firebasedatabase.network.SelectedClientStore
import com.eriknivar.firebasedatabase.viewmodel.UserViewModel
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.eriknivar.firebasedatabase.scan.CapturePortraitActivity


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
    shouldRequestFocusAfterClear: MutableState<Boolean>,
    clienteIdActual: String?
) {

    val context = LocalContext.current
    val db = remember { FirebaseFirestore.getInstance() }
    val isLoadingProductos = remember { mutableStateOf(false) }
    val zebraScanned = remember { mutableStateOf(false) }


    // ===== foco tras limpiar =====
    LaunchedEffect(shouldRequestFocusAfterClear.value) {
        if (shouldRequestFocusAfterClear.value) {
            delay(100L)
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {
            }
            shouldRequestFocusAfterClear.value = false
        }
    }

    // ===== autolookup al TECLAR (mismo comportamiento que antes) =====
    // ✅ Usa el clienteIdActual que llega por parámetro
    LaunchedEffect(sku.value, clienteIdActual) {
        val code = sku.value.trim().uppercase()
        val cid = clienteIdActual?.trim()?.uppercase()

        if (code.isEmpty()) {
            productoDescripcion.value = ""
            unidadMedida.value = ""
            return@LaunchedEffect
        }
        if (cid.isNullOrBlank()) return@LaunchedEffect

        // pequeño debounce para no disparar en cada tecla
        kotlinx.coroutines.delay(200L)
        lookupSkuForClient(
            db = db,
            clienteId = cid,
            code = code
        ) { desc, um ->
            productoDescripcion.value = desc
            unidadMedida.value = um
            showErrorSku.value =
                desc.equals("Sin descripción", ignoreCase = true) ||
                        desc.startsWith("Error", ignoreCase = true)
        }
    }

    // ===== escaneo con cámara (ZXing) =====
    val scanLauncherSku = rememberLauncherForActivityResult(ScanContract()) { result ->
        val contents = result.contents
        if (contents != null) {
            val scanned = contents.trim().uppercase()
            sku.value =
                scanned                   // ← disparará tu LaunchedEffect(sku.value, clienteIdActual)
            try {
                keyboardController?.hide(); nextFocusRequester.requestFocus()
            } catch (_: Exception) {
            }
            Log.d("ScanSku", "Escaneo SKU: $scanned")
        } else {
            Log.d("ScanSku", "Escaneo cancelado / sin contenido")
        }
    }

    // ===== escaneo Zebra (input masivo) → pasar foco =====
    LaunchedEffect(zebraScanned.value) {
        if (zebraScanned.value) {
            delay(150L)
            if (sku.value.isNotEmpty() && sku.value != "CODIGO NO ENCONTRADO") {
                try {
                    keyboardController?.hide(); nextFocusRequester.requestFocus()
                } catch (_: Exception) {
                }
            }
            zebraScanned.value = false
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
            label = { Text(text = "Código Producto", color = Color.Gray) },
            value = sku.value,
            onValueChange = { newValue ->
                val cleanSku = newValue.uppercase().replace(Regex("[^A-Z0-9_\\-]"), "")
                val isZebra = cleanSku.length >= 5 && (cleanSku.length - sku.value.length > 2)
                if (isZebra) zebraScanned.value = true

                sku.value = cleanSku
                showErrorSku.value = false

                // si queda vacío limpia descripción/unidad; el lookup lo hará el LaunchedEffect
                if (cleanSku.isEmpty()) {
                    productoDescripcion.value = ""
                    unidadMedida.value = ""
                }
                onUserInteraction()
            },
            isError = showErrorSku.value && (sku.value.isEmpty() || sku.value == "CODIGO NO ENCONTRADO"),
            trailingIcon = {
                Row {
                    IconButton(onClick = { showProductDialog.value = true }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Buscar productos"
                        )
                    }
                    IconButton(onClick = {
                        val options = ScanOptions().apply {
                            setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES)
                            setPrompt("Escanea el código")
                            setBeepEnabled(false)
                            setOrientationLocked(true)                         // ← bloquea rotación
                            setCaptureActivity(CapturePortraitActivity::class.java) // ← portrait
                        }
                        scanLauncherSku.launch(options)
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
                } catch (_: Exception) {
                }
            })
        )

        // borrar a la derecha
        if (sku.value.isNotEmpty()) {
            IconButton(
                onClick = {
                    sku.value = ""
                    productoDescripcion.value = ""
                    unidadMedida.value = ""
                    showErrorSku.value = false
                    onUserInteraction()
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

        // overlay opcional (si lo activas en otra parte)
        if (isLoadingProductos.value) {
            Dialog(
                onDismissRequest = {},
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f))
                    )
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Card(
                            elevation = CardDefaults.cardElevation(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding()
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(24.dp)
                                    .width(200.dp),
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

/* ===== Helpers dentro del mismo archivo (para no depender de otros) ===== */

// Extrae la UM robustamente (varios nombres posibles); default "UND"
private fun DocumentSnapshot.extractUnidad(): String {
    val keys = listOf(
        "unidad", "unidadMedida", "UnidadMedida",
        "um", "UM", "uom", "UOM",
        "unidad_medida", "presentacion", "presentación"
    )
    for (k in keys) {
        val v = this.get(k)
        when (v) {
            is String -> if (v.isNotBlank()) return v.trim()
            is Number -> return v.toString()
        }
    }
    return "UND"
}

// Lee clientes/{clienteId}/productos/{code} y devuelve (descripcion, um)
private fun lookupSkuForClient(
    db: FirebaseFirestore,
    clienteId: String,
    code: String,
    onResult: (String, String) -> Unit
) {
    val skuCode = code.trim().uppercase()
    if (skuCode.isEmpty()) {
        onResult("", ""); return
    }

    db.collection("clientes").document(clienteId)
        .collection("productos").document(skuCode)
        .get()
        .addOnSuccessListener { doc ->
            if (!doc.exists()) {
                onResult("Sin descripción", "")
                return@addOnSuccessListener
            }
            val desc = (doc.getString("nombreComercial")
                ?: doc.getString("nombreNormalizado")
                ?: doc.getString("descripcion")
                ?: "Sin descripción").trim()
            val um = doc.extractUnidad()
            onResult(desc, um)
        }
        .addOnFailureListener {
            onResult("Error al obtener datos", "N/A")
        }
}
