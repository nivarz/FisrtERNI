package com.eriknivar.firebasedatabase.view.imagecount

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import java.io.File

@Composable
fun ImageCountScreen(navController: NavHostController) {
    val context = LocalContext.current
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var boxCount by remember { mutableIntStateOf(0) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            // La imagen se ha guardado correctamente
        }
    }

    val tempImageFile = remember {
        File.createTempFile("IMG_", ".jpg", context.cacheDir).apply {
            createNewFile()
            deleteOnExit()
        }
    }

    val photoUri = FileProvider.getUriForFile(
        context,
        context.packageName + ".provider",
        tempImageFile
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Contar cajas tocando en la imagen")

        Button(onClick = {
            imageUri = photoUri
            cameraLauncher.launch(photoUri)
        }) {
            Text("Tomar Foto")
        }

        imageUri?.let {
            Image(
                painter = rememberAsyncImagePainter(model = it),
                contentDescription = "Foto de estiba",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clickable {
                        boxCount++
                    }
            )
        }

        Text("Cantidad de cajas: $boxCount")

        Button(onClick = {
            navController.popBackStack()
        }) {
            Text("Volver")
        }
    }
}
