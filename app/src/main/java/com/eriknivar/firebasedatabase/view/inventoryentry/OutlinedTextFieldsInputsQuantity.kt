package com.eriknivar.firebasedatabase.view.inventoryentry

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun OutlinedTextFieldsInputsQuantity(
    quantity: MutableState<String>,
    showErrorQuantity: MutableState<Boolean>,
    errorMessageQuantity: MutableState<String>,

){

    val qrCodeContentQuantity = remember { mutableStateOf("") }

    var showErrorDialog by remember { mutableStateOf(false) } // 🔥 Para el mensaje de error

    LaunchedEffect(qrCodeContentQuantity.value) {
        quantity.value = qrCodeContentQuantity.value.uppercase()
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically // 📌 Asegura alineación vertical
    ) {
        OutlinedTextField(
            modifier = Modifier
                .size(222.dp, 70.dp)
                .padding(2.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),// ACTIVA EL TECLADO NUMERICO
            label = { Text(text = "Cantidad") },
            value = quantity.value,
            onValueChange = { newValue ->
                if (newValue == ".") { // Si el usuario solo ingresa un punto
                    showErrorQuantity.value = true
                    errorMessageQuantity.value = "Ingrese un número válido"
                } else if (newValue.matches(Regex("^\\d*\\.?\\d*\$"))) { // Permite números y un solo punto
                    showErrorQuantity.value = false
                    quantity.value = newValue
                } else {
                    showErrorQuantity.value = true
                    errorMessageQuantity.value = "Formato incorrecto"
                }
            },
            isError = showErrorQuantity.value && quantity.value.isEmpty()
        )

        if (showErrorQuantity.value) {
            Text(
                text = errorMessageQuantity.value,
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
            )
        }

    }

    // 🔽 🔥 Diálogo de Campos Obligatorios

    if (showErrorDialog) {
        AlertDialog(onDismissRequest = { showErrorDialog = false },
            confirmButton = {
                TextButton(onClick = { showErrorDialog = false }) {
                    Text("Aceptar")
                }
            },
            title = { Text("Error") },
            text = { Text("Campos Obligatorios Vacíos. Completa los datos para continuar.") })
    }

}