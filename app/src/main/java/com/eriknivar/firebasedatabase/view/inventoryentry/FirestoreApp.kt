package com.eriknivar.firebasedatabase.view.inventoryentry

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import com.eriknivar.firebasedatabase.view.utility.ScreenWithNetworkBanner
import com.eriknivar.firebasedatabase.viewmodel.UserViewModel

@Composable
fun FirestoreApp(
    navController: NavHostController,
    isConnected: State<Boolean>,
    storageType: String,
    userViewModel: UserViewModel
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val productoDescripcion = remember { mutableStateOf("") }

    ScreenWithNetworkBanner(isConnected = isConnected) {
        NavigationDrawer(navController, storageType, userViewModel) {
            Scaffold(
                // ❌ Quitamos snackbarHost aquí, lo colocaremos manualmente en el centro
                topBar = { /* Puedes mantener tu TopAppBar si la tienes aquí */ }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopStart) // Contenido principal arriba
                    ) {
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
                                Log.i("LOG_TAG", "Clicked back")
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 1.dp))

                        OutlinedTextFieldsInputs(
                            productoDescripcion = productoDescripcion,
                            userViewModel = userViewModel,
                            snackbarHostState = snackbarHostState,
                            coroutineScope = coroutineScope,
                            localidad = storageType

                        )
                    }

                    // ✅ Snackbar centrado en pantalla
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 500.dp), // 🔼 Ajusta la altura según lo que necesites
                        contentAlignment = Alignment.BottomCenter // Centrado pero más arriba
                    ) {
                        SnackbarHost(hostState = snackbarHostState)
                    }

                }
            }
        }
    }
}





