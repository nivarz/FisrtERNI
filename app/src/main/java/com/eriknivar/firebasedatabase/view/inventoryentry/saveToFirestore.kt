package com.eriknivar.firebasedatabase.view.inventoryentry

import com.eriknivar.firebasedatabase.view.storagetype.DataFields
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore

fun saveToFirestore(
    db: FirebaseFirestore,
    location: String,
    sku: String,
    description: String,
    lote: String,
    expirationDate: String,
    quantity: Double,
    unidadMedida: String, // 🆕 Guardamos la UM en Firestore
    allData: MutableList<DataFields>
) {
    val data = hashMapOf(
        "ubicacion" to location,
        "codigoProducto" to sku,
        "descripcion" to description,
        "lote" to lote,
        "fechaVencimiento" to expirationDate,
        "cantidad" to quantity,
        "unidadMedida" to unidadMedida, // 🆕 Guardamos la UM en Firestore
        "fechaRegistro" to Timestamp.now()
    )

    db.collection("inventario")
        .add(data)
        .addOnSuccessListener { documentReference ->
            allData.add(
                DataFields(
                    documentReference.id,
                    location,
                    sku,
                    lote,
                    expirationDate,
                    quantity,
                    description,
                    unidadMedida, // 🆕 Guardamos la UM en Firestore
                    Timestamp.now()
                )
            )
        }
        .addOnFailureListener { e ->
            println("Error al guardar: $e")
        }
}