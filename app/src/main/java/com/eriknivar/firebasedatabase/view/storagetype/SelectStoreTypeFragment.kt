package com.eriknivar.firebasedatabase.view.storagetype

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.eriknivar.firebasedatabase.view.NavigationDrawer
import com.eriknivar.firebasedatabase.view.utility.ScreenWithNetworkBanner

@Composable
fun SelectStorageFragment(navController: NavHostController, isConnected: State<Boolean>) {

    ScreenWithNetworkBanner(isConnected) {


        NavigationDrawer(navController, "Seleccione un almacén") {

            DropDownUpScreen(navController)


            BackHandler(true) {
                Log.i("LOG_TAG", "Clicked back")//Desabilitar el boton de atras
            }
        }
    }
}
