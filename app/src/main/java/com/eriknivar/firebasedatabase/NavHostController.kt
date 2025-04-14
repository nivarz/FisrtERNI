package com.eriknivar.firebasedatabase

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.eriknivar.firebasedatabase.view.settings.SettingsFragment
import com.eriknivar.firebasedatabase.view.inventoryentry.FirestoreApp
import com.eriknivar.firebasedatabase.view.inventoryreports.InventoryReportsFragment
import com.eriknivar.firebasedatabase.view.login.LoginScreen
import com.eriknivar.firebasedatabase.view.masterdata.MasterDataFragment
import com.eriknivar.firebasedatabase.view.storagetype.SelectStorageFragment
import com.eriknivar.firebasedatabase.viewmodel.SplashScreen
import com.eriknivar.firebasedatabase.viewmodel.UserViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@Composable
fun NetworkAwareNavGraph(
    navController: NavHostController,
    userViewModel: UserViewModel
) {
    val context = LocalContext.current
    val isConnected = remember { mutableStateOf(true) }
    val wasDisconnected = remember { mutableStateOf(false) }
    val showRestoredBanner = remember { mutableStateOf(false) }

    val isLoggedOut = userViewModel.nombre.observeAsState("").value.isEmpty()
    val isInitialized = userViewModel.isInitialized.observeAsState(false).value

    LaunchedEffect(isLoggedOut, isInitialized) {
        if (isInitialized && isLoggedOut) {
            navController.navigate("login") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    DisposableEffect(Unit) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder().build()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                isConnected.value = true // ✅ Primero actualiza conexión

                if (wasDisconnected.value) {
                    showRestoredBanner.value = true
                    wasDisconnected.value = false

                    CoroutineScope(Dispatchers.Main).launch {
                        delay(3000)
                        showRestoredBanner.value = false
                    }
                }
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


    NavHost(navController = navController, startDestination = "splash") {
        composable("login") { LoginScreen(navController, isConnected, userViewModel) }
        composable("storagetype") { SelectStorageFragment(navController, isConnected, userViewModel) }
        composable("inventoryentry/{storageType}") { backStackEntry ->
            val storageType = backStackEntry.arguments?.getString("storageType")
            FirestoreApp(navController, isConnected = isConnected, storageType = storageType.orEmpty(), userViewModel)}

            composable("inventoryreports") { InventoryReportsFragment(navController, isConnected, userViewModel) }
        composable("settings") { SettingsFragment(navController, isConnected, userViewModel) }
        composable("masterdata") { MasterDataFragment(navController, isConnected, userViewModel) }
        composable("splash") { SplashScreen(navController, userViewModel) }
    }
}

