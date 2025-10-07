package com.eriknivar.firebasedatabase.view.inventoryentry

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.util.Calendar
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun DatePickerTextField(
    dateText: MutableState<String>,
    focusRequester: FocusRequester,
    nextFocusRequester: FocusRequester,
    enable: Boolean = true
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    val interaction = remember { MutableInteractionSource() }
    val isFocused by interaction.collectIsFocusedAsState()

    // 👇 NUEVO: saber si la fecha vino del date picker
    var justPicked by remember { mutableStateOf(false) }

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            dateText.value = "%02d/%02d/%d".format(dayOfMonth, month + 1, year)
            justPicked = true                     // ⬅️ marcar selección
            nextFocusRequester.requestFocus()     // tu navegación actual
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    LaunchedEffect(enable) { if (!enable) dateText.value = "-" }

    val iconAlpha = if (enable) 1f else 0.3f

    // Si el usuario borra o el texto queda vacío, quita el flag
    LaunchedEffect(dateText.value) {
        if (dateText.value.isBlank() || dateText.value == "-") justPicked = false
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = dateText.value,
            interactionSource = interaction,
            onValueChange = {
                if (enable) {
                    dateText.value = it
                    // Si el cambio fue manual, el foco decide
                    justPicked = false
                }
            },
            label = { Text("Caducidad", color = Color.Gray) },
            modifier = Modifier
                .width(275.dp)
                .height(64.dp)
                .padding(4.dp)
                .focusRequester(focusRequester),
            readOnly = true,
            enabled = enable,
            trailingIcon = {
                IconButton(
                    onClick = { if (enable) datePickerDialog.show() },
                    enabled = enable
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Seleccionar fecha",
                        modifier = Modifier.alpha(iconAlpha)
                    )
                }
            }
        )

        // 👇 NUEVO: foco O fecha recién escogida
        val shouldShowTrash =
            enable && dateText.value.isNotBlank() && dateText.value != "-" && (isFocused || justPicked)

        if (shouldShowTrash) {
            IconButton(
                onClick = {
                    dateText.value = ""
                    justPicked = false
                },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = "Borrar texto",
                    tint = Color.Red,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}


