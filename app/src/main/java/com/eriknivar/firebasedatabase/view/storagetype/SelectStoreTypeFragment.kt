package com.eriknivar.firebasedatabase.view.storagetype

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.eriknivar.firebasedatabase.view.NavigationDrawer
import com.eriknivar.firebasedatabase.viewmodel.UserViewModel

@Composable
fun SelectStorageFragment(
    navController: NavHostController,
    userViewModel: UserViewModel
) {
    val nombreUsuario = userViewModel.nombre.value ?: ""

    NavigationDrawer(navController, "Seleccionar Almacén", userViewModel) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // 🟡 Simulación de Logo ERNI (puedes reemplazar por Image si ya tienes uno)
            Icon(
                imageVector = Icons.Default.Warehouse, // O tu propio logo
                contentDescription = "Logo ERNI",
                tint = Color(0xFF001F5B),
                modifier = Modifier
                    .size(64.dp)
                    .padding(bottom = 16.dp)
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF3F9)),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Hola, $nombreUsuario 👋",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color(0xFF001F5B)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Bienvenido al sistema de toma de inventarios **ERNI**.\n\n" +
                                "Para continuar, selecciona el almacén al cual deseas realizar el proceso.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // 👇 Mano señalando hacia el Dropdown
                    Text(
                        text = "\uD83D\uDC47", // Unicode para 👇
                        fontSize = 32.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // ⬇️ Aquí el selector real
                    DropDownUpScreen(navController)
                }
            }
        }
    }
}


