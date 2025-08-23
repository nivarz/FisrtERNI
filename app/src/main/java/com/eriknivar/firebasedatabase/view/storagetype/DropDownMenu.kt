package com.eriknivar.firebasedatabase.view.storagetype

import android.util.Log
import android.widget.Toast
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
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.firestore

@Composable
fun DropDownUpScreen(
    navController: NavHostController,
    userViewModel: UserViewModel,
    onUserInteraction: () -> Unit = {}
) {
    val firestore = Firebase.firestore
    val localidades = remember { mutableStateListOf<String>() }

    var valueText by remember { mutableStateOf("") }
    var expandedDropdown by remember { mutableStateOf(false) }

    val navyBlue = Color(0xFF001F5B)

    // 👇 Observa LiveData como State
    val rolRaw by userViewModel.tipo.observeAsState("")
    val cliente by userViewModel.clienteId.observeAsState("")

    // 🔄 Cargar localidades según rol
    LaunchedEffect(rolRaw, cliente) {
        val rol = rolRaw.trim().lowercase()
        if (rol.isBlank()) return@LaunchedEffect  // evita correr con datos vacíos

        val col = firestore.collection("localidades")
        val q = if (rol == "superuser") col else col.whereEqualTo("clienteId", cliente)

        q.get()
            .addOnSuccessListener { result ->
                val nombres = result.documents.mapNotNull { it.getString("nombre") }
                Log.d("Localidades", "rol=$rol cliente=$cliente count=${nombres.size}")
                localidades.clear()
                localidades.addAll(nombres.sortedDescending())
            }
            .addOnFailureListener { e ->
                val code = (e as? FirebaseFirestoreException)?.code?.name ?: "?"
                Log.e("Localidades", "Error $code: ${e.message}", e)
                localidades.clear()
                Toast.makeText(
                    navController.context,
                    when (code) {
                        "PERMISSION_DENIED" -> "Sin permisos para leer localidades (reglas)"
                        else -> "Error cargando localidades"
                    },
                    Toast.LENGTH_LONG
                ).show()
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
                .background(Color.White, shape = RoundedCornerShape(12.dp))
                .border(2.dp, navyBlue, RoundedCornerShape(12.dp))
        ) {
            OutlinedTextField(
                value = valueText,
                onValueChange = { /* readOnly */ },
                placeholder = { Text("Selecciona una localidad", color = Color.Gray) },
                readOnly = true,
                singleLine = true,
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
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        onUserInteraction()
                        expandedDropdown = !expandedDropdown
                    },
                interactionSource = interactionSource
            )
        }

        DropdownMenu(
            expanded = expandedDropdown,
            onDismissRequest = { expandedDropdown = false },
            modifier = Modifier
                .width(200.dp)
                .background(Color.White)
        ) {
            localidades.forEach { localidad ->
                DropdownMenuItem(
                    text = { Text(localidad) },
                    onClick = {
                        onUserInteraction()
                        valueText = localidad
                        expandedDropdown = false
                        navController.navigate("inventoryentry/${valueText}")
                    }
                )
            }
        }
    }
}




