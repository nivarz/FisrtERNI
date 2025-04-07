package com.eriknivar.firebasedatabase.viewmodel

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController

@Composable
fun SplashScreen(navController: NavHostController, userViewModel: UserViewModel) {
    val isLoggedOut = userViewModel.nombre.observeAsState("").value.isEmpty()
    val isInitialized = userViewModel.isInitialized.observeAsState(false).value

    LaunchedEffect(isLoggedOut, isInitialized) {
        if (isInitialized) {
            if (isLoggedOut) {
                navController.navigate("login") {
                    popUpTo("splash") { inclusive = true }
                }
            } else {
                navController.navigate("storagetype") {
                    popUpTo("splash") { inclusive = true }
                }
            }
        }
    }

    // Mientras tanto, mostramos una pantalla blanca o de carga
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Cargando...", fontSize = 20.sp)
    }
}
