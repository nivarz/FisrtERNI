package com.eriknivar.firebasedatabase.view.inventoryreports

import android.app.DatePickerDialog
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eriknivar.firebasedatabase.view.storagetype.DataFields
import com.eriknivar.firebasedatabase.viewmodel.UserViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


@Composable
fun InventoryReportFiltersScreen(
    userViewModel: UserViewModel,
    allData: List<DataFields>,
) {
    val sku = remember { mutableStateOf("") }
    val location = remember { mutableStateOf("") }
    val startDate = remember { mutableStateOf("") }
    val endDate = remember { mutableStateOf("") }
    val usuario by userViewModel.nombre.observeAsState("")

    val filteredData = remember { mutableStateListOf<DataFields>() }

    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance() }
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    val startDatePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, day ->
                calendar.set(year, month, day)
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
            { _, year, month, day ->
                calendar.set(year, month, day)
                endDate.value = dateFormatter.format(calendar.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    // Encabezado con resumen de resultados
    val totalRegistros = filteredData.size
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Total de productos: $totalRegistros", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }

    // LazyColumn para los filtros y resultados
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Filtros de búsqueda
        item {
            Text(
                text = "Filtros de búsqueda",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            OutlinedTextField(
                value = usuario,
                onValueChange = {},
                label = { Text("Usuario") },
                enabled = false,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = sku.value,
                onValueChange = { sku.value = it },
                label = { Text("SKU o palabra clave") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = location.value,
                onValueChange = { location.value = it },
                label = { Text("Ubicación") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = startDate.value,
                    onValueChange = {},
                    label = { Text("Desde") },
                    readOnly = true,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { startDatePickerDialog.show() }
                )

                OutlinedTextField(
                    value = endDate.value,
                    onValueChange = {},
                    label = { Text("Hasta") },
                    readOnly = true,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { endDatePickerDialog.show() }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = {
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        filteredData.clear()

                        filteredData.addAll(
                            allData.filter { item ->
                                val matchesSku = sku.value.isBlank() ||
                                        item.sku.contains(sku.value, true) ||
                                        item.description.contains(sku.value, true)

                                val matchesLocation = location.value.isBlank() ||
                                        item.location.contains(location.value, true)

                                val matchesUser = usuario.isBlank() || item.usuario == usuario

                                val dateFormatted = item.fechaRegistro?.toDate()?.let { sdf.format(it) } ?: ""
                                val matchesDate = try {
                                    (startDate.value.isBlank() || dateFormatted >= startDate.value) &&
                                            (endDate.value.isBlank() || dateFormatted <= endDate.value)
                                } catch (e: Exception) {
                                    Log.e("Filtro", "Error comparando fechas", e)
                                    true
                                }

                                matchesSku && matchesLocation && matchesUser && matchesDate
                            }
                        )
                    }
                ) {
                    Text("Aplicar filtros")
                }

                Button(onClick = {
                    val file = exportToExcel(context, filteredData)
                    file?.let { shareExcelFile(context, it) }
                }) {
                    Text("Exportar Excel")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredData.isEmpty()) {
                Text("No hay resultados")
            }
        }

        // Mostrar los resultados filtrados
        items(filteredData) { item ->
            InventoryReportItem(item = item)
        }
    }
}


