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

fun fetchAllInventory(
    db: FirebaseFirestore,
    allData: MutableList<DataFields>
) {
    db.collection("inventario")
        .get()
        .addOnSuccessListener { result ->
            allData.clear()
            for (document in result) {
                val location = document.getString("ubicacion") ?: ""
                val sku = document.getString("codigoProducto") ?: ""
                val lote = document.getString("lote") ?: ""
                val expirationDate = document.getString("fechaVencimiento") ?: ""
                val quantity = document.getDouble("cantidad") ?: 0.00
                val unidadMedida = document.getString("unidadMedida") ?: "N/A"
                val fechaRegistro = document.getTimestamp("fechaRegistro")
                val descripcion = document.getString("descripcion") ?: ""
                val usuario = document.getString("usuario") ?: ""

                allData.add(
                    DataFields(
                        document.id,
                        location,
                        sku,
                        lote,
                        expirationDate,
                        quantity,
                        descripcion,
                        unidadMedida,
                        fechaRegistro,
                        usuario
                    )
                )
            }
        }
        .addOnFailureListener {
            println("Error al cargar todos los registros: $it")
        }

    Log.d("Firestore", "Total registros cargados (admin): ${allData.size}")

}

