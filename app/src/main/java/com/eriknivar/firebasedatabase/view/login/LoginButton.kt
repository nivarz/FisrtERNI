package com.eriknivar.firebasedatabase.view.login

import android.util.Log
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
import com.eriknivar.firebasedatabase.network.SelectedClientStore
import com.eriknivar.firebasedatabase.view.utility.mostrarErrorToast
import com.eriknivar.firebasedatabase.viewmodel.UserViewModel
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.auth.GetTokenResult
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.firestore
import com.google.firebase.ktx.app
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.delay
import com.eriknivar.firebasedatabase.navigation.Rutas


@Composable
fun LoginButton(
    navController: NavHostController,
    username: MutableState<String>,
    password: MutableState<String>,
    userViewModel: UserViewModel,
    onLoginClick: () -> Unit
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

    val opts = com.google.firebase.ktx.Firebase.app.options
    android.util.Log.d("FirebaseProject", "projectId=${opts.projectId}, appId=${opts.applicationId}")

    ElevatedButton(
        onClick = {
            if (isLoading || !isButtonEnabled) return@ElevatedButton
            onLoginClick()

            val usuarioInput = username.value.trim()
            val passwordInput = password.value.trim()
            if (usuarioInput.isBlank() || passwordInput.isBlank()) {
                Toast.makeText(context, "Debe completar ambos campos", Toast.LENGTH_SHORT).show()
                return@ElevatedButton
            }

            isButtonEnabled = false
            isLoading = true

            // Usa el mismo dominio que empleaste en la migración
            val email = if (usuarioInput.contains("@")) usuarioInput
            else "${usuarioInput.lowercase()}@erni.local"

            val auth = Firebase.auth
            auth.signInWithEmailAndPassword(email, passwordInput)
                .addOnSuccessListener {
                    // Ya hay currentUser no nulo
                    val user = auth.currentUser!!
                    user.getIdToken(true)
                        .addOnSuccessListener { tokenResult: GetTokenResult ->
                            val claims = tokenResult.claims
                            val tipoClaim = (claims["tipo"] as? String).orEmpty()
                            val clienteIdClaim = (claims["clienteId"] as? String).orEmpty()
                            val uid = user.uid

                            // Cargar perfil (nombre, flags) desde usuarios/{uid}
                            Firebase.firestore.collection("usuarios").document(uid).get()
                                .addOnSuccessListener { doc: DocumentSnapshot ->
                                    val nombre = doc.getString("nombre")
                                        ?: email.substringBefore("@").uppercase()
                                    val requiereCambio = doc.getBoolean("requiereCambioPassword") ?: false
                                    val sessionEnUso = doc.getString("sessionId") ?: ""
                                    val tipoDoc = doc.getString("tipo") ?: "invitado"
                                    val clienteIdDoc = doc.getString("clienteId") ?: ""

                                    // Resuelve rol/cliente (claims priorizan)
                                    val tipoFinal = if (tipoClaim.isNotBlank()) tipoClaim else tipoDoc
                                    val clienteFinal = if (clienteIdClaim.isNotBlank()) clienteIdClaim else clienteIdDoc

                                    if (sessionEnUso.isNotBlank() && !tipoFinal.equals("superuser", true) && !requiereCambio) {
                                        mostrarErrorToast(context, "Sesión activa, cerrado erróneo. Contactar al administrador.")
                                        auth.signOut()
                                        isLoading = false
                                        isButtonEnabled = true
                                    } else {
                                        // Popular VM
                                        val sessionId = java.util.UUID.randomUUID().toString()
                                        userViewModel.setUser(nombre, tipoFinal, uid)
                                        userViewModel.setClienteId(clienteFinal)
                                        if (!requiereCambio || tipoFinal.equals("superuser", true)) {
                                            userViewModel.setSessionId(sessionId)
                                        } else {
                                            userViewModel.setSessionId("") // evita mismatch en CambiarPassword
                                        }

                                        // Marca si es superuser (para que el interceptor sepa si puede enviar X-Cliente-Id)
                                        SelectedClientStore.setRolSuperuser(tipoFinal.equals("superuser", ignoreCase = true))

                                        // Si NO es superuser, limpia cualquier cliente seleccionado por seguridad
                                        if (!tipoFinal.equals("superuser", ignoreCase = true)) {
                                            SelectedClientStore.setCliente(null)
                                        }

                                        // Guardar FCM token (best-effort)
                                        FirebaseMessaging.getInstance().token
                                            .addOnCompleteListener { t: Task<String> ->
                                                if (t.isSuccessful) {
                                                    Firebase.firestore.collection("usuarios").document(uid)
                                                        .update("token", t.result)
                                                }
                                            }
                                        if (requiereCambio && !tipoFinal.equals("superuser", true)) {
                                            // Navegamos SIN tocar sessionId; se setea tras cambiar la contraseña
                                            isLoading = false
                                            isButtonEnabled = true
                                            navController.navigate(Rutas.CAMBIAR_PASSWORD) {
                                                popUpTo(0) { inclusive = true }
                                            }

                                        } else {
                                            // Flujo normal: persistir sessionId y seguir
                                            Firebase.firestore.collection("usuarios").document(uid)
                                                .update("sessionId", sessionId)
                                                .addOnSuccessListener {
                                                    userViewModel.cargarFotoUrl(uid)
                                                    nombreUsuario = nombre
                                                    showWelcomeDialog = true
                                                    isLoading = false
                                                    isButtonEnabled = true
                                                }
                                                .addOnFailureListener { e ->
                                                    isLoading = false
                                                    isButtonEnabled = true
                                                    mostrarErrorToast(context, "Error al guardar sesión: ${e.message}")
                                                }
                                        }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    mostrarErrorToast(context, "No se pudo cargar el perfil: ${e.message}")
                                    isLoading = false
                                    isButtonEnabled = true
                                }
                        }
                        .addOnFailureListener { e ->
                            mostrarErrorToast(context, "No se pudieron leer los claims: ${e.message}")
                            isLoading = false
                            isButtonEnabled = true
                        }
                }
                .addOnFailureListener { e ->
                    isLoading = false
                    isButtonEnabled = true
                    Toast.makeText(context, "Login inválido: ${e.message}", Toast.LENGTH_LONG).show()
                }
        },
        enabled = !isLoading && isButtonEnabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = navyBlue,
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()                                             // ⬅️ ancho completo
            .height(52.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(if (isLoading) "Ingresando..." else "Iniciar sesión")
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




