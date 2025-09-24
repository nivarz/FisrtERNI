package com.eriknivar.firebasedatabase.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

/**
 * Helper para armar el Query de Reportes según rol (superuser/admin/invitado).
 * Filtros soportados en `filters`:
 *  - "localidad": String (código)
 *  - "dia": String (yyyyMMdd)
 *
 * NOTA: No se usa orderBy en servidor; se ordena en memoria si hace falta.
 */
object ReportesRepo {

    fun buildReportQueryForRole(
        db: FirebaseFirestore,
        clienteId: String,
        tipoUsuario: String,
        uidActual: String?,
        filters: Map<String, String>
    ): Query {
        val cid = clienteId.trim().uppercase()

        var q: Query = db
            .collection("clientes")
            .document(cid)
            .collection("inventario")

        // Filtro por localidad (opcional)
        filters["localidad"]?.takeIf { it.isNotBlank() }?.let {
            q = q.whereEqualTo("localidad", it.trim().uppercase())
        }

        // Filtro por día exacto (opcional) - evita rangos para no chocar con reglas
        filters["dia"]?.takeIf { it.isNotBlank() }?.let { dia ->
            q = q.whereEqualTo("dia", dia.trim())
        }

        // Invitado: restringe por su propio UID
        if (tipoUsuario.equals("invitado", ignoreCase = true)) {
            val uid = (uidActual ?: "").trim()
            if (uid.isNotEmpty()) {
                q = q.whereEqualTo("usuarioUid", uid)
            }
        }

        return q
    }
}
