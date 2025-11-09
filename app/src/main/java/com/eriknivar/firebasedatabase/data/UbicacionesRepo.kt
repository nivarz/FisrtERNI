package com.eriknivar.firebasedatabase.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.util.Locale
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source

object UbicacionesRepo {

    private val db = Firebase.firestore

    private const val TAG_UBI = "UbicacionesRepo"
    private var regAll: ListenerRegistration? = null

    fun listenAll(
        clienteId: String, onData: (List<Ubicacion>) -> Unit, onErr: (Exception) -> Unit
    ): ListenerRegistration {

        try {
            regAll?.remove()
        } catch (_: Exception) {
        }
        regAll = null

        try {
            // Trae TODAS las colecciones llamadas "ubicaciones" (ruta nueva y legacy)
            // y las limita por clienteId
            regAll = db.collectionGroup("ubicaciones").whereEqualTo("clienteId", clienteId)
                .addSnapshotListener { snap, e ->
                    if (e != null) {
                        Log.e(TAG_UBI, "ALL listener error: ${e.message}", e)
                        onErr(e); return@addSnapshotListener
                    }

                    val lista = snap?.documents?.map { d ->
                        val id = d.id
                        val codigo = d.getString("codigo") ?: d.getString("codigo_ubi") ?: id
                        val nombre = d.getString("nombre") ?: d.getString("descripcion") ?: ""
                        val loc = d.getString("localidadCodigo") ?: d.getString("localidad")
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
                        TAG_UBI, "ALL: cid=$clienteId -> total=${out.size}; ej=${
                        out.take(3).joinToString { "${it.localidadCodigo}:${it.codigo}" }
                    }")
                    onData(out)
                }
        } catch (e: Exception) {
            Log.e(TAG_UBI, "ALL try/catch error: ${e.message}", e)
            onErr(e)
        }

        return ListenerRegistration {
            try {
                regAll?.remove()
            } catch (_: Exception) {
            }
            regAll = null
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
        val ref = db.collection("clientes").document(cid).collection("localidades").document(loc)
            .collection("ubicaciones").document(codigo)

        val data = mapOf(
            "codigo" to codigo, "nombre" to nombre, "clienteId" to cid,  // == {id}
            "localidadCodigo" to loc,  // == {locId}
            "activo" to true
        )

        // 3) Diagnóstico previo (útil si las reglas niegan)
        Log.d("UBICACIONES", "preflight -> cid=$cid loc=$loc codigo=$codigo nombre='$nombre'")
        Log.d("UBICACIONES", "path=/clientes/$cid/localidades/$loc/ubicaciones/$codigo data=$data")

        // 4) Upsert
        ref.get().addOnSuccessListener { snap ->
                if (snap.exists()) {
                    ref.set(data, SetOptions.merge())
                        .addOnSuccessListener { onResult(true, "Ubicación $codigo actualizada.") }
                        .addOnFailureListener { e ->
                            onResult(
                                false, e.message ?: "Error actualizando $codigo"
                            )
                        }
                } else {
                    ref.set(data)
                        .addOnSuccessListener { onResult(true, "Ubicación $codigo creada.") }
                        .addOnFailureListener { e ->
                            val msg = if ((e.message ?: "").contains(
                                    "PERMISSION_DENIED",
                                    true
                                )
                            ) "PERMISSION_DENIED: revisa rol, clienteId, localidad y que docId==código."
                            else e.message ?: "Error creando $codigo"
                            onResult(false, msg)
                        }
                }
            }.addOnFailureListener { e ->
                onResult(
                    false, "No pude verificar existencia: ${e.message}"
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
        audit: AuditInfo? = null,               // 👈 NUEVO (opcional)
        onResult: (Boolean, String) -> Unit
    ) {
        val cid = clienteIdDestino.trim().uppercase(Locale.ROOT)
        val loc = localidadCodigoDestino.trim().uppercase(Locale.ROOT)
        val cod = codigo.trim().uppercase(Locale.ROOT)
        if (cid.isBlank() || loc.isBlank() || cod.isBlank()) {
            onResult(false, "Parámetros incompletos."); return
        }

        val refNueva =
            db.collection("clientes").document(cid).collection("localidades").document(loc)
                .collection("ubicaciones").document(cod)

        val refLegacy =
            db.collection("clientes").document(cid).collection("ubicaciones").document(cod)

        // 1) Intentar en NUEVA
        refNueva.get().addOnSuccessListener { snapNueva ->
                if (snapNueva.exists()) {
                    val before = snapNueva.data ?: emptyMap<String, Any?>()
                    val updates = mutableMapOf<String, Any>()
                    if (!nuevoNombre.isNullOrBlank()) updates["nombre"] = nuevoNombre.trim()
                    if (nuevoActivo != null) updates["activo"] = nuevoActivo

                    if (updates.isEmpty()) {
                        onResult(false, "Nada para actualizar."); return@addOnSuccessListener
                    }

                    refNueva.update(updates as Map<String, Any>).addOnSuccessListener {
                            onResult(true, "Ubicación $cod actualizada (nueva).")
                            // Auditoría (no bloquea UI)
                            try {

                                writeAuditRegistro(
                                    db = db,
                                    clienteId = cid,
                                    accionEs = "editar",                  // 👈 español
                                    codigo = cod,
                                    localidadCodigo = loc,
                                    entidad = "ubicacion",
                                    audit = audit,
                                    detalle = mapOf(
                                        "before" to before, "after" to updates
                                    )
                                )

                            } catch (_: Exception) {
                            }
                        }.addOnFailureListener { e ->
                            onResult(false, e.message ?: "Error actualizando $cod (nueva).")
                        }
                } else {
                    // 2) Intentar en LEGACY (mapeo nombre -> descripcion)
                    refLegacy.get().addOnSuccessListener { snapLeg ->
                            if (!snapLeg.exists()) {
                                onResult(
                                    false, "Ubicación $cod no existe en nueva ni legacy."
                                ); return@addOnSuccessListener
                            }
                            val before = snapLeg.data ?: emptyMap<String, Any?>()

                            val updatesLegacy = mutableMapOf<String, Any>()
                            if (!nuevoNombre.isNullOrBlank()) updatesLegacy["descripcion"] =
                                nuevoNombre.trim()
                            if (nuevoActivo != null) updatesLegacy["activo"] = nuevoActivo

                            if (updatesLegacy.isEmpty()) {
                                onResult(
                                    false, "Nada para actualizar."
                                ); return@addOnSuccessListener
                            }

                            refLegacy.update(updatesLegacy as Map<String, Any>)
                                .addOnSuccessListener {
                                    onResult(true, "Ubicación $cod actualizada (legacy).")
                                    try {

                                        writeAuditRegistro(
                                            db = db,
                                            clienteId = cid,
                                            accionEs = "editar",                  // 👈 español
                                            codigo = cod,
                                            localidadCodigo = loc,
                                            entidad = "ubicacion",
                                            audit = audit,
                                            detalle = mapOf(
                                                "before" to before,
                                                "after" to updatesLegacy,     // 👈 usa updatesLegacy
                                                "legacy" to true
                                            )
                                        )

                                    } catch (_: Exception) {
                                    }
                                }.addOnFailureListener { e ->
                                    onResult(
                                        false, e.message ?: "Error actualizando $cod (legacy)."
                                    )
                                }
                        }.addOnFailureListener { e ->
                            onResult(false, e.message ?: "Error consultando legacy.")
                        }
                }
            }.addOnFailureListener { e -> onResult(false, e.message ?: "Error consultando nueva.") }
    }


    // --- DELETE: intenta NUEVA y si no existe, cae a LEGACY --------------------
    fun borrarUbicacion(
        codigo: String,
        clienteIdDestino: String,
        localidadCodigoDestino: String,
        audit: AuditInfo? = null,               // 👈 NUEVO (opcional)
        onResult: (Boolean, String) -> Unit
    ) {
        val cid = clienteIdDestino.trim().uppercase(Locale.ROOT)
        val loc = localidadCodigoDestino.trim().uppercase(Locale.ROOT)
        val cod = codigo.trim().uppercase(Locale.ROOT)
        if (cid.isBlank() || loc.isBlank() || cod.isBlank()) {
            onResult(false, "Parámetros incompletos."); return
        }

        val refNueva =
            db.collection("clientes").document(cid).collection("localidades").document(loc)
                .collection("ubicaciones").document(cod)

        val refLegacy =
            db.collection("clientes").document(cid).collection("ubicaciones").document(cod)

        refNueva.get().addOnSuccessListener { snapNueva ->
                if (snapNueva.exists()) {
                    val before = snapNueva.data ?: mapOf("codigo" to cod, "localidadCodigo" to loc)
                    refNueva.delete().addOnSuccessListener {
                            onResult(true, "Ubicación $cod eliminada (nueva).")
                            try {

                                writeAuditRegistro(
                                    db = db,
                                    clienteId = cid,
                                    accionEs = "eliminar",               // 👈 español
                                    codigo = cod,
                                    localidadCodigo = loc,
                                    entidad = "ubicacion",
                                    audit = audit,
                                    detalle = mapOf("before" to before)
                                )

                            } catch (_: Exception) {
                            }
                        }.addOnFailureListener { e ->
                            onResult(false, e.message ?: "No se pudo eliminar $cod (nueva).")
                        }
                } else {
                    refLegacy.get().addOnSuccessListener { snapLeg ->
                            if (!snapLeg.exists()) {
                                onResult(
                                    false, "Ubicación $cod no existe en nueva ni legacy."
                                ); return@addOnSuccessListener
                            }
                            val before =
                                snapLeg.data ?: mapOf("codigo" to cod, "localidadCodigo" to loc)
                            refLegacy.delete().addOnSuccessListener {
                                    onResult(true, "Ubicación $cod eliminada (legacy).")
                                    try {

                                        writeAuditRegistro(
                                            db = db,
                                            clienteId = cid,
                                            accionEs = "eliminar",               // 👈 español
                                            codigo = cod,
                                            localidadCodigo = loc,
                                            entidad = "ubicacion",
                                            audit = audit,
                                            detalle = mapOf("before" to before)
                                        )

                                    } catch (_: Exception) {
                                    }
                                }.addOnFailureListener { e ->
                                    onResult(
                                        false, e.message ?: "No se pudo eliminar $cod (legacy)."
                                    )
                                }
                        }.addOnFailureListener { e ->
                            onResult(false, e.message ?: "Error consultando legacy.")
                        }
                }
            }.addOnFailureListener { e -> onResult(false, e.message ?: "Error consultando nueva.") }
    }


    // ===== Auditar acciones sobre Ubicaciones =====
    data class AuditInfo(
        val usuarioUid: String,
        val usuarioNombre: String,
        val tipoUsuario: String,
    )

    // Guarda SIEMPRE las llaves que tu visor espera: usuarioUid/usuarioNombre/tipoUsuario
// y además deja alias byUid/byNombre/byTipo por compatibilidad.
    private fun writeAuditRegistro(
        db: FirebaseFirestore,
        clienteId: String,
        accionEs: String,            // usar "editar" | "eliminar"
        codigo: String,
        localidadCodigo: String?,
        entidad: String,             // "ubicacion" | "localidad"
        audit: AuditInfo?,
        detalle: Map<String, Any?> = emptyMap(),
    ) {
        val base = mutableMapOf<String, Any?>(
            "clienteId" to clienteId,
            "entidad" to entidad,
            "accion" to accionEs,                    // 👈 español para tu visor
            "codigo" to codigo,
            "fecha" to com.google.firebase.Timestamp.now(),
            // usuario (llaves que tu pantalla espera):
            "usuarioUid" to audit?.usuarioUid,
            "usuarioNombre" to audit?.usuarioNombre,
            "tipoUsuario" to audit?.tipoUsuario,
            // alias de compatibilidad:
            "byUid" to audit?.usuarioUid,
            "byNombre" to audit?.usuarioNombre,
            "byTipo" to audit?.tipoUsuario
        )
        if (!localidadCodigo.isNullOrBlank()) base["localidadCodigo"] = localidadCodigo
        base.putAll(detalle)

        db.collection("clientes").document(clienteId).collection("auditoria_registros").add(base)
            .addOnFailureListener { e ->
                Log.w("AUDITORIA", "No se pudo escribir auditoría: ${e.message}")
            }
    }

    suspend fun existeUbicacionOfflineFirst(
        clienteId: String, localidad: String, codigoIngresado: String
    ): Boolean? {
        val db = FirebaseFirestore.getInstance()

        // 🔧 Normaliza: evita fallas por mayúsculas/minúsculas o espacios
        val cid = clienteId.trim().uppercase()
        val loc = localidad.trim().uppercase()
        val code = codigoIngresado.trim().uppercase()

        suspend fun queryPaths(source: Source): Int {
            var total = 0

            // 1) NUEVA: /clientes/{cid}/localidades/{loc}/ubicaciones
            runCatching {
                val base =
                    db.collection("clientes").document(cid).collection("localidades").document(loc)
                        .collection("ubicaciones")

                // (a) DocId == code (más rápido y confiable)
                if (base.document(code).get(source).await().exists()) {
                    total++; return@runCatching
                }

                // (b) Campo 'codigo' == code
                total += base.whereEqualTo("codigo", code).get(source).await().size()

                // (c) Alternativas por compatibilidad
                if (total == 0) total += base.whereEqualTo("codigo_ubi", code).get(source).await()
                    .size()
                if (total == 0) total += base.whereEqualTo("code", code).get(source).await().size()
                if (total == 0) total += base.whereEqualTo("ubicacion", code).get(source).await()
                    .size()
            }

            // 2) LEGACY por cliente (filtrando localidad y localidadCodigo)
            if (total == 0) runCatching {
                val coll = db.collection("clientes").document(cid).collection("ubicaciones")

                // localidad = loc
                val s =
                    coll.whereEqualTo("localidad", loc).whereEqualTo("codigo_ubi", code).get(source)
                        .await().size()
                total += s
                if (total == 0) total += coll.whereEqualTo("localidad", loc)
                    .whereEqualTo("codigo", code).get(source).await().size()

                // localidadCodigo = loc
                if (total == 0) total += coll.whereEqualTo("localidadCodigo", loc)
                    .whereEqualTo("codigo_ubi", code).get(source).await().size()
                if (total == 0) total += coll.whereEqualTo("localidadCodigo", loc)
                    .whereEqualTo("codigo", code).get(source).await().size()
            }

            // 3) LEGACY global: /ubicaciones (si aún lo usaste alguna vez)
            if (total == 0) runCatching {
                val root = db.collection("ubicaciones").whereEqualTo("clienteId", cid)
                    .whereEqualTo("localidad", loc)

                val s = root.whereEqualTo("codigo_ubi", code).get(source).await().size()
                total += s
                if (total == 0) total += root.whereEqualTo("codigo", code).get(source).await()
                    .size()
            }

            return total
        }

        // 1) CACHE (sirve offline): si está en caché → existe
        runCatching {
            val cached = queryPaths(Source.CACHE)
            if (cached > 0) return true
        }

        // 2) SERVER: si falla (sin red / timeout) → null (permitir grabar y validar luego)
        return try {
            val server = queryPaths(Source.SERVER)
            (server > 0)
        } catch (e: Exception) {
            Log.w("UbicacionesRepo", "No se pudo verificar online: ${e.message}")
            null
        }
    }
}