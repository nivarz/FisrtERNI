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
import com.android.identity.util.UUID
import com.eriknivar.firebasedatabase.view.utility.clienteIdUsuarioActual
import com.eriknivar.firebasedatabase.view.utility.mostrarErrorToast
import com.eriknivar.firebasedatabase.viewmodel.UserViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.FirebaseMessaging
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

    // 🔄 Mostrar diálogo automáticamente durante 1 segundo
    if (showWelcomeDialog) {
        LaunchedEffect(Unit) {
            delay(2000)
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
            if (!isButtonEnabled) return@ElevatedButton

            val email = username.value.trim()           // debe ser email registrado en Firebase Auth
            val pass  = password.value.trim()

            if (email.isBlank() || pass.isBlank()) {
                Toast.makeText(context, "Debe completar ambos campos", Toast.LENGTH_SHORT).show()
                return@ElevatedButton
            }

            isButtonEnabled = false
            isLoading = true

            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()

            auth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener { authResult ->
                    val user = authResult.user
                    if (user == null) {
                        mostrarErrorToast(context, "No se pudo iniciar sesión")
                        isButtonEnabled = true; isLoading = false
                        return@addOnSuccessListener
                    }

                    // 1) Refrescar token para cargar custom claims (tipo, clienteId)
                    user.getIdToken(true)
                        .addOnSuccessListener {
                            val uid = user.uid

                            // 2) Leer perfil en tu colección `usuarios` (docId = uid)
                            Firebase.firestore.collection("usuarios").document(uid)
                                .get()
                                .addOnSuccessListener { document ->
                                    if (!document.exists()) {
                                        mostrarErrorToast(context, "Perfil no encontrado")
                                        auth.signOut()
                                        isButtonEnabled = true; isLoading = false
                                        return@addOnSuccessListener
                                    }

                                    val nombre      = document.getString("nombre").orEmpty()
                                    val tipo        = document.getString("tipo") ?: "invitado"
                                    val clienteId   = document.getString("clienteId").orEmpty()
                                    val sessionEnUso= document.getString("sessionId").orEmpty()
                                    val requiereCambio = document.getBoolean("requiereCambioPassword") ?: false

                                    if (sessionEnUso.isNotBlank() && tipo.lowercase() != "superuser") {
                                        mostrarErrorToast(context, "Sesión activa, cerrado erróneo. Contactar al administrador.")
                                        isButtonEnabled = true; isLoading = false
                                        return@addOnSuccessListener
                                    }

                                    // 3) Actualizar sessionId y continuar como antes
                                    val sessionId = java.util.UUID.randomUUID().toString()
                                    clienteIdUsuarioActual = clienteId

                                    userViewModel.setUser(nombre, tipo, uid, clienteId)
                                    userViewModel.setSessionId(sessionId)

                                    // Token FCM opcional
                                    com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                                        .addOnCompleteListener { task ->
                                            if (task.isSuccessful) {
                                                val fcm = task.result
                                                Firebase.firestore.collection("usuarios").document(uid)
                                                    .update("token", fcm)
                                            }
                                        }

                                    Firebase.firestore.collection("usuarios").document(uid)
                                        .update("sessionId", sessionId)
                                        .addOnSuccessListener {
                                            if (requiereCambio) {
                                                navController.navigate("cambiarPassword") {
                                                    popUpTo(0) { inclusive = true }
                                                }
                                            } else {
                                                userViewModel.cargarFotoUrl(uid)
                                                nombreUsuario = nombre
                                                showWelcomeDialog = true
                                            }
                                            isLoading = false
                                        }
                                        .addOnFailureListener {
                                            mostrarErrorToast(context, "Error al guardar sesión")
                                            isButtonEnabled = true; isLoading = false
                                        }
                                }
                                .addOnFailureListener {
                                    mostrarErrorToast(context, "Error al cargar perfil")
                                    auth.signOut()
                                    isButtonEnabled = true; isLoading = false
                                }
                        }
                        .addOnFailureListener {
                            mostrarErrorToast(context, "No se pudo refrescar el token")
                            auth.signOut()
                            isButtonEnabled = true; isLoading = false
                        }
                }
                .addOnFailureListener {
                    isButtonEnabled = true
                    isLoading = false
                    Toast.makeText(context, "Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show()
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