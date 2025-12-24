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
import com.eriknivar.firebasedatabase.navigation.NetworkAwareNavGraph
import com.eriknivar.firebasedatabase.view.utility.InactivityHandler
import com.eriknivar.firebasedatabase.viewmodel.UserViewModel
import com.google.firebase.crashlytics.FirebaseCrashlytics


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

            inactivityHandler = InactivityHandler(1_800_000) {


                userViewModel.logout()
            }

            MyApp(
                requestCameraPermissionLauncher = requestCameraPermissionLauncher,
                inactivityHandler = inactivityHandler,
                userViewModel = userViewModel
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (::inactivityHandler.isInitialized) {
            inactivityHandler.startTimer()
        }
    }

    override fun onPause() {
        if (::inactivityHandler.isInitialized) {
            inactivityHandler.stopTimer()
        }
        super.onPause()
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

    // ==== Crashlytics: identificar al usuario logueado ====
    val crashUserId   = userViewModel.documentId.observeAsState("").value
    val crashCliente  = userViewModel.clienteId.observeAsState("").value
    val crashTipo     = userViewModel.tipo.observeAsState("").value
    val crashNombre   = userViewModel.nombre.observeAsState("").value   // ⬅️ nombre

    LaunchedEffect(crashUserId, crashCliente, crashTipo) {
        if (crashUserId.isNotBlank()) {
            val crash = FirebaseCrashlytics.getInstance()
            crash.setUserId(crashUserId)
            crash.setCustomKey("clienteId", crashCliente)
            crash.setCustomKey("tipoUsuario", crashTipo)
            crash.setCustomKey("nombreUsuario", crashNombre)  // ⬅️ nombre visible

        }
    }
    // =======================================================

    // Solicita permiso de cámara y arranca el timer
    LaunchedEffect(Unit) {
        requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        inactivityHandler.startTimer()
    }

    // Detecta si el usuario está deslogueado (nombre vacío)
    val isLoggedOut = userViewModel.nombre.observeAsState("").value.isEmpty()

    LaunchedEffect(isLoggedOut) {
        if (isLoggedOut) {
            // Pequeño delay para asegurarnos de que el NavHost ya montó el gráfico
            kotlinx.coroutines.delay(100)

            try {
                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            } catch (e: IllegalStateException) {
                Log.e("Nav", "Error navegando a login", e)
            }
        }
    }

    // Navegación principal con detección de red
    NetworkAwareNavGraph(
        navController = navController,
        userViewModel = userViewModel
    )
}






