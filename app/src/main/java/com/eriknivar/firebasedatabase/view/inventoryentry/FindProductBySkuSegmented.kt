package com.eriknivar.firebasedatabase.view.inventoryentry

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Lee la descripción y la UM de un SKU para el cliente actual.
 * - Si clienteId != null/blank -> clientes/{clienteId}/productos/{sku}
 * - Si clienteId == null/blank -> productos/{sku}  (compatibilidad)
 *
 * onResult(descripcion, unidadMedida)
 *   - "Sin descripción","" si no existe
 *   - "Error al obtener datos","N/A" si falla la lectura
 */
fun findProductDescriptionByClient(
    db: FirebaseFirestore,
    clienteId: String?,
    sku: String,
    onResult: (String, String) -> Unit
) {
    val skuId = sku.trim().uppercase()
    if (skuId.isEmpty()) {
        onResult("Sin descripción", "")
        return
    }

    val ref = if (!clienteId.isNullOrBlank()) {
        db.collection("clientes")
            .document(clienteId.trim().uppercase())
            .collection("productos")
            .document(skuId)
    } else {
        db.collection("productos").document(skuId) // fallback
    }

    ref.get()
        .addOnSuccessListener { doc ->
            if (!doc.exists()) {
                Log.d("FirestoreDebug", "SKU $skuId no existe para cliente=${clienteId ?: "(root)"}")
                onResult("Sin descripción", "")
                return@addOnSuccessListener
            }

            // Opcional: si el doc tiene 'activo=false', puedes tratarlo como inexistente
            val activo = doc.getBoolean("activo")
            if (activo == false) {
                onResult("Sin descripción", "")
                return@addOnSuccessListener
            }

            val descripcion = (
                    doc.getString("nombreComercial")
                        ?: doc.getString("nombreNormalizado")
                        ?: doc.getString("descripcion")
                        ?: "Sin descripción"
                    ).trim()

            val unidadMedida = (
                    doc.getString("unidad")
                        ?: doc.getString("unidadMedida")
                        ?: ""
                    ).trim()

            Log.d("FirestoreDebug", "SKU=$skuId desc=$descripcion um=$unidadMedida (cliente=${clienteId ?: "(root)"})")
            onResult(descripcion, unidadMedida)
        }
        .addOnFailureListener { e ->
            Log.e("FirestoreDebug", "Error leyendo SKU $skuId", e)
            onResult("Error al obtener datos", "N/A")
        }
}