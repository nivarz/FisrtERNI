package com.eriknivar.firebasedatabase.view.inventoryreports

import android.util.Log
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.eriknivar.firebasedatabase.view.storagetype.DataFields
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

/**
 * Reporte POR USUARIO (para Invitado o para filtrar por el usuario logueado).
 * - Invitado: trae TODOS sus registros (cualquier día), cumpliendo reglas con usuarioUid.
 * - No fuerza filtro por "dia". Si en algún caso lo envías en filters, se aplica.
 *
 * filters soportados (opcionales):
 *   - "localidad": String   → whereEqualTo en servidor
 *   - "dia": String(yyyyMMdd) → whereEqualTo en servidor (opcional)
 *   - "usuario": String     → filtro en memoria por nombre mostrado (opcional)
 */
fun fetchFilteredInventoryByUser(
    db: FirebaseFirestore,
    clienteId: String,
    filters: Map<String, String> = emptyMap(),
    onResult: (List<DataFields>) -> Unit,
    onError: (Exception) -> Unit
) {
    try {
        val cid = clienteId.trim().uppercase()
        val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

        var q: Query = db.collection("clientes").document(cid).collection("inventario")

        // Localidad (opcional)
        filters["localidad"]?.takeIf { it.isNotBlank() }?.let {
            q = q.whereEqualTo("localidad", it.trim().uppercase())
        }

        // 🔒 Clave para que pase reglas (Invitado solo puede ver lo suyo)
        q = q.whereEqualTo("usuarioUid", uid)

        // 👇 Solo si explícitamente envías "dia" lo aplicamos; por defecto NO filtramos por día
        filters["dia"]?.takeIf { it.isNotBlank() }?.let { dia ->
            q = q.whereEqualTo("dia", dia.trim())
        }

        // Ordenaremos en memoria → menos índices necesarios
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
                        // preferir fechaCliente; luego fechaRegistro; luego fecha (legacy)
                        fechaRegistro = d.getTimestamp("fechaCliente")
                            ?: d.getTimestamp("fechaRegistro")
                            ?: d.getTimestamp("fecha")
                            ?: Timestamp.now(),
                        // preferir usuarioNombre; luego usuario (legacy)
                        usuario = d.getString("usuarioNombre") ?: d.getString("usuario").orEmpty(),
                        localidad = d.getString("localidad").orEmpty(),
                        tipoUsuarioCreador = d.getString("tipoUsuarioCreador").orEmpty(),
                        fotoUrl = d.getString("fotoUrl").orEmpty()
                    )
                }

                // filtro opcional por nombre de usuario (solo visual, en memoria)
                filters["usuario"]?.takeIf { it.isNotBlank() }?.let { nombre ->
                    val goal = nombre.trim().lowercase()
                    list = list.filter { it.usuario.lowercase() == goal }
                }

                list = list.sortedByDescending { it.fechaRegistro?.toDate() }
                Log.d("Reportes", "PorUsuario: ${list.size} (cid=$cid uid=$uid)")
                onResult(list)
            }
            .addOnFailureListener(onError)
    } catch (e: Exception) {
        onError(e)
    }
}


/**
 * Trae todo para admin/super (con filtro de “no mostrar super” para admin).
 * Se mantiene tal cual, sólo se asegura que siempre se use el cliente.
 */
fun fetchAllInventory(
    db: FirebaseFirestore,
    allData: SnapshotStateList<DataFields>,
    tipoActual: String,
    clienteId: String
) {
    val cid = clienteId.trim().uppercase()
    val ref = db.collection("clientes").document(cid).collection("inventario")

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
                        fechaRegistro = document.getTimestamp("fechaCliente")
                            ?: document.getTimestamp("fechaRegistro")
                            ?: document.getTimestamp("fecha")
                            ?: Timestamp.now(),
                        usuario = document.getString("usuarioNombre") ?: document.getString("usuario").orEmpty(),
                        localidad = document.getString("localidad").orEmpty(),
                        tipoUsuarioCreador = tipoCreador,
                        fotoUrl = document.getString("fotoUrl").orEmpty()
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
