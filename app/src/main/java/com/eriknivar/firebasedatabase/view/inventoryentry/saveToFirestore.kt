package com.eriknivar.firebasedatabase.view.inventoryentry

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
    unidadMedida: String, // 🆕 Guardamos la UM en Firestore
    allData: MutableList<DataFields>,
    usuario: String,
    coroutineScope: CoroutineScope,
    localidad: String,
    tipoUsuarioCreador: String,
    userViewModel: UserViewModel,
    showSuccessDialog: MutableState<Boolean>


) {

    val data = hashMapOf(
        "ubicacion" to location,
        "codigoProducto" to sku,
        "descripcion" to description,
        "lote" to lote,
        "fechaVencimiento" to expirationDate,
        "cantidad" to quantity,
        "unidadMedida" to unidadMedida, // 🆕 Guardamos la UM en Firestore
        "fechaRegistro" to Timestamp.now(),
        "usuario" to usuario,
        "localidad" to localidad,
        "tipoUsuarioCreador" to userViewModel.tipo.value.orEmpty() // ✅ aquí va

    )

    db.collection("inventario")
        .add(data)
        .addOnSuccessListener { documentReference ->
            allData.add(
                DataFields(
                    documentReference.id,
                    location,
                    sku,
                    lote,
                    expirationDate,
                    quantity,
                    description,
                    unidadMedida, // 🆕 Guardamos la UM en Firestore
                    Timestamp.now(),
                    usuario,
                    localidad,
                    tipoUsuarioCreador

                )
            )

            // CAMBIAR POR UN ALERT DIALOG QUE MUESTRE QUE SE GUARDO CORRECTAMENTE Y DURE 2 SEGUNDOS


            // ✅ Muestra el snackbar desde un scope seguro
            coroutineScope.launch {
                showSuccessDialog.value = true

            }

        }
        .addOnFailureListener { e ->
            println("Error al guardar: $e")
        }
}