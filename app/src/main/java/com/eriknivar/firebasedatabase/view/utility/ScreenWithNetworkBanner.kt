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
) {
    Column {
        if (showDisconnectedBanner && !showRestoredBanner) {
            NetworkBanner(
                message = "¡Sin conexión a Internet!",
                backgroundColor = Color.Red,
                onClose = onCloseDisconnected
            )
        }

        if (showRestoredBanner && !showDisconnectedBanner) {
            NetworkBanner(
                message = "¡Conexión restaurada!",
                backgroundColor = Color(0xFF4CAF50),
                onClose = onCloseRestored
            )
        }

        content()
    }
}








