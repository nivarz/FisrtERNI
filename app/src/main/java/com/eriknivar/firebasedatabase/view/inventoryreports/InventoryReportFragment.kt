package com.eriknivar.firebasedatabase.view.inventoryreports

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import com.eriknivar.firebasedatabase.navigation.NavigationDrawer
import com.eriknivar.firebasedatabase.view.storagetype.DataFields
import com.eriknivar.firebasedatabase.view.utility.SessionUtils
import com.eriknivar.firebasedatabase.viewmodel.UserViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.delay
import com.eriknivar.firebasedatabase.view.inventoryreports.fetchFilteredInventoryFromFirestore
import com.eriknivar.firebasedatabase.view.inventoryreports.fetchAllInventory


@Composable
fun InventoryReportsFragment(
    navController: NavHostController,
    userViewModel: UserViewModel
) {
    val allData = remember { mutableStateListOf<DataFields>() }
    val usuario by userViewModel.nombre.observeAsState("")
    val tipoUsuario by userViewModel.tipo.observeAsState("")

    val dummyLocation = remember { mutableStateOf("") }
    val dummySku = remember { mutableStateOf("") }
    val dummyQuantity = remember { mutableStateOf("") }
    val dummyLot = remember { mutableStateOf("") }
    val dummyDateText = remember { mutableStateOf("") }

    val context = LocalContext.current
    val currentUserId = userViewModel.documentId.value ?: ""
    val currentSessionId = userViewModel.sessionId.value

    DisposableEffect(currentUserId, currentSessionId) {
        val firestore = Firebase.firestore

        val listenerRegistration = firestore.collection("usuarios")
            .document(currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreListener", "Error en snapshotListener", error)
                    return@addSnapshotListener
                }

                val remoteSessionId = snapshot?.getString("sessionId") ?: ""

                if (remoteSessionId != currentSessionId && !userViewModel.isManualLogout.value) {
                    Toast.makeText(
                        context,
                        "Tu sesión fue cerrada por el administrador",
                        Toast.LENGTH_LONG
                    ).show()

                    userViewModel.clearUser()

                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }

        onDispose {
            listenerRegistration.remove()
        }
    }

    val cid = (userViewModel.clienteId.value ?: "").trim().uppercase()

    LaunchedEffect(usuario, tipoUsuario, cid) {
        if (usuario.isNotEmpty()) {
            val firestore = Firebase.firestore

            if (tipoUsuario.lowercase().trim() == "admin" || tipoUsuario.lowercase()
                    .trim() == "superuser"
            ) {
                fetchAllInventory(
                    firestore,
                    allData,
                    tipoUsuario,
                    clienteId = cid          // ✅ deja SOLO esta forma
                )
            } else {
                fetchFilteredInventoryFromFirestore(
                    db = firestore,
                    clienteId = cid,
                    filters = mapOf("usuario" to usuario),
                    onResult = { nuevos ->
                        allData.clear()
                        allData.addAll(nuevos)
                        Log.d("Reportes", "Cargados (usuario): ${allData.size}")
                    },
                    onError = { e ->
                        Log.e("Reportes", "❌ Error al cargar por usuario", e)
                    }
                )
            }
        }
    }

    var showSuccessDialog by remember { mutableStateOf(false) }

    // ✅ Dialog visual centrado para confirmación de actualización
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            confirmButton = {},
            title = { Text("✔️ Registro actualizado.") },
            text = { Text("Los datos se actualizaron correctamente.") },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        )

        LaunchedEffect(showSuccessDialog) {
            delay(2000)
            showSuccessDialog = false
        }
    }

    val lastInteractionTime =
        remember { mutableLongStateOf(SessionUtils.obtenerUltimaInteraccion(context)) }

    fun actualizarActividad(context: Context) {
        val tiempoActual = System.currentTimeMillis()
        lastInteractionTime.longValue = tiempoActual
        SessionUtils.guardarUltimaInteraccion(context, tiempoActual)
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

    NavigationDrawer(
        navController,
        "Reportes del Inventario",
        userViewModel,
        dummyLocation,
        dummySku,
        dummyQuantity,
        dummyLot,
        dummyDateText
    ) {
        InventoryReportFiltersScreen(
            userViewModel = userViewModel,
            allData = allData,
            tipoUsuario = tipoUsuario,
            onSuccess = { showSuccessDialog = true },
            puedeModificarRegistro = { usuario, tipoCreador ->
                userViewModel.puedeModificarRegistro(usuario, tipoCreador)
            },
            onUserInteraction = { actualizarActividad(context) }
        )
    }
}