package com.eriknivar.firebasedatabase.view.storagetype

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@Composable
fun DropDownUpScreen(navController: NavHostController) {

    Column(modifier = Modifier.padding()) {

        var valueText by remember { mutableStateOf("") }
        var expandedDropdown by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {

            TextField(
                label = { Text(text = "Tipos de Almacenes") },
                modifier = Modifier.fillMaxWidth(),
                value = valueText, onValueChange = { newText -> valueText = newText },
                singleLine = true,
                maxLines = 1,
                trailingIcon = {
                    IconButton(onClick = { expandedDropdown = !expandedDropdown }) {
                        Icon(
                            imageVector = if (expandedDropdown) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = "Icono desplegable"
                        )
                    }

                }

            )

            DropdownMenu(

                modifier = Modifier.fillMaxWidth(fraction = 0.91f),
                expanded = expandedDropdown, onDismissRequest = { expandedDropdown = false }) {

                DropdownMenuItem(text = { Text(text = "Almacen 1") }, onClick = {
                    valueText = "Almacen 1"
                    expandedDropdown = false
                    navController.navigate("inventoryentry/${valueText}")
                })
                DropdownMenuItem(text = { Text(text = "Almacen 2") }, onClick = {
                    valueText = "Almacen 2"
                    expandedDropdown = false
                    navController.navigate("inventoryentry/${valueText}")
                })
                DropdownMenuItem(text = { Text(text = "Almacen 3") }, onClick = {
                    valueText = "Almacen 3"
                    expandedDropdown = false
                    navController.navigate("inventoryentry/${valueText}")
                })


            }


        }
    }

}
