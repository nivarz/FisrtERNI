package com.eriknivar.firebasedatabase.view.inventoryentry

import android.util.Log
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.MutableState
import com.eriknivar.firebasedatabase.view.storagetype.DataFields
import com.eriknivar.firebasedatabase.viewmodel.UserViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import java.util.Calendar
import java.util.Locale

fun saveToFirestore(
    db: FirebaseFirestore,
    location: String,
    sku: String,
    description: String,
    lote: String,
    expirationDate: String,
    quantity: Double,
    unidadMedida: String,
    allData: MutableList<DataFields>,      // ⬅️ se mantiene por compatibilidad (no se usa aquí)
    usuario: String,                       // nombre visible en UI
    coroutineScope: CoroutineScope,
    localidad: String,
    userViewModel: UserViewModel,
    showSuccessDialog: MutableState<Boolean>,
    listState: LazyListState,              // ⬅️ se mantiene por compatibilidad (no se usa aquí)
    fotoUrl: String? = null
) {
    val cid = (userViewModel.clienteId.value ?: "").trim().uppercase(Locale.ROOT)
    val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    // /clientes/{cid}/inventario
    val invRef = db.collection("clientes").document(cid).collection("inventario")

    // “sin lote/fecha” => guarda guiones
    val loteFinal = lote.trim().uppercase(Locale.ROOT).ifBlank { "-" }
    val vencFinal = expirationDate.trim().ifBlank { "-" }

    val cal = Calendar.getInstance()          // hora local del dispositivo
    val diaHoy = cal.get(Calendar.DAY_OF_MONTH)  // <-- 1..31 (entero)

    // arriba del data:
    val hoyStr = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault())
        .format(java.util.Date())

    val data = hashMapOf(
        // Identificación
        "clienteId" to cid,
        "localidad" to localidad.trim().uppercase(Locale.ROOT),
        "ubicacion" to location.trim().uppercase(Locale.ROOT),

        // Producto
        "codigoProducto" to sku.trim().uppercase(Locale.ROOT),
        "descripcion" to description,
        //"unidad" to unidadMedida,           // alias corto (legacy)
        "unidadMedida" to unidadMedida,     // alias usado en UI/lista

        // Lote / Cantidad
        "lote" to (lote.ifBlank { "-" }.trim().uppercase(Locale.ROOT)),
        "fechaVencimiento" to vencFinal,
        "cantidad" to quantity,

        // Auditoría de usuario
        "usuarioUid" to uid,
        "usuarioNombre" to usuario.trim(),
        "tipoUsuarioCreador" to userViewModel.tipo.value.orEmpty(),

        // Timestamps
        "fecha" to FieldValue.serverTimestamp(),
        "fechaRegistro" to FieldValue.serverTimestamp(),
        "creadoEn" to FieldValue.serverTimestamp(),

        // Ayuda para filtros del día
        "dia" to hoyStr,
        "fechaCliente" to FieldValue.serverTimestamp(),

        // Foto (si llegó)
        "fotoUrl" to (fotoUrl ?: ""),

    )

    Log.d("FirestoreSave", "Data a guardar → $data")

    invRef.add(data)
        .addOnSuccessListener { ref ->
            Log.i("FirestoreSave", "✅ Guardado. DocID: ${ref.id}")
            // ❌ Ya no forzamos recarga: el listener realtime actualizará allData y el contador.
            coroutineScope.launch { showSuccessDialog.value = true }
        }
        .addOnFailureListener { e ->
            Log.e("FirestoreSave", "❌ Error al guardar", e)
        }
}
