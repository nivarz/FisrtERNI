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
                }
            }
        }
    }


