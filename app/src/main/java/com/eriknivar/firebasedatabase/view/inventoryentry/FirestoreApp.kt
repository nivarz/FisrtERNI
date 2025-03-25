package com.eriknivar.firebasedatabase.view.inventoryentry

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.eriknivar.firebasedatabase.view.NavigationDrawer



@Composable
fun FirestoreApp(
    navController: NavHostController,
    isConnected: State<Boolean>,
    showRestoredBanner: State<Boolean> // ✅ Banner conexión restaurada
) {
    val productoDescripcion = remember { mutableStateOf("") }

    Column {
        // 🔴 Mostrar banner si no hay conexión
        if (!isConnected.value) {
            NetworkBanner(message = "¡Sin conexión a Internet!", backgroundColor = Color.Red)
        }

        // ✅ Mostrar banner de conexión restaurada
        if (showRestoredBanner.value) {
            NetworkBanner(message = "¡Conexión restaurada!", backgroundColor = Color(0xFF4CAF50))
        }

        NavigationDrawer(navController) {
            Box {
                if (productoDescripcion.value.isNotBlank()) {
                    Text(
                        text = productoDescripcion.value,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Blue,
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                BackHandler(true) {
                    Log.i("LOG_TAG", "Clicked back") // Deshabilitar botón de atrás
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 1.dp))

            OutlinedTextFieldsInputs(productoDescripcion)
        }
    }
}

@Composable
fun NetworkBanner(
    message: String,
    backgroundColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}




