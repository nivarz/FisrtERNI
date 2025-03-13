package com.eriknivar.firebasedatabase.view.inventoryentry

import com.eriknivar.firebasedatabase.view.storagetype.DataFields
import com.google.firebase.firestore.FirebaseFirestore

fun deleteFromFirestore(
    db: FirebaseFirestore,
    documentId: String,
    allData: MutableList<DataFields>,
    onDeletionComplete: () -> Unit
) {
    db.collection("inventario").document(documentId)
        .delete()
        .addOnSuccessListener {
            allData.removeAll { it.documentId == documentId }
            onDeletionComplete() // Llamar a la función después de la eliminación
        }
        .addOnFailureListener { e ->
            println("Error al borrar: $e")
        }
}
