package com.eriknivar.firebasedatabase.view.utility


import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay

@Composable
fun ScreenWithNetworkBanner(
    isConnected: State<Boolean>,
    content: @Composable () -> Unit
) {
    val showRestoredBanner = remember { mutableStateOf(false) }
    val wasDisconnected = remember { mutableStateOf(false) }

    LaunchedEffect(isConnected.value) {
        if (!isConnected.value) {
            wasDisconnected.value = true
        } else if (wasDisconnected.value) {
            showRestoredBanner.value = true
            wasDisconnected.value = false

            delay(3000)
            showRestoredBanner.value = false


        }
    }

    Column {
        when {
            !isConnected.value -> {
                // 🔴 Mostrar banner rojo si no hay conexión
                NetworkBanner(message = "¡Sin conexión a Internet!", backgroundColor = Color.Red)
            }

            showRestoredBanner.value -> {
                // ✅ Mostrar banner verde cuando se recupere la conexión
                NetworkBanner(message = "¡Conexión restaurada!", backgroundColor = Color(0xFF4CAF50))
            }
        }

        content()
    }

}





