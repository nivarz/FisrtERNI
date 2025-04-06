package com.eriknivar.firebasedatabase.view.inventoryreports

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import com.eriknivar.firebasedatabase.view.NavigationDrawer
import com.eriknivar.firebasedatabase.view.storagetype.DataFields
import com.eriknivar.firebasedatabase.view.utility.ScreenWithNetworkBanner
import com.eriknivar.firebasedatabase.viewmodel.UserViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore


@Composable
fun InventoryReportsFragment(
    navController: NavHostController,
    isConnected: State<Boolean>,
    userViewModel: UserViewModel
) {
    val allData = remember { mutableStateListOf<DataFields>() }
    val usuario by userViewModel.nombre.observeAsState("")

    ScreenWithNetworkBanner(isConnected) {
        NavigationDrawer(navController, "Reportes del Inventario", userViewModel) {

            LaunchedEffect(usuario) {
                if (usuario.isNotEmpty()) {
                    val firestore = Firebase.firestore
                    fetchFilteredInventoryByUser(firestore, allData, usuario)
                }
            }

            InventoryReportFiltersScreen(
                userViewModel = userViewModel,
                allData = allData,
                onExport = {
                    Log.d("Export", "Exportar PDF/Excel")
                }
            )
        }
    }
}




