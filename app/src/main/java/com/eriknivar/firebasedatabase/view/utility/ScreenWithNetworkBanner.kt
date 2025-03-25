package com.eriknivar.firebasedatabase.view.utility

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay

@Composable
fun ScreenWithNetworkBanner(
    isConnected: State<Boolean>,
    content: @Composable () -> Unit
) {
    val showRestoredBanner = remember { mutableStateOf(false) }
    val wasDisconnected = remember { mutableStateOf(false) }

    // Efecto para mostrar el banner verde solo si antes hubo una desconexión
    LaunchedEffect(isConnected.value) {
        if (!isConnected.value) {
            wasDisconnected.value = true
        } else if (wasDisconnected.value) {
            showRestoredBanner.value = true
            wasDisconnected.value = false

            delay(3000) // ⏳ Oculta el banner luego de 3 segundos
            showRestoredBanner.value = false
        }
    }

    Column {
        if (!isConnected.value) {
            NetworkBanner(message = "¡Sin conexión a Internet!", backgroundColor = Color.Red)
        }

        if (showRestoredBanner.value) {
            NetworkBanner(message = "¡Conexión restaurada!", backgroundColor = Color(0xFF4CAF50))
        }

        content() // Aquí va el contenido de la pantalla
    }
}


