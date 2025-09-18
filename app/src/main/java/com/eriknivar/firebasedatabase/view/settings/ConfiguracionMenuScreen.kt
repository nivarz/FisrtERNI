package com.eriknivar.firebasedatabase.view.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController


@Composable
fun ConfiguracionMenuScreen(navController: NavHostController) {
    val opciones = listOf(
        MenuItem("Usuarios", Icons.Default.Person, "usuarios"),
        MenuItem("Ubicaciones", Icons.Default.Place, "ubicaciones"),
        MenuItem("Almacenes", Icons.Default.LocationCity, "localidades"),
        MenuItem("Auditoría de Registros", Icons.AutoMirrored.Filled.ListAlt, "auditoria")
    )

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {

        opciones.forEach { item ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate(item.ruta) }
                    .padding(vertical = 12.dp)
            ) {
                Icon(
                    imageVector = item.icono,
                    contentDescription = item.nombre,
                    tint = Color(0xFF003366),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = item.nombre, fontSize = 18.sp)
            }
            HorizontalDivider()
        }
    }
}

data class MenuItem(val nombre: String, val icono: ImageVector, val ruta: String)



