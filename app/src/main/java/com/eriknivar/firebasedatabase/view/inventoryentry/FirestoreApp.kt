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
fun FirestoreApp(navController: NavHostController, isConnected: State<Boolean>) {

    Column {
        if (!isConnected.value) {
            NetworkBanner()
        }

        NavigationDrawer(navController) {
            val productoDescripcion = remember { mutableStateOf("") }

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
                    Log.i("LOG_TAG", "Clicked back") //Deshabilitar el botón de atrás
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 1.dp))

            OutlinedTextFieldsInputs(productoDescripcion)
        }
    }
}

@Composable
fun NetworkBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Red)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("¡Sin conexión a Internet!", color = Color.White)
    }
}


