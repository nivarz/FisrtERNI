package com.eriknivar.firebasedatabase.view.utility.auditoria

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Timestamp
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Locale

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
    onDelete: (String) -> Unit,
    usuarioUid: String? = null
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    val fechaFormateada = fecha?.toDate()?.let { sdf.format(it) } ?: "-"

    val style = resolveActionStyle(tipoAccion)

    // Línea superior "Usuario: Nombre (email|uid)"
    val userLine = buildString {
        append("Usuario: ")
        append(usuario.ifBlank { "-" })
        val mail = usuarioEmail?.takeIf { it.isNotBlank() }
        val uid  = usuarioUid?.takeIf { it.isNotBlank() }
        if (mail != null) append(" ($mail)")
        else if (uid != null) append(" ($uid)")
    }
    Text(text = userLine, style = MaterialTheme.typography.bodyMedium)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { isExpanded = !isExpanded },
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(
                        imageVector = style.icon,
                        contentDescription = null,
                        tint = style.iconTint,
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Acción: ${style.label}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Pill(text = style.label, bg = style.pillBg, fg = style.pillFg)
                }

                if (tipoUsuario.equals("superuser", true)) {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Filled.DeleteForever, contentDescription = "Eliminar registro", tint = Color(0xFFF44336))
                    }
                }
            }

            Text("Fecha: $fechaFormateada", fontSize = 12.sp, color = Color.DarkGray)
            Text("ID del Registro: $registroId", fontSize = 12.sp, color = Color.LightGray)

            AnimatedVisibility(visible = isExpanded) {
                Column(Modifier.padding(top = 8.dp)) {
                    valoresAntes.keys.forEach { campo ->
                        val label = campo.replaceFirstChar { it.uppercase() }
                        DiffLine(
                            label = label,
                            before = valoresAntes[campo]?.toString() ?: "-",
                            after  = valoresDespues[campo]?.toString() ?: "-",
                            // Forzamos apilado para descripciones (mejor lectura)
                            stacked = label.equals("Descripcion", true)
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
            text = { Text("Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = { onDelete(registroId); showDeleteDialog = false }) {
                    Text("Sí", color = Color.Red)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") } }
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
        tipo.contains("ELIMIN") -> ActionStyle("ELIMINACIÓN", Icons.Filled.Delete, Color(0xFFE53935), Color(0xFFE53935), Color.White)
        tipo.contains("MODIFIC") -> ActionStyle("MODIFICACIÓN", Icons.Filled.Edit, Color(0xFFFFC107), Color(0xFFFFC107), Color(0xFF3E2723))
        else -> ActionStyle("EDITAR", Icons.Filled.Info, Color(0xFF0D47A1), Color(0xFF0D47A1), Color.White)
    }
}

@Composable
private fun Pill(text: String, bg: Color, fg: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(bg, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) { Text(text, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun DiffLine(label: String, before: String?, after: String?, stacked: Boolean = false) {
    val beforeText = before.orEmpty()
    val afterText = after.orEmpty()
    val changed = beforeText != afterText
    val isDescripcion = label.equals("Descripcion", true)

    val forceStack = stacked || isDescripcion || maxOf(beforeText.length, afterText.length) > 28
    val colorBefore = if (changed) Color(0xFFD32F2F) else MaterialTheme.colorScheme.onSurface
    val colorAfter  = if (changed) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
    val arrowTint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (changed) .7f else .4f)

    Column(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        if (forceStack) {
            Text(beforeText.ifBlank { "-" }, color = colorBefore)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isDescripcion) Icons.Filled.ArrowDownward else Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null, tint = arrowTint, modifier = Modifier.size(18.dp)
                )
            }
            Text(afterText.ifBlank { "-" }, color = colorAfter)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(beforeText.ifBlank { "-" }, color = colorBefore)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = arrowTint, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(afterText.ifBlank { "-" }, color = colorAfter)
            }
        }
        Spacer(Modifier.height(10.dp))
    }
}

@Suppress("unused")
private fun normalize(s: String?): String =
    Normalizer.normalize(s.orEmpty(), Normalizer.Form.NFD)
        .replace("\\p{Mn}+".toRegex(), "")
        .lowercase()
        .trim()
