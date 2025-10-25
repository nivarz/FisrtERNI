package com.eriknivar.firebasedatabase.view.utility

import android.Manifest
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

@Composable
fun EditableProfileImage(
    userName: String,
    documentId: String
) {
    val context = LocalContext.current
    val storage = FirebaseStorage.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    var fotoUrl by remember { mutableStateOf<String?>(null) }

    // === Selector de imagen ===
    val contentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            if (documentId.isBlank()) {
                Log.e("FOTO_DEBUG", "❌ documentId vacío, no se puede subir imagen.")
                Toast.makeText(context, "Error: ID de usuario vacío", Toast.LENGTH_SHORT).show()
                return@let
            }

            val imageRef = storage.reference.child("usuarios/$userName/perfil.jpg")

            imageRef.putFile(it)
                .addOnSuccessListener {
                    Log.d("FOTO_DEBUG", "✅ Imagen subida correctamente a Storage")

                    imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        fotoUrl = downloadUri.toString()
                        Log.d("FOTO_DEBUG", "📸 Nueva URL: $fotoUrl")

                        firestore.collection("usuarios")
                            .document(documentId)
                            .update("fotoUrl", fotoUrl)
                            .addOnSuccessListener {
                                Log.d("FOTO_DEBUG", "✅ fotoUrl actualizado en Firestore")
                            }
                            .addOnFailureListener { e ->
                                Log.e("FOTO_DEBUG", "❌ Error al actualizar Firestore", e)
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("FOTO_DEBUG", "❌ Error al subir imagen a Storage", e)
                }
        }
    }

    // === Permisos ===
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            contentPicker.launch("image/*")
        } else {
            Toast.makeText(context, "Permiso denegado", Toast.LENGTH_SHORT).show()
        }
    }

    // === Recuperar la imagen guardada en Firestore ===
    LaunchedEffect(documentId) {
        if (documentId.isBlank()) {
            Log.w("FOTO_DEBUG", "⚠️ documentId vacío: se omite la recuperación de fotoUrl.")
            return@LaunchedEffect
        }

        firestore.collection("usuarios")
            .document(documentId)
            .get()
            .addOnSuccessListener { doc ->
                val url = doc.getString("fotoUrl")
                fotoUrl = url
                Log.d("FOTO_DEBUG", "📥 fotoUrl recuperado: $url")
            }
            .addOnFailureListener {
                Log.e("FOTO_DEBUG", "❌ Error al recuperar fotoUrl", it)
            }
    }

    // === Animación ===
    val scale by animateFloatAsState(
        targetValue = if (fotoUrl != null) 1.1f else 1f,
        label = "ZoomAnim"
    )

    // === Diseño del componente ===
    Box(
        modifier = Modifier.size(70.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(Color.LightGray)
                .graphicsLayer(scaleX = scale, scaleY = scale),
            contentAlignment = Alignment.Center
        ) {
            if (fotoUrl != null) {
                AsyncImage(
                    model = fotoUrl,
                    contentDescription = "Foto de perfil",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Sin imagen",
                    tint = Color.DarkGray,
                    modifier = Modifier.size(50.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 4.dp, y = 4.dp)
                .background(Color(0xFF1C1C1C), CircleShape)
                .size(28.dp)
                .clickable { permissionLauncher.launch(permission) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "Cambiar foto",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
