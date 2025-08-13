package com.eriknivar.firebasedatabase.view.settings.settingsmenu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.eriknivar.firebasedatabase.view.utility.Cliente
import com.eriknivar.firebasedatabase.viewmodel.UserViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import com.eriknivar.firebasedatabase.view.NavigationDrawer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestionClientesScreen(
    navController: NavHostController,
    userViewModel: UserViewModel
) {
    // ✅ Solo superuser
    val tipo by userViewModel.tipo.observeAsState("")
    if (tipo.lowercase().trim() != "superuser") {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("⛔ Acceso restringido", color = Color.Red, fontWeight = FontWeight.Bold)
        }
        return
    }

    val firestore = Firebase.firestore
    val clientes = remember { mutableStateListOf<Cliente>() }

    // 🔔 Live updates
    DisposableEffect(Unit) {
        val reg = firestore.collection("clientes")
            .addSnapshotListener { snap, _ ->
                if (snap != null) {
                    val list = snap.documents.map { d ->
                        Cliente(
                            id = d.id,
                            nombre = d.getString("nombre").orEmpty(),
                            activo = d.getBoolean("activo") ?: true
                        )
                    }.sortedBy { it.nombre.uppercase() }
                    clientes.clear(); clientes.addAll(list)
                }
            }
        onDispose { reg.remove() }
    }

    // 🧱 Estado UI diálogo
    var showDialog by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var idEdit by remember { mutableStateOf("") }
    var idInput by remember { mutableStateOf("") }
    var nombreInput by remember { mutableStateOf("") }

    // 🧩 Para NavigationDrawer (igual que LocalidadesScreen)
    val dummy = remember { mutableStateOf("") }

    // 🔵 Colores y contexto
    val navy = Color(0xFF001F5B)
    val context = LocalContext.current

    // (opcional) si usas el control de inactividad como en LocalidadesScreen, puedes reutilizarlo aquí

    NavigationDrawer(
        navController = navController,
        storageType = "Gestión de Clientes",
        userViewModel = userViewModel,
        location = dummy,
        sku = dummy,
        quantity = dummy,
        lot = dummy,
        expirationDate = dummy
    ) {

        Column(modifier = Modifier.fillMaxSize()) {

            // 🔹 Botón igual al de Localidades
            ElevatedButton(
                onClick = {
                    isEditing = false
                    idEdit = ""
                    idInput = ""
                    nombreInput = ""
                    showDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = navy, contentColor = Color.White)
            ) {
                Text("+ ", fontWeight = FontWeight.Bold, fontSize = 30.sp, color = Color.Green)
                Text("Crear Cliente")
            }

            // 🔹 Lista con la misma card
            LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
                items(clientes, key = { it.id }) { c ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(buildAnnotatedString {
                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Código: ") }
                                    append(c.id)
                                })
                                Text(buildAnnotatedString {
                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Nombre: ") }
                                    append(c.nombre)
                                })
                                Text(buildAnnotatedString {
                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Estado: ") }
                                    append(if (c.activo) "Activo" else "Inactivo")
                                })
                            }

                            Row {
                                IconButton(onClick = {
                                    isEditing = true
                                    idEdit = c.id
                                    idInput = c.id
                                    nombreInput = c.nombre
                                    showDialog = true
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color.Blue)
                                }
                                IconButton(onClick = {
                                    firestore.collection("clientes").document(c.id)
                                        .update("activo", !c.activo)
                                }) {
                                    Icon(
                                        if (c.activo) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Activar/Desactivar",
                                        tint = if (c.activo) Color(0xFF2E7D32) else Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 🔹 Diálogo Crear/Editar (mismo look & feel)
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text(if (isEditing) "Editar Cliente" else "Nuevo Cliente") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = idInput,
                            onValueChange = { idInput = it.uppercase().replace(" ", "").trim() },
                            label = { Text("Cliente ID* (único, sin espacios)") },
                            singleLine = true,
                            enabled = !isEditing // no cambiar ID al editar
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = nombreInput,
                            onValueChange = { nombreInput = it.trimStart() },
                            label = { Text("Nombre*") },
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val idNorm = idInput.uppercase().replace(" ", "").trim()
                        val nombreNorm = nombreInput.trim()
                        if (!isEditing && idNorm.isEmpty()) return@Button
                        if (nombreNorm.isEmpty()) return@Button

                        if (isEditing) {
                            firestore.collection("clientes").document(idEdit)
                                .update("nombre", nombreNorm)
                                .addOnSuccessListener { showDialog = false }
                        } else {
                            val docRef = firestore.collection("clientes").document(idNorm)
                            docRef.get().addOnSuccessListener { d ->
                                if (!d.exists()) {
                                    docRef.set(mapOf("nombre" to nombreNorm, "activo" to true))
                                        .addOnSuccessListener { showDialog = false }
                                } else {
                                    // opcional: snackbar/alert de duplicado
                                }
                            }
                        }
                    }) { Text(if (isEditing) "Actualizar" else "Guardar") }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) { Text("Cancelar") }
                }
            )
        }
    }
}
