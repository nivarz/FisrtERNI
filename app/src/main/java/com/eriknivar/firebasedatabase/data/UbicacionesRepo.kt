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
        onData: (List<Pair<String, String>>) -> Unit,
        onErr: (Exception) -> Unit = {}
    ) {
        stop()
        val cid = clienteId.trim().uppercase(Locale.ROOT)
        val loc = localidadCodigo.trim().uppercase(Locale.ROOT)
        if (cid.isBlank() || loc.isBlank()) {
            onData(emptyList()); return
        }

        reg = db.collection("clientes").document(cid)
            .collection("localidades").document(loc)
            .collection("ubicaciones")
            .addSnapshotListener { qs: QuerySnapshot?, e ->
                if (e != null) {
                    Log.e("UBICACIONES", "listen error", e); onErr(e); return@addSnapshotListener
                }
                val items = qs?.documents?.map { d ->
                    val codigo = d.getString("codigo") ?: d.id
                    val nombre = d.getString("nombre") ?: d.id
                    codigo to nombre
                } ?: emptyList()
                onData(items)
            }
    }

    fun stop() {
        reg?.remove(); reg = null
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

    // --- Update (solo nombre/activo; acorde a reglas) ---
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

        val updates = mutableMapOf<String, Any>()
        if (!nuevoNombre.isNullOrBlank()) updates["nombre"] = nuevoNombre.trim()
        if (nuevoActivo != null) updates["activo"] = nuevoActivo
        if (updates.isEmpty()) {
            onResult(false, "Nada para actualizar."); return
        }

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
        onResult: (Boolean, String) -> Unit
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
    // imports necesarios arriba del archivo:
// import android.util.Log
// import java.util.Locale
// import kotlinx.coroutines.tasks.await

    // --- Verificar si una ubicación existe (cliente + localidad + código) ---
    suspend fun existeUbicacion(
        clienteId: String,
        localidad: String,
        codigoIngresado: String
    ): Boolean {
        val cid    = clienteId.trim().uppercase(Locale.ROOT)
        val loc    = localidad.trim().uppercase(Locale.ROOT)
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
                    Log.d("UBICACIONES", "HIT A: docId UPPER $cid/$loc/$codeUp okC=$okCliente okL=$okLoc")
                    if (okCliente && okLoc) return true
                }
            }

            // 2) NUEVO — docId lower
            ubicCol.document(codeLo).get().await().let { d ->
                if (d.exists()) {
                    val okCliente = (d.getString("clienteId") ?: cid).equals(cid, true)
                    val okLoc = (d.getString("localidadCodigo") ?: loc).equals(loc, true)
                    Log.d("UBICACIONES", "HIT B: docId lower $cid/$loc/$codeLo okC=$okCliente okL=$okLoc")
                    if (okCliente && okLoc) return true
                }
            }

            // 3) NUEVO — campo 'codigo' == UPPER, luego lower
            var dByCodigo = ubicCol.whereEqualTo("codigo", codeUp).limit(1).get().await().documents.firstOrNull()
            if (dByCodigo == null) {
                dByCodigo = ubicCol.whereEqualTo("codigo", codeLo).limit(1).get().await().documents.firstOrNull()
            }
            dByCodigo?.let { d ->
                val okCliente = (d.getString("clienteId") ?: cid).equals(cid, true)
                val okLoc = (d.getString("localidadCodigo") ?: loc).equals(loc, true)
                Log.d("UBICACIONES", "HIT C: campo 'codigo' docId=${d.id} okC=$okCliente okL=$okLoc")
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
                    Log.d("UBICACIONES", "HIT D: legacy 'codigo_ubi'=$codeUp docId=${doc.id} okL=$okLoc")
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
