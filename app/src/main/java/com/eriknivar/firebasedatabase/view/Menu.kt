package com.eriknivar.firebasedatabase.view


import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AddHome
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Report
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController


@Composable
fun InventoryEntry(navController: NavHostController) {

    Row(modifier = Modifier.padding()) {
        Icon(
            imageVector = Icons.Default.AddHome,
            contentDescription = "",
            tint = Color.Black
        )
    }

    Text(
        modifier = Modifier.padding(start = 10.dp),
        text = "Entrada de Inventario",
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = Color.DarkGray

    )


}

@Composable
fun InventoryReports(navController: NavHostController){

    Row(modifier = Modifier.padding()) {
        Icon(
            imageVector = Icons.Default.Report,
            contentDescription = "",
            tint = Color.Black
        )
    }

    Text(
        modifier = Modifier.padding(start = 10.dp),
        text = "Reportes de Inventario",
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = Color.DarkGray

    )
}

@Composable
fun EditCounts(navController: NavHostController){

    Row(modifier = Modifier.padding()) {
        Icon(
            imageVector = Icons.Default.EditNote,
            contentDescription = "",
            tint = Color.Black
        )
    }

    Text(
        modifier = Modifier.padding(start = 10.dp),
        text = "Editar Conteos",
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = Color.DarkGray

    )

}

@Composable
fun MasterData(navController: NavHostController){

    Row(modifier = Modifier.padding()) {
        Icon(
            imageVector = Icons.Default.Archive,
            contentDescription = "",
            tint = Color.Black
        )
    }

    Text(
        modifier = Modifier.padding(start = 10.dp),
        text = "Dato Maestro",
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = Color.DarkGray

    )

}

@Composable
fun Exit(){

    Row(modifier = Modifier.padding()) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
            contentDescription = "Salir",
            tint = Color.Black
        )
    }
    Text(
        modifier = Modifier.padding(start = 10.dp),
        text = "Salir",
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = Color.DarkGray

    )

}



