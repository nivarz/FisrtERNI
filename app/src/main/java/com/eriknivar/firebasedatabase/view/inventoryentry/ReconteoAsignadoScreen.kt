package com.eriknivar.firebasedatabase.view.inventoryentry

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.eriknivar.firebasedatabase.view.NavigationDrawer
import com.eriknivar.firebasedatabase.view.utility.ScreenWithNetworkBanner
import com.eriknivar.firebasedatabase.viewmodel.UserViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@Composable
fun ReconteoAsignadoScreen(
    navController: NavHostController,
    userViewModel: UserViewModel,
) {

    val usuarioId = userViewModel.documentId.observeAsState("").value
    val reconteos = remember { mutableStateListOf<Map<String, Any>>() }
    var isLoading by remember { mutableStateOf(true) }

    val dummyLocation = remember { mutableStateOf("") }
    val dummySku = remember { mutableStateOf("") }
    val dummyQuantity = remember { mutableStateOf("") }
    val dummyLot = remember { mutableStateOf("") }
    val dummyDateText = remember { mutableStateOf("") }

    LaunchedEffect(usuarioId) {
        Log.d("RECONTEO_DEBUG", "Consultando reconteos para usuario: $usuarioId")
        isLoading = true
        try {
            val snapshot = FirebaseFirestore.getInstance()
                .collection("reconteo_pendiente")
                .whereEqualTo("usuarioAsignado", usuarioId)
                .get()
                .await()

            reconteos.clear()
            reconteos.addAll(snapshot.documents.mapNotNull { it.data })
            Log.d("RECONTEO_DEBUG", "Reconteos obtenidos: ${reconteos.size}")
        } catch (e: Exception) {
            Log.e("RECONTEO_DEBUG", "Error consultando reconteos", e)
        } finally {
            isLoading = false
        }
    }

    ScreenWithNetworkBanner(
        showDisconnectedBanner = false,
        showRestoredBanner = false,
        onCloseDisconnected = {},
        onCloseRestored = {}
    ) {
        NavigationDrawer(
            navController,
            "Reconteos Asignados",
            userViewModel,
            dummyLocation,
            dummySku,
            dummyQuantity,
            dummyLot,
            dummyDateText
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                val nombreUsuarioRaw = userViewModel.nombre.observeAsState("").value
                val nombreUsuario = nombreUsuarioRaw
                    .lowercase()
                    .split(" ")
                    .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

                Text("Reconteos asignados a.... $nombreUsuario", style = MaterialTheme.typography.titleLarge)

                Spacer(modifier = Modifier.height(16.dp))

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (reconteos.isEmpty()) {
                    Text("No hay reconteos asignados.")
                } else {
                    Text("🧪 Debug info", style = MaterialTheme.typography.bodySmall)
                    Text("👤 Usuario actual: $usuarioId")
                    Text("📦 Total reconteos cargados: ${reconteos.size}")
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(
                            reconteos.filter { it["estado"] == "pendiente" },
                            key = {
                                "${it["sku"]}_${it["ubicacion"]}_${it["lote"]}_${it["cantidadEsperada"]}"
                            }
                        ) { item ->
                            ReconteoCard(
                                item = item,
                                onEliminarCard = {
                                    reconteos.remove(item) // 🔥 Elimina el card de la lista en tiempo real
                                }
                            )
                        }
                    }



                }
            }
        }
    }
}


