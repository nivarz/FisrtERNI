package com.eriknivar.firebasedatabase.work

import android.content.Context
import android.net.Uri
import androidx.work.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

class PhotoUploadWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val clienteId = inputData.getString(KEY_CLIENTE_ID) ?: return Result.failure()
        val docPath = inputData.getString(KEY_DOC_PATH) ?: return Result.failure()
        val uriStrings = inputData.getStringArray(KEY_URIS) ?: emptyArray()

        if (uriStrings.isEmpty()) return Result.success()

        val db = FirebaseFirestore.getInstance()
        val st = FirebaseStorage.getInstance().reference
        val docRef = db.document(docPath)

        // Marcar "subiendo"
        docRef.update("fotoEstado", "subiendo").await()

        val urls = mutableListOf<String>()

        try {
            uriStrings.forEachIndexed { idx, s ->
                val uri = Uri.parse(s)
                val fileName = "${UUID.randomUUID()}_${idx}.jpg"
                val ref = st.child("clientes/$clienteId/fotos/${docRef.id}/$fileName")
                val put = ref.putFile(uri).await()
                val url = ref.downloadUrl.await().toString()
                urls += url
            }

            val remoteUrl = urls.firstOrNull() ?: ""

            // A) marca "subiendo" (visibilidad inmediata en UI)
            docRef.update("fotoEstado", "subiendo").await()


            // B) update mínimo que SIEMPRE debe pasar reglas
            docRef.update(
                mapOf(
                    "fotoUrl" to remoteUrl,
                    "fotoPendiente" to false
                )
            ).await()

            // C) best-effort (si reglas dejan): estado final
            runCatching {
                docRef.update(mapOf("fotoEstado" to "subida")).await()
            }


            // Todo ok
            return Result.success()


        } catch (e: Exception) {
            // Marca estado de error sin fallar si reglas no dejan
            runCatching {
                docRef.update(
                    mapOf(
                        "fotoPendiente" to true,
                        "fotoEstado" to "error"
                    )
                ).await()
            }
            return Result.retry()
        }
    }

    companion object {
        const val KEY_CLIENTE_ID = "clienteId"
        const val KEY_DOC_PATH = "docPath"      // ej: clientes/{cid}/inventario/{docId}
        const val KEY_URIS = "uris"
    }
}

fun enqueuePhotoUpload(
    context: Context,
    clienteId: String,
    docPath: String,
    uris: List<String>
) {
    if (uris.isEmpty()) return

    val data = workDataOf(
        PhotoUploadWorker.KEY_CLIENTE_ID to clienteId,
        PhotoUploadWorker.KEY_DOC_PATH to docPath,
        PhotoUploadWorker.KEY_URIS to uris.toTypedArray()
    )

    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    val req = OneTimeWorkRequestBuilder<PhotoUploadWorker>()
        .setInputData(data)
        .setConstraints(constraints)
        .setBackoffCriteria(
            BackoffPolicy.EXPONENTIAL,
            30_000L,
            java.util.concurrent.TimeUnit.MILLISECONDS
        )
        .addTag("photo-upload")
        .build()

    WorkManager.getInstance(context).enqueue(req)
}
