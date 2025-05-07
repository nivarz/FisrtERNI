package com.eriknivar.firebasedatabase.view.settings.settingsmenu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.eriknivar.firebasedatabase.view.NavigationDrawer
import com.eriknivar.firebasedatabase.view.settings.ConfiguracionUsuariosScreen
import com.eriknivar.firebasedatabase.viewmodel.UserViewModel

@Composable
fun UsuariosScreen(navController: NavHostController, userViewModel: UserViewModel) {

    val tipo = userViewModel.tipo.value ?: ""

    if (tipo.lowercase() != "admin" && tipo.lowercase() != "superuser") {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.TopCenter
        ) {
            Text(
                text = "⛔ Acceso restringido",
                color = Color.Red,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(32.dp)
            )
        }
        return
    }

    // Valores dummy para campos que no se usan pero requiere el Drawer
    val dummyLocation = remember { mutableStateOf("") }
    val dummySku = remember { mutableStateOf("") }
    val dummyQuantity = remember { mutableStateOf("") }
    val dummyLot = remember { mutableStateOf("") }
    val dummyDateText = remember { mutableStateOf("") }

    NavigationDrawer(
        navController = navController,
        storageType = "Usuarios",
        userViewModel = userViewModel,
        location = dummyLocation,
        sku = dummySku,
        quantity = dummyQuantity,
        lot = dummyLot,
        expirationDate = dummyDateText
    ) {
        ConfiguracionUsuariosScreen(
            userViewModel = userViewModel,
            onUserInteraction = {
                // lógica opcional
            }
        )
    }
}





