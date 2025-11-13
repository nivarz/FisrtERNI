package com.eriknivar.firebasedatabase.view.storagetype

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
// …resto de imports/annotations
data class DataFields(
    // ===== ES (lo que guardas en Firestore)
    val clienteId: String = "",
    val localidad: String = "",
    val ubicacion: String = "",
    val codigoProducto: String = "",
    val descripcion: String = "",
    val unidadMedida: String = "",
    val lote: String = "-",
    val usuario: String = "",
    val tipoUsuarioCreador: String = "",
    val fotoUrl: String = "",
    val cantidad: Double = 0.0,
    val fechaVencimiento: String = "-",
    val creadoPorUid: String = "",
    var fotoUrlLocal: String? = null,
    var fotoUriLocal: String? = null,
    var fotoUrisLocales: List<String>? = null,
    val fotoEstado: String = "",
    @ServerTimestamp val fecha: Timestamp? = null,
    @ServerTimestamp val fechaRegistro: Timestamp? = null,
    @ServerTimestamp val creadoEn: Timestamp? = null,


    // ===== EN (aliases que usa la UI)
    val documentId: String = "",
    val quantity: Double = 0.0,
    val location: String = "",
    val expirationDate: String = "",
    val sku: String = "",            // 👈 alias de codigoProducto
    val description: String = ""     // 👈 alias de descripcion
){
    // Úsalo en UI: si expirationDate viene vacío, cae a fechaVencimiento
    val expirationForUi: String
        get() = expirationDate.ifBlank { fechaVencimiento.ifBlank { "-" } }
}



