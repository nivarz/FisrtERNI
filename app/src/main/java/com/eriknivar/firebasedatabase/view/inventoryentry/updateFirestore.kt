package com.eriknivar.firebasedatabase.view.inventoryentry

import android.content.Context
import android.widget.Toast
import com.eriknivar.firebasedatabase.view.storagetype.DataFields
import com.google.firebase.firestore.FirebaseFirestore

fun updateFirestore(
    context: Context, // ⬅️ nuevo parámetro
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
        .update(
            "ubicacion", location,
            "codigoProducto", sku,
            "lote", lote,
            "fechaVencimiento", expirationDate,
            "cantidad", quantity
        )
        .addOnSuccessListener {
            Toast.makeText(context, "Registro actualizado correctamente", Toast.LENGTH_SHORT).show()

            // Actualiza la lista local para reflejar el cambio en la UI
            val index = allData.indexOfFirst { it.documentId == documentId }
            if (index != -1) {
                allData[index] = allData[index].copy(
                    location = location,
                    sku = sku,
                    lote = lote,
                    expirationDate = expirationDate,
                    quantity = quantity
                )

            }
        }
        .addOnFailureListener { e ->
            println("Error al actualizar: $e")
        }
}