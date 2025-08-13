package com.eriknivar.firebasedatabase

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.eriknivar.firebasedatabase.view.settings.SettingsFragment
import com.eriknivar.firebasedatabase.view.inventoryentry.FirestoreApp
import com.eriknivar.firebasedatabase.view.inventoryentry.ReconteoAsignadoScreen
import com.eriknivar.firebasedatabase.view.inventoryreports.InventoryReportsFragment
import com.eriknivar.firebasedatabase.view.login.CambiarPasswordScreen
import com.eriknivar.firebasedatabase.view.login.LoginScreen
import com.eriknivar.firebasedatabase.view.masterdata.MasterDataFragment
import com.eriknivar.firebasedatabase.view.settings.settingsmenu.AuditoriaRegistrosScreen
import com.eriknivar.firebasedatabase.view.settings.settingsmenu.GestionClientesScreen
import com.eriknivar.firebasedatabase.view.settings.settingsmenu.LocalidadesScreen
import com.eriknivar.firebasedatabase.view.settings.settingsmenu.UbicacionesScreen
import com.eriknivar.firebasedatabase.view.settings.settingsmenu.UsuariosScreen
import com.eriknivar.firebasedatabase.view.storagetype.SelectStorageFragment
import com.eriknivar.firebasedatabase.view.utility.ScreenWithNetworkBanner
import com.eriknivar.firebasedatabase.viewmodel.SplashScreen
import com.eriknivar.firebasedatabase.viewmodel.UserViewModel
import kotlinx.coroutines.delay

@Composable
fun NetworkAwareNavGraph(
    navController: NavHostController,
    userViewModel: UserViewModel,

    ) {
    val context = LocalContext.current
    val isConnected = remember { mutableStateOf(true) }

    // ⬇️ Estados persistentes para banners
    var showDisconnectedBanner by rememberSaveable { mutableStateOf(false) }
    var showRestoredBanner by rememberSaveable { mutableStateOf(false) }
    var justRecovered by rememberSaveable { mutableStateOf(false) } // ⬅️ NUEVO

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
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder().build()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                if (!isConnected.value) {
                    isConnected.value = true
                }
                if (showDisconnectedBanner) {
                    showDisconnectedBanner = false
                    showRestoredBanner = true
                    justRecovered = true
                }
            }

            override fun onLost(network: Network) {
                isConnected.value = false

                // ✅ Solo mostrar banner si no estaba ya mostrado
                if (!showDisconnectedBanner && !justRecovered) {
                    showDisconnectedBanner = true
                }

                // 🔄 Reiniciar recuperación por si se pierde de nuevo.
                if (justRecovered) {
                    justRecovered = false
                }
            }

        }

        connectivityManager.registerNetworkCallback(request, callback)

        onDispose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }

    LaunchedEffect(showRestoredBanner) {
        if (showRestoredBanner) {
            delay(3000)
            showRestoredBanner = false
            justRecovered = false
        }
    }

    ScreenWithNetworkBanner(
        showDisconnectedBanner = showDisconnectedBanner,
        showRestoredBanner = showRestoredBanner,
        onCloseDisconnected = { showDisconnectedBanner = false },
        onCloseRestored = { showRestoredBanner = false }
    ) {
        NavHost(navController = navController, startDestination = "splash") {
            composable("splash") { SplashScreen(navController, userViewModel) }
            composable("login") { LoginScreen(navController, userViewModel) }
            composable("storagetype") { SelectStorageFragment(navController, userViewModel) }
            composable("inventoryentry/{storageType}") { backStackEntry ->
                val storageType = backStackEntry.arguments?.getString("storageType")
                FirestoreApp(navController, storageType = storageType.orEmpty(), userViewModel)
            }
            composable("inventoryreports") {
                InventoryReportsFragment(
                    navController,
                    userViewModel
                )
            }
            composable("settings") { SettingsFragment(navController, userViewModel) }
            composable("masterdata") { MasterDataFragment(navController, userViewModel) }
            composable("cambiarPassword") { CambiarPasswordScreen(navController, userViewModel) }
            composable("usuarios") {
                UsuariosScreen(
                    navController = navController,
                    userViewModel = userViewModel
                )
            }
            composable("ubicaciones") {
                UbicacionesScreen(
                    navController = navController,
                    userViewModel = userViewModel
                )
            }
            composable("localidades") {
                LocalidadesScreen(
                    navController = navController,
                    userViewModel = userViewModel
                )
            }
            composable("auditoria") {
                AuditoriaRegistrosScreen(
                    navController = navController,
                    userViewModel = userViewModel
                )
            }
            composable("reconteoAsignado") {
                ReconteoAsignadoScreen(
                    navController = navController,
                    userViewModel = userViewModel
                )
            }

            composable("gestion_clientes") {
                GestionClientesScreen(navController = navController, userViewModel = userViewModel)
            }



        }
    }
}

