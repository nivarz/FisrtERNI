package com.eriknivar.firebasedatabase.view.settings

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.eriknivar.firebasedatabase.view.NavigationDrawer
import com.eriknivar.firebasedatabase.viewmodel.UserViewModel
import kotlinx.coroutines.delay

@Composable
fun SettingsFragment(
    navController: NavHostController,
    userViewModel: UserViewModel
) {
    val tipo = userViewModel.tipo.value ?: ""

    val dummyLocation = remember { mutableStateOf("") }
    val dummySku = remember { mutableStateOf("") }
    val dummyQuantity = remember { mutableStateOf("") }
    val dummyLot = remember { mutableStateOf("") }
    val dummyDateText = remember { mutableStateOf("") }

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

    NavigationDrawer(navController, "Configuración", userViewModel, dummyLocation, dummySku, dummyQuantity, dummyLot, dummyDateText) { // ✅ Aquí está el Drawer

        ConfiguracionUsuariosScreen(
            userViewModel = userViewModel,
            onUserInteraction = { actualizarActividad() })

                BackHandler(true) {
                    Log.i("LOG_TAG", "Clicked back") // Desactiva el botón atrás
                }
            }
    }



