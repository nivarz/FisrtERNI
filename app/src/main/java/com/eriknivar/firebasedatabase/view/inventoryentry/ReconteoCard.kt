package com.eriknivar.firebasedatabase.view.inventoryentry

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ReconteoCard(data: Map<String, Any>) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("SKU: ${data["sku"] ?: "-"}", style = MaterialTheme.typography.bodyLarge)
            Text("Descripción: ${data["descripcion"] ?: "-"}")
            Text("Esperado: ${data["cantidadEsperada"] ?: "0"}")
            Text("Físico: ${data["cantidadFisica"] ?: "0"}")
            Text("Ubicación: ${data["ubicacion"] ?: "-"}")
            Text("Localidad: ${data["localidad"] ?: "-"}")
            Text("Asignado por: ${data["nombreAsignado"] ?: "-"}")
        }
    }
}