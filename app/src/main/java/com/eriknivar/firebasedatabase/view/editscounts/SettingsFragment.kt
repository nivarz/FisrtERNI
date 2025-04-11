package com.eriknivar.firebasedatabase.view.editscounts

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.eriknivar.firebasedatabase.view.NavigationDrawer
import com.eriknivar.firebasedatabase.view.utility.ScreenWithNetworkBanner
import com.eriknivar.firebasedatabase.viewmodel.UserViewModel

@Composable
fun SettingsFragment(
    navController: NavHostController,
    isConnected: State<Boolean>,
    userViewModel: UserViewModel
) {
    ScreenWithNetworkBanner(isConnected) {
        NavigationDrawer(navController, "Configuración", userViewModel) { // ✅ Aquí está el Drawer

            Box(modifier = Modifier.fillMaxSize()) {
                ConfiguracionUsuariosScreen()

                BackHandler(true) {
                    Log.i("LOG_TAG", "Clicked back") // Desactiva el botón atrás
                }
            }

        }
    }
}


