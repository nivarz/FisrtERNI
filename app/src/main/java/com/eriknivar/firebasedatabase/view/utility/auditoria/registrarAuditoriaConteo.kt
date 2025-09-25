package com.eriknivar.firebasedatabase.view.utility.auditoria

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Registra una acción de auditoría de conteos bajo:
 *   clientes/{clienteId}/auditoria_conteos
 *
 * @param clienteId       ID del cliente (obligatorio; en mayúsculas)
 * @param registroId      ID/clave del registro editado/eliminado/creado
 * @param tipoAccion      "editar" | "crear" | "eliminar" (libre)
 * @param usuarioNombre   Nombre visible quien hace la acción
 * @param usuarioUid      UID (puede ser vacío si no aplica)
 * @param valoresAntes    Mapa con valores previos (opcional)
 * @param valoresDespues  Mapa con valores posteriores (opcional)
 */
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

    // ⬇️ ruta unificada
    return db.collection("clientes")
        .document(cid)
        .collection("auditoria_registros")
        .add(data)
}

fun registrarNotaAuditoriaConteo(
    clienteId: String,
    mensaje: String,
    extra: Map<String, Any?> = emptyMap(),
    usuarioNombre: String = "",
    usuarioUid: String = ""
) {
    val db = FirebaseFirestore.getInstance()
    val payload = hashMapOf<String, Any?>(
        "clienteId" to clienteId.trim().uppercase(),
        "registro_id" to "",
        "tipo_accion" to "nota",
        "usuarioNombre" to usuarioNombre,
        "usuarioUid" to usuarioUid,
        "fecha" to FieldValue.serverTimestamp(),
        "mensaje" to mensaje
    ) + extra

    db.collection("clientes")
        .document(clienteId.trim().uppercase())
        .collection("auditoria_registros")   // ⬅️ unificado
        .add(payload)
}
