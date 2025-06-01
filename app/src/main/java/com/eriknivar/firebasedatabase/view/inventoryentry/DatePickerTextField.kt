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
import androidx.compose.material.icons.filled.Clear
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.util.Calendar

@Composable
fun DatePickerTextField(
    dateText: MutableState<String>,
    focusRequester: FocusRequester,
    nextFocusRequester: FocusRequester
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            dateText.value = "%02d/%02d/%d".format(dayOfMonth, month + 1, year)

            nextFocusRequester.requestFocus()

        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    LaunchedEffect(focusRequester) {
        // Solo muestra el date picker cuando reciba el enfoque
        focusRequester.freeFocus() // En caso de que tenga algo previo
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = dateText.value,
            onValueChange = { dateText.value = it },
            label = { Text("Caducidad") },
            modifier = Modifier
                .width(275.dp)
                .height(64.dp)
                .padding(4.dp)
                .focusRequester(focusRequester),
            trailingIcon = {
                IconButton(onClick = { datePickerDialog.show() }) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Seleccionar fecha"
                    )
                }
            },
            readOnly = true
        )

        // 🔵 Ícono de borrar separado (afuera del campo)
        if (dateText.value.isNotEmpty()) {
            IconButton(
                onClick = {
                    dateText.value = ""
                },
                modifier = Modifier
                    .size(48.dp)
                    .padding(start = 4.dp)

            ) {
                Icon(
                    modifier = Modifier.size(48.dp),
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = "Borrar texto",
                    tint = Color.Red
                )
            }
        }
    }
}


