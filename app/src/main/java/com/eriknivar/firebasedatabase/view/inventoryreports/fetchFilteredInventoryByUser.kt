package com.eriknivar.firebasedatabase.view.inventoryreports

import android.util.Log
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.eriknivar.firebasedatabase.view.storagetype.DataFields
import com.google.firebase.firestore.FirebaseFirestore

fun fetchFilteredInventoryByUser(
    db: FirebaseFirestore,
    allData: SnapshotStateList<DataFields>,
    usuario: String,
    tipoActual: String // 👈 se lo pasamos
) {
    db.collection("inventario")
        .whereEqualTo("usuario", usuario)
        .get()
        .addOnSuccessListener { result ->
            allData.clear()
            for (document in result) {
                val tipoCreador = document.getString("tipo")?.lowercase()?.trim() ?: ""

                // 🔐 Si el usuario actual es admin, ignorar registros de superuser
                if (tipoActual.lowercase().trim() == "admin" && tipoCreador == "superuser") {
                    continue
                }

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
                        usuario = document.getString("usuario").orEmpty(),
                        localidad = document.getString("localidad") ?: "",
                        tipoUsuarioCreador = tipoCreador
                    )
                )
            }
        }
        .addOnFailureListener { e ->
            Log.e("Firestore", "Error al obtener datos filtrados", e)
        }
}


fun fetchAllInventory(
    db: FirebaseFirestore,
    allData: SnapshotStateList<DataFields>,
    tipoActual: String
) {
    val query = if (tipoActual.lowercase().trim() == "admin") {
        // ✅ Admin NO debe ver registros de superuser
        db.collection("inventario").whereNotEqualTo("tipoUsuarioCreador", "superuser")
    } else {
        // ✅ Superuser ve todos
        db.collection("inventario")
    }

    query.get()
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
                val localidad = document.getString("localidad") ?: ""
                val tipoUsuarioCreador = document.getString("tipoUsuarioCreador") ?: ""


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
                        usuario,
                        localidad,
                        tipoUsuarioCreador
                    )
                )
            }

            Log.d("Firestore", "Total registros cargados ($tipoActual): ${allData.size}")
        }
        .addOnFailureListener {
            Log.e("Firestore", "Error al obtener todos los registros", it)
        }
}
