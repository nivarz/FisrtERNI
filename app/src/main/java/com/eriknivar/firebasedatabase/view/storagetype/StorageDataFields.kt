package com.eriknivar.firebasedatabase.view.storagetype

import com.google.firebase.Timestamp

data class DataFields (
    val documentId: String,
    val location: String,
    val sku: String,
    val lote: String,
    val expirationDate: String,
    val quantity: Double,
    val description: String,
    val unidadMedida: String, // 🆕 Unidad de medida
    val fechaRegistro: Timestamp?, // 🔥 Agrega el campo con valor por defecto `null`
    val usuario: String

)