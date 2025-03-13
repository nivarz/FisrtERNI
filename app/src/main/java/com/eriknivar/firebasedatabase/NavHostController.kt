package com.eriknivar.firebasedatabase

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.eriknivar.firebasedatabase.view.editscounts.EditCountsFragment
import com.eriknivar.firebasedatabase.view.inventoryentry.FirestoreApp
import com.eriknivar.firebasedatabase.view.inventoryreports.InventoryReportsFragment
import com.eriknivar.firebasedatabase.view.login.LoginScreen
import com.eriknivar.firebasedatabase.view.masterdata.MasterDataFragment
import com.eriknivar.firebasedatabase.view.storagetype.SelectStorageFragment

@Composable
fun NavGraph(navController: NavHostController) {


    NavHost(navController = navController, startDestination = "login") {

        composable(route = "login") {LoginScreen(navController) }
        composable(route = "storagetype") { SelectStorageFragment(navController) }
        composable(route = "inventoryentry") {FirestoreApp(navController) }
        composable(route = "inventoryreports") {InventoryReportsFragment(navController) }
        composable(route = "editscounts") {EditCountsFragment(navController)}
        composable(route = "masterdata") {MasterDataFragment(navController)}



    }
}