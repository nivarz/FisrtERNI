package com.eriknivar.firebasedatabase.view.inventoryentry

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
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
import com.eriknivar.firebasedatabase.navigation.NavigationDrawer
import com.eriknivar.firebasedatabase.view.storagetype.DataFields
import com.eriknivar.firebasedatabase.view.utility.contarRegistrosDelDia
import com.eriknivar.firebasedatabase.viewmodel.UserViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.delay
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.eriknivar.firebasedatabase.view.common.ConteoMode
import com.google.firebase.Timestamp

@Composable
fun FirestoreApp(
    navController: NavHostController,
    storageType: String,
    userViewModel: UserViewModel,
    conteoMode: ConteoMode = ConteoMode.CON_LOTE

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
    val currentSessionId = userViewModel.sessionId.value

    val uidActual = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    val tipoActual = userViewModel.tipo.value?.lowercase().orEmpty()

    // 🔎 Búsqueda por código/SKU
    var searchCodigo by remember { mutableStateOf("") }

    // Lista filtrada que usará el LazyColumn
    val filteredData by remember(searchCodigo, allData) {
        derivedStateOf {
            val q = searchCodigo.trim().uppercase()
            if (q.isBlank()) {
                allData
            } else {
                allData.filter { item ->
                    item.sku.uppercase().contains(q) ||
                            item.location.uppercase().contains(q)
                            || item.description.uppercase().contains(q)
                }
            }
        }
    }

    val currentUserId = userViewModel.documentId.value?.takeIf { it.isNotBlank() }

    DisposableEffect(currentUserId, currentSessionId) {
        if (currentUserId == null) {
            Log.w("FirestoreListener", "⚠️ currentUserId vacío, no se inicia listener de sesión.")
            return@DisposableEffect onDispose { }
        }

        val firestore = Firebase.firestore
        val listenerRegistration = firestore.collection("usuarios")
            .document(currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("SESSION", "listener error: ${error.message}")
                    return@addSnapshotListener
                }

                // ⚠️ Si el doc no existe, no pateamos (evita falsos positivos)
                if (snapshot == null || !snapshot.exists()) {
                    Log.d("SESSION", "snapshot inexistente (posible cache/latencia). No kick.")
                    return@addSnapshotListener
                }

                // 🔎 Si viene de cache (sin red / reconexión), NO expulsar
                val fromCache = snapshot.metadata.isFromCache

                val remoteSessionId = snapshot.getString("sessionId")?.trim().orEmpty()
                val localSessionId =
                    (currentSessionId).trim() // si fuera nullable: currentSessionId?.trim().orEmpty()

                // ✅ Solo “kick” si NO es cache y ambos IDs tienen valor y son distintos
                val mustKick = !fromCache &&
                        remoteSessionId.isNotBlank() &&
                        localSessionId.isNotBlank() &&
                        remoteSessionId != localSessionId

                Log.d(
                    "SESSION",
                    "fromCache=$fromCache remote=$remoteSessionId local=$localSessionId mustKick=$mustKick manual=${userViewModel.isManualLogout.value}"
                )

                if (mustKick && !userViewModel.isManualLogout.value) {
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

    // 🔊 Listener Realtime de inventario (reemplaza el que tenías antes)
    DisposableEffect(
        // Observa el cliente seleccionado y la localidad (storageType)
        userViewModel.clienteId.observeAsState("").value,
        storageType
    ) {
        val firestore = Firebase.firestore

        val cid = (userViewModel.clienteId.value ?: "").trim().uppercase()
        val loc = storageType.trim().uppercase()

        // Rol + UID actuales
        val tipoActual = userViewModel.tipo.value?.lowercase().orEmpty()
        val uidActual = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

        if (cid.isBlank() || loc.isBlank()) {
            return@DisposableEffect onDispose { }
        }

        val base = firestore
            .collection("clientes").document(cid)
            .collection("inventario")

        // Query HOY, por localidad. Para invitado, además por dueño.
        val hoyStr = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault())
            .format(java.util.Date())

        var q: com.google.firebase.firestore.Query = base
            .whereEqualTo("localidad", loc)
            .whereEqualTo("dia", hoyStr) // 👈 sin race
            .whereEqualTo("usuarioUid", uidActual) // 🔒 SIEMPRE solo mis registros
            .orderBy("fechaCliente", com.google.firebase.firestore.Query.Direction.DESCENDING)

        // ✅ SOLO si es invitado, filtramos por su uid
        if (tipoActual == "invitado") {
            q = q.whereEqualTo("usuarioUid", uidActual)
        }

        val reg = q.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("InvRealtime", "Listener error", e)
                return@addSnapshotListener
            }
            val docs = snapshot ?: return@addSnapshotListener

            val nuevos: List<DataFields> = docs.documents.map { doc ->
                val df = doc.toObject(DataFields::class.java) ?: DataFields()

                // ⏱ fecha para el contador (server o legacy; fallback ahora)
                val fechaTS = doc.getTimestamp("fechaRegistro")
                    ?: doc.getTimestamp("fecha")
                    ?: Timestamp.now()

                // 👤 usuario para el contador (nombre o legacy; fallback a lo que traiga df)
                val usuarioNombre = doc.getString("usuarioNombre")
                    ?: doc.getString("usuario")
                    ?: df.usuario

                val cantidad = (doc.getDouble("cantidad")
                    ?: doc.getLong("cantidad")?.toDouble()
                    ?: doc.getDouble("quantity")
                    ?: doc.getLong("quantity")?.toDouble()
                    ?: df.quantity)

                val ubicacion = (doc.getString("ubicacion")
                    ?: doc.getString("location")
                    ?: df.location)

                val unidad = (doc.getString("unidadMedida")
                    ?: doc.getString("unidad")
                    ?: df.unidadMedida)

                val sku = when {
                    df.sku.isNotBlank() -> df.sku
                    !doc.getString("codigoProducto")
                        .isNullOrBlank() -> doc.getString("codigoProducto")!!

                    else -> ""
                }

                val descripcion = when {
                    df.description.isNotBlank() -> df.description
                    !doc.getString("descripcion").isNullOrBlank() -> doc.getString("descripcion")!!
                    else -> ""
                }

                val tipoCreador = doc.getString("tipoUsuarioCreador")
                    ?.lowercase()
                    ?.trim()
                    ?: ""

                df.copy(
                    documentId = doc.id,
                    quantity = cantidad,
                    location = ubicacion,
                    unidadMedida = unidad,
                    sku = sku,
                    description = descripcion,
                    // 👇 claves que necesitaba el contador
                    fechaRegistro = fechaTS,
                    usuario = usuarioNombre,
                    tipoUsuarioCreador = tipoCreador

                )
            }

            allData.clear()
            allData.addAll(nuevos)
            Log.d(
                "InvRealtime",
                "UI actualizada: ${nuevos.size} regs (fromCache=${docs.metadata.isFromCache})"
            )

        }

        onDispose { reg.remove() }
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
                    .imePadding()                 // ⬅️ importante en Android 14/15
                    .navigationBarsPadding()
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

                    // 🔎 Search por código/SKU para los cards
                    OutlinedTextField(
                        value = searchCodigo,
                        onValueChange = { searchCodigo = it },
                        label = { Text("Buscar por código / SKU / Descripcion", fontSize = 12.sp) },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                        trailingIcon = {
                            if (searchCodigo.isNotBlank()) {
                                IconButton(onClick = { searchCodigo = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Limpiar búsqueda"
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 40.dp, max = 58.dp)   // un poco más bajito
                            .padding(horizontal = 8.dp)
                    )

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
                        conteoMode = conteoMode

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
                        items = filteredData,
                        key = { _, item -> item.documentId }
                    ) { index, item ->
                        MessageCard(
                            item = item,
                            allData = allData,
                            onSuccess = {
                                showSuccessDialog = true
                                // 🔄 Recarga inmediata con los filtros actuales
                                allData.clear()
                                fetchDataFromFirestore(
                                    db = Firebase.firestore,
                                    allData = allData,
                                    usuario = userViewModel.nombre.value.orEmpty(),
                                    listState = listState,
                                    localidad = storageType,
                                    clienteId = userViewModel.clienteId.value.orEmpty(),
                                    tipo = tipoActual,
                                    uid = uidActual
                                )
                            },
                            listState = listState,
                            index = index,
                            expandedStates = expandedStates,
                            userViewModel = userViewModel,
                            conteoMode = conteoMode
                        )
                    }
                }
            }
        }
    }
}