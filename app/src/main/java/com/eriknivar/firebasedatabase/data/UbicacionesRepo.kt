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

object UbicacionesRepo {

    private val db = Firebase.firestore

    // --- Listener por cliente+localidad (para listar en UI) ---
    private var reg: ListenerRegistration? = null
    fun listen(
        clienteId: String,
        localidadCodigo: String,
        onData: (List<Pair<String,String>>) -> Unit,
        onErr: (Exception) -> Unit = {}
    ) {
        stop()
        val cid = clienteId.trim().uppercase(Locale.ROOT)
        val loc = localidadCodigo.trim().uppercase(Locale.ROOT)
        if (cid.isBlank() || loc.isBlank()) { onData(emptyList()); return }

        reg = db.collection("clientes").document(cid)
            .collection("localidades").document(loc)
            .collection("ubicaciones")
            .addSnapshotListener { qs: QuerySnapshot?, e ->
                if (e != null) { Log.e("UBICACIONES", "listen error", e); onErr(e); return@addSnapshotListener }
                val items = qs?.documents?.map { d ->
                    val codigo = d.getString("codigo") ?: d.id
                    val nombre = d.getString("nombre") ?: d.id
                    codigo to nombre
                } ?: emptyList()
                onData(items)
            }
    }
    fun stop() { reg?.remove(); reg = null }

    // --- Crear / actualizar (UPSERT) ---
    fun crearUbicacion(
        codigoRaw: String,
        nombreRaw: String,
        clienteIdDestino: String,
        localidadCodigoDestino: String,
        onResult: (ok: Boolean, msg: String) -> Unit
    ) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) { onResult(false, "No hay sesión activa."); return }

        // 1) Normalizar entradas
        val cid = clienteIdDestino.trim().uppercase(Locale.ROOT)
        val loc = localidadCodigoDestino.trim().uppercase(Locale.ROOT)
        val codigo = codigoRaw.trim().uppercase(Locale.ROOT)
        val nombre = nombreRaw.trim().ifEmpty { codigo }

        if (cid.isBlank())   { onResult(false, "clienteId vacío."); return }
        if (loc.isBlank())   { onResult(false, "localidad no seleccionada."); return }
        if (codigo.isBlank()){ onResult(false, "Código vacío."); return }

        // 2) Path y payload mínimo (reglas exigen estas 5 keys)
        val ref = db.collection("clientes").document(cid)
            .collection("localidades").document(loc)
            .collection("ubicaciones").document(codigo)

        val data = mapOf(
            "codigo"           to codigo,
            "nombre"           to nombre,
            "clienteId"        to cid,  // == {id}
            "localidadCodigo"  to loc,  // == {locId}
            "activo"           to true
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
                        .addOnFailureListener { e -> onResult(false, e.message ?: "Error actualizando $codigo") }
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
            .addOnFailureListener { e -> onResult(false, "No pude verificar existencia: ${e.message}") }
    }

    // --- Update (solo nombre/activo; acorde a reglas) ---
    fun updateUbicacion(
        codigo: String,
        nuevoNombre: String? = null,
        nuevoActivo: Boolean? = null,
        clienteIdDestino: String,
        localidadCodigoDestino: String,
        onResult: (ok: Boolean, msg: String) -> Unit
    ) {
        val cid = clienteIdDestino.trim().uppercase(Locale.ROOT)
        val loc = localidadCodigoDestino.trim().uppercase(Locale.ROOT)
        val cod = codigo.trim().uppercase(Locale.ROOT)
        if (cid.isBlank() || loc.isBlank() || cod.isBlank()) {
            onResult(false, "Parámetros incompletos."); return
        }

        val updates = mutableMapOf<String, Any>()
        if (!nuevoNombre.isNullOrBlank()) updates["nombre"] = nuevoNombre.trim()
        if (nuevoActivo != null)          updates["activo"] = nuevoActivo
        if (updates.isEmpty()) { onResult(false, "Nada para actualizar."); return }

        val ref = db.collection("clientes").document(cid)
            .collection("localidades").document(loc)
            .collection("ubicaciones").document(cod)

        ref.update(updates as Map<String, Any>)
            .addOnSuccessListener { onResult(true, "Ubicación $cod actualizada.") }
            .addOnFailureListener { e -> onResult(false, e.message ?: "Error actualizando $cod") }
    }

    // --- Delete duro (solo superuser por reglas) ---
    fun borrarUbicacion(
        codigo: String,
        clienteIdDestino: String,
        localidadCodigoDestino: String,
        onResult: (ok: Boolean, msg: String) -> Unit
    ) {
        val cid = clienteIdDestino.trim().uppercase(Locale.ROOT)
        val loc = localidadCodigoDestino.trim().uppercase(Locale.ROOT)
        val cod = codigo.trim().uppercase(Locale.ROOT)
        if (cid.isBlank() || loc.isBlank() || cod.isBlank()) {
            onResult(false, "Parámetros incompletos."); return
        }

        val ref = db.collection("clientes").document(cid)
            .collection("localidades").document(loc)
            .collection("ubicaciones").document(cod)

        ref.delete()
            .addOnSuccessListener { onResult(true, "Ubicación $cod eliminada.") }
            .addOnFailureListener { e -> onResult(false, e.message ?: "No se pudo eliminar $cod") }
    }

    // --- Verificar si una ubicación existe (cliente + localidad + código) ---
    suspend fun existeUbicacion(
        clienteId: String,
        localidad: String,
        codigoIngresado: String
    ): Boolean {
        val cid  = clienteId.trim().uppercase()
        val loc  = localidad.trim().uppercase()
        val code = codigoIngresado.trim().uppercase()
        if (cid.isBlank() || loc.isBlank() || code.isBlank()) return false

        return try {
            val snap = db.collection("clientes").document(cid)
                .collection("localidades").document(loc)
                .collection("ubicaciones").document(code)
                .get()
                .await()                         // 👈 requiere coroutines-play-services
            // Extra (sanity check): que coincidan los campos clave
            val okCliente   = (snap.getString("clienteId") ?: "")          .equals(cid, true)
            val okLocalidad = (snap.getString("localidadCodigo") ?: "")    .equals(loc, true)
            snap.exists() && okCliente && okLocalidad
        } catch (e: Exception) {
            Log.e("UBICACIONES", "existeUbicacion error", e)
            false
        }
    }

}
