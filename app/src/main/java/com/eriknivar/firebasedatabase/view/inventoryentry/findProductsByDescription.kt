package com.eriknivar.firebasedatabase.view.inventoryentry

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Obtiene lista y mapa de productos para el cliente actual.
 * - Si clienteId es null/blank -> consulta colección raíz "productos" (compatibilidad).
 * - Si clienteId tiene valor   -> consulta "clientes/{clienteId}/productos".
 * - Aplica filtro activo = true por defecto.
 *
 * @return Pair( listaDescripcionesOrdenada, mapaDescripcion -> (codigo, unidad) )
 */
suspend fun findProductsByDescription(
    db: FirebaseFirestore,
    clienteId: String?,
    onlyActive: Boolean = true
): Pair<List<String>, Map<String, Pair<String, String>>> {

    val colRef = if (!clienteId.isNullOrBlank()) {
        db.collection("clientes")
            .document(clienteId.trim().uppercase())
            .collection("productos")
    } else {
        db.collection("productos") // fallback compatibilidad (si aún existiera)
    }

    // Filtro por activos (si aplica)
    val query = if (onlyActive) colRef.whereEqualTo("activo", true) else colRef

    val snap = query.get().await()

    val lista = mutableListOf<String>()
    val mapa  = mutableMapOf<String, Pair<String, String>>()

    for (doc in snap.documents) {
        val codigo = doc.id
        val descripcion = (
                doc.getString("nombreComercial")
                    ?: doc.getString("nombreNormalizado")
                    ?: doc.getString("descripcion")
                    ?: ""
                ).trim()

        // Intenta varios nombres para la unidad
        val unidad = (
                doc.getString("unidad")
                    ?: doc.getString("unidadMedida")
                    ?: ""
                ).trim()

        if (descripcion.isNotEmpty()) {
            lista.add(descripcion)
            mapa[descripcion] = codigo to unidad
        }
    }

    lista.sort()
    return lista to mapa
}










