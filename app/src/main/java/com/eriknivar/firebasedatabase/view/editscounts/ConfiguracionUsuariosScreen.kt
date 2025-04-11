package com.eriknivar.firebasedatabase.view.editscounts

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eriknivar.firebasedatabase.view.Usuario
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.microsoft.schemas.compatibility.AlternateContentDocument.AlternateContent.Choice.type

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguracionUsuariosScreen(

) {
    val firestore = Firebase.firestore
    val usuarios = remember { mutableStateListOf<Usuario>() }
    val context = LocalContext.current

    var showUserDialog by remember { mutableStateOf(false) }
    var selectedUser by remember { mutableStateOf<Usuario?>(null) }

    var nombre by remember { mutableStateOf("") }
    var usuario by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf("") }
    val tipoOpciones = listOf("admin", "invitado")
    var expandedTipo by remember { mutableStateOf(false) }

    // 🔁 Cargar usuarios al iniciar
    LaunchedEffect(Unit) {
        firestore.collection("usuarios")
            .get()
            .addOnSuccessListener { result ->
                usuarios.clear()
                for (document in result) {
                    val nombreDoc = document.getString("nombre") ?: ""
                    val usuarioDoc = document.getString("usuario") ?: ""
                    val tipoDoc = document.getString("tipo") ?: ""
                    usuarios.add(Usuario(document.id, nombreDoc, usuarioDoc, tipoDoc))
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error al cargar usuarios", Toast.LENGTH_SHORT).show()
            }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Button(
            onClick = {
                selectedUser = null
                nombre = ""
                usuario = ""
                tipo = ""
                showUserDialog = true
            },
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text("Agregar Usuario")
        }

        LazyColumn(modifier = Modifier.padding(16.dp)) {
            items(usuarios) { user ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Nombre: ${user.nombre}", fontWeight = FontWeight.Bold)
                            Text("Usuario: ${user.usuario}")
                            Text("Tipo: ${user.tipo}")
                        }
                        Row {
                            IconButton(onClick = {
                                selectedUser = user
                                nombre = user.nombre
                                usuario = user.usuario
                                tipo = user.tipo
                                showUserDialog = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar")
                            }
                            IconButton(onClick = {
                                firestore.collection("usuarios").document(user.id).delete()
                                usuarios.remove(user)
                                Toast.makeText(context, "Usuario eliminado", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                            }
                        }
                    }
                }
            }
        }
    }

    // 📌 AlertDialog para crear o editar
    if (showUserDialog) {
        AlertDialog(
            onDismissRequest = { showUserDialog = false },
            title = {
                Text(if (selectedUser == null) "Agregar Usuario" else "Editar Usuario")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre") })
                    OutlinedTextField(value = usuario, onValueChange = { usuario = it }, label = { Text("Usuario") })
                    ExposedDropdownMenuBox(
                        expanded = expandedTipo,
                        onExpandedChange = { expandedTipo = !expandedTipo }
                    ) {
                        OutlinedTextField(
                            value = tipo,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tipo") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTipo)
                            },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedTipo,
                            onDismissRequest = { expandedTipo = false }
                        ) {
                            tipoOpciones.forEach { opcion ->
                                DropdownMenuItem(
                                    text = { Text(opcion) },
                                    onClick = {
                                        tipo = opcion
                                        expandedTipo = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (nombre.isBlank() || usuario.isBlank() || tipo.isBlank()) {
                        Toast.makeText(context, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                        return@TextButton
                    }

                    if (selectedUser == null) {
                        firestore.collection("usuarios")
                            .add(mapOf("nombre" to nombre, "usuario" to usuario, "tipo" to tipo))
                            .addOnSuccessListener {
                                usuarios.add(Usuario(it.id, nombre, usuario, tipo))
                                Toast.makeText(context, "Usuario agregado", Toast.LENGTH_SHORT).show()
                                showUserDialog = false
                            }
                    } else {
                        firestore.collection("usuarios").document(selectedUser!!.id)
                            .update("nombre", nombre, "usuario", usuario, "tipo", tipo)
                            .addOnSuccessListener {
                                val index = usuarios.indexOfFirst { it.id == selectedUser!!.id }
                                if (index != -1) {
                                    usuarios[index] = Usuario(selectedUser!!.id, nombre, usuario, tipo)
                                }
                                Toast.makeText(context, "Usuario actualizado", Toast.LENGTH_SHORT).show()
                                showUserDialog = false
                            }
                    }
                }) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUserDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}






