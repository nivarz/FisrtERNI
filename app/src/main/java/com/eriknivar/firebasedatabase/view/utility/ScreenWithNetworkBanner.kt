package com.eriknivar.firebasedatabase.view.utility

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Color

@Composable
fun ScreenWithNetworkBanner(
    showDisconnectedBanner: Boolean,
    showRestoredBanner: Boolean,
    onCloseDisconnected: () -> Unit,
    onCloseRestored: () -> Unit,
    content: @Composable () -> Unit
){

    Column {

        // Regla: si hay “restaurada”, apaga el rojo.
        LaunchedEffect(showRestoredBanner) {
            if (showRestoredBanner) onCloseDisconnected()
        }

        when {
            showRestoredBanner -> {
                NetworkBanner(
                    message = "¡Sin conexión a Internet!",
                    backgroundColor = Color.Red,
                    onClose = onCloseRestored
                )
            }

            // 🔒 Solo deja ver “Sin conexión” si NO está restaurada
            // (y opcionalmente: solo si showDisconnectedBanner == true)
            showDisconnectedBanner && !showRestoredBanner -> {
                NetworkBanner(
                    message = "¡Conexión restaurada!",
                    backgroundColor = Color(0xFF4CAF50),
                    onClose = onCloseDisconnected
                )
            }
        }

        content()
    }
}