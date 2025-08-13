package com.eriknivar.firebasedatabase.view.storagetype

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.eriknivar.firebasedatabase.viewmodel.UserViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

@Composable
fun DropDownUpScreen(
    navController: NavHostController,
    onUserInteraction: () -> Unit = {},
    userViewModel: UserViewModel,
) {
    val firestore = Firebase.firestore
    val localidades =
        remember { mutableStateListOf<Pair<String, String>>() }  // (documentId, nombre)

    var valueText by remember { mutableStateOf("") }
    var expandedDropdown by remember { mutableStateOf(false) }

    val tipo = userViewModel.tipo.observeAsState("").value
    val clienteId = userViewModel.clienteId.observeAsState(null).value


    val navyBlue = Color(0xFF001F5B)

    // 🔄 Cargar localidades desde Firebase
    LaunchedEffect(tipo, clienteId) {
        if (tipo.isBlank()) return@LaunchedEffect

        var q: com.google.firebase.firestore.Query = firestore.collection("localidades")
        q = if (tipo.lowercase().trim() == "superuser") {
            q // sin filtro: ve todas
        } else {
            q.whereEqualTo("clienteId", clienteId)
        }

        q.orderBy("nombre")
            .get()
            .addOnSuccessListener { result ->
                localidades.clear()
                localidades.addAll(
                    result.documents.mapNotNull { doc ->
                        val nombre = doc.getString("nombre")
                        if (nombre != null) doc.id to nombre else null
                    }
                )
            }
    }



    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        val interactionSource = remember { MutableInteractionSource() }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, shape = RoundedCornerShape(12.dp)) // ✅ Fondo completo
                .border(2.dp, navyBlue, RoundedCornerShape(12.dp))            // ✅ Borde visible
        ) {
            OutlinedTextField(
                value = valueText,
                onValueChange = { }, // No editable manualmente
                placeholder = {
                    Text(
                        "Selecciona una localidad",
                        color = Color.Gray
                    )
                }, // ✅ Ajustado
                readOnly = true, // ✅ Evita teclado y cursor
                singleLine = true,
                enabled = true,
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    IconButton(onClick = {
                        onUserInteraction()
                        expandedDropdown = !expandedDropdown
                    }) {
                        Icon(
                            imageVector = if (expandedDropdown) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = "Icono desplegable",
                            tint = navyBlue
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = navyBlue,
                    unfocusedTextColor = navyBlue,
                    focusedLabelColor = navyBlue,
                    unfocusedLabelColor = navyBlue,
                    focusedContainerColor = Color.Transparent,   // Fondo lo da el Box
                    unfocusedContainerColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        onUserInteraction()
                        expandedDropdown = !expandedDropdown // ✅ Solo despliega
                    },
                interactionSource = interactionSource
            )
        }


// 👇 DropdownMenu con fondo crema
        DropdownMenu(
            expanded = expandedDropdown,
            onDismissRequest = { expandedDropdown = false },
            modifier = Modifier
                .width(200.dp)
                .background(Color.White)
        ) {
            localidades.forEach { (id, nombre) ->
                DropdownMenuItem(
                    text = { Text(nombre) },
                    onClick = {
                        onUserInteraction()
                        valueText = nombre
                        expandedDropdown = false
                        navController.navigate("inventoryentry/$id")  // ← usamos el documentId aquí
                    }
                )
            }
        }
    }
}