package com.eriknivar.firebasedatabase.view.inventoryreports

import android.util.Log
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.eriknivar.firebasedatabase.view.storagetype.DataFields
import com.google.firebase.firestore.FirebaseFirestore

fun fetchFilteredInventoryByUser(
    db: FirebaseFirestore,
    allData: SnapshotStateList<DataFields>,
    usuario: String
) {
    db.collection("inventario")
        .whereEqualTo("usuario", usuario)
        .get()
        .addOnSuccessListener { result ->
            Log.d("Firestore", "Total documentos obtenidos: ${result.size()}")
            allData.clear()
            result.documents.forEach { document ->
                allData.add(
                    DataFields(
                        documentId = document.id,
                        location = document.getString("ubicacion").orEmpty(),
                        sku = document.getString("codigoProducto").orEmpty(),
                        lote = document.getString("lote").orEmpty(),
                        expirationDate = document.getString("fechaVencimiento").orEmpty(),
                        quantity = document.getDouble("cantidad") ?: 0.0,
                        description = document.getString("descripcion").orEmpty(),
                        unidadMedida = document.getString("unidadMedida").orEmpty(),
                        fechaRegistro = document.getTimestamp("fechaRegistro"),
                        usuario = document.getString("usuario").orEmpty()
                    )
                )
            }
        }
        .addOnFailureListener { e ->
            Log.e("Firestore", "Error al obtener datos", e)
        }
}
