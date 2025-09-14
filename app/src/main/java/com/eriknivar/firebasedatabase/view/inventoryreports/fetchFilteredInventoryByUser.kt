package com.eriknivar.firebasedatabase.view.inventoryreports

import android.util.Log
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.eriknivar.firebasedatabase.view.storagetype.DataFields
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query

fun fetchFilteredInventoryFromFirestore(
    db: FirebaseFirestore,
    clienteId: String,
    filters: Map<String, String>,
    onResult: (List<DataFields>) -> Unit,
    onError: (Exception) -> Unit
) {
    val cid = clienteId.trim().uppercase()
    var q: Query = db.collection("clientes").document(cid).collection("inventario")

    filters["localidad"]?.takeIf { it.isNotBlank() }?.let {
        q = q.whereEqualTo("localidad", it.trim().uppercase())
    }
    filters["usuario"]?.takeIf { it.isNotBlank() }?.let {
        q = q.whereEqualTo("usuario", it.trim())            // 👈 case-sensitive OK
    }

    q.get()
        .addOnSuccessListener { snap ->
            val list = snap.documents.map { d ->
                DataFields(
                    documentId = d.id,
                    location = d.getString("ubicacion").orEmpty(),
                    sku = d.getString("codigoProducto") ?: d.getString("sku").orEmpty(),
                    lote = d.getString("lote") ?: "-",
                    expirationDate = d.getString("fechaVencimiento").orEmpty(),
                    quantity = d.getDouble("cantidad") ?: 0.0,
                    description = d.getString("descripcion").orEmpty(),
                    unidadMedida = d.getString("unidadMedida") ?: d.getString("unidad").orEmpty(),
                    fechaRegistro = d.getTimestamp("fechaRegistro") ?: d.getTimestamp("fecha") ?: Timestamp.now(),
                    usuario = d.getString("usuario").orEmpty(),
                    localidad = d.getString("localidad").orEmpty(),
                    tipoUsuarioCreador = d.getString("tipoUsuarioCreador").orEmpty(),
                    fotoUrl = d.getString("fotoUrl").orEmpty()
                )
            }.sortedByDescending { it.fechaRegistro?.toDate() }   // 👉 orden en memoria
            onResult(list)
        }
        .addOnFailureListener(onError)
}

fun fetchAllInventory(
    db: FirebaseFirestore,
    allData: SnapshotStateList<DataFields>,
    tipoActual: String,
    clienteId: String       // 👈 NUEVO
) {
    val cid = clienteId.trim().uppercase()
    val ref = db.collection("clientes").document(cid).collection("inventario")

    // Nada de whereNotEqualTo (pide índice). Trae todo y filtra en memoria.
    ref.get()
        .addOnSuccessListener { result ->
            allData.clear()
            for (document in result) {
                val tipoCreador = document.getString("tipoUsuarioCreador")?.lowercase()?.trim() ?: ""

                if (tipoActual.lowercase().trim() == "admin" && tipoCreador == "superuser") continue

                allData.add(
                    DataFields(
                        documentId = document.id,
                        location = document.getString("ubicacion").orEmpty(),
                        sku = document.getString("codigoProducto") ?: document.getString("sku").orEmpty(),
                        lote = document.getString("lote") ?: "-",
                        expirationDate = document.getString("fechaVencimiento").orEmpty(),
                        quantity = document.getDouble("cantidad") ?: 0.0,
                        description = document.getString("descripcion").orEmpty(),
                        unidadMedida = document.getString("unidadMedida") ?: document.getString("unidad").orEmpty(),
                        fechaRegistro = document.getTimestamp("fechaRegistro") ?: document.getTimestamp("fecha") ?: Timestamp.now(),
                        usuario = document.getString("usuario").orEmpty(),
                        localidad = document.getString("localidad").orEmpty(),
                        tipoUsuarioCreador = tipoCreador
                    )
                )
            }
            allData.sortByDescending { it.fechaRegistro?.toDate() }
            Log.d("Firestore", "Total registros cargados ($tipoActual): ${allData.size}")
        }
        .addOnFailureListener {
            Log.e("Firestore", "Error al obtener todos los registros", it)
        }
}

