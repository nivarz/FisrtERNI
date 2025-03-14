package com.eriknivar.firebasedatabase.view.inventoryentry

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.eriknivar.firebasedatabase.view.NavigationDrawer


@Composable
fun FirestoreApp(navController: NavHostController) {

    NavigationDrawer(navController) {

        val productoDescripcion = remember { mutableStateOf("") }

        Box {
            Text(
                text = " ${productoDescripcion.value}", // ✅ Este es el campo de texto que se muestra en la pantalla con la descripcion del producto
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = androidx.compose.ui.graphics.Color.Blue,
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            BackHandler(true) {
                Log.i("LOG_TAG", "Clicked back")//Desabilitar el boton de atras
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 1.dp))

        OutlinedTextFieldsInputs(productoDescripcion) //Los campos de texto + botones + Cards

    }
}