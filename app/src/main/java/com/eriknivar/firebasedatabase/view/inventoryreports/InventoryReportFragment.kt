package com.eriknivar.firebasedatabase.view.inventoryreports

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import com.eriknivar.firebasedatabase.view.NavigationDrawer
import com.eriknivar.firebasedatabase.view.storagetype.DataFields
import com.eriknivar.firebasedatabase.viewmodel.UserViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

@Composable
fun InventoryReportsFragment(
    navController: NavHostController,
    userViewModel: UserViewModel
) {
    val allData = remember { mutableStateListOf<DataFields>() }
    val usuario by userViewModel.nombre.observeAsState("")
    val tipoUsuario by userViewModel.tipo.observeAsState("")

    LaunchedEffect(usuario, tipoUsuario) {
        if (usuario.isNotEmpty()) {
            val firestore = Firebase.firestore

            //val tipo = tipoUsuario.lowercase().trim()

            if (tipoUsuario.lowercase().trim() == "admin" || tipoUsuario.lowercase().trim() == "superuser") {
                fetchAllInventory(firestore, allData, tipoUsuario)
            } else {
                fetchFilteredInventoryByUser(firestore, allData, usuario, tipoUsuario)
            }


        }
    }

    NavigationDrawer(navController, "Reportes del Inventario", userViewModel) {
        InventoryReportFiltersScreen(
            userViewModel = userViewModel,
            allData = allData,
            tipoUsuario = tipoUsuario
        )
    }
}









