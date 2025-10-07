package com.eriknivar.firebasedatabase.view.inventoryentry

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OutlinedTextFieldsInputsQuantity(
    quantity: MutableState<String>,
    showErrorQuantity: MutableState<Boolean>,
    errorMessageQuantity: MutableState<String>,
    lote: MutableState<String>,
    expirationDate: MutableState<String>,
    focusRequester: FocusRequester,
    onUserInteraction: () -> Unit = {},
    keyboardController: SoftwareKeyboardController?
) {
    val qrCodeContentQuantity = remember { mutableStateOf("") }

    var showErrorDialog by remember { mutableStateOf(false) }

    LaunchedEffect(qrCodeContentQuantity.value) {
        quantity.value = qrCodeContentQuantity.value.uppercase()
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            modifier = Modifier
                .size(222.dp, 64.dp)
                .padding(2.dp)
                .focusRequester(focusRequester)
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        if (lote.value.isBlank()) lote.value = "-"
                        if (expirationDate.value.isBlank()) expirationDate.value = "-"
                    }
                },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = {
                keyboardController?.hide()
            }),
            label = { Text("Cantidad", color = Color.Gray) },
            value = quantity.value,
            onValueChange = { newValue ->
                if (newValue == ".") {
                    showErrorQuantity.value = true
                    errorMessageQuantity.value = "Ingrese un número válido"
                } else if (newValue.matches(Regex("^\\d*\\.?\\d*\$"))) {
                    showErrorQuantity.value = false
                    quantity.value = newValue
                } else {
                    showErrorQuantity.value = true
                    errorMessageQuantity.value = "Formato incorrecto"
                }
                onUserInteraction()
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

    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            confirmButton = {
                TextButton(onClick = { showErrorDialog = false }) {
                    Text("Aceptar")
                }
            },
            title = { Text("Error") },
            text = { Text("Campos Obligatorios Vacíos. Completa los datos para continuar.") }
        )
    }
}

