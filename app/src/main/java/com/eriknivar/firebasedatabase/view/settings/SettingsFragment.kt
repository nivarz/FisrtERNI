package com.eriknivar.firebasedatabase.view.settings

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.eriknivar.firebasedatabase.navigation.NavigationDrawer
import com.eriknivar.firebasedatabase.viewmodel.UserViewModel

@Composable
fun SettingsFragment(
    navController: NavHostController,
    userViewModel: UserViewModel
) {

    val tipo = userViewModel.tipo.value ?: ""

    if (tipo.isNotBlank() && tipo.lowercase() != "admin" && tipo.lowercase() != "superuser") {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White), // 🔷 Fondo blanco
            contentAlignment = Alignment.TopStart
        ) {
            Text(
                text = "Acceso restringido",
                color = Color.Red,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
        }
        return
    }

    NavigationDrawer(
        navController = navController,
        storageType = "Configuración",
        userViewModel = userViewModel
    ) {
        ConfiguracionMenuScreen(navController = navController)

        BackHandler(true) {
            Log.i("LOG_TAG", "Clicked back")
        }
    }

}



