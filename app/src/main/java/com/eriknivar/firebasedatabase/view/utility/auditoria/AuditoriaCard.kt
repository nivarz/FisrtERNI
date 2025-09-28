package com.eriknivar.firebasedatabase.view.utility.auditoria

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
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
import androidx.compose.ui.graphics.vector.ImageVector
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

            // Usa el campo que SÍ tienes en el scope
            val style = resolveActionStyle(tipoAccion)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = style.icon,
                        contentDescription = null,
                        tint = style.iconTint,
                        modifier = Modifier
                            .size(30.dp)
                            .padding(end = 8.dp)
                    )
                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = "Acción: ${style.label}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(Modifier.width(8.dp))

                    Pill(
                        text = style.label,
                        bg = style.pillBg,
                        fg = style.pillFg,
                        modifier = Modifier.padding(start = 8.dp)
                    )
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                val effectiveName = usuario
                    .takeIf { it.isNotBlank() }
                    ?: usuarioEmail?.substringBefore("@")
                        ?.replaceFirstChar { it.uppercase() }
                    ?: "Desconocido"

                val effectiveEmail = usuarioEmail
                    ?.takeIf { it.isNotBlank() && !usuario.contains("@") }

                Text(
                    text = "Usuario: ",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = effectiveName,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                if (effectiveEmail != null) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "($effectiveEmail)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }


            // Text("Usuario: $usuario", fontSize = 14.sp, color = Color.Black)
            Text("Fecha: $fechaFormateada", fontSize = 12.sp, color = Color.DarkGray)
            Text("ID del Registro: $registroId", fontSize = 12.sp, color = Color.LightGray)

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {

                    valoresAntes.keys.forEach { campo ->
                        val label = campo.replaceFirstChar { it.uppercase() }
                        val antes = valoresAntes[campo]?.toString() ?: "-"
                        val despues = valoresDespues[campo]?.toString() ?: "-"

                        // Para "Descripcion" lo forzamos apilado
                        val stackForThisField = label.equals("Descripcion", ignoreCase = true)

                        DiffLine(
                            label   = label,
                            before  = antes,
                            after   = despues,
                            stacked = stackForThisField
                        )
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
                    Text("Cancelar", color = Color.Black)
                }
            }
        )
    }
}


private data class ActionStyle(
    val label: String,
    val icon: ImageVector,
    val iconTint: Color,
    val pillBg: Color,
    val pillFg: Color
)

@Composable
private fun resolveActionStyle(tipoAccionRaw: String?): ActionStyle {
    val tipo = (tipoAccionRaw ?: "").uppercase()

    return when {
        // 🔴 ELIMINAR / ELIMINACIÓN
        tipo.contains("ELIMIN") -> ActionStyle(
            label = "ELIMINACIÓN",
            icon = Icons.Filled.Delete,
            iconTint = Color(0xFFE53935),            // rojo 600
            pillBg = Color(0xFFE53935),
            pillFg = Color.White
        )
        // 🟡 MODIFICAR / MODIFICACIÓN
        tipo.contains("MODIFIC") -> ActionStyle(
            label = "MODIFICACIÓN",
            icon = Icons.Filled.Edit,
            iconTint = Color(0xFFFFC107),            // amber 500
            pillBg = Color(0xFFFFC107),
            pillFg = Color(0xFF3E2723)               // marrón oscuro para contraste
        )
        // 🔵 EDITAR (default)
        else -> ActionStyle(
            label = "EDITAR",
            icon = Icons.Filled.Info,
            iconTint = Color(0xFF0D47A1),            // azul marino
            pillBg = Color(0xFF0D47A1),
            pillFg = Color.White
        )
    }
}

@Composable
private fun Pill(text: String, bg: Color, fg: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(bg, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text = text, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DiffLine(
    label: String,
    before: String?,
    after: String?,
    stacked: Boolean = false,
) {
    val beforeText = before.orEmpty()
    val afterText  = after.orEmpty()
    val changed = beforeText != afterText
    val isDescripcion = label.equals("Descripcion", ignoreCase = true)

    // ¿Mostramos apilado?
    val forceStack = stacked || isDescripcion ||
            maxOf(beforeText.length, afterText.length) > 28

    // Colores de énfasis cuando hay cambio
    val colorBefore = if (changed) Color(0xFFD32F2F) else MaterialTheme.colorScheme.onSurface
    val colorAfter  = if (changed) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
    val arrowTint   = if (changed)
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    else
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        if (forceStack) {
            // ⬇️ Apilado (flecha hacia abajo si es Descripción)
            Text(
                text = beforeText.ifBlank { "-" },
                style = MaterialTheme.typography.bodyMedium,
                color = colorBefore
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isDescripcion) Icons.Default.ArrowDownward
                    else Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = arrowTint,
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = afterText.ifBlank { "-" },
                style = MaterialTheme.typography.bodyMedium,
                color = colorAfter
            )
        } else {
            // → En línea
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = beforeText.ifBlank { "-" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorBefore
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = arrowTint,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = afterText.ifBlank { "-" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorAfter
                )
            }
        }

        Spacer(Modifier.height(10.dp))
    }
}


