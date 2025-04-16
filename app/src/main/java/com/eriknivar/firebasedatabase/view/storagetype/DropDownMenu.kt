package com.eriknivar.firebasedatabase.view.storagetype

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

@Composable
fun DropDownUpScreen(navController: NavHostController) {
    val firestore = Firebase.firestore
    val localidades = remember { mutableStateListOf<String>() }

    var valueText by remember { mutableStateOf("") }
    var expandedDropdown by remember { mutableStateOf(false) }

    val navyBlue = Color(0xFF001F5B)

    // 🔄 Cargar localidades desde Firebase
    LaunchedEffect(Unit) {
        firestore.collection("localidades")
            .get()
            .addOnSuccessListener { result ->
                localidades.clear()
                localidades.addAll(result.mapNotNull { it.getString("nombre") })
            }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(12.dp))
                .background(Color.White, shape = RoundedCornerShape(12.dp))
                .border(2.dp, navyBlue, RoundedCornerShape(12.dp))
                .padding(4.dp)
        ) {
            TextField(
                value = valueText,
                onValueChange = { newText -> valueText = newText },
                label = { Text("Selecciona una localidad") },
                singleLine = true,
                maxLines = 1,
                trailingIcon = {
                    IconButton(onClick = { expandedDropdown = !expandedDropdown }) {
                        Icon(
                            imageVector = if (expandedDropdown) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = "Icono desplegable",
                            tint = navyBlue
                        )
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,

            ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            )
        }

        DropdownMenu(
            expanded = expandedDropdown,
            onDismissRequest = { expandedDropdown = false },
            modifier = Modifier
                .fillMaxWidth(0.91f)
                .background(Color.White)
        ) {
            localidades.forEach { localidad ->
                DropdownMenuItem(
                    text = { Text(text = localidad) },
                    onClick = {
                        valueText = localidad
                        expandedDropdown = false
                        navController.navigate("inventoryentry/${valueText}")
                    }
                )
            }
        }
    }
}


