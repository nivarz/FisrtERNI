package com.eriknivar.firebasedatabase.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.eriknivar.firebasedatabase.view.masterdata.Producto
import java.util.Locale

object MaestroRepo {

    // Evita leak estático de contexto
    private val db get() = FirebaseFirestore.getInstance()

    // ==== helpers ====
    private fun up(s: String?) = (s ?: "").trim().uppercase(Locale.ROOT)
    private fun clean(s: String?) = (s ?: "").trim()
    private fun sanitizeCodigo(raw: String?): String =
        up(raw).replace(Regex("[^A-Z0-9_\\-]"), "")

    // ==== CRUD ====

    /** Lista todos los productos del cliente dado */
    fun listarProductos(
        clienteId: String,
        onResult: (List<Producto>) -> Unit,
        onErr: (Exception) -> Unit
    ) {
        val cid = up(clienteId)
        db.collection("clientes").document(cid)
            .collection("productos")
            .get()
            .addOnSuccessListener { snap ->
                val items = snap.documents.mapNotNull { d ->
                    try {
                        val codigo = d.getString("codigo") ?: d.id
                        val descripcion = d.getString("descripcion") ?: ""
                        val unidad = d.getString("unidad") ?: d.getString("UM") ?: "" // por si tuvieras legacy "UM"
                        Producto(
                            id = d.id,
                            codigo = codigo,
                            descripcion = descripcion,
                            unidad = unidad
                        )
                    } catch (_: Exception) { null }
                }
                onResult(items)
            }
            .addOnFailureListener(onErr)
    }

    /** Crea un producto (docId = código) */
    fun crearProducto(
        clienteId: String,
        producto: Producto,
        onResult: (String /*nuevoId*/) -> Unit,
        onErr: (Exception) -> Unit
    ) {
        val cid = up(clienteId)
        val codigo = sanitizeCodigo(producto.codigo)
        if (codigo.isBlank()) {
            onErr(IllegalArgumentException("Código inválido")); return
        }

        val ref = db.collection("clientes").document(cid)
            .collection("productos").document(codigo)

        val data = hashMapOf(
            "codigo" to codigo,                      // == docId
            "clienteId" to cid,
            "descripcion" to clean(producto.descripcion),
            "unidad" to clean(producto.unidad)
        )

        Log.d("MAESTRO", "CREATE /clientes/$cid/productos/$codigo $data")
        ref.set(data)
            .addOnSuccessListener { onResult(codigo) }
            .addOnFailureListener(onErr)
    }

    /** Actualiza descripción/unidad (protege 'codigo' como clave) */
    fun actualizarProducto(
        clienteId: String,
        productoId: String,
        cambios: Map<String, Any?>,
        onResult: () -> Unit,
        onErr: (Exception) -> Unit
    ) {
        val cid = up(clienteId)
        val cod = sanitizeCodigo(productoId)
        if (cod.isBlank()) {
            onErr(IllegalArgumentException("Código inválido")); return
        }

        val updates = mutableMapOf<String, Any>()
        cambios.forEach { (k, v) ->
            when (k) {
                "descripcion" -> if (!v?.toString().isNullOrBlank()) updates["descripcion"] = clean(v.toString())
                "unidad", "UM" -> if (!v?.toString().isNullOrBlank()) updates["unidad"] = clean(v.toString())
                // ignora 'codigo'
            }
        }
        if (updates.isEmpty()) {
            onErr(IllegalArgumentException("Nada para actualizar")); return
        }

        val ref = db.collection("clientes").document(cid)
            .collection("productos").document(cod)

        Log.d("MAESTRO", "UPDATE /clientes/$cid/productos/$cod $updates")
        ref.update(updates as Map<String, Any>)
            .addOnSuccessListener { onResult() }
            .addOnFailureListener(onErr)
    }

    /** Borra un producto por código */
    fun borrarProducto(
        clienteId: String,
        productoId: String,
        onResult: () -> Unit,
        onErr: (Exception) -> Unit
    ) {
        val cid = up(clienteId)
        val cod = sanitizeCodigo(productoId)
        if (cod.isBlank()) {
            onErr(IllegalArgumentException("Código inválido")); return
        }

        val ref = db.collection("clientes").document(cid)
            .collection("productos").document(cod)

        Log.d("MAESTRO", "DELETE /clientes/$cid/productos/$cod")
        ref.delete()
            .addOnSuccessListener { onResult() }
            .addOnFailureListener(onErr)
    }
}
