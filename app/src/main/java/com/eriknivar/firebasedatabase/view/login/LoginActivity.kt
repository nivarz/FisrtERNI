package com.eriknivar.firebasedatabase.view.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.eriknivar.firebasedatabase.R
import com.eriknivar.firebasedatabase.view.utility.ScreenWithNetworkBanner

@Composable
fun LoginScreen(navController: NavHostController, isConnected: State<Boolean>) {
    val customColorBackGroundScreenLogin = Color(0xFF527782)

    ScreenWithNetworkBanner(isConnected) {

        Scaffold { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .background(customColorBackGroundScreenLogin),
            ) {
                Column(

                    horizontalAlignment = Alignment.CenterHorizontally, // 🔹 Centra horizontalmente
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp) // 🔹 Ajuste fino del padding
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logoerni),
                        contentDescription = "Logo ERNI",
                        modifier = Modifier
                            .height(250.dp) // 🔹 Puedes controlar el tamaño para evitar que se vea tan grande
                            .padding(bottom = 12.dp) // 🔹 Separación más suave antes del TextField
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 🔹 Campos de usuario y contraseña
                    TextFieldsLogin(navController)

                    Spacer(modifier = Modifier.height(16.dp)) // 🔹 Ajuste de espacio

                    // 🔹 Botón colocado dentro de la `Column`
                    LoginButton(navController)
                }
            }
        }
    }
}
