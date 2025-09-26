package com.eriknivar.firebasedatabase.view.inventoryentry

import android.util.Log
import androidx.compose.foundation.lazy.LazyListState
import com.eriknivar.firebasedatabase.view.storagetype.DataFields
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


/**
 * Carga puntual de inventario para HOY, por cliente/localidad.
 * - Si tipo == "invitado" filtra por usuarioUid == uid (coherente con reglas).
 * - Ordena por fechaRegistro DESC.
 * - Mapea tolerante (campos legacy).
 */

fun fetchDataFromFirestore(
    db: FirebaseFirestore,
    allData: MutableList<DataFields>,
    usuario: String,                 // (display name si lo usas en UI)
    listState: LazyListState,
    localidad: String,
    clienteId: String,
    tipo: String,                    // ya no condiciona el query, lo dejamos para compat
    uid: String                      // uid actual (OBLIGATORIO)
) {
    val cid = clienteId.trim().uppercase(Locale.ROOT)
    val loc = localidad.trim().uppercase(Locale.ROOT)

    // yyyyMMdd (mismo formato que guardas en los docs)
    val hoyStr = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault())
        .format(java.util.Date())

    // /clientes/{cid}/inventario —> SOLO mis registros de HOY
    val q: Query = db.collection("clientes").document(cid).collection("inventario")
        .whereEqualTo("localidad", loc)
        .whereEqualTo("dia", hoyStr)             // solo hoy
        .whereEqualTo("usuarioUid", uid)         // solo mis registros
        .orderBy("fechaCliente", Query.Direction.DESCENDING)

    q.get()
        .addOnSuccessListener { result ->
            Log.d("Firestore", "🔍 Documentos encontrados: ${result.size()}")
            allData.clear()

            for (d in result.documents) {
                val df = d.toObject(DataFields::class.java) ?: DataFields()

                val cantidad = (d.getDouble("cantidad")
                    ?: d.getLong("cantidad")?.toDouble()
                    ?: d.getDouble("quantity")
                    ?: d.getLong("quantity")?.toDouble()
                    ?: df.quantity)

                val ubicacion = (d.getString("ubicacion")
                    ?: d.getString("location")
                    ?: df.location)

                val unidad = (d.getString("unidadMedida")
                    ?: d.getString("unidad")
                    ?: df.unidadMedida)

                val sku = when {
                    df.sku.isNotBlank() -> df.sku
                    !d.getString("codigoProducto").isNullOrBlank() -> d.getString("codigoProducto")!!
                    else -> ""
                }

                val descripcion = when {
                    df.description.isNotBlank() -> df.description
                    !d.getString("descripcion").isNullOrBlank() -> d.getString("descripcion")!!
                    else -> ""
                }

                val fecha = d.getTimestamp("fechaRegistro")
                    ?: d.getTimestamp("fecha")
                    ?: Timestamp.now()

                val usuarioNombre = d.getString("usuarioNombre")
                    ?: d.getString("usuario")
                    ?: df.usuario

                allData.add(
                    df.copy(
                        documentId     = d.id,
                        location       = ubicacion,
                        sku            = sku,
                        lote           = d.getString("lote") ?: df.lote,
                        expirationDate = d.getString("fechaVencimiento") ?: df.expirationDate,
                        quantity       = cantidad,
                        description    = descripcion,
                        unidadMedida   = unidad,
                        fechaRegistro  = fecha,
                        usuario        = usuarioNombre,
                        localidad      = loc
                    )
                )
            }

            Log.d("Firestore", "✅ Registros cargados: ${allData.size}")
            if (allData.isNotEmpty()) {
                CoroutineScope(Dispatchers.Main).launch {
                    listState.scrollToItem(0)
                }
            }
        }
        .addOnFailureListener { e ->
            Log.e("Firestore", "❌ Error al obtener datos: $e")
        }
}
