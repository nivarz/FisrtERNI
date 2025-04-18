package com.eriknivar.firebasedatabase.view.inventoryreports

import com.eriknivar.firebasedatabase.view.storagetype.DataFields
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

fun fetchFilteredInventoryFromFirestore(
    db: FirebaseFirestore,
    filters: Map<String, String>,
    tipoUsuario: String,
    onResult: (List<DataFields>) -> Unit,
    onError: (Exception) -> Unit
) {
    var query: Query = db.collection("inventario")

    filters["usuario"]?.takeIf { it.isNotBlank() }?.let {
        if (tipoUsuario != "admin") {
            query = query.whereEqualTo("usuario", it)
        }
    }

    filters["localidad"]?.takeIf { it.isNotBlank() }?.let {
        query = query.whereEqualTo("localidad", it)
    }

    query.get()
        .addOnSuccessListener { result ->
            val lista = result.map { doc ->
                DataFields(
                    documentId = doc.id,
                    location = doc.getString("ubicacion").orEmpty(),
                    sku = doc.getString("codigoProducto").orEmpty(),
                    lote = doc.getString("lote").orEmpty(),
                    expirationDate = doc.getString("fechaVencimiento").orEmpty(),
                    quantity = doc.getDouble("cantidad") ?: 0.0,
                    description = doc.getString("descripcion").orEmpty(),
                    unidadMedida = doc.getString("unidadMedida").orEmpty(),
                    fechaRegistro = doc.getTimestamp("fechaRegistro"),
                    usuario = doc.getString("usuario").orEmpty(),
                    localidad = doc.getString("localidad").orEmpty(),
                    tipoUsuarioCreador = doc.getString("tipoUsuarioCreador").orEmpty()
                )
            }
            onResult(lista)
        }
        .addOnFailureListener { e ->
            onError(e)
        }
}

