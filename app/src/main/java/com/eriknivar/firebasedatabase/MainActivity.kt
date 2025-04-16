package com.eriknivar.firebasedatabase

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.navigation.compose.rememberNavController
import com.eriknivar.firebasedatabase.view.utility.InactivityHandler
import com.eriknivar.firebasedatabase.viewmodel.UserViewModel


class MainActivity : ComponentActivity() {

    private lateinit var inactivityHandler: InactivityHandler

    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Log.d("Permisos", "Permiso de cámara concedido")
            } else {
                Log.d("Permisos", "Permiso de cámara denegado")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val userViewModel = remember { UserViewModel() }

            inactivityHandler = InactivityHandler(600_000) {



                userViewModel.logout()
            }

            MyApp(
                requestCameraPermissionLauncher = requestCameraPermissionLauncher,
                inactivityHandler = inactivityHandler,
                userViewModel = userViewModel
            )
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (::inactivityHandler.isInitialized) {
            inactivityHandler.userInteracted()
        }
        return super.dispatchTouchEvent(ev)
    }
}

@Composable
fun MyApp(
    requestCameraPermissionLauncher: ActivityResultLauncher<String>,
    inactivityHandler: InactivityHandler,
    userViewModel: UserViewModel
) {
    val navController = rememberNavController()


    // Solicita permiso de cámara y arranca el timer
    LaunchedEffect(Unit) {
        requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        inactivityHandler.startTimer()
    }

    // Detecta si el usuario fue deslogueado
    val isLoggedOut = userViewModel.nombre.observeAsState("").value.isEmpty()

    LaunchedEffect(isLoggedOut) {
        if (isLoggedOut) {
            navController.navigate("login") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    // Navegación principal con detección de red
    NetworkAwareNavGraph(
        navController = navController,
        userViewModel = userViewModel
    )
}






