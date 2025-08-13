package com.eriknivar.firebasedatabase.view.inventoryentry

import com.eriknivar.firebasedatabase.view.utility.clienteIdUsuarioActual
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

fun findProducts(db: FirebaseFirestore, onResult: (List<String>, Map<String, Pair<String, String>>) -> Unit) {
    db.collection("productos")
        .whereEqualTo("clienteId", clienteIdUsuarioActual)
        .get()
        .addOnSuccessListener { result ->
            val productos = mutableListOf<String>()
            val productoCodigoMap = mutableMapOf<String, Pair<String, String>>() // 🔥 Map que almacena Código + UM

            for (document in result) {
                val codigo = document.id // 🔥 Ahora usamos el ID como código
                val descripcion = document.getString("descripcion") ?: continue
                val unidadMedida = document.getString("UM") ?: "N/A"

                productos.add(descripcion) // 🔥 Agrega la descripción a la lista
                productoCodigoMap[descripcion] = Pair(codigo, unidadMedida) // 🔥 Guarda Código y UM
            }

            onResult(productos, productoCodigoMap)
        }
        .addOnFailureListener {
            onResult(emptyList(), emptyMap()) // 🔥 Si hay error, devolver listas vacías
        }
}