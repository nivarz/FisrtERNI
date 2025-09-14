package com.eriknivar.firebasedatabase.data


import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class LocalidadDto(
    val id: String = "",
    val nombre: String = ""
)

suspend fun loadLocalidadesPorCliente(
    db: FirebaseFirestore,
    clienteIdActual: String
): List<LocalidadDto> {
    val cid = clienteIdActual.trim().uppercase()
    val path = "clientes/$cid/localidades"
    Log.d("LOCALIDADES", "Leyendo por ruta: $path")

    return try {
        val snap = db.collection("clientes")
            .document(cid)
            .collection("localidades")
            .get()
            .await()

        val res = snap.documents.map { d ->
            LocalidadDto(
                id = d.id,
                nombre = d.getString("nombre") ?: d.getString("descripcion") ?: d.id
            )
        }
        Log.d("LOCALIDADES", "OK (${res.size}) items")
        res
    } catch (e: Exception) {
        Log.e("LOCALIDADES", "PERMISSION / error leyendo $path -> ${e.message}", e)
        emptyList()
    }
}
