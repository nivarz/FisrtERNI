package com.eriknivar.firebasedatabase.view.inventoryentry

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
    productMap: MutableState<Map<String, Pair<String, String>>>, // descUi -> (codigo, unidad)
    showProductDialog: MutableState<Boolean>,
    sku: MutableState<String>,
    qrCodeContentSku: MutableState<String>,
    productoDescripcion: MutableState<String>,
    focusRequesterLote: FocusRequester,
    onUserInteraction: () -> Unit = {},
    unidadMedida: MutableState<String>,
    clienteIdActual: String?
) {
    if (!showProductDialog.value) return

    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val db = remember { FirebaseFirestore.getInstance() }
    val cidUi = remember(clienteIdActual) { clienteIdActual?.trim()?.uppercase() }

    // 1) CARGA DE PRODUCTOS (una sola vez por cliente al abrir el diálogo)
    LaunchedEffect(cidUi, showProductDialog.value) {
        if (!showProductDialog.value) return@LaunchedEffect

        val cid = cidUi

        if (cid.isNullOrBlank()) {
            isLoading = false
            errorMsg = "Selecciona un cliente para ver productos."
            productList.value = emptyList()
            productMap.value = emptyMap()
            return@LaunchedEffect
        }

        isLoading = true
        errorMsg = null
        productList.value = emptyList()
        productMap.value = emptyMap()

        try {
            val col = db.collection("clientes")
                .document(cid)
                .collection("productos")

            val pageSize = 10_000L      // límite Firestore
            val hardLimit = 35_000      // seguridad

            val lista = mutableListOf<String>()
            val mapa = mutableMapOf<String, Pair<String, String>>()

            var lastDoc: DocumentSnapshot? = null
            var totalCargados = 0

            while (true) {
                var query = col
                    .orderBy("descripcion")
                    .limit(pageSize)

                if (lastDoc != null) {
                    query = query.startAfter(lastDoc)
                }

                val snap = query.get().await()
                if (snap.isEmpty) break

                for (doc in snap.documents) {
                    val codigo = doc.id
                    val descRaw = (
                            doc.getString("descripcion")
                                ?: doc.getString("nombreComercial")
                                ?: doc.getString("nombreNormalizado")
                                ?: ""
                            ).trim()

                    // ⚠️ AHORA sí guardamos la UNIDAD real
                    val unidad = doc.extractUnidad()

                    val descUi = "$codigo - $descRaw"

                    lista.add(descUi)
                    mapa[descUi] = codigo to unidad
                }

                totalCargados += snap.size()
                lastDoc = snap.documents.last()

                if (snap.size() < pageSize.toInt()) break
                if (totalCargados >= hardLimit) break
            }

            productList.value = lista
            productMap.value = mapa
        } catch (e: Exception) {
            errorMsg = e.message ?: "Error cargando productos"
        } finally {
            isLoading = false
        }
    }

    // 2) FILTRO LOCAL según lo escrito por el usuario
    val filteredList by remember(productList.value, productMap.value, searchQuery) {
        mutableStateOf(
            if (searchQuery.isBlank()) {
                productList.value
            } else {
                val q = searchQuery
                productList.value.filter { descUi ->
                    val (codigo) =
                        productMap.value[descUi] ?: return@filter false

                    // descUi visible trae "CODIGO - DESCRIPCION COMPLETA"
                    // pero aquí usamos la descripción "unidadOrDesc" NO,
                    // mejor la parte después del guion para que no dependa del bug:
                    val descSolo = descUi.substringAfter(" - ", missingDelimiterValue = descUi)

                    coincideConBusqueda(
                        descripcion = descSolo,
                        codigo = codigo,
                        rawQuery = q
                    )
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = { showProductDialog.value = false },
        confirmButton = {
            TextButton(onClick = { showProductDialog.value = false }) { Text("Cerrar") }
        },
        title = { Text("Selecciona un Producto") },
        text = {
            Column(Modifier.fillMaxWidth()) {

                // Caja de búsqueda
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { newValue ->
                        searchQuery = newValue.uppercase()
                    },
                    label = { Text("Buscar producto") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Borrar búsqueda"
                                )
                            }
                        }
                    }
                )

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
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp, max = 480.dp)
                        ) {
                            items(filteredList) { descripcionUi ->
                                TextButton(
                                    onClick = {
                                        onUserInteraction()
                                        productMap.value[descripcionUi]?.let { (codigo, um) ->
                                            sku.value = codigo
                                            qrCodeContentSku.value = codigo
                                            productoDescripcion.value = descripcionUi
                                            unidadMedida.value = um
                                        }
                                        showProductDialog.value = false
                                        try { focusRequesterLote.requestFocus() } catch (_: Exception) {}
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(Modifier.fillMaxWidth()) {
                                        Text(
                                            text = descripcionUi,
                                            maxLines = 4,
                                            overflow = TextOverflow.Ellipsis,
                                            color = Color.Black,
                                        )
                                    }
                                }
                                HorizontalDivider()
                            }
                        }

                        if (!isLoading && filteredList.isEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "No hay productos que coincidan con la búsqueda.",
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

private fun coincideConBusqueda(
    descripcion: String?,
    codigo: String?,
    rawQuery: String
): Boolean {
    val query = rawQuery.trim().lowercase()
    if (query.isBlank()) return true

    val terms = query.split(Regex("\\s+"))
        .filter { it.isNotBlank() }

    if (terms.isEmpty()) return true

    val textoDescripcion = (descripcion ?: "").lowercase()
    val textoCodigo = (codigo ?: "").lowercase()

    // Todas las palabras deben aparecer en descripción O código
    return terms.all { term ->
        textoDescripcion.contains(term) || textoCodigo.contains(term)
    }
}
