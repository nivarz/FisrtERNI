package com.eriknivar.firebasedatabase.view.inventoryreports

import android.util.Log
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.compose.ui.window.DialogProperties
import com.eriknivar.firebasedatabase.navigation.NavigationDrawer
import com.eriknivar.firebasedatabase.viewmodel.UserViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.delay
import com.eriknivar.firebasedatabase.view.storagetype.DataFields

@Composable
fun InventoryReportsFragment(
    navController: NavHostController,
    userViewModel: UserViewModel
) {
    // Lista “base” para que el filtro pueda usarla en Limpiar filtros
    val allData = remember { mutableStateListOf<DataFields>() }
    val tipoUsuario by userViewModel.tipo.observeAsState("")

    // dummies para el NavigationDrawer
    val dummyLocation = remember { mutableStateOf("") }
    val dummySku = remember { mutableStateOf("") }
    val dummyQuantity = remember { mutableStateOf("") }
    val dummyLot = remember { mutableStateOf("") }
    val dummyDateText = remember { mutableStateOf("") }

    val currentUserId = userViewModel.documentId.value ?: ""
    val currentSessionId = userViewModel.sessionId.value

    // 🔄 Solo para mantener actualizado el clienteId del usuario
    DisposableEffect(currentUserId, currentSessionId) {
        if (currentUserId.isBlank()) {
            Log.w(
                "FirestoreListener",
                "No se puede suscribir a /usuarios: currentUserId vacío. " +
                        "Probablemente todavía no se ha cargado el usuario."
            )
            return@DisposableEffect onDispose { }
        }

        val firestore = Firebase.firestore

        val listenerRegistration = firestore
            .collection("usuarios")
            .document(currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreListener", "Error en snapshotListener", error)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val clienteId = snapshot.getString("clienteId") ?: ""
                    userViewModel.setClienteId(clienteId)
                }
            }

        onDispose {
            listenerRegistration.remove()
        }
    }

    var showSuccessDialog by remember { mutableStateOf(false) }

    // ✅ Dialog de confirmación de actualización
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
            }
        )
    }
}