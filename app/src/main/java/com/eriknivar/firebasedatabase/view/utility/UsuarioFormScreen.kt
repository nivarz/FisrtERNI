package com.eriknivar.firebasedatabase.view.utility

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.eriknivar.firebasedatabase.viewmodel.UsuarioFormUi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsuarioFormScreen(
    ui: UsuarioFormUi,
    onBack: () -> Unit,
    onNombre: (String) -> Unit,
    onEmail: (String) -> Unit,
    onTipo: (String) -> Unit,
    onClienteId: (String) -> Unit,
    onSave: () -> Unit
) {
    val listState = rememberLazyListState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuevo usuario") },
                navigationIcon = {
                    IconButton(enabled = !ui.loading, onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = ui.nombre,
                    onValueChange = onNombre,
                    label = { Text("Nombre completo *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = ui.email,
                    onValueChange = onEmail,
                    label = { Text("Email *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email, imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                // Selector simple de rol
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = if (ui.tipo == "admin") "Administrador" else "Invitado",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Rol *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Administrador") },
                            onClick = { onTipo("admin"); expanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Invitado") },
                            onClick = { onTipo("invitado"); expanded = false }
                        )
                    }
                }
            }
            item {
                // Por simplicidad: clienteId (más adelante podemos listar clientes con dropdown)
                OutlinedTextField(
                    value = ui.clienteId,
                    onValueChange = onClienteId,
                    label = { Text("Cliente ID *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (!ui.error.isNullOrBlank()) {
                item {
                    Text(ui.error!!, color = MaterialTheme.colorScheme.error)
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        enabled = !ui.loading,
                        onClick = onBack,
                        modifier = Modifier.weight(1f)
                    ) { Text("Cancelar") }

                    Button(
                        enabled = !ui.loading,
                        onClick = onSave,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (ui.loading) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text("Guardar")
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
