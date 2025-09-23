package com.eriknivar.firebasedatabase.view.inventoryreports

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.eriknivar.firebasedatabase.view.storagetype.DataFields
import com.eriknivar.firebasedatabase.data.Refs

/**
 * Trae inventario filtrado para Reportes (admin/super/invitado).
 *
 * Filtros soportados en [filters]:
 *  - "localidad": String (código)
 *  - "dia": String (yyyyMMdd) → recomendado para “de hoy” (sin índices)
 *  - "usuario": String (nombre) → se filtra en memoria (server usa usuarioUid cuando es invitado)
 */
fun fetchFilteredInventoryFromFirestore(
    db: FirebaseFirestore,
    clienteId: String,
    filters: Map<String, String>,
    tipoUsuario: String,
    onResult: (List<DataFields>) -> Unit,
    onError: (Exception) -> Unit
) {
    try {
        val cid = clienteId.trim().uppercase()
        var q: Query = Refs.inv(db, cid)

        // Localidad (opcional)
        filters["localidad"]?.takeIf { it.isNotBlank() }?.let {
            q = q.whereEqualTo("localidad", it.trim().uppercase())
        }

        // 🔒 Invitado: obligatorio para pasar reglas
        val isInvitado = tipoUsuario.equals("invitado", ignoreCase = true)
        if (isInvitado) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
            q = q.whereEqualTo("usuarioUid", uid)
        }

        // Filtro por día (opcional) — evita rangos/índices
        filters["dia"]?.takeIf { it.isNotBlank() }?.let { dia ->
            q = q.whereEqualTo("dia", dia.trim())
        }

        // Nada de orderBy en servidor; ordenamos en memoria
        q.get()
            .addOnSuccessListener { snap ->
                var list = snap.documents.map { d ->
                    DataFields(
                        documentId = d.id,
                        location = d.getString("ubicacion").orEmpty(),
                        sku = d.getString("codigoProducto") ?: d.getString("sku").orEmpty(),
                        lote = d.getString("lote") ?: "-",
                        expirationDate = d.getString("fechaVencimiento").orEmpty(),
                        quantity = d.getDouble("cantidad") ?: 0.0,
                        description = d.getString("descripcion").orEmpty(),
                        unidadMedida = d.getString("unidadMedida") ?: d.getString("unidad").orEmpty(),
                        // Preferimos fechaCliente; si no, fechaRegistro; si no, fecha (legacy)
                        fechaRegistro = d.getTimestamp("fechaCliente")
                            ?: d.getTimestamp("fechaRegistro")
                            ?: d.getTimestamp("fecha")
                            ?: Timestamp.now(),
                        // Preferimos usuarioNombre; si no, usuario (legacy)
                        usuario = d.getString("usuarioNombre") ?: d.getString("usuario").orEmpty(),
                        localidad = d.getString("localidad").orEmpty(),
                        tipoUsuarioCreador = d.getString("tipoUsuarioCreador").orEmpty(),
                        fotoUrl = d.getString("fotoUrl").orEmpty()
                    )
                }

                // Filtro por "usuario" (nombre) en memoria — útil para admin/super
                filters["usuario"]?.takeIf { it.isNotBlank() }?.let { nombre ->
                    val goal = nombre.trim().lowercase()
                    list = list.filter { it.usuario.lowercase() == goal }
                }

                // Orden descendente por fecha (cliente/registro/legacy ya resuelto arriba)
                list = list.sortedByDescending { it.fechaRegistro?.toDate() }

                Log.d("Reportes", "Filtrados=${list.size} (tipo=$tipoUsuario, cid=$cid)")
                onResult(list)
            }
            .addOnFailureListener(onError)
    } catch (e: Exception) {
        onError(e)
    }
}
