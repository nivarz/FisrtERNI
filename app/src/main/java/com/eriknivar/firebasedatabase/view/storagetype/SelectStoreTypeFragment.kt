package com.eriknivar.firebasedatabase.view.storagetype

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.eriknivar.firebasedatabase.navigation.NavigationDrawer
import com.eriknivar.firebasedatabase.view.utility.SessionUtils
import com.eriknivar.firebasedatabase.viewmodel.UserViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.delay

@Composable
fun SelectStorageFragment(
    navController: NavHostController,
    userViewModel: UserViewModel
) {
    val nombreUsuario = userViewModel.nombre.value ?: ""
    val context = LocalContext.current
    val currentUserId = userViewModel.documentId.value ?: ""
    val currentSessionId = userViewModel.sessionId.value

    DisposableEffect(currentUserId, currentSessionId) {
        val firestore = Firebase.firestore

        val listenerRegistration = firestore.collection("usuarios")
            .document(currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreListener", "Error en snapshotListener", error)
                    return@addSnapshotListener
                }

                val remoteSessionId = snapshot?.getString("sessionId") ?: ""

                if (remoteSessionId != currentSessionId && !userViewModel.isManualLogout.value) {
                    Toast.makeText(
                        context,
                        "Tu sesión fue cerrada por el administrador",
                        Toast.LENGTH_LONG
                    ).show()

                    userViewModel.clearUser()

                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }

        onDispose {
            listenerRegistration.remove()
        }
    }

    val dummyLocation = remember { mutableStateOf("") }
    val dummySku = remember { mutableStateOf("") }
    val dummyQuantity = remember { mutableStateOf("") }
    val dummyLot = remember { mutableStateOf("") }
    val dummyDateText = remember { mutableStateOf("") }

    val lastInteractionTime = remember { mutableLongStateOf(SessionUtils.obtenerUltimaInteraccion(context)) }

    fun actualizarActividad(context: Context) {
        val tiempoActual = System.currentTimeMillis()
        lastInteractionTime.longValue = tiempoActual
        SessionUtils.guardarUltimaInteraccion(context, tiempoActual)
    }

    LaunchedEffect(lastInteractionTime.longValue) {
        while (true) {
            delay(60_000)
            val tiempoActual = System.currentTimeMillis()
            val tiempoInactivo = tiempoActual - lastInteractionTime.longValue

            if (tiempoInactivo >= 30 * 60_000) {
                val documentId = userViewModel.documentId.value ?: ""
                Firebase.firestore.collection("usuarios")
                    .document(documentId)
                    .update("sessionId", "")
                Toast.makeText(context, "Sesión finalizada por inactividad", Toast.LENGTH_LONG).show()

                userViewModel.clearUser()

                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                }

                break
            }
        }
    }


    NavigationDrawer(navController, "Seleccionar Almacén", userViewModel, dummyLocation, dummySku, dummyQuantity, dummyLot, dummyDateText) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // 🟡 Simulación de Logo ERNI (puedes reemplazar por Image si ya tienes uno)
            Icon(
                imageVector = Icons.Default.Warehouse, // O tu propio logo
                contentDescription = "Logo ERNI",
                tint = Color(0xFF001F5B),
                modifier = Modifier
                    .size(64.dp)
                    .padding(bottom = 16.dp)
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF3F9)),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Hola, $nombreUsuario 👋",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color(0xFF001F5B)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Bienvenido al sistema de toma de inventarios **ERNI**.\n\n" +
                                "Para continuar, selecciona el almacén al cual deseas realizar el proceso.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // 👇 Mano señalando hacia el Dropdown
                    Text(
                        text = "\uD83D\uDC47", // Unicode para 👇
                        fontSize = 32.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // ⬇️ Aquí el selector real
                    DropDownUpScreen(
                        navController,
                        onUserInteraction = { actualizarActividad(context) },
                        userViewModel = userViewModel

                    )
                }
            }
        }
    }

    BackHandler(true) {
        Log.i("LOG_TAG", "Clicked back") // Desactiva el botón atrás
    }
}


