package com.eriknivar.firebasedatabase.view.settings

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
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

    LaunchedEffect(Unit) {
        if (selectedUser == null) {
            onContrasenaChange("12345") // ✅ Forzar el valor por defecto al crear
        }
    }

    AlertDialog(onDismissRequest = onDismiss, title = {
        Text(if (selectedUser == null) "Agregar Usuario" else "Editar Usuario")
        Log.d("Debug", "Creando nuevo usuario")

    }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = nombre.uppercase(),
                onValueChange = onNombreChange,
                label = { Text("Nombre y Apellido") },
                singleLine = true
            )
            OutlinedTextField(
                value = usuario.uppercase(),
                onValueChange = onUsuarioChange,
                label = { Text("Usuario") },
                singleLine = true
            )


            val requiereCambioPassword = selectedUser?.requiereCambioPassword ?: true

            OutlinedTextField(
                value = contrasena,
                onValueChange = if (requiereCambioPassword) ({ _: String -> }) else onContrasenaChange,
                label = { Text("Contraseña") },
                singleLine = true,
                enabled = !requiereCambioPassword,
                trailingIcon = {
                    if (requiereCambioPassword) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Campo bloqueado"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            ExposedDropdownMenuBox(
                expanded = expandedTipo, onExpandedChange = onExpandedTipoChange
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
                ExposedDropdownMenu(expanded = expandedTipo,
                    onDismissRequest = { onExpandedTipoChange(false) }) {
                    tipoOpciones.forEach { opcion ->
                        DropdownMenuItem(text = { Text(opcion) }, onClick = {
                            onTipoChange(opcion)
                            onExpandedTipoChange(false)
                        })
                    }
                }
            }
        }
    }, confirmButton = {
        TextButton(
            onClick = {
            if (nombre.isBlank() || usuario.isBlank() || contrasena.isBlank() || tipo.isBlank()) {
                return@TextButton
            }

            onSave(
                Usuario(
                    id = selectedUser?.id ?: "",
                    nombre = nombre,
                    usuario = usuario,
                    contrasena = contrasena,
                    tipo = tipo,
                    requiereCambioPassword = selectedUser?.requiereCambioPassword ?: true
                )
            )


        }) {
            Text("Guardar")
        }
    }, dismissButton = {
        TextButton(onClick = onDismiss) {
            Text("Cancelar")
        }
    })
}

