package com.eriknivar.firebasedatabase.view.utility

import android.util.Log
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Verifica si YA existe un registro igual HOY para:
 *   - localidad, ubicacion, codigoProducto, lote
 *   - (si invitado) el mismo usuarioUid actual
 *
 * Usa igualdad por 'dia' (yyyyMMdd) + limit(1) para evitar índices compuestos
 * y problemas de serverTimestamp.
 */

fun validarRegistroDuplicado(
    db: FirebaseFirestore,
    ubicacion: String,
    sku: String,
    lote: String,
    cantidad: Double,
    localidad: String,
    clienteId: String,
    sesionId: String = "",
    onResult: (
        existeDuplicado: Boolean, usuarioNombre: String?, docRef: DocumentReference?, cantidadExistente: Double?
    ) -> Unit,
    onError: (Exception) -> Unit
) {
    try {
        // Normalización
        val cid = clienteId.trim().uppercase(Locale.ROOT)
        val loc = localidad.trim().uppercase(Locale.ROOT)
        val ubi = ubicacion.trim().uppercase(Locale.ROOT)
        val cod = sku.trim().uppercase(Locale.ROOT)
        val lot = lote.trim().ifBlank { "-" }.uppercase(Locale.ROOT)
        val sid = sesionId.trim()

        // mismo formato que el campo "dia" en Firestore: yyyyMMdd
        val hoyStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())

        if (cid.isEmpty() || loc.isEmpty() || ubi.isEmpty() || cod.isEmpty()) {
            Log.w("DupCheck", "Parámetros insuficientes para validar duplicado")
            onResult(false, null, null, null)
            return
        }

        val ref = db.collection("clientes").document(cid).collection("inventario")

        var query = ref.whereEqualTo("localidad", loc).whereEqualTo("ubicacion", ubi)
            .whereEqualTo("codigoProducto", cod).whereEqualTo("lote", lot)
        // 👆 YA NO filtramos por usuarioUid → aplica a TODOS los usuarios

        if (sid.isNotBlank()) {
            // 🧩 En modo sesiones, el duplicado vive dentro de la sesión activa
            query = query.whereEqualTo("sesionId", sid)
        } else {
            // 🧩 Modo clásico: mantiene comportamiento actual por día
            query = query.whereEqualTo("dia", hoyStr)
        }

        query.limit(1).get().addOnSuccessListener { snap ->
                val doc = snap.documents.firstOrNull()

                if (doc == null) {
                    onResult(false, null, null, null)
                    return@addOnSuccessListener
                }

                val usuarioNombre =
                    doc.getString("usuarioNombre") ?: doc.getString("usuario") // compatibilidad

                val cantidadExistente =
                    doc.getDouble("cantidad") ?: doc.getLong("cantidad")?.toDouble()

                onResult(true, usuarioNombre, doc.reference, cantidadExistente)
            }.addOnFailureListener { e ->
                Log.e("DupCheck", "Fallo dup-check", e)
                onError(e)
            }
    } catch (e: Exception) {
        Log.e("DupCheck", "Excepción en dup-check", e)
        onError(e)
    }
}
