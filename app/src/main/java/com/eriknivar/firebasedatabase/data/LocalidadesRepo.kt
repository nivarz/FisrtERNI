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
        if (user == null) { onResult(false, "No hay sesión activa."); return }

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
                if (codigo.isBlank()) { onResult(false, "Código vacío o inválido."); return@addOnSuccessListener }
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

                Log.d("LOCALIDADES", "CREATE preflight tipo=$tipoTok targetCid=$targetCid codigo=$codigo nombre='$nombre'")
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
        clienteIdDestino: String? = null,
        onResult: (ok: Boolean, msg: String) -> Unit
    ) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) { onResult(false, "No hay sesión activa."); return }

        user.getIdToken(false)
            .addOnSuccessListener { res ->
                val tipoTok = lower(res.claims["tipo"] as? String)
                val clienteIdTok = upper(res.claims["clienteId"] as? String)

                val isSuper = (tipoTok == "superuser")
                val isAdmin = (tipoTok == "admin")

                if (!isSuper && !isAdmin) {
                    onResult(false, "Tu rol ($tipoTok) no puede actualizar localidades.")
                    return@addOnSuccessListener
                }

                val targetCid = when {
                    isSuper && !clienteIdDestino.isNullOrBlank() -> upper(clienteIdDestino)
                    else -> clienteIdTok
                }
                if (targetCid.isBlank()) {
                    onResult(false, "clienteId destino vacío.")
                    return@addOnSuccessListener
                }
                if (isAdmin && targetCid != clienteIdTok) {
                    onResult(false, "Admin solo puede escribir en su cliente ($clienteIdTok).")
                    return@addOnSuccessListener
                }

                val cod = sanitizeCodigo(codigo)
                if (cod.isBlank()) { onResult(false, "Código inválido."); return@addOnSuccessListener }

                val updates = mutableMapOf<String, Any>()
                if (!nuevoNombre.isNullOrBlank()) updates["nombre"] = nuevoNombre.trim()
                if (nuevoActivo != null) updates["activo"] = nuevoActivo
                if (updates.isEmpty()) { onResult(false, "Nada para actualizar."); return@addOnSuccessListener }

                val ref = db.collection("clientes").document(targetCid)
                    .collection("localidades").document(cod)

                // Reglas solo permiten tocar nombre/activo; no mandamos otros campos
                ref.update(updates as Map<String, Any>)
                    .addOnSuccessListener { onResult(true, "Localidad $cod actualizada.") }
                    .addOnFailureListener { e ->
                        Log.e("LOCALIDADES", "Update $cod falló", e)
                        onResult(false, e.message ?: "Error actualizando $cod")
                    }
            }
            .addOnFailureListener { e ->
                onResult(false, "No pude leer claims del token: ${e.message}")
            }
    }

    /**
     * Borra una localidad.
     * ADMIN: solo en su cliente; SUPERUSER: en cualquiera.
     */
    fun borrarLocalidad(
        codigo: String,
        clienteIdDestino: String? = null,
        onResult: (ok: Boolean, msg: String) -> Unit
    ) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) { onResult(false, "No hay sesión activa."); return }

        user.getIdToken(false)
            .addOnSuccessListener { res ->
                val tipoTok = lower(res.claims["tipo"] as? String)
                val clienteIdTok = upper(res.claims["clienteId"] as? String)

                val isSuper = (tipoTok == "superuser")
                val isAdmin = (tipoTok == "admin")

                if (!isSuper && !isAdmin) {
                    onResult(false, "Tu rol ($tipoTok) no puede borrar localidades.")
                    return@addOnSuccessListener
                }

                val targetCid = when {
                    isSuper && !clienteIdDestino.isNullOrBlank() -> upper(clienteIdDestino)
                    else -> clienteIdTok
                }
                if (targetCid.isBlank()) {
                    onResult(false, "clienteId destino vacío.")
                    return@addOnSuccessListener
                }
                if (isAdmin && targetCid != clienteIdTok) {
                    onResult(false, "Admin solo puede borrar en su cliente ($clienteIdTok).")
                    return@addOnSuccessListener
                }

                val cod = sanitizeCodigo(codigo)
                if (cod.isBlank()) { onResult(false, "Código inválido."); return@addOnSuccessListener }

                val ref = db.collection("clientes").document(targetCid)
                    .collection("localidades").document(cod)

                ref.delete()
                    .addOnSuccessListener { onResult(true, "Localidad $cod eliminada.") }
                    .addOnFailureListener { e -> onResult(false, e.message ?: "No se pudo eliminar $cod") }
            }
            .addOnFailureListener { e ->
                onResult(false, "No pude leer claims del token: ${e.message}")
            }
    }
}
