package com.eriknivar.firebasedatabase.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Locale

object LocalidadesRepo {

    private val db = Firebase.firestore

    // ==== utils ====
    private fun sanitizeCodigo(raw: String): String =
        raw.trim()
            .uppercase(Locale.ROOT)
            // solo letras, números, _ y -, elimina todo lo demás (puntos, espacios, etc.)
            .replace(Regex("[^A-Z0-9_\\-]"), "")

    private fun upper(s: String?) = (s ?: "").trim().uppercase(Locale.ROOT)
    private fun lower(s: String?) = (s ?: "").trim().lowercase(Locale.ROOT)

    // --- listener de localidades (para SelectStoreTypeFragment / LocalidadesScreen) ---
    private var locReg: ListenerRegistration? = null

    fun listen(
        clienteId: String,
        onData: (List<String>) -> Unit,
        onErr: (Exception) -> Unit = {}
    ) {
        stop()

        val cid = upper(clienteId)
        if (cid.isBlank()) {
            onData(emptyList()); return
        }

        locReg = db.collection("clientes")
            .document(cid)
            .collection("localidades")
            .addSnapshotListener { qs: QuerySnapshot?, e ->
                if (e != null) {
                    Log.e("LOCALIDADES", "listen() error", e)
                    onErr(e); return@addSnapshotListener
                }
                val lista = qs?.documents?.map { it.getString("codigo") ?: it.id } ?: emptyList()
                onData(lista)
            }
    }

    fun stop() {
        locReg?.remove()
        locReg = null
    }

    fun invalidate(nuevoClienteId: String?) {
        Log.d("LOCALIDADES", "invalidate() cliente=${(nuevoClienteId ?: "").trim()}")
        stop()
    }

    /**
     * Crea una Localidad cumpliendo reglas (token-only):
     * - DocID == codigo (normalizado)
     * - Keys exactas: ["codigo","clienteId","nombre","activo"]
     * - ADMIN: solo en su clienteId (token)
     * - SUPERUSER: puede usar clienteIdDestino o el de su token si no se pasa
     */
    fun crearLocalidad(
        codigoRaw: String,
        nombreRaw: String,
        clienteIdDestino: String? = null,
        onResult: (Boolean, String) -> Unit
    ) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            onResult(false, "No hay sesión activa."); return
        }

        user.getIdToken(false)
            .addOnSuccessListener { res ->
                val tipoTok = lower(res.claims["tipo"] as? String)
                val clienteIdTok = upper(res.claims["clienteId"] as? String)

                val isSuper = (tipoTok == "superuser")
                val isAdmin = (tipoTok == "admin")

                if (!isSuper && !isAdmin) {
                    onResult(false, "Tu rol ($tipoTok) no puede crear localidades.")
                    return@addOnSuccessListener
                }

                val targetCid = when {
                    isSuper && !clienteIdDestino.isNullOrBlank() -> upper(clienteIdDestino)
                    else -> clienteIdTok
                }

                if (targetCid.isBlank()) {
                    onResult(false, "clienteId destino vacío (elige un cliente).")
                    return@addOnSuccessListener
                }
                if (isAdmin && targetCid != clienteIdTok) {
                    onResult(false, "Admin solo puede escribir en su cliente ($clienteIdTok).")
                    return@addOnSuccessListener
                }

                val codigo = sanitizeCodigo(codigoRaw)
                if (codigo.isBlank()) {
                    onResult(false, "Código vacío o inválido."); return@addOnSuccessListener
                }
                val nombre = nombreRaw.trim().ifEmpty { codigo }

                val ref = db.collection("clientes")
                    .document(targetCid)
                    .collection("localidades")
                    .document(codigo) // DocID == codigo

                // Solo llaves permitidas por las reglas
                val data = mapOf(
                    "codigo" to codigo,     // == locId
                    "clienteId" to targetCid, // == {cid} del path
                    "nombre" to nombre,
                    "activo" to true
                )

                Log.d(
                    "LOCALIDADES",
                    "CREATE preflight tipo=$tipoTok targetCid=$targetCid codigo=$codigo nombre='$nombre'"
                )
                Log.d("LOCALIDADES", "path=/clientes/$targetCid/localidades/$codigo data=$data")

                ref.set(data)
                    .addOnSuccessListener { onResult(true, "Localidad $codigo creada.") }
                    .addOnFailureListener { e ->
                        Log.e("LOCALIDADES", "Create $codigo falló", e)
                        val msg =
                            if (e.message?.contains("PERMISSION_DENIED", true) == true)
                                "PERMISSION_DENIED: verifica token (tipo/clienteId), docId==código, y llaves del esquema."
                            else e.message ?: "Error creando $codigo"
                        onResult(false, msg)
                    }
            }
            .addOnFailureListener { e ->
                onResult(false, "No pude leer claims del token: ${e.message}")
            }
    }

    /**
     * Actualiza solo campos mutables: nombre y/o activo.
     * ADMIN restringido a su cliente; SUPERUSER puede elegir clienteIdDestino.
     */
    fun updateLocalidad(
        codigo: String,
        nuevoNombre: String? = null,
        nuevoActivo: Boolean? = null,
        clienteIdDestino: String,
        audit: AuditInfo? = null,                  // 👈 NUEVO (opcional)
        onResult: (Boolean, String) -> Unit
    ) {
        val cid = clienteIdDestino.trim().uppercase(java.util.Locale.ROOT)
        val loc = codigo.trim().uppercase(java.util.Locale.ROOT)
        if (cid.isBlank() || loc.isBlank()) {
            onResult(false, "Parámetros incompletos."); return
        }

        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val ref = db.collection("clientes").document(cid)
            .collection("localidades").document(loc)

        ref.get()
            .addOnSuccessListener { snap ->
                if (!snap.exists()) {
                    onResult(false, "La localidad $loc no existe."); return@addOnSuccessListener
                }

                val before = snap.data ?: emptyMap<String, Any?>()
                val updates = mutableMapOf<String, Any>()
                if (!nuevoNombre.isNullOrBlank()) updates["nombre"] = nuevoNombre.trim()
                if (nuevoActivo != null) updates["activo"] = nuevoActivo
                if (updates.isEmpty()) {
                    onResult(false, "Nada para actualizar."); return@addOnSuccessListener
                }

                ref.update(updates as Map<String, Any>)
                    .addOnSuccessListener {
                        onResult(true, "Localidad $loc actualizada.")
                        try {
                            writeAuditRegistro(
                                db = db,
                                clienteId = cid,
                                accionEs = "editar",
                                codigo = loc,
                                entidad = "localidad",
                                localidadCodigo = null,
                                audit = audit,
                                detalle = mapOf(
                                    "before" to before,
                                    "after" to updates
                                )
                            )
                        } catch (_: Exception) { /* no romper flujo si falla auditoría */
                        }

                    }.addOnFailureListener { e ->
                        onResult(false, e.message ?: "Error actualizando $loc")
                    }
            }
            .addOnFailureListener { e ->
                onResult(false, e.message ?: "Error leyendo $loc")
            }
    }

    /**
     * Borra una localidad.
     * ADMIN: solo en su cliente; SUPERUSER: en cualquiera.
     */
    fun borrarLocalidad(
        codigo: String,
        clienteIdDestino: String,
        audit: AuditInfo? = null,                 // 👈 NUEVO (opcional)
        onResult: (Boolean, String) -> Unit
    ) {
        val cid = clienteIdDestino.trim().uppercase(java.util.Locale.ROOT)
        val loc = codigo.trim().uppercase(java.util.Locale.ROOT)
        if (cid.isBlank() || loc.isBlank()) {
            onResult(false, "Parámetros incompletos."); return
        }

        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val ref = db.collection("clientes").document(cid)
            .collection("localidades").document(loc)

        ref.get()
            .addOnSuccessListener { snap ->
                if (!snap.exists()) {
                    onResult(false, "La localidad $loc no existe."); return@addOnSuccessListener
                }

                val before = snap.data ?: mapOf("codigo" to loc)
                ref.delete()
                    .addOnSuccessListener {
                        onResult(true, "Localidad $loc eliminada.")
                        try {
                            writeAuditRegistro(
                                db = db,
                                clienteId = cid,
                                accionEs = "eliminar",
                                codigo = loc,
                                entidad = "localidad",
                                localidadCodigo = null,
                                audit = audit,
                                detalle = mapOf("before" to before)   // en delete no hay "after"
                            )
                        } catch (_: Exception) { /* no romper flujo si falla auditoría */
                        }

                    }
                    .addOnFailureListener { e ->
                        onResult(false, e.message ?: "No se pudo eliminar $loc")
                    }
            }.addOnFailureListener { e ->
                onResult(false, e.message ?: "Error leyendo $loc")
            }
    }

    // ===== Auditar acciones =====
    // ===== Auditar acciones =====
    data class AuditInfo(
        val usuarioUid: String,
        val usuarioNombre: String,
        val tipoUsuario: String,
    )

    /**
     * Helper unificado para auditoría (llaves compatibles con tu visor).
     * accionEs: "editar" | "eliminar"
     * entidad : "localidad" (por defecto) u otra si reutilizas
     */
    private fun writeAuditRegistro(
        db: com.google.firebase.firestore.FirebaseFirestore,
        clienteId: String,
        accionEs: String,                  // "editar" | "eliminar"
        codigo: String,
        entidad: String = "localidad",
        localidadCodigo: String? = null,
        audit: AuditInfo? = null,
        detalle: Map<String, Any?> = emptyMap(),
    ) {
        val payload = mutableMapOf<String, Any?>(
            "clienteId" to clienteId,
            "entidad" to entidad,
            "accion" to accionEs,                           // 👈 en español para tu visor
            "codigo" to codigo,
            "fecha" to com.google.firebase.Timestamp.now(),

            // Llaves que tu pantalla de auditoría espera:
            "usuarioUid" to audit?.usuarioUid,
            "usuarioNombre" to audit?.usuarioNombre,
            "tipoUsuario" to audit?.tipoUsuario,

            // Alias de compatibilidad (por si alguna pantalla antigua los lee):
            "byUid" to audit?.usuarioUid,
            "byNombre" to audit?.usuarioNombre,
            "byTipo" to audit?.tipoUsuario
        )

        if (!localidadCodigo.isNullOrBlank()) {
            payload["localidadCodigo"] = localidadCodigo
        }

        // Mezcla datos extra (before/after/legacy, etc.)
        payload.putAll(detalle)

        db.collection("clientes")
            .document(clienteId)
            .collection("auditoria_registros")
            .add(payload)
            .addOnFailureListener { e ->
                android.util.Log.w("AUDITORIA_LOC", "No se pudo escribir auditoría: ${e.message}")
            }
    }


}
