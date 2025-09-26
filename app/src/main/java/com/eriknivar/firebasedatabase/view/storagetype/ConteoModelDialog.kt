package com.eriknivar.firebasedatabase.view.storagetype

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
        title = { Text("Tipo de conteo") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { selected = ConteoMode.CON_LOTE }
                        .padding(vertical = 6.dp)
                ) {
                    RadioButton(
                        selected = selected == ConteoMode.CON_LOTE,
                        onClick = { selected = ConteoMode.CON_LOTE }
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Conteo con Lote")
                        Text("Habilita Lote y Fecha Venc.", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { selected = ConteoMode.SIN_LOTE }
                        .padding(vertical = 6.dp)
                ) {
                    RadioButton(
                        selected = selected == ConteoMode.SIN_LOTE,
                        onClick = { selected = ConteoMode.SIN_LOTE }
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Conteo sin Lote")
                        Text("Campos de lote apagados y “-”.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) { Text("Continuar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
