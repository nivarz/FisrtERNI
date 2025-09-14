package com.eriknivar.firebasedatabase.view.utility.auditoria

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Locale
import com.google.firebase.Timestamp

@Composable
fun AuditoriaCard(
    tipoAccion: String,
    registroId: String,
    usuario: String,
    fecha: Timestamp,
    valoresAntes: Map<String, Any?>?,
    valoresDespues: Map<String, Any?>?,
    tipoUsuario: String,
    onDelete: (String) -> Unit

) {
    var isExpanded by remember { mutableStateOf(false) }

    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    val fechaFormateada = sdf.format(fecha.toDate())
    var showDeleteDialog by remember { mutableStateOf(false) }


    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { isExpanded = !isExpanded },
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            val colorAccion = when (tipoAccion.lowercase()) {
                "eliminación" -> Color.Red
                "modificación" -> Color(0xFF0D47A1)
                else -> Color.Black
            }

            val iconAccion = when (tipoAccion.lowercase()) {
                "eliminación" -> Icons.Default.Delete
                "modificación" -> Icons.Default.Edit
                else -> Icons.Default.Info
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = iconAccion,
                    contentDescription = null,
                    tint = colorAccion,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Acción: $tipoAccion",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = colorAccion
                )

                Spacer(Modifier.weight(1f))

                if (tipoUsuario.lowercase() == "superuser") {
                    IconButton(onClick = {
                        showDeleteDialog = true // ✅ Solo abre el diálogo
                    }) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = "Eliminar registro",
                            tint = Color.Red
                        )
                    }
                }


                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Expandir",
                    tint = Color.Gray
                )
            }

            Text("Usuario: $usuario", fontSize = 14.sp, color = Color.Black)
            Text("Fecha: $fechaFormateada", fontSize = 12.sp, color = Color.DarkGray)
            Text("ID del Registro: $registroId", fontSize = 12.sp, color = Color.LightGray)

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    valoresAntes?.keys?.forEach { campo ->
                        val antes = valoresAntes[campo]?.toString() ?: "-"
                        val despues = valoresDespues?.get(campo)?.toString() ?: "-"
                        val huboCambio = antes != despues

                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            Text(
                                text = campo.replaceFirstChar { it.uppercase() },
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF003366)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Text(
                                    text = antes,
                                    color = if (huboCambio) Color.Red else Color.Black,
                                    fontSize = 13.sp
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = if (huboCambio) Color.Gray else Color.LightGray,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                                Text(
                                    text = despues,
                                    color = if (huboCambio) Color(0xFF2E7D32) else Color.Black,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("¿Eliminar registro?") },
            text = { Text("¿Estás seguro de que deseas eliminar este registro de auditoría? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(registroId)
                    showDeleteDialog = false
                }) {
                    Text("Sí", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
