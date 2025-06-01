package com.eriknivar.firebasedatabase.view.utility

import com.google.firebase.firestore.FirebaseFirestore

fun validarRegistroDuplicado(
    db: FirebaseFirestore,
    ubicacion: String,
    sku: String,
    lote: String,
    cantidad: Double,
    localidad: String,
    onResult: (Boolean) -> Unit,
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
            val existeDuplicado = result.isEmpty.not()
            onResult(existeDuplicado)
        }
        .addOnFailureListener { e ->
            onError(e)
        }
}


