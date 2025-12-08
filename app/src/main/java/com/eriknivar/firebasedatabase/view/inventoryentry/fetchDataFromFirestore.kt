package com.eriknivar.firebasedatabase.view.inventoryentry

import android.util.Log
import androidx.compose.foundation.lazy.LazyListState
import com.eriknivar.firebasedatabase.view.storagetype.DataFields
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
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

// ANCLA: reemplazo total de fetchDataFromFirestore(...)
fun fetchDataFromFirestore(
    db: FirebaseFirestore,
    allData: MutableList<DataFields>,
    usuario: String,                 // no se usa (ok)
    listState: LazyListState,
    localidad: String,
    clienteId: String,
    tipo: String,                    // no se usa (ok)
    uid: String
) {
    val cid = clienteId.trim().uppercase(Locale.ROOT)
    val loc = localidad.trim().uppercase(Locale.ROOT)

    val hoyStr = java.text.SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        .format(java.util.Date())

    val q: Query = db.collection("clientes").document(cid).collection("inventario")
        .whereEqualTo("localidad", loc).whereEqualTo("dia", hoyStr).whereEqualTo("usuarioUid", uid)
        .orderBy("fechaCliente", Query.Direction.DESCENDING)

    q.get().addOnSuccessListener { result ->
            Log.d("Firestore", "🔍 Documentos encontrados: ${result.size()}")
            allData.clear()

            for (d in result.documents) {
                val df = d.toObject(DataFields::class.java) ?: DataFields()

                val cantidad = (d.getDouble("cantidad") ?: d.getLong("cantidad")?.toDouble()
                ?: d.getDouble("quantity") ?: d.getLong("quantity")?.toDouble() ?: df.quantity)

                val ubicacion = (d.getString("ubicacion") ?: d.getString("location") ?: df.location)

                val unidad =
                    (d.getString("unidadMedida") ?: d.getString("unidad") ?: df.unidadMedida)

                val sku = when {
                    df.sku.isNotBlank() -> df.sku
                    !d.getString("codigoProducto")
                        .isNullOrBlank() -> d.getString("codigoProducto")!!

                    else -> ""
                }

                val descripcion = when {
                    df.description.isNotBlank() -> df.description
                    !d.getString("descripcion").isNullOrBlank() -> d.getString("descripcion")!!
                    else -> ""
                }

                val fecha =
                    d.getTimestamp("fechaRegistro") ?: d.getTimestamp("fecha") ?: Timestamp.now()

                val usuarioNombre =
                    d.getString("usuarioNombre") ?: d.getString("usuario") ?: df.usuario

                // --- FOTOS (remota y locales)
                val fotosArray: List<String> = when (val raw = d.get("fotoUrls")) {
                    is List<*> -> raw.mapNotNull { it?.toString() }
                    else -> emptyList()
                }

                val fotoRemota = (
                        d.getString("fotoUrl")
                            ?: fotosArray.firstOrNull()
                            ?: d.getString("foto_url")
                            ?: d.getString("imageUrl")
                            ?: df.fotoUrl
                        ).orEmpty()

                val fotoUriLocal = d.getString("fotoUriLocal")
                    ?: d.getString("foto_url_local")
                    ?: d.getString("localPhotoUri")
                    ?: df.fotoUriLocal

                val fotoUrisLocales: List<String> = when (val raw = d.get("fotoUrisLocales") ?: d.get("fotosLocales")) {
                    is List<*> -> raw.mapNotNull { it?.toString() }
                    else -> df.fotoUrisLocales ?: emptyList()
                }

                val fotoEstado = d.getString("fotoEstado").orEmpty()
                val fotoPendiente = when (val v = d.get("fotoPendiente")) {
                    is Boolean -> v
                    is String -> v.equals("true", true)
                    else -> false
                }


                allData.add(
                    df.copy(
                        documentId = d.id,
                        location = ubicacion,
                        sku = sku,
                        lote = d.getString("lote") ?: df.lote,
                        expirationDate = d.getString("fechaVencimiento") ?: df.expirationDate,
                        quantity = cantidad,
                        description = descripcion,
                        unidadMedida = unidad,
                        fechaRegistro = fecha,
                        usuario = usuarioNombre,
                        localidad = loc,
                        fotoUrl = fotoRemota,
                        fotoEstado = fotoEstado,
                        fotoUriLocal = fotoUriLocal,
                        fotoUrisLocales = fotoUrisLocales,


                        )
                )
            }

            Log.d("Firestore", "✅ Registros cargados: ${allData.size}")
            if (allData.isNotEmpty()) {
                CoroutineScope(Dispatchers.Main).launch {
                    listState.scrollToItem(0)
                }
            }
        }.addOnFailureListener { e ->
            Log.e("Firestore", "❌ Error al obtener datos: $e")
        }
}