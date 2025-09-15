package com.eriknivar.firebasedatabase.view.inventoryreports

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import com.eriknivar.firebasedatabase.view.storagetype.DataFields
import com.eriknivar.firebasedatabase.data.Refs

fun fetchFilteredInventoryFromFirestore(
    db: FirebaseFirestore,
    clienteId: String,
    filters: Map<String, String>,
    tipoUsuario: String,
    onResult: (List<DataFields>) -> Unit,
    onError: (Exception) -> Unit
) {
    val cid = clienteId.trim().uppercase()
    var q: Query = Refs.inv(db, cid)

    filters["localidad"]?.let { if (it.isNotBlank()) q = q.whereEqualTo("localidad", it.trim().uppercase()) }
    filters["usuario"]?.let { if (it.isNotBlank()) q = q.whereEqualTo("usuario", it.trim()) }

    // Nota: si luego quieres filtrar por fecha en servidor, lo añadimos; por ahora se filtra en cliente.

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
            }.sortedByDescending { it.fechaRegistro?.toDate() }

            onResult(list)
        }
        .addOnFailureListener(onError)
}


