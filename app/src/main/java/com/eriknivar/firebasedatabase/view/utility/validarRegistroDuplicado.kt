package com.eriknivar.firebasedatabase.view.utility

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
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
    cantidad: Double,                  // se mantiene por compatibilidad
    localidad: String,
    clienteId: String,
    onResult: (Boolean, String?) -> Unit,
    onError: (Exception) -> Unit
) {
    try {
        // Normalización
        val cid   = clienteId.trim().uppercase(Locale.ROOT)
        val loc   = localidad.trim().uppercase(Locale.ROOT)
        val ubi   = ubicacion.trim().uppercase(Locale.ROOT)
        val cod   = sku.trim().uppercase(Locale.ROOT)
        val lot   = lote.trim().ifBlank { "-" }.uppercase(Locale.ROOT)

        // Día actual (clave estable)
        val hoyStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            .format(Date())

        // UID actual (reglas: invitado solo puede ver/validar lo suyo)
        val uidActual = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

        // /clientes/{cid}/inventario
        val ref = db.collection("clientes").document(cid).collection("inventario")

        // ✅ Solo igualdades + limit(1) → no requiere índice
        var q = ref
            .whereEqualTo("localidad", loc)
            .whereEqualTo("ubicacion", ubi)
            .whereEqualTo("codigoProducto", cod)
            .whereEqualTo("lote", lot)
            .whereEqualTo("dia", hoyStr)
            .whereEqualTo("usuarioUid", uidActual) // clave para cumplir reglas del invitado

        q = q.limit(1)

        q.get()
            .addOnSuccessListener { snap ->
                val doc = snap.documents.firstOrNull()
                val usuarioNombre = doc?.getString("usuarioNombre")
                    ?: doc?.getString("usuario")   // compatibilidad legacy
                onResult(doc != null, usuarioNombre)
            }
            .addOnFailureListener { e ->
                Log.e("DupCheck", "Fallo dup-check", e)
                onError(e)
            }
    } catch (e: Exception) {
        Log.e("DupCheck", "Excepción en dup-check", e)
        onError(e)
    }
}
