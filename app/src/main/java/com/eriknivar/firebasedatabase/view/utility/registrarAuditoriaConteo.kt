import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore

fun registrarAuditoriaConteo(
    registroId: String,
    tipoAccion: String,
    usuario: String,
    valoresAntes: Map<String, Any?>? = null,
    valoresDespues: Map<String, Any?>? = null
) {
    val firestore = FirebaseFirestore.getInstance()

    val data = hashMapOf<String, Any>(
        "registro_id" to registroId,
        "tipo_accion" to tipoAccion,
        "usuario" to usuario,
        "fecha" to Timestamp.now()
    )

    valoresAntes?.let {
        data.put("valores_antes", it)
    }

    valoresDespues?.let {
        data.put("valores_despues", it)
    }


    firestore.collection("auditoria_conteos")
        .add(data)
        .addOnSuccessListener {
            Log.d("Auditoría", "Auditoría registrada correctamente.")
        }
        .addOnFailureListener { e ->
            Log.e("Auditoría", "Error al registrar auditoría", e)
        }
}

