package com.eriknivar.firebasedatabase.view.utility

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.util.Log
import androidx.core.app.NotificationCompat
import com.eriknivar.firebasedatabase.MainActivity
import com.eriknivar.firebasedatabase.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val titulo = remoteMessage.notification?.title ?: "Notificación"
        val cuerpo = remoteMessage.notification?.body ?: "Tienes un nuevo mensaje"

        Log.d("FCM", "📩 Notificación recibida: $cuerpo")

        // ✅ Mostrar notificación manual si la app está abierta
        val channelId = "canal_fcm_id"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crear canal si es Android 8+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                channelId,
                "Canal de Notificaciones FCM",
                NotificationManager.IMPORTANCE_HIGH
            )
            canal.description = "Canal usado para mensajes push"
            notificationManager.createNotificationChannel(canal)
        }

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // 👈 Usa tu ícono blanco aquí
            .setContentTitle(titulo)
            .setContentText(cuerpo)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        // Mostrar la notificación
        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }


    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "🔄 Nuevo token generado: $token")

        val userId = obtenerUsuarioIdActual()

        if (userId != null) {
            FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(userId)
                .update("token", token)
                .addOnSuccessListener {
                    Log.d("FCM", "✅ Token actualizado en Firestore")
                }
                .addOnFailureListener {
                    Log.e("FCM", "❌ Error al actualizar token", it)
                }
        }
    }

    private fun obtenerUsuarioIdActual(): String? {
        val user = FirebaseAuth.getInstance().currentUser
        return user?.uid
    }

    private fun showNotification(title: String, message: String) {
        val channelId = "reconteo_channel_id"
        val notificationId = System.currentTimeMillis().toInt()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crear canal si es necesario
        val channel = NotificationChannel(
            channelId,
            "Canal de Reconteo",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)

        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}