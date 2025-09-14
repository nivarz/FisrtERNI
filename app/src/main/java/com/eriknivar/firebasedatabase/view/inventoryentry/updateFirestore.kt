package com.eriknivar.firebasedatabase.view.inventoryentry

import android.util.Log
import com.eriknivar.firebasedatabase.view.storagetype.DataFields
import com.google.firebase.firestore.FirebaseFirestore

fun updateFirestore(
    db: FirebaseFirestore,
    clienteId: String,               // 👈 nuevo: para apuntar al cliente
    documentId: String,
    location: String,
    sku: String,
    lote: String,
    expirationDate: String,
    quantity: Double,
    allData: MutableList<DataFields>,
    onSuccess: () -> Unit,
) {
    val cid = clienteId.trim().uppercase()

    val updates = mapOf(
        "ubicacion" to location.trim().uppercase(),
        "codigoProducto" to sku.trim().uppercase(),
        "lote" to lote.trim().ifBlank { "-" }.uppercase(),
        "fechaVencimiento" to expirationDate.trim(),
        "cantidad" to quantity
    )

    // 👇 ahora en /clientes/{cid}/inventario/{documentId}
    db.collection("clientes").document(cid)
        .collection("inventario")
        .document(documentId)
        .update(updates)
        .addOnSuccessListener {
            // Refleja el cambio en la lista local para la UI
            val idx = allData.indexOfFirst { it.documentId == documentId }
            if (idx != -1) {
                allData[idx] = allData[idx].copy(
                    location = updates["ubicacion"] as String,
                    sku = updates["codigoProducto"] as String,
                    lote = updates["lote"] as String,
                    expirationDate = updates["fechaVencimiento"] as String,
                    quantity = updates["cantidad"] as Double
                )
            }
            onSuccess()
        }
        .addOnFailureListener { e ->
            Log.e("UpdateInv", "❌ Error al actualizar", e)
        }
}
