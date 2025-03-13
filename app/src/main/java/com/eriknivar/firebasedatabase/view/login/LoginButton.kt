package com.eriknivar.firebasedatabase.view.login

import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController


@Composable
fun LoginButton(navController: NavHostController) {


    ElevatedButton(colors = ButtonDefaults.buttonColors(
        containerColor = Color.Black, contentColor = Color.White
    ),
        modifier = Modifier
            .fillMaxWidth()
            .width(300.dp),
        onClick = {
            Log.d("Navigation", "Navegando a storagetype")
            navController.navigate("storagetype")
        }
    ) {
        Text(
            text = "Iniciar Sesion", color = Color.White
        )
    }
}