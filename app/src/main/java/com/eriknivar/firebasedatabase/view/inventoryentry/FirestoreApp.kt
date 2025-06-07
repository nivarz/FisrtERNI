package com.eriknivar.firebasedatabase.view.inventoryentry

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import com.eriknivar.firebasedatabase.view.NavigationDrawer
import com.eriknivar.firebasedatabase.view.storagetype.DataFields
import com.eriknivar.firebasedatabase.view.utility.SessionUtils
import com.eriknivar.firebasedatabase.view.utility.contarRegistrosDelDia
import com.eriknivar.firebasedatabase.viewmodel.UserViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.delay

@Composable
fun FirestoreApp(
    navController: NavHostController,
    storageType: String,
    userViewModel: UserViewModel,

    ) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val productoDescripcion = remember { mutableStateOf("") }
    val unidadMedida = remember { mutableStateOf("") }
    val location = remember { mutableStateOf("") }
    val sku = remember { mutableStateOf("") }
    val lot = remember { mutableStateOf("") }
    val dateText = remember { mutableStateOf("") }
    val quantity = remember { mutableStateOf("") }

    val allData = remember { mutableStateListOf<DataFields>() } // ✅ ahora aquí
    val listState = rememberLazyListState() // ✅ ahora aquí
    val expandedStates = remember { mutableStateMapOf<String, Boolean>() }
    val expandedForm = remember { mutableStateOf(true) }

    val context = LocalContext.current
    val currentUserId = userViewModel.documentId.value ?: ""
    val currentSessionId = userViewModel.sessionId.value



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

    var showSuccessDialog by remember { mutableStateOf(false) } // ✅ Aquí también

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            confirmButton = {},
            title = { Text("✔️ Registro actualizado") },
            text = { Text("Los datos se actualizaron correctamente.") },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        )

        LaunchedEffect(showSuccessDialog) {
            delay(2000)
            showSuccessDialog = false
        }
    }

    val usuario by userViewModel.nombre.observeAsState("")

    LaunchedEffect(usuario, storageType) {
        Log.d(
            "FirestoreApp",
            "🔄 LaunchedEffect lanzado para localidad: $storageType y usuario: $usuario"
        )

        if (usuario.isNotEmpty()) {
            Log.d("FotoDebug", "🔄 Llamando a fetchFilteredInventoryFromFirestore...")
            // 🔵 Limpiamos la lista antes de cargar nuevos datos
            allData.clear()


            fetchDataFromFirestore(
                db = Firebase.firestore,
                allData = allData,
                usuario = usuario,
                listState = listState,
                localidad = storageType
            )
        }
    }

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

                // 🧹 Limpiar los campos ANTES de salir
                sku.value = ""
                location.value = ""
                lot.value = ""
                dateText.value = ""
                quantity.value = ""
                productoDescripcion.value = ""
                unidadMedida.value = ""

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

    NavigationDrawer(
        navController,
        storageType,
        userViewModel,
        location,
        sku,
        quantity,
        lot,
        dateText
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = { }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {

                val nombre = userViewModel.nombre.observeAsState("").value
                val cantidadRegistrosHoy by remember(allData, nombre) {
                    derivedStateOf {
                        contarRegistrosDelDia(allData, nombre.uppercase())
                    }
                }

                // 🔷 TÍTULO Y TOGGLE
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,

                    ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedForm.value = !expandedForm.value },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween // 👈 Distribuye el texto a la izquierda y el ícono a la derecha

                    ) {

                        // 🎯 Contador animado
                        AnimatedContent(
                            targetState = cantidadRegistrosHoy,
                            transitionSpec = {
                                slideInVertically { height -> height } + fadeIn() togetherWith
                                        slideOutVertically { height -> -height } + fadeOut()
                            },
                            label = "ContadorAnimado"
                        ) { targetCount ->
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(
                                        style = SpanStyle(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = Color(0xFF003366)
                                        )
                                    ) {
                                        append("📋 Registros de Hoy : ")
                                    }
                                    withStyle(
                                        style = SpanStyle(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 22.sp,
                                            color = Color(0xFF1565C0)
                                        )
                                    ) {
                                        append(targetCount.toString())
                                    }
                                },
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }


                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = if (expandedForm.value) "Ocultar formulario" else "Mostrar formulario",
                            modifier = Modifier
                                .size(28.dp)
                                .graphicsLayer { rotationZ = if (expandedForm.value) 0f else 180f },
                            tint = Color.Blue
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 5.dp))

                    if (productoDescripcion.value.isNotBlank()) {
                        Text(
                            text = productoDescripcion.value,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Blue,
                            textAlign = TextAlign.Center
                        )
                        if (unidadMedida.value.isNotBlank()) {
                            Text(
                                text = "(${unidadMedida.value})",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // 🔷 FORMULARIO (colapsable)
                Box(modifier = Modifier.fillMaxWidth()) {
                    FormEntradaDeInventario(
                        productoDescripcion = productoDescripcion,
                        unidadMedida = unidadMedida,
                        userViewModel = userViewModel,
                        coroutineScope = coroutineScope,
                        localidad = storageType,
                        allData = allData,
                        listState = listState,
                        location = location,
                        sku = sku,
                        lot = lot,
                        dateText = dateText,
                        quantity = quantity,
                        isVisible = expandedForm.value, // 🔵 Usamos esto para mostrar/ocultar internamente
                        onUserInteraction = { actualizarActividad(context) }
                    )
                }

                // 🔷 LISTA SCROLLABLE
                LazyColumn(
                    modifier = Modifier
                        .weight(1f) // ⬅️ Solo ocupa el espacio disponible
                        .background(Color.White),
                    state = listState
                ) {
                    itemsIndexed(
                        items = allData,
                        key = { _, item -> item.documentId }
                    ) { index, item ->
                        MessageCard(
                            item = item,
                            firestore = Firebase.firestore,
                            allData = allData,
                            onSuccess = { showSuccessDialog = true },
                            listState = listState,
                            index = index,
                            expandedStates = expandedStates,
                            userViewModel = userViewModel,
                        )
                    }
                }
            }
        }

    }
}
