package com.eriknivar.firebasedatabase.data

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

/**
 * Utilidades mínimas para listar clientes y resolver su nombre.
 * Pensado para el picker de "cliente activo" cuando el usuario es superuser.
 */
object ClientesRepo {

    private val db = Firebase.firestore

    /**
     * Devuelve los clientes activos como pares (clienteId, nombre).
     * Requiere permisos de lectura sobre /clientes (en reglas: superuser).
     */
    fun listarActivos(
        onResult: (ok: Boolean, items: List<Pair<String, String>>, msg: String) -> Unit
    ) {
        db.collection("clientes")
            .whereEqualTo("activo", true)
            .get()
            .addOnSuccessListener { qs ->
                val items = qs.documents.map { d ->
                    val nombre = d.getString("nombreComercial")
                        ?: d.getString("nombre")
                        ?: d.getString("nombreNormalizado")
                        ?: d.id
                    d.id to nombre
                }
                onResult(true, items, "")
            }
            .addOnFailureListener { e ->
                onResult(false, emptyList(), e.message ?: "Error listando clientes")
            }
    }

    /**
     * Trae el nombre de un cliente (por si quieres mostrarlo después de setear el clienteId).
     */
    fun getNombreCliente(
        clienteId: String,
        onResult: (nombre: String) -> Unit
    ) {
        if (clienteId.isBlank()) {
            onResult("")
            return
        }
        db.collection("clientes").document(clienteId)
            .get()
            .addOnSuccessListener { d ->
                val nombre = d.getString("nombreComercial")
                    ?: d.getString("nombre")
                    ?: d.getString("nombreNormalizado")
                    ?: clienteId
                onResult(nombre)
            }
            .addOnFailureListener { onResult(clienteId) }
    }
}
