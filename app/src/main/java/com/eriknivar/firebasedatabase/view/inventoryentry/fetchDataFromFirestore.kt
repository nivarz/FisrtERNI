package com.eriknivar.firebasedatabase.view.inventoryentry

import android.util.Log
import com.eriknivar.firebasedatabase.view.storagetype.DataFields
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.*

fun fetchDataFromFirestore(
    db: FirebaseFirestore,
    allData: MutableList<DataFields>,
    usuario: String
) {

    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)

    val startOfDay = Timestamp(calendar.time) // 🔥 Fecha desde la medianoche de hoy

    db.collection("inventario")
        .whereGreaterThanOrEqualTo("fechaRegistro", startOfDay) // 🔥 Filtra solo registros de hoy
        .whereEqualTo("usuario", usuario)// 👈 Filtra solo los registros del usuario logueado
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
                val fechaRegistro = document.getTimestamp("fechaRegistro") ?: Timestamp.now() // ✅ Obtener la fecha

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
                        fechaRegistro, // ✅ Agregar la fecha al objeto
                        usuario,
                        localidad = document.getString("localidad") ?: "",
                        tipoUsuarioCreador = document.getString("tipoUsuarioCreador") ?: ""
                    )
                )
            }
        }
        .addOnFailureListener { e ->
            println("Error al obtener datos: $e")
        }
    Log.d("Firestore", "Registros cargados: ${allData.size}")

}

