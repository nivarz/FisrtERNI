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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ButtonDefaults
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
import com.google.firebase.firestore.firestore
import java.util.UUID

@Composable
fun CambiarPasswordScreen(
    navController: NavHostController,
    userViewModel: UserViewModel
) {
    val context = LocalContext.current
    val nuevaPassword = remember { mutableStateOf("") }
    val confirmarPassword = remember { mutableStateOf("") }
    val error = remember { mutableStateOf("") }
    val customColorBackGroundScreenLogin = Color(0xFF527782)

    val navyBlue = Color(0xFF001F5B)

    var showPassword by remember { mutableStateOf(false) }
    var showPasswordConfirm by remember { mutableStateOf(false) }

    val currentUserId = userViewModel.documentId.value ?: ""
    val currentSessionId = userViewModel.sessionId.value

    DisposableEffect(currentUserId, currentSessionId) {
        val listenerRegistration = Firebase.firestore.collection("usuarios")
            .document(currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                val remoteSessionId = snapshot?.getString("sessionId") ?: ""
                if (remoteSessionId != currentSessionId && !userViewModel.isManualLogout.value) {
                    Toast.makeText(context, "Tu sesión fue cerrada por el administrador", Toast.LENGTH_LONG).show()
                    userViewModel.clearUser()
                    (context as? Activity)?.finishAffinity()
                }
            }

        onDispose {
            listenerRegistration.remove()
        }
    }

    val activity = (context as? Activity)

    DisposableEffect(currentUserId, currentSessionId) {
        val handler = Handler(Looper.getMainLooper())
        var lastInteractionTime = System.currentTimeMillis()
        val logoutTimeout = 10 * 60 * 1000L // 10 minutos

        val runnable = object : Runnable {
            override fun run() {
                if (System.currentTimeMillis() - lastInteractionTime >= logoutTimeout) {
                    userViewModel.clearUser()
                    activity?.finishAffinity()
                } else {
                    handler.postDelayed(this, 1000L)
                }
            }
        }

        val listener = View.OnTouchListener { _, _ ->
            lastInteractionTime = System.currentTimeMillis()
            false
        }

        activity?.window?.decorView?.rootView?.setOnTouchListener(listener)
        handler.post(runnable)

        onDispose {
            handler.removeCallbacks(runnable)
            activity?.window?.decorView?.rootView?.setOnTouchListener(null)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(customColorBackGroundScreenLogin)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Actualizar Contraseña", fontSize = 22.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            leadingIcon = { Icon(Icons.Filled.Key, contentDescription = "Password") },
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle Password Visibility"
                    )
                }
            },

            value = nuevaPassword.value,
            onValueChange = {
                nuevaPassword.value = it.trim()
                error.value = ""
            },
            label = { Text("Nueva Contraseña") },
            singleLine = true,
            maxLines = 1,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            leadingIcon = { Icon(Icons.Filled.Key, contentDescription = "Password") },
            trailingIcon = {
                IconButton(onClick = { showPasswordConfirm = !showPasswordConfirm }) {
                    Icon(
                        imageVector = if (showPasswordConfirm) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle Password Visibility"
                    )
                }
            },

            value = confirmarPassword.value,
            onValueChange = {
                confirmarPassword.value = it.trim()
                error.value = ""
            },
            label = { Text("Confirmar Contraseña") },
            singleLine = true,
            maxLines = 1,
            visualTransformation = if (showPasswordConfirm) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        if (error.value.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(error.value, color = Color.Red)
        }

        Spacer(modifier = Modifier.height(24.dp))

        ElevatedButton(
            onClick = {
                val nueva = nuevaPassword.value
                val confirmar = confirmarPassword.value

                if (nueva.length < 4) {
                    error.value = "La contraseña debe tener al menos 4 caracteres"
                } else if (nueva != confirmar) {
                    error.value = "Las contraseñas no coinciden"
                } else {
                    val documentId = userViewModel.documentId.value ?: return@ElevatedButton
                    Firebase.firestore.collection("usuarios")
                        .document(documentId)
                        .update(
                            mapOf(
                                "contrasena" to nueva,
                                "requiereCambioPassword" to false
                            )
                        )
                        .addOnSuccessListener {
                            Toast.makeText(context, "Contraseña actualizada", Toast.LENGTH_SHORT)
                                .show()

                            val nuevoSessionId = UUID.randomUUID().toString()
                            userViewModel.setSessionId(nuevoSessionId)

                            Firebase.firestore.collection("usuarios")
                                .document(documentId)
                                .update("sessionId", nuevoSessionId)


                            // Redirigir a pantalla principal
                            navController.navigate("storagetype") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                        .addOnFailureListener {
                            error.value = "Error al actualizar contraseña"
                        }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = navyBlue),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Actualizar")
        }
    }
}
