package com.eriknivar.firebasedatabase.data

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore

object Refs {
    fun inv(db: FirebaseFirestore, cid: String): CollectionReference =
        db.collection("clientes").document(cid).collection("inventario")
    fun ubic(db: FirebaseFirestore, cid: String): CollectionReference =
        db.collection("clientes").document(cid).collection("ubicaciones")
    fun prod(db: FirebaseFirestore, cid: String): CollectionReference =
        db.collection("clientes").document(cid).collection("productos")
    fun loc(db: FirebaseFirestore, cid: String): CollectionReference =
        db.collection("clientes").document(cid).collection("localidades")
}
