package com.eriknivar.firebasedatabase.view.utility

import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import java.text.Normalizer
import java.util.Locale

object UserUtils {
    fun normalizarNombre(s: String?): String {
        if (s.isNullOrBlank()) return ""
        val tmp = Normalizer.normalize(s.trim(), Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        return tmp.uppercase(Locale.ROOT)
    }
    fun limpiarEmail(s: String?): String = s?.trim()?.lowercase(Locale.ROOT).orEmpty()
}

data class UsuarioCreateInput(
    val nombre: String,
    val email: String,
    val tipo: String,             // "admin" | "invitado"
    val clienteId: String,
    val creadoPorUid: String
)

data class UsuarioUpdateInput(
    val uidDoc: String,           // id del documento en /usuarios (no Auth)
    val nombre: String? = null,
    val email: String? = null,
    val tipo: String? = null,     // solo superuser cambiaría el rol
    val clienteId: String? = null,// solo superuser cambiaría cliente
    val actualizadoPorUid: String
)

class UsuariosRepository {

    private val db = Firebase.firestore

    suspend fun crearUsuarioDoc(input: UsuarioCreateInput): String {
        // Validaciones mínimas
        require(input.nombre.isNotBlank()) { "El nombre es obligatorio" }
        require(input.email.isNotBlank()) { "El email es obligatorio" }
        require(input.tipo in listOf("admin", "invitado")) { "Rol inválido" }
        require(input.clienteId.isNotBlank()) { "clienteId es obligatorio" }

        val docRef = db.collection("usuarios").document() // id autogenerado
        val now = FieldValue.serverTimestamp()

        val data = hashMapOf(
            // identidad app (NO Auth todavía)
            "authUid" to null, // quedará nulo hasta que creemos cuenta de Auth
            "clienteId" to input.clienteId,

            // perfil
            "nombre" to input.nombre.trim(),
            "nombreNormalizado" to UserUtils.normalizarNombre(input.nombre),
            "email" to UserUtils.limpiarEmail(input.email),
            "tipo" to input.tipo,             // "admin" | "invitado"
            "estado" to "activo",

            // flags útiles
            "requiereCambioPassword" to true,
            "sessionId" to "",
            "token" to "",

            // auditoría
            "creadoPorUid" to input.creadoPorUid,
            "creadoEn" to now,
            "actualizadoPorUid" to input.creadoPorUid,
            "actualizadoEn" to now
        )

        docRef.set(data, SetOptions.merge()).await()
        return docRef.id
    }

    suspend fun actualizarUsuarioDoc(input: UsuarioUpdateInput) {
        val updates = mutableMapOf<String, Any?>()
        input.nombre?.let {
            updates["nombre"] = it.trim()
            updates["nombreNormalizado"] = UserUtils.normalizarNombre(it)
        }
        input.email?.let { updates["email"] = UserUtils.limpiarEmail(it) }
        input.tipo?.let { updates["tipo"] = it }
        input.clienteId?.let { updates["clienteId"] = it }

        updates["actualizadoPorUid"] = input.actualizadoPorUid
        updates["actualizadoEn"] = FieldValue.serverTimestamp()

        db.collection("usuarios").document(input.uidDoc).update(updates).await()
    }

    suspend fun borrarUsuarioDoc(uidDoc: String) {
        db.collection("usuarios").document(uidDoc).delete().await()
    }

    // Listado simple (para superuser: todos; para admin usar where("clienteId","==", su cliente))
    suspend fun listarUsuarios(clienteIdFilter: String? = null): List<Map<String, Any?>> {
        var q = db.collection("usuarios")
            .orderBy("nombreNormalizado")

        if (!clienteIdFilter.isNullOrBlank()) {
            q = q.whereEqualTo("clienteId", clienteIdFilter)
        }

        val snap = q.get().await()
        return snap.documents.map { it.data!! + mapOf("uidDoc" to it.id) }
    }
}
