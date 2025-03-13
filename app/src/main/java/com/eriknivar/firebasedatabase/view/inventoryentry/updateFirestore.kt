package com.eriknivar.firebasedatabase.view.inventoryentry

import com.eriknivar.firebasedatabase.view.storagetype.DataFields
import com.google.firebase.firestore.FirebaseFirestore

fun updateFirestore(
    db: FirebaseFirestore,
    documentId: String,
    location: String,
    sku: String,
    lote: String,
    expirationDate: String,
    quantity: Double,

    allData: MutableList<DataFields>
) {
    db.collection("inventario").document(documentId)
        .update("ubicacion", location, "codigoProducto", sku, "lote", lote, "fechaVencimiento", expirationDate, "cantidad", quantity)
        .addOnSuccessListener {
            println("Mensajes actualizados correctamente")

            // Actualiza la lista local para reflejar el cambio en la UI
            val index = allData.indexOfFirst { it.documentId == documentId }
            if (index != -1) {
                allData[index] = DataFields(documentId, location, sku, lote, expirationDate,
                    quantity, "")
            }
        }
        .addOnFailureListener { e ->
            println("Error al actualizar: $e")
        }
}