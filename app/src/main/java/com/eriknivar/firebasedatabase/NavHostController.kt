package com.eriknivar.firebasedatabase

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.eriknivar.firebasedatabase.view.editscounts.EditCountsFragment
import com.eriknivar.firebasedatabase.view.inventoryentry.FirestoreApp
import com.eriknivar.firebasedatabase.view.inventoryreports.InventoryReportsFragment
import com.eriknivar.firebasedatabase.view.login.LoginScreen
import com.eriknivar.firebasedatabase.view.masterdata.MasterDataFragment
import com.eriknivar.firebasedatabase.view.storagetype.SelectStorageFragment
import com.eriknivar.firebasedatabase.view.utility.NetworkObserver

@Composable
fun NetworkAwareNavGraph() {
    val context = LocalContext.current
    val isConnected = remember { mutableStateOf(true) }
    val observer = remember { NetworkObserver(context, isConnected) }
    val navController = rememberNavController()

    DisposableEffect(Unit) {
        observer.startObserving()
        onDispose {
            observer.stopObserving()
        }
    }


    NavHost(navController = navController, startDestination = "login") {

        composable(route = "login") {LoginScreen(navController, isConnected) }
        composable(route = "storagetype") { SelectStorageFragment(navController, isConnected) }
        composable(route = "inventoryentry") {FirestoreApp(navController, isConnected) }
        composable(route = "inventoryreports") {InventoryReportsFragment(navController, isConnected) }
        composable(route = "editscounts") {EditCountsFragment(navController, isConnected)}
        composable(route = "masterdata") {MasterDataFragment(navController, isConnected)}


    }
}