package com.eriknivar.firebasedatabase.view.inventoryentry

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.MutableState
import com.eriknivar.firebasedatabase.view.storagetype.DataFields
import com.eriknivar.firebasedatabase.viewmodel.UserViewModel
import com.eriknivar.firebasedatabase.work.enqueuePhotoUpload
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
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
    allData: MutableList<DataFields>,        // compat
    usuario: String,
    coroutineScope: CoroutineScope,
    localidad: String,
    userViewModel: UserViewModel,
    showSuccessDialog: MutableState<Boolean>,
    listState: LazyListState,                // compat
    fotoUrl: String?,                        // no se usa aquí (upload asíncrono)
    hadPhoto: Boolean,
    fotoUriLocal: String?,                   // uri local para preview y worker
    appContext: Context
) {
    val cid = (userViewModel.clienteId.value ?: "").trim().uppercase(Locale.ROOT)
    val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    // Ruta correcta: clientes/{cid}/inventario
    val invRef = db.collection("clientes").document(cid).collection("inventario")

    val vencFinal = expirationDate.trim().ifBlank { "-" }
    val hoyStr = java.text.SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        .format(java.util.Date())

    // ---------- Construcción del documento (UNA sola vez) ----------
    val data = hashMapOf(
        // Identificación
        "clienteId" to cid,
        "localidad" to localidad.trim().uppercase(Locale.ROOT),
        "ubicacion" to location.trim().uppercase(Locale.ROOT),

        // Producto
        "codigoProducto" to sku.trim().uppercase(Locale.ROOT),
        "descripcion" to description,
        "unidadMedida" to unidadMedida,

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

        // Ayudas de filtrado/orden
        "dia" to hoyStr,
        "fechaCliente" to FieldValue.serverTimestamp()
    ).apply {
        // Estado de foto (una sola vez)
        // --- FOTOS (al crear) ---
        if (hadPhoto) {
            this["fotoPendiente"] = true            // ← clave para que NO sea un flash
            this["fotoEstado"] = "pendiente"        // "pendiente" | "subiendo" | "subida"/"ok" | "error"
            this["fotoUrl"] = ""                    // aún no hay URL remota
            fotoUriLocal?.let {
                this["fotoUriLocal"] = it           // preview local
                // opcional si guardas varias: this["fotoUrisLocales"] = listOf(it)
            }
        }
    }

    if (!fotoUriLocal.isNullOrBlank()) {
        Log.d("FotoDebug", "📤 Enviando a Firestore. uriLocal=$fotoUriLocal")
    } else {
        Log.d("FotoDebug", "📤 Enviando a Firestore. uriLocal=null")
    }

    Log.d("FotoDebug", "📤 Enviando a Firestore. uriLocal=${fotoUriLocal ?: "null"}")
    Log.d("FirestoreSave", "Data a guardar → $data")

    // ---------- Guardar (UNA sola vez) ----------
    invRef.add(data)
        .addOnSuccessListener { ref ->
            Log.i("FirestoreSave", "✅ Guardado. DocID: ${ref.id}")
            if (hadPhoto && !fotoUriLocal.isNullOrBlank()) {
                enqueuePhotoUpload(
                    context = appContext,
                    clienteId = cid,
                    docPath = ref.path,
                    uris = listOf(fotoUriLocal)
                )
            }
            coroutineScope.launch { showSuccessDialog.value = true }
        }
        .addOnFailureListener { e ->
            Log.e("FirestoreSave", "❌ Error al guardar", e)
        }

        .addOnFailureListener { e ->
            val msg = if (e is FirebaseFirestoreException) {
                "Firestore: ${e.code} — ${e.message}"
            } else {
                "Error al guardar: ${e.message}"
            }
            Log.e("FirestoreSave", "❌ $msg", e)
            Toast.makeText(appContext, msg, Toast.LENGTH_LONG).show()
        }
}
