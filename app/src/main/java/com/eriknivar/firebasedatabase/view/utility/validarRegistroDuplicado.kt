package com.eriknivar.firebasedatabase.view.utility

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

fun validarRegistroDuplicado(
    db: FirebaseFirestore,
    ubicacion: String,
    sku: String,
    lote: String,
    cantidad: Double,          // se mantiene para no romper llamadas
    localidad: String,
    clienteId: String,
    onResult: (Boolean, String?) -> Unit,
    onError: (Exception) -> Unit
) {
    // --- Normalización
    val cid  = clienteId.trim().uppercase()
    val loc  = localidad.trim().uppercase()
    val ubi  = ubicacion.trim().uppercase()
    val skuN = sku.trim().uppercase()
    val loteN = lote.trim().ifBlank { "-" }.uppercase()

    // --- Ref a SUBCOLECCIÓN: /clientes/{cid}/inventario
    val ref = db.collection("clientes").document(cid).collection("inventario")

    // --- Ventana HOY [00:00, 24:00)
    val cal = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }
    val tsIni = com.google.firebase.Timestamp(cal.time)
    cal.add(java.util.Calendar.DAY_OF_MONTH, 1)
    val tsFin = com.google.firebase.Timestamp(cal.time)

    // --- Consulta principal (rápida; requiere índice)
    ref.whereEqualTo("localidad", loc)
        .whereEqualTo("ubicacion", ubi)
        .whereEqualTo("codigoProducto", skuN)   // usa "sku" si ese fuera tu campo
        .whereEqualTo("lote", loteN)
        .whereGreaterThanOrEqualTo("fecha", tsIni)
        .whereLessThan("fecha", tsFin)
        .orderBy("fecha")                       // para que calce con el índice
        .limit(1)
        .get()
        .addOnSuccessListener { snap ->
            val doc = snap.documents.firstOrNull()
            onResult(doc != null, doc?.getString("usuario"))
        }
        .addOnFailureListener { e ->
            android.util.Log.e("DupCheck", "Fallo consulta principal (índice/reglas): ${e.message}", e)
            // Fallback: sin rango de fecha (evita índice); filtramos HOY en memoria
            ref.whereEqualTo("localidad", loc)
                .whereEqualTo("ubicacion", ubi)
                .whereEqualTo("codigoProducto", skuN)
                .whereEqualTo("lote", loteN)
                .limit(20)
                .get()
                .addOnSuccessListener { s2 ->
                    val existeHoy = s2.documents.any { d ->
                        val t = d.getTimestamp("fecha") ?: return@any false
                        t >= tsIni && t < tsFin
                    }
                    val usuario = s2.documents.firstOrNull()?.getString("usuario")
                    onResult(existeHoy, usuario)
                }
                .addOnFailureListener { e2 ->
                    android.util.Log.e("DupCheck", "Fallo también el fallback: ${e2.message}", e2)
                    onError(e2)
                }
        }
}






