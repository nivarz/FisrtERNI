package com.eriknivar.firebasedatabase.view.inventoryentry

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore

fun findProductDescription(db: FirebaseFirestore, sku: String, onResult: (String) -> Unit) {
    db.collection("productos") // 🔹 Asegúrate de que la colección se llama "productos"
        .document(sku) // ✅ Ahora buscamos directamente por el SKU como ID del documento
        .get()
        .addOnSuccessListener { document ->
            if (document.exists()) {
                val descripcion = document.getString("descripcion") ?: "Sin descripción"
                onResult(descripcion) // 🔥 Enviar la descripción al estado
            } else {
                Log.d("FirestoreDebug", "No se encontró el SKU en Firestore") // 🔹 Para depurar
                onResult("Producto no encontrado") // 🔹 Si no hay datos, mostrar esto
            }
        }
        .addOnFailureListener { e ->
            Log.e("FirestoreDebug", "Error al obtener descripción", e) // 🔹 Si hay error, mostrarlo
            onResult("Error al obtener datos")
        }
}
