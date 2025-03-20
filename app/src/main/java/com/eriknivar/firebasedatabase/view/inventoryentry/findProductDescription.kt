package com.eriknivar.firebasedatabase.view.inventoryentry

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore

fun findProductDescription(db: FirebaseFirestore, sku: String, onResult: (String, String) -> Unit) {
    db.collection("productos")
        .document(sku) // 🔥 Busca directamente el documento por su ID (SKU)
        .get()
        .addOnSuccessListener { document ->
            if (document.exists()) {
                val descripcion = document.getString("descripcion") ?: "Sin descripción"
                val unidadMedida = document.getString("UM") ?: "N/A"

                Log.d("FirestoreDebug", "SKU: $sku - Descripción: $descripcion - UM: $unidadMedida") // 🔥 Verifica los datos obtenidos
                onResult(descripcion, unidadMedida) // ✅ Devuelve la descripción y UM
            } else {
                Log.d("FirestoreDebug", "SKU no encontrado en Firestore") // 🔹 Para depurar
                onResult("Sin descripción", "")
            }
        }
        .addOnFailureListener { e ->
            Log.e("FirestoreDebug", "Error al obtener descripción", e) // 🔹 Si hay error, mostrarlo
            onResult("Error al obtener datos", "N/A")
        }
}
