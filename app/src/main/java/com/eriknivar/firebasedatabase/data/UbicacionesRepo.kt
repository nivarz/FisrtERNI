package com.eriknivar.firebasedatabase.data

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

object UbicacionesRepo {
    private val db = Firebase.firestore

    // cache simple: nombre->codigo y codigo->codigo por cliente
    private val cacheLocalidades: MutableMap<String, Map<String, String>> = mutableMapOf()

    private suspend fun localidadCodigo(cid: String, input: String): String {
        val inRaw = (input).trim()
        if (inRaw.isEmpty()) return ""
        val inUp = inRaw.uppercase()

        // si parece código (tiene _ o es corto sin espacios), úsalo directo
        if ('_' in inUp || ' ' !in inUp) return inUp

        // buscar en cache
        cacheLocalidades[cid]?.let { map ->
            return map[inUp] ?: inUp
        }

        // cargar localidades del cliente y mapear nombre/código
        val snap = db.collection("clientes").document(cid)
            .collection("localidades").get().await()

        val map = mutableMapOf<String, String>()
        for (doc in snap.documents) {
            val codigo = (doc.getString("codigo") ?: doc.id).trim().uppercase()
            val nombre = (doc.getString("nombre") ?: doc.getString("descripcion") ?: "").trim().uppercase()
            if (codigo.isNotEmpty()) {
                map[codigo] = codigo          // código → código
            }
            if (nombre.isNotEmpty()) {
                map[nombre] = codigo          // nombre → código
            }
        }
        cacheLocalidades[cid] = map

        return map[inUp] ?: inUp
    }

    suspend fun existeUbicacion(
        clienteId: String,
        codigoIngresado: String,
        localidad: String
    ): Boolean {
        val cid = clienteId.trim().uppercase()
        val cod = codigoIngresado.trim().uppercase()
        val locCode = localidadCodigo(cid, localidad) // 👈 normaliza nombre→código

        if (cid.isBlank() || cod.isBlank() || locCode.isBlank()) return false

        // 1) Por docId compuesto: LOCALIDAD_CODIGO
        val docId = "${locCode}_${cod}"
        val byId = db.collection("clientes").document(cid)
            .collection("ubicaciones").document(docId).get().await()
        if (byId.exists()) return true

        // 2) Por campos (acepta "codigo_ubi" o "codigo")
        val col = db.collection("clientes").document(cid).collection("ubicaciones")

        val q1 = col.whereEqualTo("localidad", locCode)
            .whereEqualTo("codigo_ubi", cod).limit(1).get().await()
        if (!q1.isEmpty) return true

        val q2 = col.whereEqualTo("localidad", locCode)
            .whereEqualTo("codigo", cod).limit(1).get().await()
        if (!q2.isEmpty) return true

        return false
    }
}
