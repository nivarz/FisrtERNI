package com.eriknivar.firebasedatabase.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object UserRepo {
    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()

    fun cargarPerfil(onResult: (ok: Boolean, perfil: Map<String, Any?>, msg: String) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) { onResult(false, emptyMap(), "Sin sesión"); return }
        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { d ->
                onResult(true, d.data ?: emptyMap(), "OK")
            }
            .addOnFailureListener { e ->
                onResult(false, emptyMap(), e.message ?: "No pude leer /usuarios/$uid")
            }
    }
}
