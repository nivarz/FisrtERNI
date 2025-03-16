package com.eriknivar.firebasedatabase.view.inventoryentry

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun ProductSelectionDialog(
    productList: MutableState<List<String>>,
    productMap: MutableState<Map<String, Pair<String, String>>>,
    showProductDialog : MutableState<Boolean>,
    sku: MutableState<String>,
    qrCodeContentSku: MutableState<String>,
    productoDescripcion: MutableState<String>,
    unidadMedida: MutableState<String>,
    productDescriptions: MutableState<List<String>>
)

{

    if (showProductDialog.value) {

        var searchQuery by remember { mutableStateOf("") } // Estado para la búsqueda
        var isLoading by remember { mutableStateOf(true) } // Estado para mostrar el loading

        val db = FirebaseFirestore.getInstance()

        // Llamar a la función de búsqueda de productos cuando se abra el diálogo

        LaunchedEffect(Unit) {
            db.collection("productos").get().addOnSuccessListener { result ->
                productDescriptions.value = result.documents.mapNotNull { it.getString("descripcion") }
            }.addOnFailureListener { e ->
                Log.e("Firestore", "Error al obtener descripciones: ", e)
            }
        }

        // Llamar a la función de búsqueda de productos cuando se abra el diálogo

        LaunchedEffect(Unit) {
            isLoading = true // 🔥 Muestra el indicador de carga antes de obtener los datos
            buscarProductos(db) { lista, mapa ->
                productList.value = lista.sorted() // 🔥 Ordena los productos alfabéticamente
                productMap.value = mapa
                productDescriptions.value = lista // ✅ Ahora `productDescriptions` tiene uso
                isLoading = false // 🔥 Oculta el loading cuando se cargan los datos
            }
        }

        AlertDialog(
            onDismissRequest = { showProductDialog.value = false },
            confirmButton = {
                TextButton(onClick = { showProductDialog.value = false }) {
                    Text("Cerrar")
                }
            },
            title = { Text("Selecciona un Producto") },
            text = {
                Column {
                    // 🔍 Campo de búsqueda
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Buscar producto") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )

                    // 🔥 Mostrar indicador de carga mientras los productos se obtienen
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        // 🔥 Filtrar y ordenar productos por orden alfabético
                        val filteredProducts = productList.value.filter { it.contains(searchQuery, ignoreCase = true) }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 0.dp) // 🔥 Reduce espacio vertical general
                        ) {
                            items(filteredProducts) { descripcion ->
                                TextButton(
                                    onClick = {
                                        val productoSeleccionado = productMap.value[descripcion] // 🔥 Ahora accede a `value`
                                        if (productoSeleccionado != null) {
                                            val (codigoSeleccionado, unidadMedidaSeleccionada) = productoSeleccionado
                                            sku.value = codigoSeleccionado  // 🔥 Actualizar SKU
                                            qrCodeContentSku.value = codigoSeleccionado
                                            productoDescripcion.value = descripcion
                                            unidadMedida.value = unidadMedidaSeleccionada // ✅ Actualizar UM correctamente
                                        }
                                        showProductDialog.value = false // 🔥 Cierra el diálogo
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 0.dp, vertical = 0.dp)
                                ) {
                                    Text(
                                        text = descripcion,
                                        fontSize = 14.sp,
                                        color = Color.Black,
                                        textAlign = TextAlign.Start,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }


                                HorizontalDivider(
                                    color = Color.Gray, // Color de la línea
                                    thickness = 1.dp, // Grosor de la línea
                                    modifier = Modifier.padding(horizontal = 8.dp) // Espaciado lateral
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}