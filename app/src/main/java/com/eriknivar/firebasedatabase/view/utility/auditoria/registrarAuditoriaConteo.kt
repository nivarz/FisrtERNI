import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue

fun registrarAuditoriaConteo(
    clienteId: String,
    registroId: String,
    tipoAccion: String,
    usuarioNombre: String,
    usuarioUid: String?,
    valoresAntes: Map<String, Any?>? = null,
    valoresDespues: Map<String, Any?>? = null
) {
    val db = FirebaseFirestore.getInstance()

    val data = hashMapOf<String, Any>(
        "clienteId" to clienteId,
        "registro_id" to registroId,
        "tipo_accion" to tipoAccion,
        "usuarioNombre" to usuarioNombre,
        "usuarioUid" to (usuarioUid ?: ""),
        "fecha" to FieldValue.serverTimestamp() // ⬅️ servidor, no reloj del móvil
    ).apply {
        valoresAntes?.let { put("valores_antes", it) }
        valoresDespues?.let { put("valores_despues", it) }
    }

    db.collection("clientes").document(clienteId)
        .collection("auditoria_conteos")
        .add(data)
        .addOnSuccessListener { Log.d("Auditoría", "OK") }
        .addOnFailureListener { e -> Log.e("Auditoría", "Error", e) }
}


