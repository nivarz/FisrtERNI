package com.eriknivar.firebasedatabase.view.inventoryentry

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.eriknivar.firebasedatabase.view.NavigationDrawer
import com.eriknivar.firebasedatabase.view.utility.ScreenWithNetworkBanner
import com.eriknivar.firebasedatabase.viewmodel.UserViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

@Composable
fun ReconteoAsignadoScreen(
    navController: NavHostController,
    userViewModel: UserViewModel,
) {

    val usuarioId = userViewModel.documentId.observeAsState("").value
    val reconteos = remember { mutableStateListOf<Map<String, Any>>() }
    var isLoading by remember { mutableStateOf(true) }
    val context = LocalContext.current

    val dummyLocation = remember { mutableStateOf("") }
    val dummySku = remember { mutableStateOf("") }
    val dummyQuantity = remember { mutableStateOf("") }
    val dummyLot = remember { mutableStateOf("") }
    val dummyDateText = remember { mutableStateOf("") }

    DisposableEffect(usuarioId) {
        val listener = FirebaseFirestore.getInstance()
            .collection("reconteo_pendiente")
            .whereEqualTo("usuarioAsignado", usuarioId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("RECONTEO_DEBUG", "Error escuchando reconteos", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    reconteos.clear()
                    reconteos.addAll(snapshot.documents.mapNotNull { it.data })
                    Log.d("RECONTEO_DEBUG", "🔁 Actualización en tiempo real: ${reconteos.size}")
                } else {
                    Log.w("RECONTEO_DEBUG", "❗Snapshot nulo sin excepción")
                }
                isLoading = false

            }

        onDispose {
            listener.remove()
        }
    }

    val lastInteractionTime = remember { mutableLongStateOf(System.currentTimeMillis()) }

    fun actualizarActividad() {
        lastInteractionTime.longValue = System.currentTimeMillis()

    }

    LaunchedEffect(lastInteractionTime.longValue) {
        while (true) {
            delay(60_000)
            val tiempoActual = System.currentTimeMillis()
            val tiempoInactivo = tiempoActual - lastInteractionTime.longValue

            if (tiempoInactivo >= 30 * 60_000) {
                val documentId = userViewModel.documentId.value ?: ""
                Firebase.firestore.collection("usuarios")
                    .document(documentId)
                    .update("sessionId", "")
                Toast.makeText(context, "Sesión finalizada por inactividad", Toast.LENGTH_LONG)
                    .show()

                userViewModel.clearUser()

                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                }

                break
            }
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

                Text(
                    "Reconteos asignados a.... $nombreUsuario",
                    style = MaterialTheme.typography.titleLarge
                )

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
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures {
                                    actualizarActividad()
                                }
                            }
                    ) {
                        Log.d(
                            "RECONTEOS_FILTRADOS",
                            "Total filtrados: ${reconteos.filter { (it["estado"] as? String)?.lowercase() == "pendiente" }.size}"
                        )
                        items(
                            reconteos.filter {
                                (it["estado"] as? String)?.lowercase() == "pendiente"
                            },
                            key = {
                                "${it["sku"]}_${it["ubicacion"]}_${it["lote"]}_${it["cantidadEsperada"]}"
                            }
                        ) { item ->
                            ReconteoCard(
                                item = item,
                                onEliminarCard = {
                                    reconteos.remove(item)
                                    actualizarActividad()
                                },
                                actualizarActividad = { actualizarActividad() }

                            )

                        }
                    }
                }
            }
        }
    }
}


