package com.eriknivar.firebasedatabase.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eriknivar.firebasedatabase.network.CatalogoViewModel
import com.eriknivar.firebasedatabase.network.dto.LocalidadDto
import com.eriknivar.firebasedatabase.network.dto.UbicacionDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalidadUbicacionSelector(
    vm: CatalogoViewModel = viewModel(),
    labelLocalidad: String = "Localidad",
    labelUbicacion: String = "Ubicación",
    onLocalidadSelected: (LocalidadDto?) -> Unit = {},
    onUbicacionSelected: (UbicacionDto?) -> Unit = {}
) {
    val localidades by vm.localidades.collectAsState()
    val ubicaciones by vm.ubicaciones.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()

    var locExpanded by remember { mutableStateOf(false) }
    var ubiExpanded by remember { mutableStateOf(false) }

    var localidadSel by remember { mutableStateOf<LocalidadDto?>(null) }
    var ubicacionSel by remember { mutableStateOf<UbicacionDto?>(null) }

    // Cargar localidades al entrar si ya hay cliente listo (admin/invitado o super con cliente elegido)
    LaunchedEffect(Unit) {
        if (vm.isClientReady()) vm.cargarLocalidades()
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // ----- Localidad -----
        ExposedDropdownMenuBox(
            expanded = locExpanded,
            onExpandedChange = { if (vm.isClientReady() && !loading) locExpanded = !locExpanded }
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                readOnly = true,
                value = localidadSel?.let { "${it.codigo}${it.nombre?.let { n -> " — $n" } ?: ""}" } ?: "",
                onValueChange = {},
                label = { Text(labelLocalidad) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = locExpanded) },
                enabled = vm.isClientReady() && !loading
            )
            ExposedDropdownMenu(
                expanded = locExpanded,
                onDismissRequest = { locExpanded = false }
            ) {
                localidades.forEach { item ->
                    DropdownMenuItem(
                        text = { Text("${item.codigo}${item.nombre?.let { n -> " — $n" } ?: ""}") },
                        onClick = {
                            locExpanded = false
                            localidadSel = item
                            onLocalidadSelected(item)

                            // Reset y cargar ubicaciones de la nueva localidad
                            ubicacionSel = null
                            vm.cargarUbicaciones(item.codigo)
                        }
                    )
                }
            }
        }

        // ----- Ubicación -----
        ExposedDropdownMenuBox(
            expanded = ubiExpanded,
            onExpandedChange = {
                val enabled = vm.isClientReady() && !loading && localidadSel != null
                if (enabled) ubiExpanded = !ubiExpanded
            }
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                readOnly = true,
                value = ubicacionSel?.codigoUbi ?: "",
                onValueChange = {},
                label = { Text(labelUbicacion) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ubiExpanded) },
                enabled = vm.isClientReady() && !loading && localidadSel != null
            )
            ExposedDropdownMenu(
                expanded = ubiExpanded,
                onDismissRequest = { ubiExpanded = false }
            ) {
                ubicaciones.forEach { item ->
                    DropdownMenuItem(
                        text = {
                            Text(buildString {
                                append(item.codigoUbi)
                                if (!item.descripcion.isNullOrBlank()) append(" — ${item.descripcion}")
                            })
                        },
                        onClick = {
                            ubiExpanded = false
                            ubicacionSel = item
                            onUbicacionSelected(item)
                        }
                    )
                }
            }
        }

        // ----- Error (si aplica) -----
        if (!error.isNullOrBlank()) {
            Text(text = error ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}
