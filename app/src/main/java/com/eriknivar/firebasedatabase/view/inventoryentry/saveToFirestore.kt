package com.eriknivar.firebasedatabase.view.inventoryentry

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.MutableState
import com.eriknivar.firebasedatabase.view.storagetype.DataFields
import com.eriknivar.firebasedatabase.viewmodel.UserViewModel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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
    listState: LazyListState
) {

    val data = hashMapOf(
        "ubicacion" to location,
        "codigoProducto" to sku,
        "descripcion" to description,
        "lote" to lote,
        "fechaVencimiento" to expirationDate,
        "cantidad" to quantity,
        "unidadMedida" to unidadMedida,
        "fechaRegistro" to Timestamp.now(),
        "usuario" to usuario,
        "localidad" to localidad,
        "tipoUsuarioCreador" to userViewModel.tipo.value.orEmpty()
    )

    db.collection("inventario")
        .add(data)
        .addOnSuccessListener {
            // ✅ Recarga los datos desde Firestore para evitar duplicados visuales
            fetchDataFromFirestore(
                db = db,
                allData = allData,
                usuario = usuario,
                listState = listState,
                localidad = localidad
            )

            coroutineScope.launch {
                showSuccessDialog.value = true
            }
        }
        .addOnFailureListener { e ->
            println("Error al guardar: $e")
        }
}
