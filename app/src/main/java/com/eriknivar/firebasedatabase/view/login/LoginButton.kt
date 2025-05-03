package com.eriknivar.firebasedatabase.view.login

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import com.eriknivar.firebasedatabase.viewmodel.UserViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.delay

@Composable
fun LoginButton(
    navController: NavHostController,
    username: MutableState<String>,
    password: MutableState<String>,
    userViewModel: UserViewModel
) {
    val context = LocalContext.current
    val navyBlue = Color(0xFF001F5B)

    var isButtonEnabled by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }

    var showWelcomeDialog by remember { mutableStateOf(false) }
    var nombreUsuario by remember { mutableStateOf("") }

    // 🔄 Mostrar diálogo automáticamente durante 2 segundos
    if (showWelcomeDialog) {
        LaunchedEffect(Unit) {
            delay(4000)
            showWelcomeDialog = false
            navController.navigate("storagetype")
        }

        Dialog(onDismissRequest = { showWelcomeDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = navyBlue),
                elevation = CardDefaults.cardElevation(8.dp),
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Bienvenido",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "¡Bienvenido!",
                        style = MaterialTheme.typography.headlineSmall.copy(color = Color.White)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Acceso concedido. Bienvenido de nuevo, $nombreUsuario.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = {
                        showWelcomeDialog = false
                        navController.navigate("storagetype")
                    }) {
                        Text("Continuar", color = Color.White)
                    }
                }
            }
        }
    }

    ElevatedButton(
        onClick = {
            if (isButtonEnabled) {
                if (username.value.isNotBlank() && password.value.isNotBlank()) {
                    isButtonEnabled = false
                    isLoading = true

                    Firebase.firestore.collection("usuarios")
                        .whereEqualTo("usuario", username.value)
                        .whereEqualTo("contrasena", password.value)
                        .get()
                        .addOnSuccessListener { result ->
                            isButtonEnabled = true
                            isLoading = false

                            if (!result.isEmpty) {
                                val nombre = result.documents[0].getString("nombre") ?: ""
                                val tipo = result.documents[0].getString("tipo") ?: "invitado"

                                val documentId = result.documents[0].id
                                userViewModel.setUser(nombre, tipo, documentId)
                                userViewModel.cargarFotoUrl(documentId)


                                nombreUsuario = nombre
                                showWelcomeDialog = true
                            } else {
                                Toast.makeText(context, "Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener {
                            isButtonEnabled = true
                            isLoading = false
                            Toast.makeText(context, "Error al conectar con Firestore", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(context, "Debe completar ambos campos", Toast.LENGTH_SHORT).show()
                }
            }
        },
        enabled = isButtonEnabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = navyBlue,
            contentColor = Color.White
        ),
        modifier = Modifier.width(200.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(if (isLoading) "Ingresando..." else "Iniciar Sesión")

            if (isLoading) {
                Spacer(modifier = Modifier.width(8.dp))
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}




