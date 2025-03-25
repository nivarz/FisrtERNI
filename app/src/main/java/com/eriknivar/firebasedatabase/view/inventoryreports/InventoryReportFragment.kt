package com.eriknivar.firebasedatabase.view.inventoryreports

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.eriknivar.firebasedatabase.view.NavigationDrawer
import com.eriknivar.firebasedatabase.view.utility.ScreenWithNetworkBanner

@Composable
fun InventoryReportsFragment(navController: NavHostController, isConnected: State<Boolean>) {

    ScreenWithNetworkBanner(isConnected) {

        NavigationDrawer(navController) {

            Box {
                Text(
                    "Reportes del Inventario",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center
                )



                BackHandler(true)
                {
                    Log.i("LOG_TAG", "Clicked back")//Desabilitar el boton de atras
                }
            }
        }
    }
}
