package com.eriknivar.firebasedatabase.view.utility

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.eriknivar.firebasedatabase.viewmodel.ClienteFormMode
import com.eriknivar.firebasedatabase.viewmodel.ClienteFormInput
import androidx.compose.material.icons.automirrored.filled.ArrowBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClienteFormScreen(
    mode: com.eriknivar.firebasedatabase.viewmodel.ClienteFormMode,
    initial: ClienteFormInput = ClienteFormInput(),
    loading: Boolean = false,
    errorGlobal: String? = null,            // para mostrar errores del ViewModel (cuando conectemos)
    onBack: () -> Unit,
    onSubmit: (ClienteFormInput) -> Unit
) {
    var nombre by remember(initial) { mutableStateOf(initial.nombreComercial) }
    var rnc by remember(initial) { mutableStateOf(initial.rncOCedula) }
    var tel by remember(initial) { mutableStateOf(initial.telefono) }
    var mail by remember(initial) { mutableStateOf(initial.email) }
    var dir by remember(initial) { mutableStateOf(initial.direccion) }
    var notas by remember(initial) { mutableStateOf(initial.notas) }

    // Errores por campo
    var eNombre by remember { mutableStateOf<String?>(null) }
    var eRnc by remember { mutableStateOf<String?>(null) }
    var eMail by remember { mutableStateOf<String?>(null) }
    var eTel by remember { mutableStateOf<String?>(null) }

    fun validarCampos(): Boolean {
        eNombre = ClienteUtils.validarNombre(nombre)
        eRnc = ClienteUtils.validarRnc(rnc)
        eMail = ClienteUtils.validarEmailBasico(mail)
        eTel = ClienteUtils.validarTelefonoRD(tel)
        return listOf(eNombre, eRnc, eMail, eTel).all { it == null }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (mode == ClienteFormMode.CREATE) "Nuevo cliente" else "Editar cliente") },
                navigationIcon = {
                    IconButton(enabled = !loading, onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )

                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (mode == ClienteFormMode.EDIT && !initial.clienteId.isNullOrBlank()) {
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text("ID: ${initial.clienteId}") }
                )
            }

            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it; if (eNombre != null) eNombre = null },
                label = { Text("Nombre comercial *") },
                singleLine = true,
                isError = eNombre != null,
                supportingText = { if (eNombre != null) Text(eNombre!!) },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = rnc,
                onValueChange = { rnc = it; if (eRnc != null) eRnc = null },
                label = { Text("RNC / Cédula *") },
                singleLine = true,
                isError = eRnc != null,
                supportingText = { if (eRnc != null) Text(eRnc!!) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Ascii, // acepta dígitos y letras
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = tel,
                onValueChange = { tel = it; if (eTel != null) eTel = null },
                label = { Text("Teléfono (10 dígitos)") },
                singleLine = true,
                isError = eTel != null,
                supportingText = { if (eTel != null) Text(eTel!!) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = mail,
                onValueChange = { mail = it; if (eMail != null) eMail = null },
                label = { Text("Email") },
                singleLine = true,
                isError = eMail != null,
                supportingText = { if (eMail != null) Text(eMail!!) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = dir,
                onValueChange = { dir = it },
                label = { Text("Dirección") },
                singleLine = false,
                minLines = 2,
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = notas,
                onValueChange = { notas = it },
                label = { Text("Notas") },
                singleLine = false,
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            if (!errorGlobal.isNullOrBlank()) {
                Text(
                    text = errorGlobal,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onBack,
                    enabled = !loading,
                    modifier = Modifier.weight(1f)
                ) { Text("Cancelar") }

                Button(
                    onClick = {
                        if (validarCampos() && !loading) {
                            onSubmit(
                                ClienteFormInput(
                                    clienteId = initial.clienteId,
                                    nombreComercial = nombre.trim(),
                                    rncOCedula = rnc.trim(),
                                    telefono = tel.trim(),
                                    email = mail.trim(),
                                    direccion = dir.trim(),
                                    notas = notas.trim()
                                )
                            )
                        }
                    },
                    enabled = !loading,
                    modifier = Modifier.weight(1f)
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier
                                .size(18.dp)
                                .padding(end = 8.dp)
                        )
                    }
                    Text(if (mode == ClienteFormMode.CREATE) "Guardar" else "Guardar cambios")
                }
            }
        }
    }
}
