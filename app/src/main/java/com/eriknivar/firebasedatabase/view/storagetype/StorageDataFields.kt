package com.eriknivar.firebasedatabase.view.storagetype
import com.google.firebase.Timestamp

@com.google.firebase.firestore.IgnoreExtraProperties
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
    val usuario: String,
    val localidad: String,
    val tipoUsuarioCreador: String,
    @com.google.firebase.firestore.PropertyName("fotoUrl")
    val fotoUrl: String = ""
) {

}