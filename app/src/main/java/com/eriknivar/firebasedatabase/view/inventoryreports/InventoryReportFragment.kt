package com.eriknivar.firebasedatabase.view.inventoryreports

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import com.eriknivar.firebasedatabase.view.NavigationDrawer
import com.eriknivar.firebasedatabase.view.storagetype.DataFields
import com.eriknivar.firebasedatabase.viewmodel.UserViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.delay

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


    LaunchedEffect(usuario, tipoUsuario) {
        if (usuario.isNotEmpty()) {
            val firestore = Firebase.firestore

            if (tipoUsuario.lowercase().trim() == "admin" || tipoUsuario.lowercase()
                    .trim() == "superuser"
            ) {
                fetchAllInventory(firestore, allData, tipoUsuario)
            } else {
                fetchFilteredInventoryByUser(firestore, allData, usuario, tipoUsuario)
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

    val lastInteractionTime = remember { mutableLongStateOf(System.currentTimeMillis()) }

    fun actualizarActividad() {
        lastInteractionTime.longValue = System.currentTimeMillis()

    }

    LaunchedEffect(lastInteractionTime.longValue) {
        while (true) {
            delay(60_000)
            val tiempoActual = System.currentTimeMillis()
            val tiempoInactivo = tiempoActual - lastInteractionTime.longValue

            if (tiempoInactivo >= 10 * 60_000) {

                userViewModel.clearUser()

                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                }

                break
            }
        }
    }

    NavigationDrawer(navController, "Reportes del Inventario", userViewModel, dummyLocation, dummySku, dummyQuantity, dummyLot, dummyDateText) {
        InventoryReportFiltersScreen(
            userViewModel = userViewModel,
            allData = allData,
            tipoUsuario = tipoUsuario,
            onSuccess = { showSuccessDialog = true },
            puedeModificarRegistro = { usuario, tipoCreador ->
                userViewModel.puedeModificarRegistro(usuario, tipoCreador)
            },
            onUserInteraction = { actualizarActividad() }
        )
    }
}









