package com.eriknivar.firebasedatabase.view.inventoryentry

import android.util.Log
import androidx.compose.foundation.lazy.LazyListState
import com.eriknivar.firebasedatabase.view.storagetype.DataFields
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

fun fetchDataFromFirestore(
    db: FirebaseFirestore,
    allData: MutableList<DataFields>,
    usuario: String,
    listState: LazyListState,
    localidad: String,
    clienteId: String
) {
    val cid = clienteId.trim().uppercase()
    val loc = localidad.trim().uppercase()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // HOY [00:00, 24:00)
    val cal = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }
    val startOfDay = com.google.firebase.Timestamp(cal.time)

    // 👇 ahora leemos de /clientes/{cid}/inventario
    val invRef = db.collection("clientes").document(cid).collection("inventario")

    invRef
        .whereEqualTo("localidad", loc)
        .whereEqualTo("creadoPorUid", uid)                         // el dueño del registro
        .whereGreaterThanOrEqualTo("fechaRegistro", startOfDay)
        .orderBy("fechaRegistro", Query.Direction.DESCENDING)
        .get()
        .addOnSuccessListener { result ->
            Log.d("Firestore", "🔍 Documentos encontrados: ${result.size()}")
            allData.clear()

            for (document in result) {
                allData.add(
                    DataFields(
                        documentId = document.id,
                        location = document.getString("ubicacion").orEmpty(),
                        sku = document.getString("codigoProducto")
                            ?: document.getString("sku").orEmpty(),
                        lote = document.getString("lote") ?: "-",
                        expirationDate = document.getString("fechaVencimiento").orEmpty(),
                        quantity = document.getDouble("cantidad") ?: 0.0,
                        description = document.getString("descripcion").orEmpty(),
                        unidadMedida = document.getString("unidadMedida")
                            ?: document.getString("unidad").orEmpty(),
                        fechaRegistro = document.getTimestamp("fechaRegistro")
                            ?: document.getTimestamp("fecha") ?: Timestamp.now(),
                        usuario = document.getString("usuario").orEmpty(),
                        localidad = document.getString("localidad").orEmpty(),
                        tipoUsuarioCreador = document.getString("tipoUsuarioCreador").orEmpty(),
                        fotoUrl = document.getString("fotoUrl").orEmpty()
                    )
                )
            }
            Log.d("Firestore", "✅ Registros cargados: ${allData.size}")

            if (allData.isNotEmpty()) {
                CoroutineScope(Dispatchers.Main).launch { listState.scrollToItem(0) }
            }
        }
        .addOnFailureListener { e ->
            Log.e("Firestore", "❌ Error al obtener datos: $e")
        }
}
