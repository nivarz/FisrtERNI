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

fun saveToFirestore(
    db: FirebaseFirestore,
    location: String,
    sku: String,
    description: String,
    lote: String,
    expirationDate: String,
    quantity: Double,
    unidadMedida: String,
    allData: MutableList<DataFields>,
    usuario: String,
    coroutineScope: CoroutineScope,
    localidad: String,
    userViewModel: UserViewModel,
    showSuccessDialog: MutableState<Boolean>,
    listState: LazyListState,
    fotoUrl: String? = null
) {
    val cid = (userViewModel.clienteId.value ?: "").trim().uppercase()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // 🔗 Referencia a subcolección por cliente
    val invRef = db.collection("clientes").document(cid).collection("inventario")

    val data = hashMapOf(
        "clienteId" to cid,
        "creadoPorUid" to uid,
        "usuario" to usuario.trim(),

        "localidad" to localidad.trim().uppercase(),
        "ubicacion" to location.trim().uppercase(),
        "codigoProducto" to sku.trim().uppercase(),
        "lote" to (lote.ifBlank { "-" }.trim().uppercase()),

        "fecha" to FieldValue.serverTimestamp(),
        "fechaRegistro" to FieldValue.serverTimestamp(),
        "creadoEn" to FieldValue.serverTimestamp(),

        "descripcion" to description,
        "cantidad" to quantity,
        "unidadMedida" to unidadMedida,
        "fechaVencimiento" to expirationDate,
        "tipoUsuarioCreador" to userViewModel.tipo.value.orEmpty(),
        "fotoUrl" to (fotoUrl ?: "")
    )
    Log.d("FirestoreSave", "Data a guardar → $data")

    invRef.add(data)   // 👈 ahora guarda en /clientes/{cid}/inventario
        .addOnSuccessListener { ref ->
            Log.i("FirestoreSave", "✅ Guardado. DocID: ${ref.id}")

            // (tu fetch deberá leer también desde /clientes/{cid}/inventario)
            fetchDataFromFirestore(
                db = db,
                allData = allData,
                usuario = usuario,
                listState = listState,
                localidad = localidad,
                clienteId = userViewModel.clienteId.value.orEmpty()
            )

            coroutineScope.launch { showSuccessDialog.value = true }
        }
        .addOnFailureListener { e ->
            Log.e("FirestoreSave", "❌ Error al guardar", e)
        }
}

