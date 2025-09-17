package com.eriknivar.firebasedatabase.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

object Audit {
    fun log(
        clienteId: String,
        entidad: String,       // "localidad" | "ubicacion" | ...
        entidadId: String,
        accion: String,        // "CREATE" | "UPDATE" | "DELETE" | "TOGGLE"
        byUid: String,
        byNombre: String,
        rol: String,
        diff: Map<String, Any?> = emptyMap(),
        origen: String = "app"
    ) {
        val db = Firebase.firestore
        val doc = hashMapOf(
            "entidad" to entidad,
            "entidadId" to entidadId,
            "accion" to accion,
            "byUid" to byUid,
            "byNombre" to byNombre,
            "rol" to rol,
            "origen" to origen,
            "timestamp" to FieldValue.serverTimestamp(),
            "diff" to diff
        )
        db.collection("clientes").document(clienteId)
            .collection("auditorias")
            .add(doc)
    }
}
