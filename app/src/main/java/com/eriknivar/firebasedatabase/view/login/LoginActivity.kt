package com.eriknivar.firebasedatabase.view.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.eriknivar.firebasedatabase.R
import com.eriknivar.firebasedatabase.viewmodel.UserViewModel
import android.widget.Toast
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.material3.TextButton
import com.google.firebase.auth.FirebaseAuth
import androidx.wear.compose.material3.TextButtonDefaults
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp


@Composable
fun ForgotPasswordLink(username: MutableState<String>) {
    val context = LocalContext.current
    var show by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var input by remember(show) { mutableStateOf(username.value.trim()) }

    // 👇 Reglas de validación:
    // - Si contiene "@": debe parecer correo.
    // - Si NO contiene "@": lo tratamos como usuario y basta con que no esté vacío.
    val looksLikeEmail: (String) -> Boolean = { s ->
        android.util.Patterns.EMAIL_ADDRESS.matcher(s).matches()
    }
    val isInputValid by remember(input) {
        mutableStateOf(
            if (input.contains("@")) looksLikeEmail(input) else input.isNotBlank()
        )
    }
    val showEmailFormatError by remember(input) {
        mutableStateOf(input.contains("@") && input.isNotEmpty() && !looksLikeEmail(input))
    }

    // Enlace en NEGRO
    TextButton(
        onClick = { input = username.value.trim(); show = true },
        colors = TextButtonDefaults.textButtonColors(contentColor = Color.Black),
        modifier = Modifier
            .fillMaxWidth()           // 👈 ocupa todo el ancho
            .padding(top = 16.dp)     // 👈 separación del botón de “Iniciar sesión”
    ) {
        Text(
            "¿Olvidaste tu contraseña?",
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth(), // 👈 el texto también ocupa todo el ancho
            textAlign = TextAlign.Center,       // 👈 centrado
            maxLines = 1                        // 👈 evita saltos raros
        )
    }


    if (show) {
        AlertDialog(
            onDismissRequest = { if (!loading) show = false },
            title = { Text("Restablecer contraseña", color = Color.Black) },   // título negro
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)  // padding interior
                ) {
                    Text("Te enviaremos un enlace al correo:", color = Color.Black)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it.trim() },
                        singleLine = true,
                        label = { Text("Correo o usuario", color = Color.Black) }, // label negro
                        isError = showEmailFormatError,
                        supportingText = {
                            if (showEmailFormatError) {
                                Text("Formato de correo inválido", color = Color.Black)
                            }
                        },
                        enabled = !loading,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            disabledTextColor = Color.Black.copy(alpha = 0.6f),

                            focusedLabelColor = Color.Black,
                            unfocusedLabelColor = Color.Black,
                            disabledLabelColor = Color.Black.copy(alpha = 0.6f),

                            cursorColor = Color.Black,

                            focusedBorderColor = Color.Black,
                            unfocusedBorderColor = Color.Black,
                            disabledBorderColor = Color.Black.copy(alpha = 0.3f)
                        )
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Cancelar
                    TextButton(
                        onClick = { if (!loading) show = false },
                        enabled = !loading,
                        colors = TextButtonDefaults.textButtonColors(contentColor = Color.Black),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "Cancelar",
                            color = Color.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Enviar
                    TextButton(
                        onClick = {
                            val email =
                                if (input.contains("@")) input else "${input.lowercase()}@erni.local"
                            loading = true
                            FirebaseAuth.getInstance()
                                .sendPasswordResetEmail(email)
                                .addOnCompleteListener { task ->
                                    loading = false
                                    show = false
                                    val msg = if (task.isSuccessful)
                                        "Si existe una cuenta con ese correo, te enviamos un enlace para restablecerla."
                                    else task.exception?.localizedMessage
                                        ?: "No se pudo enviar el correo."
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                        },
                        enabled = !loading && isInputValid,
                        colors = TextButtonDefaults.textButtonColors(contentColor = Color.Black),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            if (loading) "Enviando..." else "Enviar enlace",
                            color = Color.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            dismissButton = {}
        )
    }
}


@Composable
fun LoginScreen(navController: NavHostController, userViewModel: UserViewModel) {
    val customColorBackGroundScreenLogin = Color(0xFF527782)

    // 🔹 Estado elevado para compartir entre campos y botón
    val username = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(customColorBackGroundScreenLogin),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logoernilupatransparente),
                    contentDescription = "Logo ERNI",
                    modifier = Modifier
                        .height(180.dp) //Controla el tamaño de la imagen
                        .padding(bottom = 4.dp)
                )

                // 🔹 Pasa los estados aquí
                TextFieldsLogin(username, password)

                // 🔹 Pasa los estados al botón
                LoginButton(navController, username, password, userViewModel)
                //Spacer(Modifier.height(8.dp))
                ForgotPasswordLink(username = username) // 👈 nuevo

            }
        }
    }
}


