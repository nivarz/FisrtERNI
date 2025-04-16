package com.eriknivar.firebasedatabase.view.utility

import android.Manifest
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter

@Composable
fun EditableProfileImage(userName: String) {
    val context = LocalContext.current
    val imageManager = remember { ProfileImageManager(context) }

    var selectedImageUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    // Cargar imagen del usuario una vez
    LaunchedEffect(userName) {
        imageManager.getImageUri(userName)?.let { savedUri ->
            selectedImageUri = Uri.parse(savedUri)
        }
    }

    val contentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            imageManager.saveImageUri(userName, it.toString())
        }
    }

    // 📱 Permiso dinámico según la versión
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
            Toast.makeText(context, "Permiso denegado para acceder a imágenes", Toast.LENGTH_SHORT).show()
        }
    }

    // ✅ Animación de zoom solo si hay imagen seleccionada
    val scale by animateFloatAsState(
        targetValue = if (selectedImageUri != null) 1.1f else 1f,
        label = "ZoomAnim"
    )

    Box(
        modifier = Modifier.size(70.dp),
        contentAlignment = Alignment.Center
    ) {
        // Fondo circular
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(Color.LightGray)
                .graphicsLayer(scaleX = scale, scaleY = scale), // ✅ Zoom dinámico
            contentAlignment = Alignment.Center
        ) {
            if (selectedImageUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(selectedImageUri),
                    contentDescription = "Imagen de perfil",
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

        // ✅ Botón con ícono de cámara (clickable)
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
