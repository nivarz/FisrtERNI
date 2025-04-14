package com.eriknivar.firebasedatabase.view.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.eriknivar.firebasedatabase.view.Usuario

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsuarioDialog(
    selectedUser: Usuario?,
    nombre: String,
    usuario: String,
    contrasena: String,
    tipo: String,
    tipoOpciones: List<String>,
    expandedTipo: Boolean,
    onNombreChange: (String) -> Unit,
    onUsuarioChange: (String) -> Unit,
    onContrasenaChange: (String) -> Unit,
    onTipoChange: (String) -> Unit,
    onExpandedTipoChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onSave: (Usuario) -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (selectedUser == null) "Agregar Usuario" else "Editar Usuario")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nombre.uppercase(),
                    onValueChange = onNombreChange,
                    label = { Text("Nombre") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = usuario.uppercase(),
                    onValueChange = onUsuarioChange,
                    label = { Text("Usuario") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = contrasena,
                    onValueChange = onContrasenaChange,
                    label = { Text("Contraseña") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(icon, contentDescription = null)
                        }
                    }
                )
                ExposedDropdownMenuBox(
                    expanded = expandedTipo,
                    onExpandedChange = onExpandedTipoChange
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
                        onDismissRequest = { onExpandedTipoChange(false) }
                    ) {
                        tipoOpciones.forEach { opcion ->
                            DropdownMenuItem(
                                text = { Text(opcion) },
                                onClick = {
                                    onTipoChange(opcion)
                                    onExpandedTipoChange(false)
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (nombre.isBlank() || usuario.isBlank() || contrasena.isBlank() || tipo.isBlank()) {
                    return@TextButton
                }
                onSave(Usuario("", nombre, usuario, contrasena, tipo))
            }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

