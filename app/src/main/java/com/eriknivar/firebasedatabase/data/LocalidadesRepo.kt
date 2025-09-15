package com.eriknivar.firebasedatabase.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await

object LocalidadesRepo {

    // 🔒 Un solo listener activo
    private var reg: ListenerRegistration? = null

    // 🧠 Caché en memoria: cliente -> lista
    private val cache = mutableMapOf<String, List<String>>()

    /** Carga one-shot (tu función original, con pequeños ajustes de robustez) */
    suspend fun getSuspend(
        db: FirebaseFirestore,
        clienteId: String
    ): List<String> {
        val cid = clienteId.trim().uppercase()
        if (cid.isBlank()) return emptyList()

        cache[cid]?.let { return it } // cache hit

        val snap = db.collection("clientes")
            .document(cid)
            .collection("localidades")
            .orderBy("nombre")      // ajusta si tu campo es otro
            .get()
            .await()

        val lista = snap.documents
            .mapNotNull(::mapDoc)
            .distinct()
            .sorted()

        cache[cid] = lista
        return lista
    }

    /**
     * 🎧 Escucha en tiempo real la subcolección clientes/{cid}/localidades.
     * - Devuelve caché inmediatamente si existe.
     * - Mantiene un único listener (evita duplicados).
     */
    fun listen(
        db: FirebaseFirestore,
        clienteId: String,
        onData: (List<String>) -> Unit,
        onErr: (Exception) -> Unit = {}
    ) {
        val cid = clienteId.trim().uppercase()
        if (cid.isBlank()) {
            onData(emptyList())
            return
        }

        // 1) Emitir caché al instante si existe
        cache[cid]?.let { onData(it) }

        // 2) Garantizar un solo listener
        reg?.remove()
        reg = db.collection("clientes")
            .document(cid)
            .collection("localidades")
            .orderBy("nombre")
            .addSnapshotListener { snap, e ->
                if (e != null) { onErr(e); return@addSnapshotListener }

                val lista = snap?.documents
                    ?.mapNotNull(::mapDoc)
                    ?.distinct()
                    ?.sorted()
                    ?: emptyList()

                cache[cid] = lista
                onData(lista)
            }
    }

    /** 🧼 Detener escucha */
    fun stop() {
        reg?.remove()
        reg = null
    }

    /** 🔁 Invalidar caché (todo o uno) */
    fun invalidate(cid: String? = null) {
        if (cid == null) cache.clear() else cache.remove(cid.trim().uppercase())
    }

    /** Mapeo robusto para distintos esquemas posibles */
    private fun mapDoc(d: DocumentSnapshot): String? {
        return d.getString("nombre")
            ?: d.getString("codigo_ubi")
            ?: d.getString("codigo")
            ?: d.getString("descripcion")
            ?: d.id.takeIf { it.isNotBlank() }
    }
}
