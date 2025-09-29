package com.eriknivar.firebasedatabase.view.common

import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.navigation.NavHostController
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.eriknivar.firebasedatabase.view.utility.SessionTimeouts
import com.eriknivar.firebasedatabase.viewmodel.UserViewModel
import kotlin.time.Duration.Companion.minutes

@Composable
fun SessionGuard(
    userViewModel: UserViewModel,
    navController: NavHostController
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    // Estado de sesión del VM
    val isInitialized by userViewModel.isInitialized.observeAsState(false)
    val nombre by userViewModel.nombre.observeAsState("")
    val isLoggedOut = nombre.isEmpty()
    val tipo by userViewModel.tipo.observeAsState("")
    val sessionStartMs by userViewModel.sessionStartMs.observeAsState(0L)

    // Inactividad (última interacción global guardada en preferencias)
    val lastInteractionTime = remember {
        mutableLongStateOf(
            com.eriknivar.firebasedatabase.view.utility.SessionUtils.obtenerUltimaInteraccion(context)
        )
    }

    // Al entrar con sesión válida: arranca absolute y resetea la inactividad
    LaunchedEffect(isInitialized, isLoggedOut) {
        if (isInitialized && !isLoggedOut) {
            userViewModel.resetSessionStart()
            com.eriknivar.firebasedatabase.view.utility.SessionUtils
                .guardarUltimaInteraccion(context, System.currentTimeMillis())
            lastInteractionTime.longValue =
                com.eriknivar.firebasedatabase.view.utility.SessionUtils.obtenerUltimaInteraccion(context)
        }
    }

    // Timeouts por rol
    val timeouts = remember(tipo) { SessionTimeouts.forRole(tipo) }
    val idleMs = timeouts.idle.inWholeMilliseconds
    val absoluteMs = timeouts.absolute.inWholeMilliseconds

    // Watcher global (1 solo en toda la app)
    LaunchedEffect(lastInteractionTime.longValue, sessionStartMs, tipo) {
        while (true) {
            kotlinx.coroutines.delay(1.minutes)

            if (isLoggedOut) break

            val now = System.currentTimeMillis()
            val idleElapsed = now - lastInteractionTime.longValue
            val absoluteElapsed = if (sessionStartMs > 0L) now - sessionStartMs else 0L

            val reachedIdle = idleElapsed >= idleMs
            val reachedAbsolute = sessionStartMs > 0L && absoluteElapsed >= absoluteMs

            if (reachedIdle || reachedAbsolute) {
                try {
                    userViewModel.documentId.value?.let { docId ->
                        if (docId.isNotBlank()) {
                            Firebase.firestore.collection("usuarios")
                                .document(docId).update("sessionId", "")
                        }
                    }
                } catch (_: Exception) {}

                Toast.makeText(
                    context,
                    "Sesión finalizada por ${if (reachedIdle) "inactividad" else "tiempo máximo"}",
                    Toast.LENGTH_LONG
                ).show()

                userViewModel.clearUser()
                navController.navigate("login") { popUpTo(0) { inclusive = true } }
                break
            }
        }
    }

    // Exponer una forma de refrescar la actividad desde cualquier screen:
    LaunchedEffect(Unit) {
        userViewModel.onUserInteracted = {
            val t = System.currentTimeMillis()
            com.eriknivar.firebasedatabase.view.utility.SessionUtils.guardarUltimaInteraccion(context, t)
            lastInteractionTime.longValue = t
        }
    }
}
