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
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp

@Composable
fun LoginScreen(navController: NavHostController, userViewModel: UserViewModel) {
    val customColorBackGroundScreenLogin = Color(0xFF527782)

    val emailError = remember { mutableStateOf<String?>(null) }
    val passError  = remember { mutableStateOf<String?>(null) }
    val globalError = remember { mutableStateOf<String?>(null) }

    // 🔹 Estado elevado para compartir entre campos y botón
    val username = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }

    fun validate(): Boolean {
        var ok = true
        emailError.value = null
        passError.value = null
        globalError.value = null

        val u = username.value.trim()
        val p = password.value.trim()

        if (u.isEmpty()) {
            emailError.value = "Escribe tu usuario/correo."
            ok = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(u).matches()) {
            // Si tu “usuario” NO es email, comenta esta línea.
            emailError.value = "Formato de correo no válido."
            ok = false
        }
        if (p.isEmpty()) {
            passError.value = "Escribe tu contraseña."
            ok = false
        }
        return ok
    }

    // === Handler compartido para iniciar sesión (usado por Enter y por el botón) ===
    val handleLogin: () -> Unit = login@ {     // ⬅️ etiqueta del lambda:  login@
        globalError.value = null               // ← limpia global
        emailError.value = null                // opcional
        passError.value = null                 // opcional
        if (!validate()) return@login          // ⬅️ usa la etiqueta, no el nombre de la variable
        // aquí va tu lógica de login (la misma del botón)
    }

    val clearGlobalError: () -> Unit = { globalError.value = null }

    Scaffold(
        containerColor = customColorBackGroundScreenLogin,   // ⬅️ nuevo
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(customColorBackGroundScreenLogin) // ⬅️ nuevo
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {

            }
        }) { innerPadding ->

        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logoernilupatransparente),
                    contentDescription = "Logo ERNI",
                    modifier = Modifier
                        .height(140.dp)
                        .padding(bottom = 0.dp)
                )

                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp)
                        .align(Alignment.CenterHorizontally)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Campos
                        TextFieldsLogin(
                            username = username,
                            password = password,
                            onLogin = handleLogin, // ⏎ en contraseña llama lo mismo que el botón
                            emailError = emailError.value,
                            passError = passError.value,
                            onAnyEdit = {
                                globalError.value = null
                                if (emailError.value != null) emailError.value = null
                                if (passError.value != null)  passError.value  = null
                            }
                        )

                        val isFormOk =
                            username.value.isNotBlank() &&
                                    password.value.isNotBlank() &&
                                    emailError.value == null &&
                                    passError.value == null

                        LoginButton(
                            navController = navController,
                            username = username,
                            password = password,
                            userViewModel = userViewModel,
                            isButtonEnabled = isFormOk,
                            onLoginClick = handleLogin
                        )

                        if (globalError.value != null) {
                            Text(
                                text = globalError.value!!,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 13.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        // Enlace "¿Olvidaste tu contraseña?"
                        ForgotPasswordLink(username = username)
                    }
                }
            }
        }
    }
}


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
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        Text(
            "¿Olvidaste tu contraseña?",
            color = Color.Black,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            maxLines = 1,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold

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
                        onClick = { if (!loading) show = false }, enabled = !loading,
                        //colors = TextButtonDefaults.textButtonColors(contentColor = Color.Black),
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
                            FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                                .addOnCompleteListener { task ->
                                    loading = false
                                    show = false
                                    val msg =
                                        if (task.isSuccessful) "Si existe una cuenta con ese correo, te enviamos un enlace para restablecerla."
                                        else task.exception?.localizedMessage
                                            ?: "No se pudo enviar el correo."
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                        }, enabled = !loading && isInputValid,
                        //colors = TextButtonDefaults.textButtonColors(contentColor = Color.Black),
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
            dismissButton = {})
    }
}