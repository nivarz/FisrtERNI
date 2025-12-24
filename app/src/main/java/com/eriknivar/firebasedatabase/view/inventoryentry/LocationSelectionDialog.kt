package com.eriknivar.firebasedatabase.view.inventoryentry

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

@Composable
fun LocationSelectionDialog(
    locations: List<String>,
    title: String = "Seleccionar ubicación",
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf(TextFieldValue("")) }

    // Filtrado simple por contiene, ignorando mayúsculas
    val filtered = remember(locations, query) {
        val q = query.text.trim().uppercase()
        val base = if (q.isEmpty()) locations else locations.filter { it.uppercase().contains(q) }
        base.distinct() // <- evita duplicados en la UI
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { newValue ->
                        val upper = newValue.text.uppercase()
                        query = newValue.copy(
                            text = upper,
                            selection = TextRange(upper.length)  // cursor al final
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    singleLine = true,
                    placeholder = { Text("Buscar") },
                    label = { Text("Buscar") },
                    maxLines = 1,

                    trailingIcon = {
                        if (query.text.isNotEmpty()) {
                            IconButton(onClick = { query = TextFieldValue("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Borrar búsqueda")
                            }
                        }
                    }
                )


                Box(Modifier.heightIn(min = 120.dp, max = 360.dp)) {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        // usamos la posición como key implícita; no forzamos String como key
                        items(filtered) { code ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelect(code)
                                        onDismiss()
                                    }
                                    .padding(vertical = 10.dp, horizontal = 8.dp)
                            ) {
                                Text(code)
                            }
                        }
                    }
                }

                if (filtered.isEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Sin resultados.")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0D3B66),   // dark blue
                    contentColor   = Color.White
                )
            ) { Text("Cerrar") }
        }
        ,

    )
}