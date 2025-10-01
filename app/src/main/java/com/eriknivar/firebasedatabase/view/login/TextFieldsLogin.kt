package com.eriknivar.firebasedatabase.view.login

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.sp

@Composable
fun TextFieldsLogin(
    username: MutableState<String>,
    password: MutableState<String>,
    onLogin: () -> Unit = {},  // ← callback para Enter en contraseña
    emailError: String? = null,
    passError: String? = null,
    onAnyEdit: () -> Unit = {}
) {
    val focusRequesterUser = remember { FocusRequester() }
    val focusRequesterPassword = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        try {
            focusRequesterUser.requestFocus()
        } catch (_: Exception) {
        }
    }


    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp, 0.dp, 16.dp, 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Usuario / Email
        OutlinedTextField(
            isError = emailError != null,
            supportingText = {
                if (emailError != null) {
                    Text(
                        text = emailError,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            },
            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = "User") },
            label = { Text("Usuario", color = Color.Black, fontWeight = FontWeight.Thin) },
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
            placeholder = { Text("usuario@example.com", fontSize = 14.sp) },
            value = username.value,
            onValueChange = { username.value = it.trim() ; onAnyEdit() },
            singleLine = true,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequesterUser),
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = {
                    try {
                        focusRequesterPassword.requestFocus()
                    } catch (e: Exception) {
                        Log.e("KeyboardFocus", "Error pasando foco a contraseña: ${e.message}")
                    }
                }
            )
        )

        // Estado local para mostrar/ocultar contraseña
        var showPassword by remember { mutableStateOf(false) }

        // Contraseña
        OutlinedTextField(
            isError = passError != null,
            supportingText = {
                if (passError != null) {
                    Text(
                        text = passError,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            },
            leadingIcon = { Icon(Icons.Filled.Key, contentDescription = "Password") },
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        imageVector = if (showPassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = "Toggle Password Visibility"
                    )
                }
            },
            label = { Text("Contraseña", color = Color.Black, fontWeight = FontWeight.Thin) },
            placeholder = { Text("••••••••") },
            value = password.value,
            onValueChange = { password.value = it.trim() ; onAnyEdit() },
            singleLine = true,
            maxLines = 1,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequesterPassword),
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide()
                    Log.d("Login", "⏎ Enter presionado en contraseña → onLogin()")
                    onLogin()
                }
            )
        )
    }
}




