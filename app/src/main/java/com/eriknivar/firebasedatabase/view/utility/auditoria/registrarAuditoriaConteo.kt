// file: view/utility/auditoria/registrarAuditoriaConteo.kt
package com.eriknivar.firebasedatabase.view.utility.auditoria

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

fun registrarAuditoriaConteo(
    clienteId: String,
    registroId: String,
    tipoAccion: String,
    usuarioNombre: String,
    usuarioUid: String? = "",
    valoresAntes: Map<String, Any?> = emptyMap(),
    valoresDespues: Map<String, Any?> = emptyMap(),
    usuarioEmail: String? = null
): Task<DocumentReference> {
    require(clienteId.isNotBlank()) { "clienteId vacío" }

    val cid = clienteId.trim().uppercase()
    val db = FirebaseFirestore.getInstance()
    val data = hashMapOf<String, Any?>(
        "clienteId" to cid,
        "registro_id" to registroId,
        "tipo_accion" to tipoAccion.lowercase(),
        "usuarioNombre" to usuarioNombre,
        "usuarioUid" to usuarioUid,
        "usuarioEmail" to usuarioEmail,
        "fecha" to FieldValue.serverTimestamp(),
        "valores_antes" to valoresAntes,
        "valores_despues" to valoresDespues
    )

    return db.collection("clientes").document(cid)
        .collection("auditoria_registros")
        .add(data)
}
