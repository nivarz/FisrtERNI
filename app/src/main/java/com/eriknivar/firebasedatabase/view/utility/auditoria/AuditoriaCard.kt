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
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
    fecha: Timestamp?,
    valoresAntes: Map<String, Any?> = emptyMap(),
    valoresDespues: Map<String, Any?> = emptyMap(),
    tipoUsuario: String,
    usuarioEmail: String? = null,
    onDelete: (String) -> Unit

) {
    var isExpanded by remember { mutableStateOf(false) }

    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    val fechaFormateada = fecha?.toDate()?.let { sdf.format(it) } ?: "-"
    var showDeleteDialog by remember { mutableStateOf(false) }


    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { isExpanded = !isExpanded },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White) // ⬅️ fondo blanco
    ) {

        Column(modifier = Modifier.padding(16.dp)) {

            val (colorAccion, iconAccion) = when (tipoAccion.lowercase()) {
                "eliminación" -> Color(0xFFF44336) to Icons.Default.Delete      // rojo
                "modificación" -> Color(0xFFFFC107) to Icons.Default.Edit       // ámbar
                "creación", "crear" -> Color(0xFF4CAF50) to Icons.Default.Info  // verde
                else -> Color(0xFF0D47A1) to Icons.Default.Info                 // azul
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = iconAccion, contentDescription = null, tint = colorAccion)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Acción: ${tipoAccion.uppercase()}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                    )
                    Spacer(Modifier.width(8.dp))

                    // ⬇️ Badge de color según la acción
                    Badge(
                        containerColor = colorAccion,
                        contentColor = Color.White
                    ) {
                        Text(
                            text = tipoAccion.uppercase(),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                // (Si aquí tenías el botón de eliminar para superuser, déjalo igual)
                if (tipoUsuario.equals("superuser", ignoreCase = true)) {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = "Eliminar registro de auditoría",
                            tint = Color(0xFFF44336)
                        )
                    }
                }
            }
            Text(
                text = if (usuarioEmail.isNullOrBlank())
                    "Usuario: $usuario"
                else
                    "Usuario: $usuario ($usuarioEmail)",
                style = MaterialTheme.typography.bodySmall
            )

           // Text("Usuario: $usuario", fontSize = 14.sp, color = Color.Black)
            Text("Fecha: $fechaFormateada", fontSize = 12.sp, color = Color.DarkGray)
            Text("ID del Registro: $registroId", fontSize = 12.sp, color = Color.LightGray)

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    valoresAntes.keys.forEach { campo ->
                        val antes = valoresAntes[campo]?.toString() ?: "-"
                        val despues = valoresDespues.get(campo)?.toString() ?: "-"
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
