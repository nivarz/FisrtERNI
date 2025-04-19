package com.eriknivar.firebasedatabase.view.inventoryentry

import android.util.Log
import androidx.compose.foundation.lazy.LazyListState
import com.eriknivar.firebasedatabase.view.storagetype.DataFields
import com.google.firebase.Timestamp
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
    listState: LazyListState // ✅ Parámetro aún presente por compatibilidad
) {

    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)

    val startOfDay = Timestamp(calendar.time)

    db.collection("inventario")
        .whereGreaterThanOrEqualTo("fechaRegistro", startOfDay)
        .whereEqualTo("usuario", usuario)
        .orderBy("fechaRegistro", Query.Direction.DESCENDING)
        .get()
        .addOnSuccessListener { result ->
            allData.clear()
            for (document in result) {
                val location = document.getString("ubicacion") ?: ""
                val sku = document.getString("codigoProducto") ?: ""
                val lote = document.getString("lote") ?: ""
                val expirationDate = document.getString("fechaVencimiento") ?: ""
                val quantity = document.getDouble("cantidad") ?: 0.00
                val unidadMedida = document.getString("unidadMedida") ?: "N/A"
                val fechaRegistro = document.getTimestamp("fechaRegistro") ?: Timestamp.now()

                allData.add(
                    DataFields(
                        document.id,
                        location,
                        sku,
                        lote,
                        expirationDate,
                        quantity,
                        document.getString("descripcion") ?: "",
                        unidadMedida,
                        fechaRegistro,
                        usuario,
                        localidad = document.getString("localidad") ?: "",
                        tipoUsuarioCreador = document.getString("tipoUsuarioCreador") ?: ""
                    )
                )
            }
            Log.d("Firestore", "✅ Registros cargados: ${allData.size}")

            // ✅ Scroll automático al tope (registro más reciente)
            if (allData.isNotEmpty()) {
                CoroutineScope(Dispatchers.Main).launch {
                    listState.scrollToItem(0)
                }
            }
        }
        .addOnFailureListener { e ->
            println("Error al obtener datos: $e")
        }
}




