package com.eriknivar.firebasedatabase.view.inventoryreports

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eriknivar.firebasedatabase.view.inventoryentry.updateFirestore
import com.eriknivar.firebasedatabase.view.storagetype.DataFields
import com.eriknivar.firebasedatabase.viewmodel.UserViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun InventoryReportFiltersScreen(
    userViewModel: UserViewModel,
    allData: List<DataFields>,
    tipoUsuario: String,
    puedeModificarRegistro: (String, String) -> Boolean
)
 {
    val sku = remember { mutableStateOf("") }
    val location = remember { mutableStateOf("") }
    val startDate = remember { mutableStateOf("") }
    val endDate = remember { mutableStateOf("") }
    val filtrosExpandido = remember { mutableStateOf(false) }

    val usuarioFiltro = remember { mutableStateOf("") }
    val filteredData = remember { mutableStateListOf<DataFields>() }

    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance() }
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    val firestore = Firebase.firestore
    val listaLocalidades = remember { mutableStateListOf<String>() }
    val localidadSeleccionada = remember { mutableStateOf("") }

    val isLoading = remember { mutableStateOf(false) }


// 🔄 Cargar localidades desde Firestore
    LaunchedEffect(Unit) {
        firestore.collection("localidades")
            .get()
            .addOnSuccessListener { result ->
                listaLocalidades.clear()
                listaLocalidades.addAll(result.mapNotNull { it.getString("nombre") })
            }
    }


    LaunchedEffect(tipoUsuario, userViewModel.nombre) {
        if (tipoUsuario != "admin" && tipoUsuario != "superuser") {
            usuarioFiltro.value = userViewModel.nombre.value ?: ""
        }
    }

    val startDatePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, y, m, d ->
                calendar.set(y, m, d)
                startDate.value = dateFormatter.format(calendar.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    val endDatePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, y, m, d ->
                calendar.set(y, m, d)
                endDate.value = dateFormatter.format(calendar.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // 🔽 Encabezado expandible
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { filtrosExpandido.value = !filtrosExpandido.value }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (filtrosExpandido.value) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Filtros de búsqueda",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }

        // 🧩 Filtros dentro de AnimatedVisibility
        AnimatedVisibility(visible = filtrosExpandido.value) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                val tipo = tipoUsuario.lowercase().trim()

                fun limpiarFiltros() {
                    usuarioFiltro.value = if (tipo == "invitado") {
                        userViewModel.nombre.value ?: ""
                    } else {
                        ""
                    }

                    sku.value = ""
                    location.value = ""
                    startDate.value = ""
                    endDate.value = ""
                    localidadSeleccionada.value = ""
                    filteredData.clear()
                    filteredData.addAll(allData.sortedByDescending { it.fechaRegistro?.toDate() })
                }


                OutlinedTextField(
                    value = usuarioFiltro.value.uppercase(),
                    onValueChange = { usuarioFiltro.value = it },
                    label = { Text("Nombre de Usuario") },
                    singleLine = true,
                    enabled = tipoUsuario.lowercase().trim() in listOf("admin", "superuser"),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = sku.value.uppercase(),
                    onValueChange = { sku.value = it },
                    label = { Text("SKU o palabra clave") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = location.value.uppercase(),
                    onValueChange = { location.value = it },
                    label = { Text("Ubicación") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                LocalidadDropdown(
                    localidadSeleccionada = localidadSeleccionada,
                    listaLocalidades = listaLocalidades
                )


                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startDate.value,
                        onValueChange = {},
                        label = { Text("Desde") },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { startDatePickerDialog.show() }) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .clickable { startDatePickerDialog.show() }
                    )

                    OutlinedTextField(
                        value = endDate.value,
                        onValueChange = {},
                        label = { Text("Hasta") },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { endDatePickerDialog.show() }) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .clickable { endDatePickerDialog.show() }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {

                    val azulMarino = Color(0xFF001F5B)

                    Button(
                        onClick = {
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            isLoading.value = true

                            val filtros = mapOf(
                                "usuario" to usuarioFiltro.value.uppercase(),
                                "localidad" to localidadSeleccionada.value
                                // Puedes agregar más si tu estructura de Firestore lo permite
                            )

                            fetchFilteredInventoryFromFirestore(
                                db = Firebase.firestore,
                                filters = filtros,
                                tipoUsuario = tipoUsuario,
                                onResult = { nuevosDatos ->
                                    filteredData.clear()
                                    filteredData.addAll(
                                        nuevosDatos.filter { item ->
                                            val matchesSku = sku.value.isBlank() || item.sku.contains(sku.value, true) || item.description.contains(sku.value, true)
                                            val matchesLocation = location.value.isBlank() || item.location.equals(location.value, true)

                                            val dateFormatted = item.fechaRegistro?.toDate()?.let { sdf.format(it) } ?: ""
                                            val matchesDate = try {
                                                (startDate.value.isBlank() || dateFormatted >= startDate.value) &&
                                                        (endDate.value.isBlank() || dateFormatted <= endDate.value)
                                            } catch (e: Exception) {
                                                true
                                            }

                                            val matchesLocalidad = localidadSeleccionada.value.isBlank() ||
                                                    item.localidad.equals(localidadSeleccionada.value, ignoreCase = true)

                                            matchesSku && matchesLocation && matchesDate && matchesLocalidad
                                        }.sortedByDescending { it.fechaRegistro?.toDate() }
                                    )
                                    filtrosExpandido.value = false
                                    isLoading.value = false
                                },
                                onError = {
                                    isLoading.value = false
                                    Toast.makeText(context, "Error al consultar Firestore", Toast.LENGTH_SHORT).show()
                                }
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = azulMarino,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Aplicar filtros")
                    }


                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val file = exportToExcel(context, filteredData)
                            file?.let { shareExcelFile(context, it) }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = tipo != "invitado", // ✅ deshabilitado para invitados
                        colors = ButtonDefaults.buttonColors(
                            containerColor = azulMarino,
                            contentColor = Color.White,
                            disabledContainerColor = Color.LightGray, // opcional: color deshabilitado
                            disabledContentColor = Color.DarkGray      // opcional: texto deshabilitado
                        )
                    ) {
                        Text("Exportar Excel")
                    }

                }

                val azulMarino = Color(0xFF001F5B)

                Button(
                    onClick = { limpiarFiltros() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = azulMarino,
                        contentColor = Color.White
                    )
                ) {
                    Text("Limpiar filtros")
                }
            }
        }

        AnimatedVisibility(
            visible = isLoading.value,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            AlertDialog(
                onDismissRequest = {}, // No se puede cerrar manualmente
                confirmButton = {},
                title = null,
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth()
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF001F5B),
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Cargando...",
                            fontSize = 16.sp,
                            color = Color.Black,
                            fontStyle = FontStyle.Italic
                        )
                    }
                },
                containerColor = Color.Black.copy(alpha = 0.3f), // ✅ Ligero blur
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 4.dp,

            )
        }


        Spacer(modifier = Modifier.height(16.dp))

        // 🧾 Total de resultados
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Total de Registros: ${filteredData.size}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 🧩 Lista de resultados (siempre visible)
        if (filteredData.isEmpty()) {
            Text("No hay resultados", modifier = Modifier.padding(top = 8.dp))
        } else {

            LazyColumn {
                items(filteredData) { item ->
                    InventoryReportItem(
                        item = item,
                        puedeModificarRegistro = puedeModificarRegistro,
                        onDelete = { documentId ->
                            Firebase.firestore.collection("inventario").document(documentId)
                                .delete()
                                .addOnSuccessListener {
                                    Toast.makeText(
                                        context,
                                        "Registro eliminado",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    filteredData.removeIf { it.documentId == documentId }
                                }
                                .addOnFailureListener {
                                    Toast.makeText(context, "Error al eliminar", Toast.LENGTH_SHORT)
                                        .show()
                                }
                        },
                        onEdit = { updatedItem ->
                            updateFirestore(
                                context,
                                Firebase.firestore,
                                updatedItem.documentId,
                                updatedItem.location,
                                updatedItem.sku,
                                updatedItem.lote,
                                updatedItem.expirationDate,
                                updatedItem.quantity,
                                filteredData
                            )
                        }
                    )
                }
            }


        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalidadDropdown(
    localidadSeleccionada: MutableState<String>,
    listaLocalidades: List<String>
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            readOnly = true,
            value = localidadSeleccionada.value,
            onValueChange = { },
            label = { Text("Localidad", color = Color.Black) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded)
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary, // ✅ Igual al resto
                unfocusedBorderColor = Color.Gray,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = Color.Gray,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            listaLocalidades.forEach { localidad ->
                DropdownMenuItem(
                    text = { Text(localidad) },
                    onClick = {
                        localidadSeleccionada.value = localidad
                        expanded = false
                    }
                )
            }
        }
    }
}






