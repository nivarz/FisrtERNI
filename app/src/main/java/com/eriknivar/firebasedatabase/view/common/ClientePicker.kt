package com.eriknivar.firebasedatabase.view.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore

data class ClienteItem(val id: String, val nombre: String)

fun cargarClientes(
    db: FirebaseFirestore,
    onOk: (List<ClienteItem>) -> Unit,
    onErr: (Exception) -> Unit
) {
    db.collection("clientes")
        .orderBy("nombreNormalizado") // usa el campo que tengas para ordenar
        .get()
        .addOnSuccessListener { snap ->
            val lista = snap.documents.mapNotNull { d ->
                // Escoge el mejor “display name” disponible
                val rawName =
                    d.getString("nombre")
                        ?: d.getString("razonSocial")
                        ?: d.getString("nombreComercial")
                        ?: d.getString("alias")
                        ?: d.id
                val display = rawName.ifBlank { d.id }
                ClienteItem(id = d.id, nombre = display)
            }
            onOk(lista)
        }
        .addOnFailureListener(onErr)
}

@Composable
fun ClientePickerDialog(
    open: MutableState<Boolean>,
    clientes: List<ClienteItem>,
    onSelect: (ClienteItem) -> Unit
) {
    if (!open.value) return

    AlertDialog(
        onDismissRequest = { /* forzamos selección */ },
        title = { Text("Selecciona un cliente") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                items(clientes.size) { i ->
                    val c = clientes[i]

                    // Título y subtítulo sin duplicar id
                    val titulo = c.nombre.ifBlank { c.id }
                    val subtitulo = c.id.takeUnless { it.equals(titulo, ignoreCase = true) }

                    ListItem(
                        headlineContent = { Text(titulo) },
                        supportingContent = { subtitulo?.let { Text(it) } },
                        modifier = Modifier.clickable {
                            onSelect(c)
                        }
                    )
                    Divider()
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { /* no hacemos nada sin tocar un item */ }) {
                Text("Elegir de la lista")
            }
        }
    )
}

