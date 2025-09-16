package com.eriknivar.firebasedatabase.view.inventoryentry

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@Composable
fun ProductSelectionDialog(
    productList: MutableState<List<String>>,
    productMap: MutableState<Map<String, Pair<String, String>>>, // desc -> (codigo, unidad)
    showProductDialog: MutableState<Boolean>,
    sku: MutableState<String>,
    qrCodeContentSku: MutableState<String>,
    productoDescripcion: MutableState<String>,
    focusRequesterLote: FocusRequester,
    onUserInteraction: () -> Unit = {},
    unidadMedida: MutableState<String>,
    clienteIdActual: String?
) {
    // Si no está visible, no pintamos nada
    if (!showProductDialog.value) return

    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val db = remember { FirebaseFirestore.getInstance() }
    val cidUi = remember(clienteIdActual) { clienteIdActual?.trim()?.uppercase() }

    // Carga de productos al abrir/cambiar cliente
    LaunchedEffect(cidUi, showProductDialog.value) {
        if (!showProductDialog.value) return@LaunchedEffect
        isLoading = true
        errorMsg = null
        productList.value = emptyList()
        productMap.value = emptyMap()

        val cid = cidUi
        if (cid.isNullOrBlank()) {
            isLoading = false
            return@LaunchedEffect
        }

        suspend fun cargar(query: com.google.firebase.firestore.Query) {
            val snap = query.get().await()
            val lista = mutableListOf<String>()
            val mapa = mutableMapOf<String, Pair<String, String>>()
            for (doc in snap.documents) {
                val codigo = doc.id
                val desc = (
                        doc.getString("nombreComercial")
                            ?: doc.getString("nombreNormalizado")
                            ?: doc.getString("descripcion")
                            ?: ""
                        ).trim()
                val um = doc.extractUnidad()
                if (desc.isNotEmpty()) {
                    lista.add(desc)
                    mapa[desc] = codigo to um
                }
            }
            productList.value = lista.sorted()
            productMap.value = mapa
        }

        try {
            val col = db.collection("clientes").document(cid).collection("productos")
            // 1) primero con filtro 'activo'
            cargar(col.whereEqualTo("activo", true))
            // 2) si quedó vacío, probamos sin filtro (por si faltara el campo en algunos docs)
            if (productList.value.isEmpty()) cargar(col)
        } catch (e: Exception) {
            errorMsg = e.message ?: "Error desconocido al cargar productos"
        } finally {
            isLoading = false
        }
    }

    AlertDialog(
        onDismissRequest = { showProductDialog.value = false },
        confirmButton = {
            TextButton(onClick = { showProductDialog.value = false }) { Text("Cerrar") }
        },
        title = { Text("Selecciona un Producto") },
        text = {
            Column(Modifier.fillMaxWidth()) {

                // Buscador
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Buscar producto") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                // Estado
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) { CircularProgressIndicator() }
                    }
                    !errorMsg.isNullOrBlank() -> {
                        Text(errorMsg!!, color = Color.Red, fontSize = 12.sp)
                    }
                    else -> {
                        val filtered = productList.value.filter {
                            it.contains(searchQuery, ignoreCase = true)
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp, max = 480.dp)
                        ) {
                            items(filtered) { descripcion ->
                                TextButton(
                                    onClick = {
                                        onUserInteraction()
                                        productMap.value[descripcion]?.let { (codigo, um) ->
                                            sku.value = codigo
                                            qrCodeContentSku.value = codigo
                                            productoDescripcion.value = descripcion
                                            unidadMedida.value = um
                                        }
                                        showProductDialog.value = false
                                        try { focusRequesterLote.requestFocus() } catch (_: Exception) {}
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(Modifier.fillMaxWidth()) {
                                        Text(
                                            text = descripcion,
                                            maxLines = 4,
                                            overflow = TextOverflow.Ellipsis,
                                            color = Color.Black,
                                        )
                                        // Si quieres mostrar la UM dentro del diálogo, descomenta:
                                        // productMap.value[descripcion]?.second?.takeIf { it.isNotBlank() }?.let { um ->
                                        //     Text(text = um, fontSize = 12.sp, color = Color.Gray)
                                        // }
                                    }
                                }
                                HorizontalDivider()
                            }
                        }

                        if (productList.value.isEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "No hay productos para este cliente.",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    )
}

/* ===== Helpers ===== */

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
