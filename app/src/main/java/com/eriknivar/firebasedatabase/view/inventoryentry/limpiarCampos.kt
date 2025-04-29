package com.eriknivar.firebasedatabase.view.inventoryentry

import androidx.compose.runtime.MutableState

fun limpiarCampos(
    location: MutableState<String>,
    sku: MutableState<String>,
    quantity: MutableState<String>,
    lot: MutableState<String>,
    expirationDate: MutableState<String>
) {
    location.value = ""
    sku.value = ""
    quantity.value = ""
    lot.value = ""
    expirationDate.value = ""
}
