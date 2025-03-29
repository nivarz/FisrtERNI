package com.eriknivar.firebasedatabase.view.inventoryentry

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.util.Calendar

@Composable
fun DatePickerTextField(dateText: MutableState<String>) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            dateText.value = "%02d/%02d/%d".format(dayOfMonth, month + 1, year)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically // 🔥 Asegura que  esté alineado
    ) {
        OutlinedTextField(
            value = dateText.value,
            onValueChange = { dateText.value = it },
            label = { Text("Caducidad") },
            modifier = Modifier.fillMaxWidth(.80f).padding(2.dp),
            trailingIcon = {
                IconButton(onClick = { datePickerDialog.show() }) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth, // ✅ Cambiado a DateRange
                        contentDescription = "Seleccionar fecha"
                    )
                }
            },
            readOnly = true // Para que solo se pueda seleccionar con el ícono
        )


    }

}


