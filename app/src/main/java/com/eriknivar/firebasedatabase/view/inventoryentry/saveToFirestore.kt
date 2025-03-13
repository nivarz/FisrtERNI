package com.eriknivar.firebasedatabase.view.inventoryentry

import com.eriknivar.firebasedatabase.view.storagetype.DataFields
import com.google.firebase.firestore.FirebaseFirestore

fun saveToFirestore(
    db: FirebaseFirestore,
    location: String,
    sku: String,
    description: String,
    lote: String,
    expirationDate: String,
    quantity: Double,
    allData: MutableList<DataFields>
) {
    val data = hashMapOf("ubicacion" to location, "codigoProducto" to sku, "descripcion" to description, "lote" to lote, "fechaVencimiento" to expirationDate, "cantidad" to quantity)

    db.collection("inventario")
        .add(data)
        .addOnSuccessListener { documentReference ->
            allData.add(DataFields(documentReference.id, location, sku, lote, expirationDate, quantity, description ))
        }
        .addOnFailureListener { e ->
            println("Error al guardar: $e")
        }
}