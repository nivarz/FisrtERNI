package com.eriknivar.firebasedatabase.view.login

import android.widget.Toast

import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.eriknivar.firebasedatabase.viewmodel.UserViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore


@Composable
fun LoginButton(
    navController: NavHostController,
    username: MutableState<String>,
    password: MutableState<String>,
    userViewModel: UserViewModel
) {
    val context = LocalContext.current
    val navyBlue = Color(0xFF001F5B)


    ElevatedButton(
        colors = ButtonDefaults.buttonColors(
            containerColor = navyBlue, contentColor = Color.White
        ),
        modifier = Modifier.width(200.dp),
        onClick = {
            if (username.value.isNotBlank() && password.value.isNotBlank()) {
                Firebase.firestore.collection("usuarios")
                    .whereEqualTo("usuario", username.value)
                    .whereEqualTo("contrasena", password.value)
                    .get()
                    .addOnSuccessListener { result ->
                        if (!result.isEmpty) {
                            val nombre = result.documents[0].getString("nombre") ?: ""
                            val tipo = result.documents[0].getString("tipo") ?: "invitado"

                            // ✅ Actualiza el ViewModel con los datos del usuario
                            userViewModel.setUser(nombre,tipo)

                            Toast.makeText(context, "Bienvenido", Toast.LENGTH_SHORT).show()

                            if (tipo == "admin") {
                                navController.navigate("storagetype")
                            } else {
                                navController.navigate("storagetype")
                            }

                        } else {
                            Toast.makeText(context, "Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Error al conectar con Firestore", Toast.LENGTH_SHORT).show()

                    }
            } else {
                Toast.makeText(context, "Debe completar ambos campos", Toast.LENGTH_SHORT).show()
            }
        }

    ) {
        Text("Iniciar Sesión")
    }
}

