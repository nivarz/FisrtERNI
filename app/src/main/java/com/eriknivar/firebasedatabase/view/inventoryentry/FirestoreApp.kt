package com.eriknivar.firebasedatabase.view.inventoryentry

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.eriknivar.firebasedatabase.view.NavigationDrawer
import com.eriknivar.firebasedatabase.view.utility.ScreenWithNetworkBanner


@Composable
fun FirestoreApp(
    navController: NavHostController,
    isConnected: State<Boolean>,
    storageType: String
)
 {

    ScreenWithNetworkBanner(isConnected = isConnected) {


        val productoDescripcion = remember { mutableStateOf("") }



            NavigationDrawer(navController, storageType) {
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






