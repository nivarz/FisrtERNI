package com.eriknivar.firebasedatabase.view.storagetype

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.eriknivar.firebasedatabase.view.common.ConteoMode

@Composable
fun ConteoModeDialog(
    visible: Boolean,
    initial: ConteoMode = ConteoMode.CON_LOTE,
    onDismiss: () -> Unit,
    onConfirm: (ConteoMode) -> Unit
) {
    if (!visible) return

    var selected by remember(visible) { mutableStateOf(initial) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Tipo de conteo",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Elige cómo quieres capturar. Puedes cambiarlo más tarde.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(Modifier.fillMaxWidth()) {

                OptionCard(
                    title = "Conteo con Lote",
                    subtitle = "Habilita campos Lote y Fecha de vencimiento",
                    icon = Icons.Filled.CalendarMonth,
                    selected = (selected == ConteoMode.CON_LOTE),
                    onClick = { selected = ConteoMode.CON_LOTE }
                )

                Spacer(Modifier.height(10.dp))
                HorizontalDivider(Modifier.padding(horizontal = 4.dp))
                Spacer(Modifier.height(10.dp))

                OptionCard(
                    title = "Conteo sin Lote",
                    subtitle = "Campos de lote apagados y valor “-” automático",
                    icon = Icons.Filled.Inventory2,
                    selected = (selected == ConteoMode.SIN_LOTE),
                    onClick = { selected = ConteoMode.SIN_LOTE }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selected) }
            ) {
                Text("Continuar", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Black)
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun OptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    // Azul marino cuando está seleccionado
    val navy = Color(0xFF0D3B66)

    val containerColor by animateColorAsState(
        targetValue = if (selected) navy else MaterialTheme.colorScheme.surfaceVariant,
        label = "option-bg"
    )
    val contentOn = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
    val subtitleOn = if (selected) Color.White.copy(alpha = 0.85f)
    else MaterialTheme.colorScheme.onSurfaceVariant

    ElevatedCard(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = containerColor,
            contentColor   = contentOn
        )
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) navy.copy(alpha = .15f)
                        else MaterialTheme.colorScheme.surface
                    )
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentOn
                )
            }

            Spacer(Modifier.width(12.dp))

            // Textos
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = contentOn
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = subtitleOn
                )
            }

            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = contentOn,
                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

