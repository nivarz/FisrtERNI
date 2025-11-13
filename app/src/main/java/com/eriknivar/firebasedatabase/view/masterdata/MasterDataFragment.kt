package com.eriknivar.firebasedatabase.view.masterdata

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.eriknivar.firebasedatabase.navigation.NavigationDrawer
import com.eriknivar.firebasedatabase.view.utility.ScreenWithNetworkBanner
import com.eriknivar.firebasedatabase.viewmodel.UserViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import com.eriknivar.firebasedatabase.data.MaestroRepo
import com.eriknivar.firebasedatabase.security.RoleRules
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.saveable.rememberSaveable
import com.google.firebase.firestore.SetOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalSoftwareKeyboardController


// =========================
// Utilidades: auto-código
// =========================
private const val CODIGO_PAD = 5            // dígitos del correlativo (ajusta si quieres)
private const val CODIGO_PREFIJO = "TEMP"       // ejemplo: "S" si quieres S000001
private fun formatearCodigo(n: Long) = "$CODIGO_PREFIJO${n.toString().padStart(CODIGO_PAD, '0')}"

// Solo previsualiza (no consume número)
private fun previewSiguienteCodigo(clienteId: String, onReady: (String) -> Unit) {
    val ref = Firebase.firestore.collection("clientes").document(clienteId).collection("contadores")
        .document("productos")
    ref.get().addOnSuccessListener { snap ->
        val ultimo = snap.getLong("ultimoNumero") ?: 0L
        onReady(formatearCodigo(ultimo + 1))
    }.addOnFailureListener {
        onReady(formatearCodigo(1))
    }
}

// Reserva el siguiente código en transacción (consume número)
private fun reservarSiguienteCodigo(
    clienteId: String, onOk: (String) -> Unit, onErr: (Exception) -> Unit
) {
    val db = Firebase.firestore
    val ref =
        db.collection("clientes").document(clienteId).collection("contadores").document("productos")

    db.runTransaction { tx ->
        val snap = tx.get(ref)
        val next = (snap.getLong("ultimoNumero") ?: 0L) + 1L
        tx.set(ref, mapOf("ultimoNumero" to next), SetOptions.merge())
        next
    }.addOnSuccessListener { next -> onOk(formatearCodigo(next)) }
        .addOnFailureListener { e -> onErr(e) }
}

// =========================

@Composable
fun MasterDataFragment(
    navController: NavHostController,
    userViewModel: UserViewModel,
) {
    val context = LocalContext.current
    val firestore = Firebase.firestore
    val productos = remember { mutableStateListOf<Producto>() }
    var showDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var productoAEliminar by remember { mutableStateOf<Producto?>(null) }
    var selectedProduct by remember { mutableStateOf<Producto?>(null) }

    var codigoInput by remember { mutableStateOf("") }
    var descripcionInput by remember { mutableStateOf("") }
    var unidadInput by remember { mutableStateOf("") }
    var busqueda by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // SUPERUSER: empieza sin cliente seleccionado
    // ADMIN: se llenará luego en el LaunchedEffect con su clienteId
    var clienteSel by rememberSaveable { mutableStateOf("") }
    var clienteNombreSel by rememberSaveable { mutableStateOf("") }

    var menuClientesAbierto by remember { mutableStateOf(false) }

    // --- Selector de Cliente (solo superuser) ---
    data class Cliente(val id: String, val nombre: String)

    val esSuper = (userViewModel.tipo.value ?: "").equals("superuser", ignoreCase = true)
    LaunchedEffect(esSuper, userViewModel.clienteId.value) {
        if (!esSuper) {
            clienteSel = userViewModel.clienteId.value?.trim().orEmpty()
            clienteNombreSel = clienteSel
        }
    }

    val clientes = remember { mutableStateListOf<Cliente>() }

    val dummyLocation = remember { mutableStateOf("") }
    val dummySku = remember { mutableStateOf("") }
    val dummyQuantity = remember { mutableStateOf("") }
    val dummyLot = remember { mutableStateOf("") }
    val dummyDateText = remember { mutableStateOf("") }

    val navyBlue = Color(0xFF001F5B)
    val softNavy = Color(0x1A001F5B) // navy con transparencia para tarjetas suaves

    val currentUserId = userViewModel.documentId.value ?: ""
    val currentSessionId = userViewModel.sessionId.value

    val kb = LocalSoftwareKeyboardController.current

    // Listener de sesión activa
    DisposableEffect(currentUserId, currentSessionId) {
        val listenerRegistration = firestore.collection("usuarios").document(currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreListener", "Error en snapshotListener", error)
                    return@addSnapshotListener
                }
                val remoteSessionId = snapshot?.getString("sessionId") ?: ""
                if (remoteSessionId != currentSessionId && !userViewModel.isManualLogout.value) {
                    Toast.makeText(
                        context, "Tu sesión fue cerrada por el administrador", Toast.LENGTH_LONG
                    ).show()
                    userViewModel.clearUser()
                    navController.navigate("login") { popUpTo(0) { inclusive = true } }
                }
            }
        onDispose { listenerRegistration.remove() }
    }

    // Acceso
    val tipo = userViewModel.tipo.value
    if (!RoleRules.canAccessMasterData(tipo)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.TopCenter
        ) {
            Text(
                text = "\u26D4\uFE0F Acceso restringido",
                color = Color.Red,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(32.dp)
            )
        }
        return
    }

    // =========================
    // CARGA usando MaestroRepo
    // =========================
    fun cargarProductos() {
        val clienteId = userViewModel.clienteId.value?.trim().orEmpty()
        if (clienteId.isBlank()) {
            Toast.makeText(context, "Selecciona un cliente primero", Toast.LENGTH_SHORT).show()
            return
        }
        isLoading = true
        MaestroRepo.listarProductos(clienteId = clienteId, onResult = { lista ->
            productos.clear()
            productos.addAll(lista.sortedBy { it.descripcion })
            isLoading = false
        }, onErr = { e ->
            Log.e("MD", "Error listando", e)
            Toast.makeText(
                context, "Error al cargar productos: ${e.message}", Toast.LENGTH_SHORT
            ).show()
            isLoading = false
        })
    }

    fun cargarClientes() {
        val db = Firebase.firestore

        if (esSuper) {
            // SUPERUSER: lista completa para el dropdown
            db.collection("clientes")
                .get()
                .addOnSuccessListener { snap ->
                    clientes.clear()
                    clientes.addAll(
                        snap.documents.map { d ->
                            Cliente(id = d.id, nombre = d.getString("nombre") ?: d.id)
                        }.sortedBy { it.nombre }
                    )
                }
                .addOnFailureListener { e ->
                    Log.e("MD", "Error cargando clientes", e)
                    Toast.makeText(
                        context,
                        "Error cargando clientes: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } else {
            // ADMIN: solo su cliente, para mostrar NOMBRE + ID
            val cid = clienteSel.ifBlank { userViewModel.clienteId.value?.trim().orEmpty() }
            if (cid.isBlank()) return

            db.collection("clientes")
                .document(cid)
                .get()
                .addOnSuccessListener { d ->
                    if (d.exists()) {
                        clienteSel = cid
                        clienteNombreSel = d.getString("nombre") ?: cid
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("MD", "Error cargando cliente actual", e)
                    Toast.makeText(
                        context,
                        "Error cargando datos del cliente: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    LaunchedEffect(Unit) {
        cargarClientes()
    }

    ScreenWithNetworkBanner(
        showDisconnectedBanner = false,
        showRestoredBanner = false,
        onCloseDisconnected = {},
        onCloseRestored = {}) {
        NavigationDrawer(
            navController,
            "Datos Maestro",
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
                    .padding(16.dp)
            ) {

                // === Cliente (visual mejorada) ===
                if (esSuper) {
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = softNavy),
                            elevation = CardDefaults.cardElevation(0.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Cliente",
                                        color = navyBlue,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(2.dp))

                                    if (clienteNombreSel.isBlank() && clienteSel.isBlank()) {
                                        Text(
                                            "Selecciona un cliente",
                                            fontSize = 14.sp,
                                            color = Color.Gray
                                        )
                                    } else {
                                        Text(
                                            text = clienteNombreSel.ifBlank { clienteSel },
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (clienteNombreSel.isNotBlank()) {
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                text = clienteSel,
                                                fontSize = 13.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }

                                TextButton(onClick = { menuClientesAbierto = true }) {
                                    Text(
                                        "Cambiar",
                                        color = navyBlue,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        DropdownMenu(
                            expanded = menuClientesAbierto,
                            onDismissRequest = { menuClientesAbierto = false },
                            modifier = Modifier
                                .fillMaxWidth(.9f)           // 👉 mismo ancho que la card
                                .heightIn(max = 260.dp)   // 👉 si hay muchos, hace scroll
                        ) {
                            if (clientes.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No hay clientes disponibles") },
                                    onClick = { menuClientesAbierto = false }
                                )
                            } else {
                                clientes.forEach { c ->
                                    DropdownMenuItem(
                                        text = { Text("${c.nombre} (${c.id})") },
                                        onClick = {
                                            clienteSel = c.id
                                            clienteNombreSel = c.nombre
                                            userViewModel.setClienteId(c.id)
                                            menuClientesAbierto = false
                                            productos.clear()
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = softNavy),
                        elevation = CardDefaults.cardElevation(0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Cliente",
                                    color = navyBlue,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(2.dp))

                                val textoPrincipal =
                                    clienteNombreSel.ifBlank { clienteSel.ifBlank { userViewModel.clienteId.value?.trim().orEmpty() } }

                                Text(
                                    text = textoPrincipal.ifBlank { "Cliente asignado a tu usuario" },
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )

                                if (clienteNombreSel.isNotBlank() && clienteSel.isNotBlank()) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = clienteSel,
                                        fontSize = 13.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                }

                // Botón: Agregar Producto (ahora muestra preview del próximo código)
                if (RoleRules.canMutateMasterData(userViewModel.tipo.value)) {
                    // === Botones lado a lado con iconos ===
                    Row(modifier = Modifier.fillMaxWidth()) {

                        // Asume que ya tienes:
                        val clienteElegido = clienteSel.isNotBlank()

                        // Botón: Agregar Producto
                        ElevatedButton(
                            colors = ButtonDefaults.buttonColors(
                                containerColor = navyBlue, contentColor = Color.White
                            ),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                            onClick = {
                                if (!clienteElegido) {
                                    Toast.makeText(
                                        context,
                                        "Selecciona un cliente primero",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@ElevatedButton
                                }

                                // === lógica de crear ===
                                selectedProduct = null
                                descripcionInput = ""
                                unidadInput = ""

                                val clienteId = userViewModel.clienteId.value?.trim().orEmpty()
                                previewSiguienteCodigo(clienteId) { next ->
                                    codigoInput = next // preview (no consume)
                                    showDialog = true
                                }
                            },
                            enabled = clienteElegido,              // ← deshabilitado si no hay cliente
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Agregar")
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        // Botón: Cargar Datos Maestro
                        ElevatedButton(
                            colors = ButtonDefaults.buttonColors(
                                containerColor = navyBlue, contentColor = Color.White
                            ),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                            onClick = {
                                // 1) Validar que haya un cliente seleccionado
                                if (!clienteElegido) {
                                    Toast.makeText(
                                        context,
                                        "Selecciona un cliente primero",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@ElevatedButton
                                }

                                // 2) Reglas de rol/cliente (como ya tenías)
                                val tipoUser = userViewModel.tipo.value
                                val userCid = userViewModel.clienteId.value
                                val targetCid = userViewModel.clienteId.value
                                if (!RoleRules.canActOnCliente(tipoUser, userCid, targetCid)) {
                                    Toast.makeText(
                                        context,
                                        "No puedes ver datos de otro cliente",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@ElevatedButton
                                }

                                // 3) Acción
                                cargarProductos()
                            },
                            enabled = clienteElegido && !isLoading,   // ← deshabilitado si no hay cliente
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(if (isLoading) "Cargando..." else "Cargar")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // === Buscador con lupa y botón "X" para limpiar ===
                    OutlinedTextField(
                        value = busqueda.uppercase(),
                        onValueChange = { busqueda = it },
                        singleLine = true,
                        label = { Text("Buscar por descripción", color = Color.Gray) },
                        placeholder = { Text("Buscar por descripción", color = Color.Gray) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search, contentDescription = "Buscar"
                            )
                        },
                        trailingIcon = {
                            if (busqueda.isNotEmpty()) {
                                IconButton(onClick = { busqueda = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Limpiar"
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        // Opcional: UX de teclado
                        ///////////////////////////////////////
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                kb?.hide()
                            }
                        )
                    )

                    // === Contador de productos (total y filtrados) ===
                    val total = productos.size
                    val filtrados =
                        productos.count { it.descripcion.contains(busqueda, ignoreCase = true) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Chip de total
                        Box(
                            modifier = Modifier
                                .background(
                                    navyBlue,
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                "Artículos: $total",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Mostrando (según filtro)
                        Text(
                            text = if (busqueda.isBlank()) "Mostrando: $filtrados" else "Filtro: $filtrados / $total",
                            color = Color.Gray
                        )
                    }

                    if (isLoading) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    val productosFiltrados = productos.filter {
                        it.descripcion.contains(busqueda, ignoreCase = true)
                    }

                    LazyColumn {
                        if (productosFiltrados.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Sin resultados", color = Color.Gray)
                                }
                            }
                        } else {
                            items(productosFiltrados) { producto ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(
                                            0xFFF5F6FA
                                        )
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {

                                        // Línea Código (azul + bold en valor)
                                        Text(
                                            buildAnnotatedString {
                                                withStyle(style = SpanStyle(color = navyBlue)) {
                                                    append(
                                                        "Código: "
                                                    )
                                                }
                                                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                                    append(producto.codigo)
                                                }
                                            },
                                            fontSize = 15.sp
                                        )

                                        Spacer(Modifier.height(4.dp))

                                        // Línea Descripción
                                        Text(
                                            buildAnnotatedString {
                                                withStyle(style = SpanStyle(color = navyBlue)) {
                                                    append(
                                                        "Descripción: "
                                                    )
                                                }
                                                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                                    append(producto.descripcion)
                                                }
                                            },
                                            fontSize = 15.sp
                                        )

                                        Spacer(Modifier.height(4.dp))

                                        // Línea Unidad
                                        Text(
                                            buildAnnotatedString {
                                                withStyle(style = SpanStyle(color = navyBlue)) {
                                                    append(
                                                        "Unidad: "
                                                    )
                                                }
                                                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                                    append(producto.unidad)
                                                }
                                            },
                                            fontSize = 15.sp
                                        )

                                        Spacer(Modifier.height(8.dp))

                                        // Botones de acción (editar / eliminar)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(onClick = {
                                                selectedProduct = producto
                                                codigoInput = producto.codigo
                                                descripcionInput = producto.descripcion
                                                unidadInput = producto.unidad
                                                showDialog = true
                                            }) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Editar",
                                                    tint = navyBlue
                                                )
                                            }
                                            Spacer(Modifier.width(4.dp))
                                            IconButton(onClick = {
                                                productoAEliminar = producto
                                                showDeleteDialog = true
                                            }) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Eliminar",
                                                    tint = Color(0xFFD32F2F) // rojo agradable
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }


                    SnackbarHost(hostState = snackbarHostState)

                    if (showDialog) {
                        AlertDialog(
                            onDismissRequest = { showDialog = false },
                            title = { Text(if (selectedProduct == null) "Agregar Producto" else "Editar Producto") },
                            text = {
                                Column {
                                    // Código: read-only siempre (evita cambios manuales)
                                    OutlinedTextField(
                                        value = codigoInput.uppercase(),
                                        singleLine = true,
                                        onValueChange = { /* read-only */ },
                                        label = { Text("Código") },
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = false,
                                        readOnly = true
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = descripcionInput.uppercase(),
                                        singleLine = true,
                                        onValueChange = { descripcionInput = it },
                                        label = { Text("Descripción") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = unidadInput.uppercase(),
                                        singleLine = true,
                                        onValueChange = { unidadInput = it },
                                        label = { Text("Unidad de Medida") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            },
                            confirmButton = {
                                TextButton(
                                    enabled = !isSaving, onClick = {
                                        if (isSaving) return@TextButton

                                        // Permisos
                                        val tipoUser = userViewModel.tipo.value
                                        val userCid = userViewModel.clienteId.value
                                        val targetCid = userViewModel.clienteId.value
                                        if (!RoleRules.canMutateMasterData(tipoUser) || !RoleRules.canActOnCliente(
                                                tipoUser,
                                                userCid,
                                                targetCid
                                            )
                                        ) {
                                            Toast.makeText(
                                                context,
                                                "No tienes permisos para esta acción",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            return@TextButton
                                        }

                                        val clienteId =
                                            userViewModel.clienteId.value?.trim().orEmpty()
                                        if (clienteId.isBlank()) {
                                            Toast.makeText(
                                                context,
                                                "Selecciona un cliente primero",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            return@TextButton
                                        }

                                        if (descripcionInput.isBlank() || unidadInput.isBlank()) {
                                            Toast.makeText(
                                                context,
                                                "Completa todos los campos",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            return@TextButton
                                        }

                                        val desc = descripcionInput.trim().uppercase()
                                        val uni = unidadInput.trim().uppercase()

                                        if (selectedProduct == null) {
                                            // Crear — reservar código y luego crear
                                            // Valida duplicados por descripción/unidad en la lista actual
                                            val yaExiste = productos.any {
                                                it.descripcion.equals(
                                                    desc,
                                                    ignoreCase = true
                                                ) && it.unidad.equals(uni, ignoreCase = true)
                                            }
                                            if (yaExiste) {
                                                Toast.makeText(
                                                    context,
                                                    "Este producto ya existe",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                return@TextButton
                                            }

                                            isSaving = true
                                            reservarSiguienteCodigo(
                                                clienteId = clienteId,
                                                onOk = { codigoFinal ->
                                                    val nuevo = Producto(
                                                        id = null,
                                                        codigo = codigoFinal,
                                                        descripcion = desc,
                                                        unidad = uni
                                                    )
                                                    MaestroRepo.crearProducto(
                                                        clienteId = clienteId,
                                                        producto = nuevo,
                                                        onResult = {
                                                            isSaving = false
                                                            productos.add(
                                                                Producto(
                                                                    id = codigoFinal,
                                                                    codigo = codigoFinal,
                                                                    descripcion = nuevo.descripcion,
                                                                    unidad = nuevo.unidad
                                                                )
                                                            )
                                                            productos.sortBy { it.descripcion }
                                                            showDialog = false
                                                            coroutineScope.launch {
                                                                snackbarHostState.showSnackbar("Producto agregado")
                                                            }
                                                        },
                                                        onErr = { e ->
                                                            isSaving = false
                                                            Log.e("MD", "Error creando", e)
                                                            Toast.makeText(
                                                                context,
                                                                "No se pudo crear: ${e.message}",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        })
                                                },
                                                onErr = { e ->
                                                    isSaving = false
                                                    Log.e("MD", "Error reservando código", e)
                                                    Toast.makeText(
                                                        context,
                                                        "No se pudo reservar código: ${e.message}",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                })
                                        } else {
                                            // Actualizar — sin cambiar código
                                            val duplicado = productos.any {
                                                it.codigo != selectedProduct!!.codigo && it.descripcion.equals(
                                                    desc, ignoreCase = true
                                                ) && it.unidad.equals(uni, ignoreCase = true)
                                            }
                                            if (duplicado) {
                                                Toast.makeText(
                                                    context,
                                                    "Ya existe otro producto con esta descripción y unidad",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                return@TextButton
                                            }

                                            val cambios = mapOf(
                                                "descripcion" to desc, "unidad" to uni
                                            )
                                            isSaving = true
                                            MaestroRepo.actualizarProducto(
                                                clienteId = clienteId,
                                                productoId = selectedProduct!!.codigo,
                                                cambios = cambios,
                                                onResult = {
                                                    isSaving = false
                                                    val idx =
                                                        productos.indexOfFirst { it.codigo == selectedProduct!!.codigo }
                                                    if (idx != -1) {
                                                        productos[idx] = productos[idx].copy(
                                                            descripcion = desc, unidad = uni
                                                        )
                                                    }
                                                    showDialog = false
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar("Producto actualizado")
                                                    }
                                                },
                                                onErr = { e ->
                                                    isSaving = false
                                                    Log.e("MD", "Error actualizando", e)
                                                    Toast.makeText(
                                                        context,
                                                        "No se pudo actualizar: ${e.message}",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                })
                                        }
                                    }) {
                                    Text(
                                        "Guardar",
                                        color = navyBlue,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDialog = false }) {
                                    Text(
                                        "Cancelar",
                                        color = Color.Red,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            })
                    }

                    if (showDeleteDialog && productoAEliminar != null) {
                        AlertDialog(
                            onDismissRequest = { showDeleteDialog = false },
                            title = { Text("Eliminar Producto") },
                            text = {
                                Text(
                                    buildAnnotatedString {
                                        append("¿Estás seguro de que deseas eliminar el producto \"")
                                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                            append(productoAEliminar?.descripcion ?: "")
                                        }
                                        append("\"?")
                                    })
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    val tipoUser = userViewModel.tipo.value
                                    val userCid = userViewModel.clienteId.value
                                    val targetCid = userViewModel.clienteId.value
                                    if (!RoleRules.canMutateMasterData(tipoUser) || !RoleRules.canActOnCliente(
                                            tipoUser,
                                            userCid,
                                            targetCid
                                        )
                                    ) {
                                        Toast.makeText(
                                            context,
                                            "No tienes permisos para esta acción",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        return@TextButton
                                    }

                                    val clienteId = userViewModel.clienteId.value?.trim().orEmpty()
                                    if (clienteId.isBlank()) {
                                        Toast.makeText(
                                            context,
                                            "Selecciona un cliente primero",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        return@TextButton
                                    }

                                    MaestroRepo.borrarProducto(
                                        clienteId = clienteId,
                                        productoId = productoAEliminar!!.codigo,
                                        onResult = {
                                            productos.remove(productoAEliminar)
                                            showDeleteDialog = false
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Producto eliminado")
                                            }
                                        },
                                        onErr = { e ->
                                            Log.e("MD", "Error borrando", e)
                                            Toast.makeText(
                                                context,
                                                "No se pudo eliminar: ${e.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        })
                                }) { Text("Sí") }
                            },
                            dismissButton = {
                                TextButton(onClick = {
                                    showDeleteDialog = false
                                }) {
                                    Text(
                                        "Cancelar",
                                        color = Color.Red,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            })
                    }
                }
            }
        }
    }
}