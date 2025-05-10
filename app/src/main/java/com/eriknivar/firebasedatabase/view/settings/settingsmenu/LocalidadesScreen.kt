package com.eriknivar.firebasedatabase.view.settings.settingsmenu

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.navigation.NavHostController
import com.eriknivar.firebasedatabase.view.NavigationDrawer
import com.eriknivar.firebasedatabase.viewmodel.UserViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.delay

@Composable
fun LocalidadesScreen(navController: NavHostController, userViewModel: UserViewModel) {

    val tipo = userViewModel.tipo.value ?: ""
    if (tipo.lowercase() != "admin" && tipo.lowercase() != "superuser") {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.White),
            contentAlignment = Alignment.TopCenter
        ) {
            Text(
                text = "\u26D4\uFE0F Acceso restringido",
                color = Color.Red,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(32.dp)
            )
        }
        return
    }

    val firestore = Firebase.firestore
    val localidades = remember { mutableStateListOf<Pair<String, String>>() } // ID y nombre
    val dummy = remember { mutableStateOf("") }

    var showDialog by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var docIdToEdit by remember { mutableStateOf("") }
    var nombreInput by remember { mutableStateOf("") }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var localidadAEliminar by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showNombreExistenteDialog by remember { mutableStateOf(false) }

    fun cargarLocalidades() {
        firestore.collection("localidades")
            .orderBy("nombre")
            .get()
            .addOnSuccessListener { result ->
                localidades.clear()
                result.forEach { doc ->
                    val nombre = doc.getString("nombre") ?: ""
                    localidades.add(doc.id to nombre)
                }
            }
    }

    LaunchedEffect(Unit) { cargarLocalidades() }

    val context = LocalContext.current
    val lastInteractionTime = remember { mutableLongStateOf(System.currentTimeMillis()) }

    fun actualizarActividad() {
        lastInteractionTime.longValue = System.currentTimeMillis()

    }

    LaunchedEffect(lastInteractionTime.longValue) {
        while (true) {
            delay(60_000)
            val tiempoActual = System.currentTimeMillis()
            val tiempoInactivo = tiempoActual - lastInteractionTime.longValue

            if (tiempoInactivo >= 10 * 60_000) {
                val documentId = userViewModel.documentId.value ?: ""
                Firebase.firestore.collection("usuarios")
                    .document(documentId)
                    .update("sessionId", "")
                Toast.makeText(context, "Sesión finalizada por inactividad", Toast.LENGTH_LONG).show()

                userViewModel.clearUser()

                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                }

                break
            }
        }
    }

    NavigationDrawer(
        navController = navController,
        storageType = "Localidades",
        userViewModel = userViewModel,
        location = dummy,
        sku = dummy,
        quantity = dummy,
        lot = dummy,
        expirationDate = dummy
    ) {

        Column(modifier = Modifier.fillMaxSize()) {
            ElevatedButton(
                onClick = {
                    actualizarActividad()
                    nombreInput = ""
                    docIdToEdit = ""
                    isEditing = false
                    showDialog = true
                },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF001F5B), contentColor = Color.White)
            ) {
                Text("+ ", fontWeight = FontWeight.Bold, fontSize = 30.sp, color = Color.Green)
                Text("Crear Localidad")
            }


            LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
                items(
                    items = localidades,
                    key = { it.first } // Usa el docId como key
                ) { (docId, codigo) ->
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
                            Text(buildAnnotatedString {
                                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append("Código: ")
                                }
                                append(codigo)
                            })

                            Row {
                                IconButton(
                                    onClick = {
                                    isEditing = true
                                    docIdToEdit = docId
                                    nombreInput = codigo
                                    showDialog = true
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color.Blue)
                                }
                                IconButton(onClick = {
                                    localidadAEliminar = docId to codigo
                                    showDeleteDialog = true
                                }) {
                                    Icon(Icons.Default.DeleteForever, contentDescription = "Eliminar", tint = Color.Red)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text(if (isEditing) "Editar Localidad" else "Nueva Localidad") },
                text = {
                    OutlinedTextField(
                        value = nombreInput,
                        onValueChange = { nombreInput = it.uppercase().trimStart() },
                        label = { Text("Nombre de la Localidad*") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        if (nombreInput.isNotEmpty()) {
                            firestore.collection("localidades")
                                .whereEqualTo("nombre", nombreInput)
                                .get()
                                .addOnSuccessListener { documents ->
                                    val yaExiste = documents.any { doc -> !isEditing || doc.id != docIdToEdit }

                                    if (yaExiste) {
                                        showNombreExistenteDialog = true
                                    } else {
                                        val data = mapOf("nombre" to nombreInput)
                                        val operacion = if (isEditing) {
                                            firestore.collection("localidades").document(docIdToEdit).update(data)
                                        } else {
                                            firestore.collection("localidades").add(data)
                                        }
                                        operacion.addOnSuccessListener {
                                            successMessage = if (isEditing) "Localidad actualizada" else "Localidad creada"
                                            showSuccessDialog = true
                                            showDialog = false
                                            cargarLocalidades()
                                        }
                                    }
                                }
                        }
                    }) {
                        Text(if (isEditing) "Actualizar" else "Guardar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) { Text("Cancelar") }
                }
            )
        }

        if (showNombreExistenteDialog) {
            AlertDialog(
                onDismissRequest = { showNombreExistenteDialog = false },
                title = { Text("Localidad existente") },
                text = { Text("Ya existe una localidad con ese nombre.") },
                confirmButton = {
                    TextButton(onClick = { showNombreExistenteDialog = false }) {
                        Text("Aceptar")
                    }
                }
            )
        }

        if (showDeleteDialog) {
            localidadAEliminar?.let { (id, nombre) ->
                AlertDialog(
                    onDismissRequest = {
                        showDeleteDialog = false
                        localidadAEliminar = null
                    },
                    title = { Text("¿Eliminar localidad?") },
                    text = {
                        Text(buildAnnotatedString {
                            append("¿Estás seguro de que deseas eliminar la localidad ")
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("\"$nombre\"") }
                            append("? Esta acción no se puede deshacer.")
                        })
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            firestore.collection("localidades").document(id).delete().addOnSuccessListener {
                                showDeleteDialog = false
                                localidadAEliminar = null
                                cargarLocalidades()
                                successMessage = "Localidad eliminada"
                                showSuccessDialog = true
                            }
                        }) {
                            Text("Sí")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showDeleteDialog = false
                            localidadAEliminar = null
                        }) {
                            Text("Cancelar")
                        }
                    }
                )
            }
        }

        if (showSuccessDialog) {
            AlertDialog(
                onDismissRequest = { showSuccessDialog = false },
                title = { Text("Éxito") },
                text = { Text(successMessage) },
                confirmButton = {
                    TextButton(onClick = { showSuccessDialog = false }) {
                        Text("Aceptar")
                    }
                }
            )
        }
    }
}
