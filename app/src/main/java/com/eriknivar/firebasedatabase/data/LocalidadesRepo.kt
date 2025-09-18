package com.eriknivar.firebasedatabase.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Locale

object LocalidadesRepo {

    private val db = Firebase.firestore

    // --- listener de localidades (para SelectStoreTypeFragment / LocalidadesScreen) ---
    private var locReg: ListenerRegistration? = null

    fun listen(
        clienteId: String,
        onData: (List<String>) -> Unit,
        onErr: (Exception) -> Unit = {}
    ) {
        stop()

        val cid = clienteId.trim().uppercase(Locale.ROOT)
        if (cid.isBlank()) {
            onData(emptyList())
            return
        }

        locReg = db.collection("clientes")
            .document(cid)
            .collection("localidades")
            .addSnapshotListener { qs: QuerySnapshot?, e ->
                if (e != null) {
                    Log.e("LOCALIDADES", "listen() error", e)
                    onErr(e)
                    return@addSnapshotListener
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
     * Crea o actualiza una Localidad cumpliendo las reglas:
     * - DocID == codigo (MAYÚSCULAS, sin espacios)
     * - Keys mínimas: ["codigo","nombre","clienteId","activo"]
     * - clienteId del doc == {id} del path (/clientes/{id}/localidades/{codigo})
     *
     * superuser: puede escribir en [clienteIdDestino] si se pasa.
     * admin: ignorará [clienteIdDestino] y escribe en su propio cliente.
     * invitado: solo lectura.
     */
    fun crearLocalidad(
        codigoRaw: String,
        nombreRaw: String,
        clienteIdDestino: String? = null,
        onResult: (ok: Boolean, msg: String) -> Unit
    ) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            onResult(false, "No hay sesión activa.")
            return
        }

        val uid = user.uid

        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { d ->
                val clienteId = (d.getString("clienteId") ?: "")
                    .trim().uppercase(Locale.ROOT)
                val tipo = (d.getString("tipo") ?: "")
                    .trim().lowercase(Locale.ROOT)

                Log.d("AUTH", "uid=$uid tipo=$tipo clienteId=$clienteId")

                if (clienteId.isBlank()) {
                    onResult(false, "clienteId vacío en /usuarios/$uid.")
                    return@addOnSuccessListener
                }

                // superuser puede elegir destino; admin solo su cliente
                val selected = (clienteIdDestino ?: "")
                    .trim().uppercase(Locale.ROOT)
                val targetCid =
                    if (tipo == "superuser" && selected.isNotBlank()) selected else clienteId

                // Guard: no permitir "__TODAS__"
                if (targetCid == "__TODAS__") {
                    onResult(false, "Selecciona un cliente específico (no '__TODAS__').")
                    return@addOnSuccessListener
                }

                if (tipo !in listOf("admin", "superuser")) {
                    onResult(false, "Tu rol ($tipo) solo permite leer localidades.")
                    return@addOnSuccessListener
                }
                if (tipo == "admin" && targetCid != clienteId) {
                    onResult(false, "Admin solo puede escribir en su cliente ($clienteId).")
                    return@addOnSuccessListener
                }

                // Normalizar entradas
                val codigo = codigoRaw.trim().uppercase(Locale.ROOT)
                if (codigo.isBlank()) {
                    onResult(false, "Código vacío.")
                    return@addOnSuccessListener
                }
                val nombre = nombreRaw.trim().ifEmpty { codigo }

                // Path y payload mínimos exigidos por tus reglas
                val ref = db.collection("clientes")
                    .document(targetCid)
                    .collection("localidades")
                    .document(codigo) // docId == codigo

                val data = mapOf(
                    "codigo" to codigo,
                    "nombre" to nombre,
                    "clienteId" to targetCid, // debe igualar {id} del path
                    "activo" to true
                )

                // --- DIAGNÓSTICO PREVIO A CREATE ---
                Log.d(
                    "LOCALIDADES",
                    "preflight -> tipo=$tipo targetCid=$targetCid codigo=$codigo nombre='$nombre'"
                )
                val condKeys =
                    setOf("codigo", "nombre", "clienteId", "activo").all { data.containsKey(it) }
                val condCid = (data["clienteId"] == targetCid)
                Log.d(
                    "LOCALIDADES",
                    "preflight -> condKeys=$condKeys condCid=$condCid data=$data path=/clientes/$targetCid/localidades/$codigo"
                )

                // Upsert: si existe -> update; si no -> create
                ref.get()
                    .addOnSuccessListener { snap ->
                        if (snap.exists()) {
                            ref.set(data, SetOptions.merge())
                                .addOnSuccessListener {
                                    onResult(true, "Localidad $codigo actualizada.")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("LOCALIDADES", "Update $codigo falló", e)
                                    onResult(false, e.message ?: "Error actualizando $codigo")
                                }
                        } else {
                            ref.set(data)
                                .addOnSuccessListener {
                                    onResult(true, "Localidad $codigo creada.")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("LOCALIDADES", "Create $codigo falló", e)
                                    val msg =
                                        if (e.message?.contains("PERMISSION_DENIED", true) == true)
                                            "PERMISSION_DENIED: revisa rol, clienteId y que docId==código."
                                        else e.message ?: "Error creando $codigo"
                                    onResult(false, msg)
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        onResult(false, "No pude verificar existencia de $codigo: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                onResult(false, "No pude leer /usuarios/$uid: ${e.message}")
            }
    }

    fun updateLocalidad(
        codigo: String,
        nuevoNombre: String? = null,
        nuevoActivo: Boolean? = null,
        clienteIdDestino: String? = null,
        onResult: (ok: Boolean, msg: String) -> Unit
    ) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            onResult(false, "No hay sesión activa."); return
        }

        val uid = user.uid
        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { d ->
                val clienteId = (d.getString("clienteId") ?: "").trim().uppercase()
                val tipo = (d.getString("tipo") ?: "").trim().lowercase()
                val targetCid = if (tipo == "superuser" && !clienteIdDestino.isNullOrBlank())
                    clienteIdDestino.trim().uppercase() else clienteId

                val cod = codigo.trim().uppercase()
                val ref = db.collection("clientes").document(targetCid)
                    .collection("localidades").document(cod)

                // Solo mandamos los campos permitidos por reglas
                val updates = mutableMapOf<String, Any>()
                if (!nuevoNombre.isNullOrBlank()) updates["nombre"] = nuevoNombre.trim()
                if (nuevoActivo != null) updates["activo"] = nuevoActivo
                if (updates.isEmpty()) {
                    onResult(false, "Nada para actualizar."); return@addOnSuccessListener
                }

                ref.update(updates as Map<String, Any>)
                    .addOnSuccessListener { onResult(true, "Localidad $cod actualizada.") }
                    .addOnFailureListener { e ->
                        onResult(
                            false,
                            e.message ?: "Error actualizando $cod"
                        )
                    }
            }
            .addOnFailureListener { e -> onResult(false, "No pude leer tu usuario: ${e.message}") }
    }

    // LocalidadesRepo.kt (dentro del object)
    fun borrarLocalidad(
        codigo: String,
        clienteIdDestino: String? = null,
        onResult: (ok: Boolean, msg: String) -> Unit
    ) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            onResult(false, "No hay sesión activa."); return
        }

        val uid = user.uid
        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { d ->
                val clienteId = (d.getString("clienteId") ?: "").trim().uppercase(Locale.ROOT)
                val tipo = (d.getString("tipo") ?: "").trim().lowercase(Locale.ROOT)

                val selected = (clienteIdDestino ?: "").trim().uppercase(Locale.ROOT)
                val targetCid =
                    if (tipo == "superuser" && selected.isNotBlank()) selected else clienteId
                if (targetCid == "__TODAS__") {
                    onResult(false, "Elige un cliente específico."); return@addOnSuccessListener
                }

                val cod = codigo.trim().uppercase(Locale.ROOT)
                val ref = db.collection("clientes").document(targetCid)
                    .collection("localidades").document(cod)

                ref.delete()
                    .addOnSuccessListener { onResult(true, "Localidad $cod eliminada.") }
                    .addOnFailureListener { e ->
                        onResult(false, e.message ?: "No se pudo eliminar $cod")
                    }
            }
            .addOnFailureListener { e -> onResult(false, "No pude leer tu usuario: ${e.message}") }
    }


}
