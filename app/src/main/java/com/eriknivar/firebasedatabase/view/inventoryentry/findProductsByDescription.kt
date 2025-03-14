package com.eriknivar.firebasedatabase.view.inventoryentry

import com.google.firebase.firestore.FirebaseFirestore

// 🔥 Función para buscar productos en Firestore

fun buscarProductos(db: FirebaseFirestore, onResult: (List<String>, Map<String, String>) -> Unit) {
    db.collection("productos")
        .get()
        .addOnSuccessListener { result ->
            val productos = mutableListOf<String>()
            val productoCodigoMap = mutableMapOf<String, String>()

            for (document in result) {
                val codigo = document.getString("sku") ?: continue
                val descripcion = document.getString("descripcion") ?: continue

                productos.add(descripcion) // 🔥 Agrega solo la descripción a la lista
                productoCodigoMap[descripcion] = codigo // 🔥 Asocia la descripción con el código
            }

            onResult(productos, productoCodigoMap)
        }
        .addOnFailureListener {
            onResult(emptyList(), emptyMap()) // Si hay error, devuelve listas vacías
        }
}



