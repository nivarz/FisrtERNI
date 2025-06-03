package com.eriknivar.firebasedatabase.view.utility

import com.google.firebase.firestore.FirebaseFirestore

fun validarRegistroDuplicado(
    db: FirebaseFirestore,
    ubicacion: String,
    sku: String,
    lote: String,
    cantidad: Double,
    localidad: String,
    onResult: (Boolean, String?) -> Unit, // 🔴 Agregamos el usuario como segundo parámetro
    onError: (Exception) -> Unit
) {
    db.collection("inventario")
        .whereEqualTo("ubicacion", ubicacion)
        .whereEqualTo("codigoProducto", sku)
        .whereEqualTo("lote", lote)
        .whereEqualTo("cantidad", cantidad)
        .whereEqualTo("localidad", localidad)
        .get()
        .addOnSuccessListener { result ->
            if (!result.isEmpty) {
                val usuarioDuplicado = result.documents.firstOrNull()?.getString("usuario") ?: "Desconocido"
                onResult(true, usuarioDuplicado) // 🔴 Pasamos el nombre del usuario
            } else {
                onResult(false, null)
            }
        }
        .addOnFailureListener { e ->
            onError(e)
        }
}



