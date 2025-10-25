package com.eriknivar.firebasedatabase

import android.app.Application
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.storage.FirebaseStorage
import java.util.concurrent.TimeUnit

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val firestore = FirebaseFirestore.getInstance()

        // ✅ Configuración de Firestore con caché persistente sin errores
        val cacheSettings = PersistentCacheSettings.newBuilder()
            .build() // 🔥 No es necesario CACHE_SIZE_UNLIMITED, se usa el valor por defecto

        firestore.firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(cacheSettings) // ✅ Usa la nueva forma correcta
            .build()

        // ✅ Reintentos/timeout de Firebase Storage (barato y seguro)
        FirebaseStorage.getInstance().apply {
            maxUploadRetryTimeMillis    = TimeUnit.MINUTES.toMillis(2)
            maxOperationRetryTimeMillis = TimeUnit.MINUTES.toMillis(2)
            maxDownloadRetryTimeMillis  = TimeUnit.MINUTES.toMillis(1)
        }
    }
}