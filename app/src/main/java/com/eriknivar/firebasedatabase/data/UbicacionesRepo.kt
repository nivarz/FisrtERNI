package com.eriknivar.firebasedatabase.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.util.Locale
import com.eriknivar.firebasedatabase.data.Ubicacion

object UbicacionesRepo {

    private val db = Firebase.firestore

    // --- Listener por cliente+localidad (para listar en UI) ---
    private var reg: ListenerRegistration? = null

    // ============== LISTEN combinando NUEVA y LEGACY ==============
    private var regNueva: ListenerRegistration? = null
    private var regLegacy: ListenerRegistration? = null
    private var regLegacyLoc: ListenerRegistration? = null
    private var regLegacyLocCodigo: ListenerRegistration? = null
    private const val TAG_UBI = "UbicacionesRepo"
    private var regAll: ListenerRegistration? = null
    fun listen(
        clienteId: String,
        localidadCodigo: String,
        onData: (List<Ubicacion>) -> Unit,
        onErr: (Exception) -> Unit
    ): ListenerRegistration {

        var cacheNueva: List<Ubicacion> = emptyList()
        var cacheLegacyLoc: List<Ubicacion> = emptyList()
        var cacheLegacyLocCodigo: List<Ubicacion> = emptyList()

        fun emitir() {
            val mapa = linkedMapOf<String, Ubicacion>()
            (cacheLegacyLoc + cacheLegacyLocCodigo + cacheNueva).forEach { u ->
                val key = (u.codigo.ifBlank { u.id }).uppercase()
                if (key.isNotBlank() && !mapa.containsKey(key)) mapa[key] = u
            }
            val lista = mapa.values.toList()
            Log.d(TAG_UBI, "EMITIR -> total=${lista.size} (loc=$localidadCodigo)")
            onData(lista)
        }

        // --- NUEVA: /clientes/{cid}/localidades/{loc}/ubicaciones
        try {
            regNueva?.remove()
            regNueva = db.collection("clientes")
                .document(clienteId)
                .collection("localidades")
                .document(localidadCodigo)
                .collection("ubicaciones")
                .addSnapshotListener { snap, e ->
                    if (e != null) {
                        Log.e(TAG_UBI, "NUEVA listener error: ${e.message}", e)
                        onErr(e); return@addSnapshotListener
                    }
                    cacheNueva = snap?.documents?.map { d ->
                        Ubicacion(
                            id = d.id,
                            codigo = d.getString("codigo") ?: d.id,
                            nombre = d.getString("nombre") ?: "",
                            clienteId = d.getString("clienteId") ?: clienteId,
                            localidadCodigo = d.getString("localidadCodigo") ?: localidadCodigo,
                            activo = d.getBoolean("activo") ?: true
                        )
                    }.orEmpty()

                    Log.d(
                        TAG_UBI,
                        "NUEVA: cid=$clienteId loc=$localidadCodigo -> ${cacheNueva.size} docs; ej=${
                            cacheNueva.take(
                                3
                            ).joinToString { it.codigo }
                        }"
                    )
                    emitir()
                }
        } catch (e: Exception) {
            Log.e(TAG_UBI, "NUEVA try/catch error: ${e.message}", e)
            onErr(e)
        }

        // --- LEGACY A: /clientes/{cid}/ubicaciones where localidad == {loc}
        try {
            regLegacyLoc?.remove()
            regLegacyLoc = db.collection("clientes")
                .document(clienteId)
                .collection("ubicaciones")
                .whereEqualTo("localidad", localidadCodigo)
                .addSnapshotListener { snap, e ->
                    if (e != null) {
                        Log.e(TAG_UBI, "LEGACY(localidad) listener error: ${e.message}", e)
                        onErr(e); return@addSnapshotListener
                    }
                    cacheLegacyLoc = snap?.documents?.map { d ->
                        Ubicacion(
                            id = d.id,
                            codigo = d.getString("codigo")
                                ?: d.getString("codigo_ubi")
                                ?: d.id,
                            nombre = d.getString("nombre")
                                ?: d.getString("descripcion")
                                ?: "",
                            clienteId = d.getString("clienteId") ?: clienteId,
                            localidadCodigo = d.getString("localidad")
                                ?: d.getString("localidadCodigo")
                                ?: localidadCodigo,
                            activo = d.getBoolean("activo") ?: true
                        )
                    }.orEmpty()

                    Log.d(
                        TAG_UBI,
                        "LEGACY(localidad): cid=$clienteId loc=$localidadCodigo -> ${cacheLegacyLoc.size} docs; ej=${
                            cacheLegacyLoc.take(
                                3
                            ).joinToString { it.codigo }
                        }"
                    )
                    emitir()
                }
        } catch (e: Exception) {
            Log.e(TAG_UBI, "LEGACY(localidad) try/catch error: ${e.message}", e)
            onErr(e)
        }

        // --- LEGACY B: /clientes/{cid}/ubicaciones where localidadCodigo == {loc}
        try {
            regLegacyLocCodigo?.remove()
            regLegacyLocCodigo = db.collection("clientes")
                .document(clienteId)
                .collection("ubicaciones")
                .whereEqualTo("localidadCodigo", localidadCodigo)
                .addSnapshotListener { snap, e ->
                    if (e != null) {
                        Log.e(TAG_UBI, "LEGACY(localidadCodigo) listener error: ${e.message}", e)
                        onErr(e); return@addSnapshotListener
                    }
                    cacheLegacyLocCodigo = snap?.documents?.map { d ->
                        Ubicacion(
                            id = d.id,
                            codigo = d.getString("codigo")
                                ?: d.getString("codigo_ubi")
                                ?: d.id,
                            nombre = d.getString("nombre")
                                ?: d.getString("descripcion")
                                ?: "",
                            clienteId = d.getString("clienteId") ?: clienteId,
                            localidadCodigo = d.getString("localidad")
                                ?: d.getString("localidadCodigo")
                                ?: localidadCodigo,
                            activo = d.getBoolean("activo") ?: true
                        )
                    }.orEmpty()

                    Log.d(
                        TAG_UBI,
                        "LEGACY(localidadCodigo): cid=$clienteId loc=$localidadCodigo -> ${cacheLegacyLocCodigo.size} docs; ej=${
                            cacheLegacyLocCodigo.take(
                                3
                            ).joinToString { it.codigo }
                        }"
                    )
                    emitir()
                }
        } catch (e: Exception) {
            Log.e(TAG_UBI, "LEGACY(localidadCodigo) try/catch error: ${e.message}", e)
            onErr(e)
        }

        // Devolver un ListenerRegistration válido
        return object : ListenerRegistration {
            override fun remove() {
                try {
                    regNueva?.remove()
                } catch (_: Exception) {
                }
                try {
                    regLegacyLoc?.remove()
                } catch (_: Exception) {
                }
                try {
                    regLegacyLocCodigo?.remove()
                } catch (_: Exception) {
                }
                regNueva = null
                regLegacyLoc = null
                regLegacyLocCodigo = null
            }
        }
    }

    fun listenAll(
        clienteId: String,
        onData: (List<Ubicacion>) -> Unit,
        onErr: (Exception) -> Unit
    ): ListenerRegistration {

        try {
            regAll?.remove()
        } catch (_: Exception) {
        }
        regAll = null

        try {
            // Trae TODAS las colecciones llamadas "ubicaciones" (ruta nueva y legacy)
            // y las limita por clienteId
            regAll = db.collectionGroup("ubicaciones")
                .whereEqualTo("clienteId", clienteId)
                .addSnapshotListener { snap, e ->
                    if (e != null) {
                        Log.e(TAG_UBI, "ALL listener error: ${e.message}", e)
                        onErr(e); return@addSnapshotListener
                    }

                    val lista = snap?.documents?.map { d ->
                        val id = d.id
                        val codigo = d.getString("codigo")
                            ?: d.getString("codigo_ubi")
                            ?: id
                        val nombre = d.getString("nombre")
                            ?: d.getString("descripcion")
                            ?: ""
                        val loc = d.getString("localidadCodigo")
                            ?: d.getString("localidad")
                            ?: "" // puede venir vacío en algunos legacy

                        Ubicacion(
                            id = id,
                            codigo = codigo,
                            nombre = nombre,
                            clienteId = d.getString("clienteId") ?: clienteId,
                            localidadCodigo = loc,
                            activo = d.getBoolean("activo") ?: true
                        )
                    }.orEmpty()

                    // dedupe por código (o id)
                    val mapa = linkedMapOf<String, Ubicacion>()
                    lista.forEach { u ->
                        val key = (u.codigo.ifBlank { u.id }).uppercase()
                        if (key.isNotBlank() && !mapa.containsKey(key)) mapa[key] = u
                    }
                    val out = mapa.values.toList()

                    Log.d(
                        TAG_UBI,
                        "ALL: cid=$clienteId -> total=${out.size}; ej=${
                            out.take(3).joinToString { "${it.localidadCodigo}:${it.codigo}" }
                        }"
                    )
                    onData(out)
                }
        } catch (e: Exception) {
            Log.e(TAG_UBI, "ALL try/catch error: ${e.message}", e)
            onErr(e)
        }

        return object : ListenerRegistration {
            override fun remove() {
                try {
                    regAll?.remove()
                } catch (_: Exception) {
                }
                regAll = null
            }
        }
    }

    // Ajusta stop() para remover ambos
    fun stop() {
        try {
            regAll?.remove()
        } catch (_: Exception) {
        }
        regAll = null
        // (y si ya tenías regNueva/regLegacy*, también los remueves aquí)
    }


    // --- Crear / actualizar (UPSERT) ---
    fun crearUbicacion(
        codigoRaw: String,
        nombreRaw: String,
        clienteIdDestino: String,
        localidadCodigoDestino: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            onResult(false, "No hay sesión activa."); return
        }

        // 1) Normalizar entradas
        val cid = clienteIdDestino.trim().uppercase(Locale.ROOT)
        val loc = localidadCodigoDestino.trim().uppercase(Locale.ROOT)
        val codigo = codigoRaw.trim().uppercase(Locale.ROOT)
        val nombre = nombreRaw.trim().ifEmpty { codigo }

        if (cid.isBlank()) {
            onResult(false, "clienteId vacío."); return
        }
        if (loc.isBlank()) {
            onResult(false, "localidad no seleccionada."); return
        }
        if (codigo.isBlank()) {
            onResult(false, "Código vacío."); return
        }

        // 2) Path y payload mínimo (reglas exigen estas 5 keys)
        val ref = db.collection("clientes").document(cid)
            .collection("localidades").document(loc)
            .collection("ubicaciones").document(codigo)

        val data = mapOf(
            "codigo" to codigo,
            "nombre" to nombre,
            "clienteId" to cid,  // == {id}
            "localidadCodigo" to loc,  // == {locId}
            "activo" to true
        )

        // 3) Diagnóstico previo (útil si las reglas niegan)
        Log.d("UBICACIONES", "preflight -> cid=$cid loc=$loc codigo=$codigo nombre='$nombre'")
        Log.d("UBICACIONES", "path=/clientes/$cid/localidades/$loc/ubicaciones/$codigo data=$data")

        // 4) Upsert
        ref.get()
            .addOnSuccessListener { snap ->
                if (snap.exists()) {
                    ref.set(data, SetOptions.merge())
                        .addOnSuccessListener { onResult(true, "Ubicación $codigo actualizada.") }
                        .addOnFailureListener { e ->
                            onResult(
                                false,
                                e.message ?: "Error actualizando $codigo"
                            )
                        }
                } else {
                    ref.set(data)
                        .addOnSuccessListener { onResult(true, "Ubicación $codigo creada.") }
                        .addOnFailureListener { e ->
                            val msg = if ((e.message ?: "").contains("PERMISSION_DENIED", true))
                                "PERMISSION_DENIED: revisa rol, clienteId, localidad y que docId==código."
                            else e.message ?: "Error creando $codigo"
                            onResult(false, msg)
                        }
                }
            }
            .addOnFailureListener { e ->
                onResult(
                    false,
                    "No pude verificar existencia: ${e.message}"
                )
            }
    }

// --- UPDATE: intenta NUEVA y si no existe, cae a LEGACY --------------------
    fun updateUbicacion(
        codigo: String,
        nuevoNombre: String? = null,
        nuevoActivo: Boolean? = null,
        clienteIdDestino: String,
        localidadCodigoDestino: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val cid = clienteIdDestino.trim().uppercase(Locale.ROOT)
        val loc = localidadCodigoDestino.trim().uppercase(Locale.ROOT)
        val cod = codigo.trim().uppercase(Locale.ROOT)
        if (cid.isBlank() || loc.isBlank() || cod.isBlank()) {
            onResult(false, "Parámetros incompletos."); return
        }

        val updatesNueva = mutableMapOf<String, Any>()
        if (!nuevoNombre.isNullOrBlank()) updatesNueva["nombre"] = nuevoNombre.trim()
        if (nuevoActivo != null) updatesNueva["activo"] = nuevoActivo

        if (updatesNueva.isEmpty()) {
            onResult(false, "Nada para actualizar."); return
        }

        val refNueva = db.collection("clientes").document(cid)
            .collection("localidades").document(loc)
            .collection("ubicaciones").document(cod)

        val refLegacy = db.collection("clientes").document(cid)
            .collection("ubicaciones").document(cod)

        // 1) Probar NUEVA
        refNueva.get()
            .addOnSuccessListener { snap ->
                if (snap.exists()) {
                    refNueva.update(updatesNueva as Map<String, Any>)
                        .addOnSuccessListener {
                            onResult(
                                true,
                                "Ubicación $cod actualizada (nueva)."
                            )
                        }
                        .addOnFailureListener { e ->
                            onResult(
                                false,
                                e.message ?: "Error actualizando $cod (nueva)."
                            )
                        }
                } else {
                    // 2) LEGACY: mapear 'nombre' -> 'descripcion'
                    val updatesLegacy = mutableMapOf<String, Any>()
                    if (!nuevoNombre.isNullOrBlank()) updatesLegacy["descripcion"] =
                        nuevoNombre.trim()
                    if (nuevoActivo != null) updatesLegacy["activo"] = nuevoActivo

                    refLegacy.get()
                        .addOnSuccessListener { snapLeg ->
                            if (!snapLeg.exists()) {
                                onResult(
                                    false,
                                    "Ubicación $cod no existe en nueva ni legacy."
                                ); return@addOnSuccessListener
                            }
                            if (updatesLegacy.isEmpty()) {
                                onResult(
                                    false,
                                    "Nada para actualizar."
                                ); return@addOnSuccessListener
                            }
                            refLegacy.update(updatesLegacy as Map<String, Any>)
                                .addOnSuccessListener {
                                    onResult(
                                        true,
                                        "Ubicación $cod actualizada (legacy)."
                                    )
                                }
                                .addOnFailureListener { e ->
                                    onResult(
                                        false,
                                        e.message ?: "Error actualizando $cod (legacy)."
                                    )
                                }
                        }
                        .addOnFailureListener { e ->
                            onResult(
                                false,
                                e.message ?: "Error consultando legacy."
                            )
                        }
                }
            }
            .addOnFailureListener { e -> onResult(false, e.message ?: "Error consultando nueva.") }
    }

    // --- DELETE: intenta NUEVA y si no existe, cae a LEGACY --------------------
    fun borrarUbicacion(
        codigo: String,
        clienteIdDestino: String,
        localidadCodigoDestino: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val cid = clienteIdDestino.trim().uppercase(Locale.ROOT)
        val loc = localidadCodigoDestino.trim().uppercase(Locale.ROOT)
        val cod = codigo.trim().uppercase(Locale.ROOT)
        if (cid.isBlank() || loc.isBlank() || cod.isBlank()) {
            onResult(false, "Parámetros incompletos."); return
        }

        val refNueva = db.collection("clientes").document(cid)
            .collection("localidades").document(loc)
            .collection("ubicaciones").document(cod)

        val refLegacy = db.collection("clientes").document(cid)
            .collection("ubicaciones").document(cod)

        refNueva.get()
            .addOnSuccessListener { snap ->
                if (snap.exists()) {
                    refNueva.delete()
                        .addOnSuccessListener {
                            onResult(
                                true,
                                "Ubicación $cod eliminada (nueva)."
                            )
                        }
                        .addOnFailureListener { e ->
                            onResult(
                                false,
                                e.message ?: "No se pudo eliminar $cod (nueva)."
                            )
                        }
                } else {
                    refLegacy.get()
                        .addOnSuccessListener { snapLeg ->
                            if (!snapLeg.exists()) {
                                onResult(
                                    false,
                                    "Ubicación $cod no existe en nueva ni legacy."
                                ); return@addOnSuccessListener
                            }
                            refLegacy.delete()
                                .addOnSuccessListener {
                                    onResult(
                                        true,
                                        "Ubicación $cod eliminada (legacy)."
                                    )
                                }
                                .addOnFailureListener { e ->
                                    onResult(
                                        false,
                                        e.message ?: "No se pudo eliminar $cod (legacy)."
                                    )
                                }
                        }
                        .addOnFailureListener { e ->
                            onResult(
                                false,
                                e.message ?: "Error consultando legacy."
                            )
                        }
                }
            }
            .addOnFailureListener { e -> onResult(false, e.message ?: "Error consultando nueva.") }
    }


    // --- Verificar si una ubicación existe (cliente + localidad + código) ---
    suspend fun existeUbicacion(
        clienteId: String,
        localidad: String,
        codigoIngresado: String
    ): Boolean {
        val cid = clienteId.trim().uppercase(Locale.ROOT)
        val loc = localidad.trim().uppercase(Locale.ROOT)
        val codeUp = codigoIngresado.trim().uppercase(Locale.ROOT)
        val codeLo = codeUp.lowercase(Locale.ROOT)

        if (cid.isBlank() || loc.isBlank() || codeUp.isBlank()) return false

        return try {
            val ubicCol = db.collection("clientes").document(cid)
                .collection("localidades").document(loc)
                .collection("ubicaciones")

            // 1) NUEVO — docId UPPER
            ubicCol.document(codeUp).get().await().let { d ->
                if (d.exists()) {
                    val okCliente = (d.getString("clienteId") ?: cid).equals(cid, true)
                    val okLoc = (d.getString("localidadCodigo") ?: loc).equals(loc, true)
                    Log.d(
                        "UBICACIONES",
                        "HIT A: docId UPPER $cid/$loc/$codeUp okC=$okCliente okL=$okLoc"
                    )
                    if (okCliente && okLoc) return true
                }
            }

            // 2) NUEVO — docId lower
            ubicCol.document(codeLo).get().await().let { d ->
                if (d.exists()) {
                    val okCliente = (d.getString("clienteId") ?: cid).equals(cid, true)
                    val okLoc = (d.getString("localidadCodigo") ?: loc).equals(loc, true)
                    Log.d(
                        "UBICACIONES",
                        "HIT B: docId lower $cid/$loc/$codeLo okC=$okCliente okL=$okLoc"
                    )
                    if (okCliente && okLoc) return true
                }
            }

            // 3) NUEVO — campo 'codigo' == UPPER, luego lower
            var dByCodigo = ubicCol.whereEqualTo("codigo", codeUp).limit(1).get()
                .await().documents.firstOrNull()
            if (dByCodigo == null) {
                dByCodigo = ubicCol.whereEqualTo("codigo", codeLo).limit(1).get()
                    .await().documents.firstOrNull()
            }
            dByCodigo?.let { d ->
                val okCliente = (d.getString("clienteId") ?: cid).equals(cid, true)
                val okLoc = (d.getString("localidadCodigo") ?: loc).equals(loc, true)
                Log.d(
                    "UBICACIONES",
                    "HIT C: campo 'codigo' docId=${d.id} okC=$okCliente okL=$okLoc"
                )
                if (okCliente && okLoc) return true
            }

            // 4) VIEJO — /clientes/{cid}/ubicaciones  (campo 'codigo_ubi')
            db.collection("clientes").document(cid)
                .collection("ubicaciones")
                .whereEqualTo("codigo_ubi", codeUp)
                .limit(1)
                .get()
                .await()
                .documents
                .firstOrNull()
                ?.let { doc ->
                    // En legacy la localidad puede estar en 'localidad' o 'localidadCodigo'
                    val okLoc = listOf(
                        doc.getString("localidad"),
                        doc.getString("localidadCodigo")
                    ).filterNotNull().any { it.equals(loc, true) }
                    Log.d(
                        "UBICACIONES",
                        "HIT D: legacy 'codigo_ubi'=$codeUp docId=${doc.id} okL=$okLoc"
                    )
                    if (okLoc) return true
                }

            Log.d("UBICACIONES", "MISS: $cid/$loc/$codeUp")
            false
        } catch (e: Exception) {
            Log.e("UBICACIONES", "existeUbicacion error", e)
            false
        }
    }
}
