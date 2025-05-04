package com.eriknivar.firebasedatabase.view.settings

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eriknivar.firebasedatabase.view.Usuario
import com.eriknivar.firebasedatabase.viewmodel.UserViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

@Composable
fun ConfiguracionUsuariosScreen(
    userViewModel: UserViewModel,
    onUserInteraction: () -> Unit
) {
    val firestore = Firebase.firestore
    val usuarios = remember { mutableStateListOf<Usuario>() }
    val context = LocalContext.current
    val currentUserId = userViewModel.documentId.value ?: ""
    val currentSessionId = userViewModel.sessionId.value

    var showUserDialog by remember { mutableStateOf(false) }
    var selectedUser by remember { mutableStateOf<Usuario?>(null) }
    var userToDelete by remember { mutableStateOf<Usuario?>(null) }

    var nombre by remember { mutableStateOf("") }
    var usuario by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf("") }
    val tipoOpciones = listOf("admin", "invitado")
    var expandedTipo by remember { mutableStateOf(false) }

    var showUserExistsDialog by remember { mutableStateOf(false) }

    val navyBlue = Color(0xFF001F5B)

    fun recargarUsuarios() {
        firestore.collection("usuarios")
            .get()
            .addOnSuccessListener { result ->
                usuarios.clear()
                for (document in result) {
                    val nombreDoc = document.getString("nombre") ?: ""
                    val usuarioDoc = document.getString("usuario") ?: ""
                    val contrasenaDoc = document.getString("contrasena") ?: ""
                    val tipoDoc = document.getString("tipo") ?: ""
                    val sessionIdDoc = document.getString("sessionId") ?: ""

                    val tipoUsuarioActual = userViewModel.tipo.value ?: ""
                    if (tipoUsuarioActual == "admin" && tipoDoc == "superuser") continue

                    usuarios.add(
                        Usuario(
                            document.id,
                            nombreDoc,
                            usuarioDoc,
                            contrasenaDoc,
                            tipoDoc,
                            sessionIdDoc
                        )
                    )
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error al cargar usuarios", Toast.LENGTH_SHORT).show()
            }
    }

    // 🔁 Cargar usuarios al iniciar
    LaunchedEffect(true) {
        recargarUsuarios()
    }


    LaunchedEffect(userViewModel.sessionId.value) {
        if ((userViewModel.tipo.value ?: "") == "superuser") {
            firestore.collection("usuarios")
                .get()
                .addOnSuccessListener { result ->
                    usuarios.clear()
                    for (document in result) {
                        val nombreDoc = document.getString("nombre") ?: ""
                        val usuarioDoc = document.getString("usuario") ?: ""
                        val contrasenaDoc = document.getString("contrasena") ?: ""
                        val tipoDoc = document.getString("tipo") ?: ""
                        val sessionIdDoc = document.getString("sessionId") ?: ""

                        val tipoUsuarioActual = userViewModel.tipo.value ?: ""
                        if (tipoUsuarioActual == "admin" && tipoDoc == "superuser") continue

                        usuarios.add(
                            Usuario(
                                document.id,
                                nombreDoc,
                                usuarioDoc,
                                contrasenaDoc,
                                tipoDoc,
                                sessionIdDoc
                            )
                        )
                    }
                }
        }
    }

    LaunchedEffect(userViewModel.recargarUsuarios.value) {
        firestore.collection("usuarios")
            .get()
            .addOnSuccessListener { result ->
                usuarios.clear()
                for (document in result) {
                    val nombreDoc = document.getString("nombre") ?: ""
                    val usuarioDoc = document.getString("usuario") ?: ""
                    val contrasenaDoc = document.getString("contrasena") ?: ""
                    val tipoDoc = document.getString("tipo") ?: ""
                    val sessionIdDoc = document.getString("sessionId") ?: ""

                    val tipoUsuarioActual = userViewModel.tipo.value ?: ""
                    if (tipoUsuarioActual == "admin" && tipoDoc == "superuser") continue

                    usuarios.add(
                        Usuario(
                            document.id,
                            nombreDoc,
                            usuarioDoc,
                            contrasenaDoc,
                            tipoDoc,
                            sessionIdDoc
                        )
                    )
                }
            }
    }

    DisposableEffect(currentUserId, currentSessionId) {
        val listenerRegistration = Firebase.firestore.collection("usuarios")
            .document(currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreListener", "Error en snapshotListener", error)
                    return@addSnapshotListener
                }

                val remoteSessionId = snapshot?.getString("sessionId") ?: ""

                if (remoteSessionId != currentSessionId && !userViewModel.isManualLogout.value) {
                    Toast.makeText(
                        context,
                        "Tu sesión fue cerrada por el administrador",
                        Toast.LENGTH_LONG
                    ).show()

                    userViewModel.clearUser()

                    // 🔁 Forzar salida
                    val activity = (context as? Activity)
                    activity?.finishAffinity()
                }
            }

        onDispose {
            listenerRegistration.remove()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ElevatedButton(
            colors = ButtonDefaults.buttonColors(
                containerColor = navyBlue, contentColor = Color.White
            ),
            onClick = {
                onUserInteraction()
                selectedUser = null
                nombre = ""
                usuario = ""
                contrasena = ""
                tipo = ""
                showUserDialog = true
            },
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text("+ ", fontWeight = FontWeight.Bold, fontSize = 30.sp, color = Color.Green)

            Text("Crear usuario")
        }

        ElevatedButton(
            onClick = {
                onUserInteraction()
                firestore.collection("usuarios")
                    .get()
                    .addOnSuccessListener { result ->
                        usuarios.clear()
                        for (document in result) {
                            val nombreDoc = document.getString("nombre") ?: ""
                            val usuarioDoc = document.getString("usuario") ?: ""
                            val contrasenaDoc = document.getString("contrasena") ?: ""
                            val tipoDoc = document.getString("tipo") ?: ""
                            val sessionIdDoc = document.getString("sessionId") ?: ""

                            val tipoUsuarioActual = userViewModel.tipo.value ?: ""
                            if (tipoUsuarioActual == "admin" && tipoDoc == "superuser") continue

                            usuarios.add(
                                Usuario(
                                    document.id,
                                    nombreDoc,
                                    usuarioDoc,
                                    contrasenaDoc,
                                    tipoDoc,
                                    sessionIdDoc
                                )
                            )
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Error al actualizar la lista", Toast.LENGTH_SHORT)
                            .show()
                    }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF001F5B),
                contentColor = Color.White
            ),
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)

            ) {
                Text("Actualizar (", fontWeight = FontWeight.Bold)

                Text("Activos", fontWeight = FontWeight.Bold)

                // 🔵 LED verde
                Box(
                    modifier = Modifier
                        .size(10.dp) // un poco más grande para equilibrar visualmente
                        .background(Color.Green, CircleShape)
                )

                Text("/", fontWeight = FontWeight.Bold)

                Text("Inactivos", fontWeight = FontWeight.Bold)

                // 🔴 LED rojo
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Color.Red, CircleShape)
                )

                Text(")", fontWeight = FontWeight.Bold)
            }

        }

        LazyColumn(modifier = Modifier.padding(16.dp)) {
            items(usuarios) { user ->


                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // 🔹 Fila con LED y Nombre
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(
                                        color = if (user.sessionId?.isNotBlank() == true) Color.Green else Color.Red,
                                        shape = CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(buildAnnotatedString {
                                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append(
                                        "Nombre: "
                                    )
                                }
                                append(user.nombre)
                            })
                        }

                        Text(buildAnnotatedString {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) { append("Usuario: ") }
                            append(user.usuario)
                        })

                        Text(buildAnnotatedString {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) { append("Contraseña: ") }
                            append("********")
                        })

                        Text(buildAnnotatedString {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) { append("Tipo: ") }
                            append(user.tipo)
                        })

                        // 🔹 Fila de íconos alineados
                        if (user.tipo != "superuser") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row {
                                    IconButton(onClick = {
                                        selectedUser = user
                                        nombre = user.nombre
                                        usuario = user.usuario
                                        contrasena = user.contrasena
                                        tipo = user.tipo
                                        showUserDialog = true
                                    }) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Editar",
                                            tint = Color.Blue
                                        )
                                    }

                                    IconButton(onClick = {
                                        userToDelete = user
                                    }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Eliminar",
                                            tint = Color.DarkGray
                                        )
                                    }
                                }

                                // ✅ Botón de forzar logout solo visible para superadmin
                                if ((userViewModel.tipo.value ?: "") == "superuser") {

                                    IconButton(onClick = {
                                        Firebase.firestore.collection("usuarios")
                                            .document(user.id)
                                            .update("sessionId", "")
                                            .addOnSuccessListener {
                                                recargarUsuarios()
                                                // 🔄 Recargar lista de usuarios luego del logout
                                                firestore.collection("usuarios")
                                                    .get()
                                                    .addOnSuccessListener { result ->
                                                        usuarios.clear()
                                                        for (document in result) {
                                                            val nombreDoc =
                                                                document.getString("nombre") ?: ""
                                                            val usuarioDoc =
                                                                document.getString("usuario") ?: ""
                                                            val contrasenaDoc =
                                                                document.getString("contrasena")
                                                                    ?: ""
                                                            val tipoDoc =
                                                                document.getString("tipo") ?: ""
                                                            val sessionIdDoc =
                                                                document.getString("sessionId")
                                                                    ?: ""

                                                            val tipoUsuarioActual =
                                                                userViewModel.tipo.value ?: ""
                                                            if (tipoUsuarioActual == "admin" && tipoDoc == "superuser") continue

                                                            usuarios.add(
                                                                Usuario(
                                                                    document.id,
                                                                    nombreDoc,
                                                                    usuarioDoc,
                                                                    contrasenaDoc,
                                                                    tipoDoc,
                                                                    sessionIdDoc
                                                                )
                                                            )
                                                        }
                                                    }
                                            }
                                    }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Logout,
                                            contentDescription = "Forzar Logout",
                                            tint = Color.Gray
                                        )
                                    }


                                }
                            }
                        }
                    }
                }


            }
        }
    }

    if (showUserDialog) {
        UsuarioDialog(
            selectedUser,
            nombre,
            usuario,
            contrasena,
            tipo,
            tipoOpciones,
            expandedTipo,
            { nombre = it },
            { usuario = it },
            { contrasena = it },
            { tipo = it },
            { expandedTipo = it },
            onDismiss = { showUserDialog = false },
            onSave = { nuevoUsuario ->
                val nombreUpper = nuevoUsuario.nombre.uppercase()
                val usuarioUpper = nuevoUsuario.usuario.uppercase()

                if (selectedUser == null) {
                    // ✅ Verificar duplicados
                    firestore.collection("usuarios")
                        .whereEqualTo("usuario", usuarioUpper)
                        .get()
                        .addOnSuccessListener { documents ->
                            if (documents.isEmpty) {
                                // No existe, lo podemos guardar
                                firestore.collection("usuarios")
                                    .add(
                                        mapOf(
                                            "nombre" to nombreUpper,
                                            "usuario" to usuarioUpper,
                                            "contrasena" to nuevoUsuario.contrasena,
                                            "tipo" to nuevoUsuario.tipo
                                        )
                                    )

                                    .addOnSuccessListener {
                                        usuarios.add(
                                            Usuario(
                                                it.id,
                                                nombreUpper,
                                                usuarioUpper,
                                                nuevoUsuario.contrasena,
                                                nuevoUsuario.tipo
                                            )
                                        )
                                        showUserDialog =
                                            false // ✅ solo cerrar el diálogo, no mostrar alerta
                                    }

                            } else {
                                showUserExistsDialog = true
                            }
                        }
                } else {
                    // ✅ Si estamos editando, permitimos que mantenga su propio nombre de usuario
                    firestore.collection("usuarios")
                        .whereEqualTo("usuario", usuarioUpper)
                        .get()
                        .addOnSuccessListener { documents ->
                            val yaExiste = documents.any { it.id != selectedUser!!.id }
                            if (yaExiste) {
                                showUserExistsDialog = true

                            } else {
                                firestore.collection("usuarios").document(selectedUser!!.id)
                                    .update(
                                        "nombre", nombreUpper,
                                        "usuario", usuarioUpper,
                                        "contrasena", nuevoUsuario.contrasena,
                                        "tipo", nuevoUsuario.tipo
                                    )
                                    .addOnSuccessListener {
                                        val index =
                                            usuarios.indexOfFirst { it.id == selectedUser!!.id }
                                        if (index != -1) {
                                            usuarios[index] = Usuario(
                                                selectedUser!!.id,
                                                nombreUpper,
                                                usuarioUpper,
                                                nuevoUsuario.contrasena,
                                                nuevoUsuario.tipo
                                            )
                                        }
                                        Toast.makeText(
                                            context,
                                            "Usuario actualizado",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        showUserDialog = false
                                    }
                            }
                        }
                }
            }

        )
    }

    if (showUserExistsDialog) {
        AlertDialog(
            onDismissRequest = { showUserExistsDialog = false },
            title = { Text("Usuario existente") },
            text = { Text("Ya existe un usuario con ese nombre. Por favor elige otro.") },
            confirmButton = {
                TextButton(onClick = { showUserExistsDialog = false }) {
                    Text("Aceptar")
                }
            }
        )
    }


    userToDelete?.let { user ->
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = { Text("Confirmar eliminación") },
            text = { Text("¿Estás seguro de que deseas eliminar al usuario \"${user.usuario}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    firestore.collection("usuarios").document(user.id).delete()
                        .addOnSuccessListener {
                            usuarios.remove(user)
                            Toast.makeText(context, "Usuario eliminado", Toast.LENGTH_SHORT).show()
                            userToDelete = null
                        }
                }) {
                    Text("Sí")
                }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}





