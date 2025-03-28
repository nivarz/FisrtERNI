package com.eriknivar.firebasedatabase.view.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@Composable
fun TextFieldsLogin(navController: NavHostController) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp), // 🔹 Espaciado uniforme entre elementos
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth() // 🔹 Evita ocupar toda la pantalla
            .padding(16.dp, 0.dp, 16.dp, 16.dp) // 🔹 Ajuste del espacio superior
            .verticalScroll(rememberScrollState())
    ) {
        var username by remember { mutableStateOf("") }

        OutlinedTextField(
            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = "User") },
            label = { Text("Usuario", color = Color.White, fontWeight = FontWeight.Thin) },
            value = username,
            onValueChange = { username = it.uppercase() },
            singleLine = true,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth() // 🔹 Ocupar el ancho disponible
        )

        var password by remember { mutableStateOf("") }
        var showPassword by remember { mutableStateOf(false) }

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
            label = { Text("Contraseña", color = Color.White, fontWeight = FontWeight.Thin) },
            value = password,
            onValueChange = { password = it },
            singleLine = true,
            maxLines = 1,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth() // 🔹 Ocupar el ancho disponible
        )
    }
}

