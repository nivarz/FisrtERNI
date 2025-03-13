package com.eriknivar.firebasedatabase.view.inventoryentry

import com.eriknivar.firebasedatabase.view.storagetype.DataFields
import com.google.firebase.firestore.FirebaseFirestore

fun fetchDataFromFirestore(
    db: FirebaseFirestore,
    allData: MutableList<DataFields>
) {
    db.collection("inventario").get()
        .addOnSuccessListener { result ->
            allData.clear()
            for (document in result) {
                val location = document.getString("ubicacion") ?: ""
                val sku = document.getString("codigoProducto") ?: ""
                val lote = document.getString("lote") ?: ""
                val expirationDate = document.getString("fechaVencimiento") ?: ""
                val quantity = document.getLong("cantidad")?.toDouble() ?: 0.00
                allData.add(DataFields(document.id, location, sku, lote, expirationDate, quantity, document.getString("descripcion") ?: ""))
            }
        }
        .addOnFailureListener { e ->
            println("Error al obtener datos: $e")
        }
}

