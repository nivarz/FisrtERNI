package com.eriknivar.firebasedatabase.view.storagetype

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.eriknivar.firebasedatabase.navigation.NavigationDrawer
import com.eriknivar.firebasedatabase.view.common.ClienteItem
import com.eriknivar.firebasedatabase.view.common.ClientePickerDialog
import com.eriknivar.firebasedatabase.view.common.cargarClientes
import com.eriknivar.firebasedatabase.view.utility.SessionUtils
import com.eriknivar.firebasedatabase.viewmodel.UserViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.delay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.eriknivar.firebasedatabase.data.LocalidadesRepo
import com.eriknivar.firebasedatabase.data.Refs
import com.google.firebase.firestore.ktx.firestore
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults


@Composable
fun SelectStorageFragment(
    navController: NavHostController,
    userViewModel: UserViewModel
) {
    val context = LocalContext.current

    val nombreUsuario by userViewModel.nombre.observeAsState("")
    val currentUserId by userViewModel.documentId.observeAsState("")
    val currentSessionId by userViewModel.sessionId

    val tipoRaw by userViewModel.tipo.observeAsState("")
    val cidRaw by userViewModel.clienteId.observeAsState("")
    val tipo = tipoRaw.lowercase()
    val db = Firebase.firestore
    val cidActual = cidRaw.trim().uppercase()

    //val localidadesOptions = remember { mutableStateListOf<String>() }   // <- NUEVO
    var valueText by remember { mutableStateOf("") }                      // si ya existe, no lo declares de nuevo

    val localidades = remember { mutableStateListOf<String>() }
    var isLocalidadesLoading by rememberSaveable { mutableStateOf(false) }
    var localidadSeleccionada by rememberSaveable { mutableStateOf<String?>(null) }
    var expandedLocalidad by remember { mutableStateOf(false) }


    // === estados para el picker ===
    val showClientePicker = remember { mutableStateOf(false) }
    val clientes = remember { mutableStateListOf<ClienteItem>() }


    DisposableEffect(currentUserId, currentSessionId) {
        val firestore = Firebase.firestore

        val listenerRegistration = firestore.collection("usuarios")
            .document(currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreListener", "Error en snapshotListener", error)
                    return@addSnapshotListener
                }

                val remoteSessionId = snapshot?.getString("sessionId") ?: ""

                if (remoteSessionId != currentSessionId && !userViewModel.isManualLogout.value) {
                    Toast.makeText(
                        context,
                        "Tu sesión fue cerrada por el administrador",
                        Toast.LENGTH_LONG
                    ).show()

                    userViewModel.clearUser()

                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }

        onDispose {
            listenerRegistration.remove()
        }
    }

    val dummyLocation = remember { mutableStateOf("") }
    val dummySku = remember { mutableStateOf("") }
    val dummyQuantity = remember { mutableStateOf("") }
    val dummyLot = remember { mutableStateOf("") }
    val dummyDateText = remember { mutableStateOf("") }

    val lastInteractionTime =
        remember { mutableLongStateOf(SessionUtils.obtenerUltimaInteraccion(context)) }

    fun actualizarActividad(context: Context) {
        val tiempoActual = System.currentTimeMillis()
        lastInteractionTime.longValue = tiempoActual
        SessionUtils.guardarUltimaInteraccion(context, tiempoActual)
    }

    LaunchedEffect(lastInteractionTime.longValue) {
        while (true) {
            delay(60_000)
            val tiempoActual = System.currentTimeMillis()
            val tiempoInactivo = tiempoActual - lastInteractionTime.longValue

            if (tiempoInactivo >= 30 * 60_000) {
                val documentId = userViewModel.documentId.value ?: ""
                Firebase.firestore.collection("usuarios")
                    .document(documentId)
                    .update("sessionId", "")
                Toast.makeText(context, "Sesión finalizada por inactividad", Toast.LENGTH_LONG)
                    .show()

                userViewModel.clearUser()

                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                }

                break
            }
        }
    }

    LaunchedEffect(tipo, cidActual) {
        if (tipo == "superuser" && cidActual.isBlank()) {
            cargarClientes(
                db = Firebase.firestore,
                onOk = { lista ->
                    clientes.clear()
                    clientes.addAll(
                        lista
                            .distinctBy { it.id }                 // 🔒 evita repetidos por ID
                            .sortedBy { it.nombre.uppercase() }   // 👁️ mejor orden visual
                    )
                    if (lista.size == 1) {
                        val sel = lista.first()
                        LocalidadesRepo.invalidate(sel.id)
                        userViewModel.setClienteId(sel.id)
                        val docId = userViewModel.documentId.value ?: ""
                        if (docId.isNotBlank()) {
                            Firebase.firestore.collection("usuarios")
                                .document(docId)
                                .update("clienteId", sel.id)
                        }
                    } else {
                        showClientePicker.value = true
                    }
                },
                onErr = { /* opcional: snackbar/log */ }
            )
        }
    }

    DisposableEffect(cidActual) {
        // Al cambiar de cliente:
        localidades.clear()
        localidadSeleccionada = null
        isLocalidadesLoading = cidActual.isNotBlank()

        if (cidActual.isNotBlank()) {
            // Escucha en tiempo real solo si hay cliente
            LocalidadesRepo.listen(
                clienteId = cidActual,
                onData = { lista ->
                    localidades.clear()
                    localidades.addAll(lista)
                    isLocalidadesLoading = false
                },
                onErr = { e ->
                    Log.e("LOCALIDADES", "listen error", e)
                    localidades.clear()
                    isLocalidadesLoading = false
                    // TODO: snackbar/log si quieres
                }
            )
        } else {
            // Si no hay cliente, asegúrate de detener cualquier listener previo
            LocalidadesRepo.stop()
        }

        // IMPORTANTÍSIMO: siempre retornar un DisposableEffectResult
        onDispose {
            LocalidadesRepo.stop()
        }
    }

    NavigationDrawer(
        navController,
        "Seleccionar Almacén",
        userViewModel,
        dummyLocation,
        dummySku,
        dummyQuantity,
        dummyLot,
        dummyDateText
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // 🟡 Simulación de Logo ERNI (puedes reemplazar por Image si ya tienes uno)
            Icon(
                imageVector = Icons.Default.Warehouse, // O tu propio logo
                contentDescription = "Logo ERNI",
                tint = Color(0xFF001F5B),
                modifier = Modifier
                    .size(64.dp)
                    .padding(bottom = 16.dp)
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF3F9)),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Hola, $nombreUsuario 👋",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color(0xFF001F5B)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Bienvenido al sistema de toma de inventarios **ERNI**.\n\n" +
                                "Para continuar, selecciona el almacén al cual deseas realizar el proceso.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // 👇 Mano señalando hacia el Dropdown
                    Text(
                        text = "\uD83D\uDC47", // Unicode para 👇
                        fontSize = 32.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // ⬇️ Aquí el selector real
                    DropDownUpScreen(
                        navController,
                        onUserInteraction = { actualizarActividad(context) },
                        userViewModel = userViewModel,
                        localidades = localidades,
                        isLocalidadesLoading = isLocalidadesLoading,
                        localidadSeleccionada = localidadSeleccionada,
                        onSelectLocalidad = { loc ->
                            if (loc == "__TODAS__") {
                                localidadSeleccionada = null
                                Toast.makeText(
                                    context,
                                    "En esta pantalla debes elegir una localidad específica.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@DropDownUpScreen
                            }
                            localidadSeleccionada = loc
                            navController.navigate("inventoryentry/$loc")
                        },
                        hasClienteSeleccionado = cidActual.isNotBlank(),
                        isSuperuser = (tipo == "superuser")
                    )
                }
            }
        }
    }

    // === diálogo ===
    ClientePickerDialog(
        open = showClientePicker,
        clientes = clientes
    ) { elegido ->
        // 👇 invalida cache ANTES de cambiar el cliente
        LocalidadesRepo.invalidate(elegido.id)
        userViewModel.setClienteId(elegido.id)

        val docId = userViewModel.documentId.value ?: ""
        if (docId.isNotBlank()) {
            Firebase.firestore.collection("usuarios").document(docId)
                .update("clienteId", elegido.id)
        }
        showClientePicker.value = false
    }



    BackHandler(true) {
        Log.i("LOG_TAG", "Clicked back") // Desactiva el botón atrás
    }
}


