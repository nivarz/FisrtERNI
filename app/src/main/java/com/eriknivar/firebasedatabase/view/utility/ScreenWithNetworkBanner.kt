package com.eriknivar.firebasedatabase.view.utility

import android.media.MediaPlayer
import androidx.compose.animation.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import com.eriknivar.firebasedatabase.R // Ajusta tu paquete

@Composable
fun ScreenWithNetworkBanner(
    isConnected: State<Boolean>,
    content: @Composable () -> Unit
) {
    val showRestoredBanner = remember { mutableStateOf(false) }
    val wasDisconnected = remember { mutableStateOf(false) }
    val context = LocalContext.current

    fun playSound(soundResId: Int) {
        try {
            val mediaPlayer = MediaPlayer.create(context, soundResId)
            mediaPlayer.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    LaunchedEffect(isConnected.value) {
        if (!isConnected.value) {
            wasDisconnected.value = true
            playSound(R.raw.alerta_internet) // 🔴 Sonido al perder conexión
        } else if (wasDisconnected.value) {
            showRestoredBanner.value = true
            wasDisconnected.value = false

            playSound(R.raw.alerta_internet) // ✅ Sonido al restaurar conexión (puedes usar otro si gustas)

            delay(3000)
            showRestoredBanner.value = false
        }
    }

    Column {
        AnimatedVisibility(
            visible = !isConnected.value || showRestoredBanner.value,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -40 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -40 })
        ) {
            if (!isConnected.value) {
                NetworkBanner(
                    message = "¡Sin conexión a Internet!",
                    backgroundColor = Color.Red
                )
            } else if (showRestoredBanner.value) {
                NetworkBanner(
                    message = "¡Conexión restaurada!",
                    backgroundColor = Color(0xFF4CAF50)
                )
            }
        }

        content()
    }
}






