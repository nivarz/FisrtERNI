package com.eriknivar.firebasedatabase.view.storagetype

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.navigation.NavHostController
import com.eriknivar.firebasedatabase.view.NavigationDrawer
import com.eriknivar.firebasedatabase.view.utility.ScreenWithNetworkBanner
import com.eriknivar.firebasedatabase.viewmodel.UserViewModel

@Composable
fun SelectStorageFragment(navController: NavHostController, isConnected: State<Boolean>, userViewModel: UserViewModel) {

    ScreenWithNetworkBanner(isConnected) {


        NavigationDrawer(navController, "Seleccione un almacén", userViewModel) {

            DropDownUpScreen(navController)


            BackHandler(true) {
                Log.i("LOG_TAG", "Clicked back")//Desabilitar el boton de atras
            }
        }
    }
}
