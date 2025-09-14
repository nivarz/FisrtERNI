package com.eriknivar.firebasedatabase.view.login

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.eriknivar.firebasedatabase.viewmodel.UserViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import java.util.UUID
import androidx.compose.runtime.LaunchedEffect


@Composable
fun CambiarPasswordScreen(
    navController: NavHostController,
    userViewModel: UserViewModel
) {
    val context = LocalContext.current
    val navyBlue = Color(0xFF001F5B)

    val nuevaPassword = remember { mutableStateOf("") }
    val confirmarPassword = remember { mutableStateOf("") }
    val error = remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var mostrarNueva by remember { mutableStateOf(false) }
    var mostrarConfirmar by remember { mutableStateOf(false) } // lo usaremos en el paso 2


    val currentUserId = userViewModel.documentId.value ?: ""
    val currentSessionId = userViewModel.sessionId.value

    // 👇 Evita que el snapshot listener cierre la app mientras actualizamos sesión
    var ignoreSessionCheck by remember { mutableStateOf(false) }

    // 🛡️ PASO 2-C: blindaje al entrar a CambiarPassword
    LaunchedEffect(currentUserId) {
        // Evita que cualquier listener cierre sesión mientras estabilizamos el flujo
        ignoreSessionCheck = true

        // Opcional pero recomendable: neutraliza sesión remota para evitar cierres globales
        if (currentUserId.isNotBlank()) {
            try {
                Firebase.firestore
                    .collection("usuarios")
                    .document(currentUserId)
                    .update("sessionId", "")
            } catch (_: Exception) { /* no-op */ }
        }
    }

    // 🔒 Listener de cierre de sesión remoto (respeta ignoreSessionCheck)
    DisposableEffect(currentUserId, currentSessionId, ignoreSessionCheck) {
        if (currentUserId.isBlank()) return@DisposableEffect onDispose { }
        val reg = Firebase.firestore.collection("usuarios")
            .document(currentUserId)
            .addSnapshotListener { snap, _ ->
                if (ignoreSessionCheck) return@addSnapshotListener
                val remote = snap?.getString("sessionId") ?: ""
                if (remote != currentSessionId && !userViewModel.isManualLogout.value) {
                    Toast.makeText(context, "Tu sesión fue cerrada por el administrador", Toast.LENGTH_LONG).show()
                    userViewModel.clearUser()
                    (context as? Activity)?.finishAffinity()
                }
            }
        onDispose { reg.remove() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF527782))
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Actualizar Contraseña", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            leadingIcon = { Icon(Icons.Filled.Key, contentDescription = null) },
            value = nuevaPassword.value,
            onValueChange = { nuevaPassword.value = it.trim(); error.value = "" },
            label = { Text("Nueva Contraseña") },
            singleLine = true,
            visualTransformation = if (mostrarNueva) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { mostrarNueva = !mostrarNueva }) {
                    Icon(
                        imageVector = if (mostrarNueva) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (mostrarNueva) "Ocultar contraseña" else "Mostrar contraseña"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            leadingIcon = { Icon(Icons.Filled.Key, contentDescription = null) },
            value = confirmarPassword.value,
            onValueChange = { confirmarPassword.value = it.trim(); error.value = "" },
            label = { Text("Confirmar Contraseña") },
            singleLine = true,
            visualTransformation = if (mostrarNueva) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { mostrarNueva = !mostrarNueva }) {
                    Icon(
                        imageVector = if (mostrarNueva) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (mostrarNueva) "Ocultar contraseña" else "Mostrar contraseña"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        if (error.value.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(error.value, color = Color.Yellow)
        }

        Spacer(Modifier.height(24.dp))

        ElevatedButton(
            onClick = {
                val nueva = nuevaPassword.value
                val confirmar = confirmarPassword.value

                // 🔐 Reglas de Auth: mínimo 6 caracteres
                when {
                    nueva.length < 6 -> {
                        error.value = "La contraseña debe tener al menos 6 caracteres"
                        return@ElevatedButton
                    }
                    nueva != confirmar -> {
                        error.value = "Las contraseñas no coinciden"
                        return@ElevatedButton
                    }
                }

                val authUser = Firebase.auth.currentUser
                if (authUser == null) {
                    Toast.makeText(context, "Debes iniciar sesión nuevamente", Toast.LENGTH_LONG).show()
                    navController.navigate("login") { popUpTo(0) { inclusive = true } }
                    return@ElevatedButton
                }

                isLoading = true
                ignoreSessionCheck = true

                authUser.updatePassword(nueva)
                    .addOnSuccessListener {
                        // ✅ Actualiza flags + sessionId en Firestore (en una sola operación)
                        val nuevoSessionId = UUID.randomUUID().toString()
                        userViewModel.setSessionId(nuevoSessionId)

                        Firebase.firestore.collection("usuarios")
                            .document(authUser.uid)
                            .update(
                                mapOf(
                                    "requiereCambioPassword" to false,
                                    "sessionId" to nuevoSessionId
                                )
                            )
                            .addOnSuccessListener {
                                // 🔄 Refresca el ID Token para que próximos requests usen credenciales frescas
                                authUser.getIdToken(true)
                                    .addOnCompleteListener {
                                        // pequeña ventana para que el listener no dispare
                                        Handler(Looper.getMainLooper()).postDelayed({
                                            ignoreSessionCheck = false
                                        }, 400)

                                        Toast.makeText(context, "Contraseña actualizada", Toast.LENGTH_SHORT).show()
                                        navController.navigate("storagetype") {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                            }

                            .addOnFailureListener { e ->
                                ignoreSessionCheck = false
                                error.value = "Error al guardar sesión: ${e.message}"
                            }
                    }
                    .addOnFailureListener { e ->
                        ignoreSessionCheck = false
                        when (e) {
                            is FirebaseAuthRecentLoginRequiredException -> {
                                // Por seguridad, Auth exige re-login reciente
                                Toast.makeText(
                                    context,
                                    "Por seguridad debes volver a iniciar sesión para cambiar la contraseña.",
                                    Toast.LENGTH_LONG
                                ).show()
                                Firebase.auth.signOut()
                                userViewModel.clearUser()
                                navController.navigate("login") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                            else -> {
                                error.value = "No se pudo actualizar la contraseña: ${e.message}"
                            }
                        }
                    }
                    .addOnCompleteListener {
                        isLoading = false
                    }
            },
            colors = ButtonDefaults.buttonColors(containerColor = navyBlue),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(if (isLoading) "Actualizando..." else "Actualizar")
        }
    }
}

