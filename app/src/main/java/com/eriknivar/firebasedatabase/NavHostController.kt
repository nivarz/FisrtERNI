package com.eriknivar.firebasedatabase

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay


@Composable
fun NetworkAwareNavGraph() {
    val context = LocalContext.current
    val isConnected = remember { mutableStateOf(true) }
    val wasDisconnected = remember { mutableStateOf(false) }
    val showRestoredBanner = remember { mutableStateOf(false) }

    val navController = rememberNavController()

    DisposableEffect(Unit) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder().build()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                if (wasDisconnected.value) {
                    showRestoredBanner.value = true
                    wasDisconnected.value = false

                    // Ocultar el banner después de 3 segundos
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(3000)
                        showRestoredBanner.value = false
                    }
                }
                isConnected.value = true
            }

            override fun onLost(network: Network) {
                isConnected.value = false
                wasDisconnected.value = true
            }
        }

        connectivityManager.registerNetworkCallback(request, callback)

        onDispose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }

    NavHost(navController = navController, startDestination = "login") {
        composable("login") { LoginScreen(navController, isConnected) }
        composable("storagetype") { SelectStorageFragment(navController, isConnected) }
        composable("inventoryentry/{storageType}") { backStackEntry ->
            val storageType = backStackEntry.arguments?.getString("storageType")
            FirestoreApp(navController, isConnected = isConnected, showRestoredBanner, storageType.toString())}
        //composable("inventoryentry") {FirestoreApp(navController, isConnected = isConnected, showRestoredBanner)}
        composable("inventoryreports") { InventoryReportsFragment(navController, isConnected) }
        composable("editscounts") { EditCountsFragment(navController, isConnected) }
        composable("masterdata") { MasterDataFragment(navController, isConnected) }
    }
}

