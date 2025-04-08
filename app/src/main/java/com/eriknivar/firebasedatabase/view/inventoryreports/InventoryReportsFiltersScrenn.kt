package com.eriknivar.firebasedatabase.view.inventoryreports

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    tipoUsuario: String
) {
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
        if (tipoUsuario != "admin") {
            usuarioFiltro.value = userViewModel.nombre.value ?: ""
        }
    }

    val startDatePickerDialog = remember {
        DatePickerDialog(context, { _, y, m, d ->
            calendar.set(y, m, d)
            startDate.value = dateFormatter.format(calendar.time)
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
    }

    val endDatePickerDialog = remember {
        DatePickerDialog(context, { _, y, m, d ->
            calendar.set(y, m, d)
            endDate.value = dateFormatter.format(calendar.time)
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {

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

                fun limpiarFiltros() {
                    usuarioFiltro.value = if (tipoUsuario == "admin") "" else userViewModel.nombre.value ?: ""
                    sku.value = ""
                    location.value = ""
                    startDate.value = ""
                    endDate.value = ""
                    filteredData.clear()
                    filteredData.addAll(allData.sortedByDescending { it.fechaRegistro?.toDate() })
                }

                OutlinedTextField(
                    value = usuarioFiltro.value,
                    onValueChange = { usuarioFiltro.value = it },
                    label = { Text("Usuario") },
                    singleLine = true,
                    enabled = tipoUsuario == "admin",
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = sku.value,
                    onValueChange = { sku.value = it },
                    label = { Text("SKU o palabra clave") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = location.value,
                    onValueChange = { location.value = it },
                    label = { Text("Ubicación") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                var expanded by remember { mutableStateOf(false) }

                Box(modifier = Modifier.fillMaxWidth()) {
                    TextField(
                        value = localidadSeleccionada.value,
                        onValueChange = { localidadSeleccionada.value = it },
                        label = { Text("Localidad") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { expanded = !expanded }) {
                                Icon(
                                    imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    contentDescription = null
                                )
                            }
                        }
                    )

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth()
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
                            filteredData.clear()
                            filteredData.addAll(
                                allData.filter { item ->
                                    val matchesSku = sku.value.isBlank() || item.sku.contains(sku.value, true) || item.description.contains(sku.value, true)
                                    val matchesLocation = location.value.isBlank() || item.location.contains(location.value, true)
                                    val matchesUser = usuarioFiltro.value.isBlank() || item.usuario.contains(usuarioFiltro.value, true)
                                    val dateFormatted = item.fechaRegistro?.toDate()?.let { sdf.format(it) } ?: ""

                                    val matchesDate = try {
                                        (startDate.value.isBlank() || dateFormatted >= startDate.value) &&
                                                (endDate.value.isBlank() || dateFormatted <= endDate.value)
                                    } catch (e: Exception) { true }

                                    val matchesLocalidad = localidadSeleccionada.value.isBlank() ||
                                            item.localidad.equals(localidadSeleccionada.value, ignoreCase = true)


                                    matchesSku && matchesLocation && matchesUser && matchesDate && matchesLocalidad
                                }.sortedByDescending { it.fechaRegistro?.toDate() }
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = azulMarino, contentColor = Color.White)

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
                        colors = ButtonDefaults.buttonColors(containerColor = azulMarino, contentColor = Color.White)

                    ) {
                        Text("Exportar Excel")
                    }
                }

                val azulMarino = Color(0xFF001F5B)

                Button(
                    onClick = { limpiarFiltros() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = azulMarino, contentColor = Color.White)
                ) {
                    Text("Limpiar filtros")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 🧾 Total de resultados
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Total de Registros: ${filteredData.size}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 🧩 Lista de resultados (siempre visible)
        if (filteredData.isEmpty()) {
            Text("No hay resultados", modifier = Modifier.padding(top = 8.dp))
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredData) { item ->
                    InventoryReportItem(item = item)
                }
            }
        }
    }
}





