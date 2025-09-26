package com.eriknivar.firebasedatabase.view.storagetype

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.eriknivar.firebasedatabase.viewmodel.UserViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.firestore
import com.eriknivar.firebasedatabase.network.SelectedClientStore
import com.eriknivar.firebasedatabase.view.common.ClienteItem
import com.eriknivar.firebasedatabase.view.common.ClientePickerDialog
import com.eriknivar.firebasedatabase.view.common.cargarClientes
import androidx.compose.runtime.livedata.observeAsState
import com.eriknivar.firebasedatabase.data.LocalidadesRepo
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.text.style.TextAlign
import com.eriknivar.firebasedatabase.view.common.ConteoMode

@Composable
fun DropDownUpScreen(
    navController: NavHostController,
    userViewModel: UserViewModel,
    onUserInteraction: () -> Unit = {},
    localidades: List<String> = emptyList(),
    isLocalidadesLoading: Boolean = false,
    localidadSeleccionada: String? = null,
    onSelectLocalidad: (String) -> Unit = {},
    hasClienteSeleccionado: Boolean = false,
    isSuperuser: Boolean = false

) {
    val firestore = Firebase.firestore

    var valueText by remember { mutableStateOf("") }
    var expandedDropdown by remember { mutableStateOf(false) }

    val navyBlue = Color(0xFF001F5B)

    // 👇 Observa LiveData como State
    val rolRaw by userViewModel.tipo.observeAsState("")
    val cliente by userViewModel.clienteId.observeAsState("")

    // estados y refs
    val db = Firebase.firestore
    val cid by userViewModel.clienteId.observeAsState("")     // ya lo usas para el tipo
    val cidActual = cid.trim().uppercase()

    var showConteoDialog by remember { mutableStateOf(false) }
    var localidadElegida by remember { mutableStateOf("") }
    var pendingDialog by remember { mutableStateOf(false) }

    val canOpenMenu =
        hasClienteSeleccionado && !isLocalidadesLoading && (localidades.isNotEmpty() || isSuperuser)

    LaunchedEffect(hasClienteSeleccionado, isLocalidadesLoading, localidades.size, isSuperuser) {
        Log.d(
            "DDM",
            "hasCliente=${hasClienteSeleccionado}, loading=${isLocalidadesLoading}, size=${localidades.size}, super=${isSuperuser}"
        )
    }

    // estados del picker (arriba del DropdownMenu, en el mismo composable)
    val tipo by userViewModel.tipo.observeAsState("")
    val showClientePicker = remember { mutableStateOf(false) }
    val clientes = remember { mutableStateListOf<ClienteItem>() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        val interactionSource = remember { MutableInteractionSource() }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, shape = RoundedCornerShape(12.dp))
                .border(2.dp, navyBlue, RoundedCornerShape(12.dp))
        ) {
            OutlinedTextField(
                value = localidadSeleccionada ?: "",
                onValueChange = { /* readOnly */ },
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                placeholder = {
                    Text(
                        "Selecciona un Almacen",
                        color = Color(0xFF001F5B),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                readOnly = true,
                singleLine = true,
                enabled = hasClienteSeleccionado,
                shape = RoundedCornerShape(12.dp),
                supportingText = {
                    when {
                        !hasClienteSeleccionado -> Text("Selecciona un cliente para ver sus almacenes")
                        isLocalidadesLoading -> Text("Cargando…")
                        localidades.isEmpty() -> Text(
                            "Sin almacenes disponibles para este cliente",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (canOpenMenu) {
                                expandedDropdown = !expandedDropdown
                                onUserInteraction()
                            }
                        },
                        enabled = canOpenMenu
                    ) {
                        Icon(
                            imageVector = if (expandedDropdown) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                            contentDescription = if (expandedDropdown) "Cerrar lista" else "Abrir lista"
                        )
                    }
                },

                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = navyBlue,
                    unfocusedTextColor = navyBlue,
                    focusedLabelColor = navyBlue,
                    unfocusedLabelColor = navyBlue,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth(),

                )
        }

        DropdownMenu(
            expanded = expandedDropdown,
            onDismissRequest = { expandedDropdown = false },
            modifier = Modifier
                .width(200.dp)
                .background(Color.White)
        ) {
            when {
                !hasClienteSeleccionado -> {
                    DropdownMenuItem(
                        text = { Text("Selecciona un cliente") },
                        onClick = {},
                        enabled = false
                    )
                }

                isLocalidadesLoading -> {
                    DropdownMenuItem(
                        text = { Text("Cargando almacenes…") },
                        onClick = {},
                        enabled = false
                    )
                }

                localidades.isEmpty() -> {
                    DropdownMenuItem(
                        text = { Text("Sin almacenes para este cliente") },
                        onClick = {},
                        enabled = false
                    )
                }

                else -> {
                    localidades.forEach { loc ->
                        DropdownMenuItem(
                            text = { Text(loc) },
                            onClick = {
                                onUserInteraction()
                                localidadElegida = loc
                                pendingDialog = true        // pedir abrir diálogo
                                expandedDropdown = false     // ⬅️ abre el diálogo

                            }
                        )
                    }
                }
            }

            // Botón extra solo para superuser (con separador)
            if (isSuperuser) {
                androidx.compose.material3.HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Cambiar cliente") },
                    onClick = {
                        cargarClientes(
                            db = Firebase.firestore,
                            onOk = { lista ->
                                clientes.clear(); clientes.addAll(lista)
                                showClientePicker.value = true
                            },
                            onErr = { /* opcional */ }
                        )
                        expandedDropdown = false
                    }
                )
            }

            if (isSuperuser && localidades.isNotEmpty()) {
                DropdownMenuItem(
                    text = { Text("Todos los Almacenes") },
                    onClick = {
                        expandedDropdown = false
                        onUserInteraction()
                        onSelectLocalidad("__TODAS__")   // 👈 enviamos sentinela
                    }
                )
                HorizontalDivider()
            }
        }

        // Abre el diálogo solo cuando el menú terminó de cerrarse
        LaunchedEffect(expandedDropdown, pendingDialog) {
            if (!expandedDropdown && pendingDialog) {
                kotlinx.coroutines.delay(120)   // 100–150 ms
                showConteoDialog = true
                pendingDialog = false
            }
        }

        // Diálogo de selección de modo
        ConteoModeDialog(
            visible = showConteoDialog,
            onDismiss = { showConteoDialog = false },
            onConfirm = { modo ->
                showConteoDialog = false
                val loc = localidadElegida.orEmpty()
                navController.navigate("appEntrada?loc=$loc&mode=${modo.name}")
            }
        )


        ClientePickerDialog(
            open = showClientePicker,
            clientes = clientes
        ) { elegido ->
            LocalidadesRepo.invalidate(elegido.id)
            userViewModel.setClienteId(elegido.id)

            val docId = userViewModel.documentId.value ?: ""
            if (docId.isNotBlank()) {
                Firebase.firestore.collection("usuarios")
                    .document(docId)
                    .update("clienteId", elegido.id)
            }

            showClientePicker.value = false
        }

    }

}