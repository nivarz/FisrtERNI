package com.eriknivar.firebasedatabase.view.inventoryentry

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore

fun findProductDescription(db: FirebaseFirestore, sku: String, onResult: (String) -> Unit) {
    db.collection("productos") // 🔹 Asegúrate de que esta es la colección correcta en Firestore
        .whereEqualTo("sku", sku) // 🔹 Asegúrate de que el campo "sku" existe en Firestore
        .limit(1) // 🔹 Solo obtener el primer resultado encontrado
        .get()
        .addOnSuccessListener { documents ->
            if (!documents.isEmpty) {
                val descripcion = documents.documents[0].getString("descripcion") ?: "Sin descripción"
                onResult(descripcion) // 🔥 Enviar la descripción al estado
            } else {
                Log.d("FirestoreDebug", "No se encontró el SKU en Firestore") // 🔹 Para depurar
                onResult("Sin descripción") // 🔹 Si no hay datos, mostrar esto
            }
        }
        .addOnFailureListener { e ->
            Log.e("FirestoreDebug", "Error al obtener descripción", e) // 🔹 Si hay error, mostrarlo
            onResult("Error al obtener datos")
        }
}

