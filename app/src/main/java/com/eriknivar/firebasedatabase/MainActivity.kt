package com.eriknivar.firebasedatabase

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController



class MainActivity : ComponentActivity() {

    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Log.d("Permisos", "Permiso de cámara concedido") // ✅ Mensaje en la consola
                // Permiso concedido, puedes realizar la acción necesaria
            } else {
                Log.d("Permisos", "Permiso de cámara denegado") // ❌ Mensaje en la consola

                // Permiso denegado, maneja el caso apropiadamente
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {

            MyApp(requestCameraPermissionLauncher)
        }
    }
}


@Composable
fun MyApp(requestCameraPermissionLauncher: ActivityResultLauncher<String>) {
    val navController = rememberNavController()
    LocalContext.current

    LaunchedEffect(Unit) {
        // Aquí puedes realizar las operaciones de inicialización asíncronas
        requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // Configura tu NavHostController y otras configuraciones iniciales de tu aplicación
    NavGraph(navController)
}

