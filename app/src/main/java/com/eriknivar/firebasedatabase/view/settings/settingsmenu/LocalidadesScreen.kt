package com.eriknivar.firebasedatabase.view.settings.settingsmenu

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
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
import com.eriknivar.firebasedatabase.view.utility.SessionUtils
import com.eriknivar.firebasedatabase.viewmodel.UserViewModel
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import kotlinx.coroutines.delay
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material3.HorizontalDivider
import com.google.firebase.auth.FirebaseAuth
import com.eriknivar.firebasedatabase.data.LocalidadesRepo


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalidadesScreen(navController: NavHostController, userViewModel: UserViewModel) {

    // --- Sesión / gating ---
    val isLoggedOut = userViewModel.nombre.observeAsState("").value.isEmpty()
    val isInitialized = userViewModel.isInitialized.observeAsState(false).value
    if (isInitialized && isLoggedOut) return

    val tipo by userViewModel.tipo.observeAsState("")
    val isSuper = tipo.equals("superuser", ignoreCase = true)

    var showClientesDialog by remember { mutableStateOf(false) }
    var loadingClientes by remember { mutableStateOf(false) }
    var clientesError by remember { mutableStateOf<String?>(null) }

    val ctx = LocalContext.current

    var isSaving by remember { mutableStateOf(false) }

    if (!tipo.equals("admin", true) && !isSuper) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.TopCenter
        ) {
            Text(
                text = "⛔ Acceso restringido",
                color = Color.Red,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(32.dp)
            )
        }
        return
    }

    // --- Firebase / estados base ---
    val firestore = Firebase.firestore
    val localidades = remember { mutableStateListOf<Pair<String, String>>() } // (codigo, nombre)
    val dummy = remember { mutableStateOf("") }

    var showDialog by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var docIdToEdit by remember { mutableStateOf("") }

    var codigoInput by remember { mutableStateOf("") }
    var nombreInput by remember { mutableStateOf("") }

    var showSuccessDialog by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var codeToDelete by remember { mutableStateOf<String?>(null) }
    var isDeleting by remember { mutableStateOf(false) }

    var showNombreExistenteDialog by remember { mutableStateOf(false) }

    // --- Cliente seleccionado ---
    val clienteIdFromVm by userViewModel.clienteId.observeAsState()
    var selectedCid by remember { mutableStateOf(userViewModel.clienteId.value?.uppercase() ?: "") }
    var selectedClientLabel by remember { mutableStateOf("") } // ej. "Acme (000002)"

    // Lista de clientes (solo para superuser)
    val clientes = remember { mutableStateListOf<Pair<String, String>>() } // (cid, nombre)
    var showClientesMenu by remember { mutableStateOf(false) }

    // --- Cargas ---
    fun cargarClientesActivos(
        ctx: Context,
        onDone: () -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        clientesError = null

        Firebase.firestore.collection("clientes")
            .get()
            .addOnSuccessListener { snap ->
                clientes.clear()
                for (d in snap.documents) {
                    val cid = d.id.uppercase()
                    val activo = d.getBoolean("activo") == true
                    val nombre = (d.getString("nombre") ?: "").trim()
                    val myCid = (userViewModel.clienteId.value ?: "").uppercase()
                    val permitido = if (isSuper) activo else (activo && cid == myCid)
                    if (permitido) clientes.add(cid to nombre)
                }
                clientes.sortBy { if (it.second.isNotBlank()) it.second else it.first }
                Toast.makeText(ctx, "Clientes cargados: ${clientes.size}", Toast.LENGTH_SHORT)
                    .show()
                onDone()
            }
            .addOnFailureListener { e ->
                clientesError = e.message ?: "Error cargando clientes"
                Toast.makeText(ctx, "Error clientes: ${clientesError}", Toast.LENGTH_LONG).show()
                onError(e)
            }
    }

    // Estados cerca de los demás (si no lo tienes ya)
    var localidadesError by remember { mutableStateOf<String?>(null) }

    fun cargarLocalidades(cidParam: String, ctx: Context) {
        val cid = cidParam.trim().uppercase()
        if (cid.isBlank()) return

        localidadesError = null

        Firebase.firestore
            .collection("clientes").document(cid)
            .collection("localidades")
            .orderBy("codigo")
            .get() // ⬅️ sin whereEqualTo
            .addOnSuccessListener { snap ->
                localidades.clear()
                var total = 0
                for (d in snap.documents) {
                    total++
                    val activo =
                        d.getBoolean("activo") ?: true   // ⬅️ si no existe, lo tratamos como activo
                    if (activo) {
                        val codigo = (d.getString("codigo") ?: d.id).uppercase()
                        val nombre = d.getString("nombre") ?: ""
                        localidades.add(codigo to nombre)
                    }
                }
                localidades.sortBy { it.first } // orden por código
                Toast.makeText(
                    ctx,
                    "Localidades activas: ${localidades.size} / total: $total",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { e ->
                localidadesError = e.message ?: "Error cargando localidades"
                Toast.makeText(ctx, "Localidades: $localidadesError", Toast.LENGTH_LONG).show()
            }
    }

    // Decide cargas según rol, cuando el VM ya resolvió
    LaunchedEffect(isSuper, clienteIdFromVm) {
        if (isSuper) {
            selectedCid = ""
            selectedClientLabel = ""
            cargarClientesActivos(ctx)
        } else {
            selectedCid = (clienteIdFromVm ?: "").uppercase()
            selectedClientLabel = selectedCid
            if (selectedCid.isNotBlank()) cargarLocalidades(selectedCid, ctx)
        }
    }

    // Recargar al cambiar de cliente
    LaunchedEffect(selectedCid) {
        if (selectedCid.isNotBlank()) cargarLocalidades(selectedCid, ctx)
    }

    // --- Inactividad (igual que en otras pantallas) ---
    val context = LocalContext.current
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
                navController.navigate("login") { popUpTo(0) { inclusive = true } }
                break
            }
        }
    }

    val auth = FirebaseAuth.getInstance()
    val db = Firebase.firestore
    val uid = auth.currentUser?.uid

    auth.currentUser?.getIdToken(true)?.addOnSuccessListener {
        Log.d("AUTH", "Token refreshed")
    }
    if (uid != null) {
        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { d ->
                Log.d(
                    "AUTH",
                    "uid=$uid tipo=${d.getString("tipo")} clienteId=${d.getString("clienteId")}"
                )
            }
            .addOnFailureListener { e -> Log.e("AUTH", "No pude leer /usuarios/$uid", e) }
    }

    // --- UI ---
    NavigationDrawer(
        navController = navController,
        storageType = "Localidades",
        userViewModel = userViewModel,
        location = dummy, sku = dummy, quantity = dummy, lot = dummy, expirationDate = dummy
    ) {
        // Selector de cliente (solo superuser)
        if (isSuper) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {

                // Abridor centralizado en una sola función
                fun abrirSelectorClientes() {
                    showClientesDialog = true
                    if (clientes.isEmpty() && !loadingClientes) {
                        loadingClientes = true
                        cargarClientesActivos(
                            ctx,
                            onDone = { loadingClientes = false },
                            onError = { loadingClientes = false }
                        )
                    }
                    Toast.makeText(ctx, "Abriendo selector de clientes…", Toast.LENGTH_SHORT).show()
                }

                // Row para asegurar que cualquier tap abra (campo + ícono)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { abrirSelectorClientes() }, // 👈 tap en TODO el row
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = selectedClientLabel,
                        onValueChange = {},
                        label = { Text("Cliente") },
                        placeholder = { Text("Selecciona un cliente") },
                        readOnly = true,
                        modifier = Modifier
                            .weight(1f) // ocupa todo el ancho disponible del Row
                    )

                    // Ícono con botón explícito (segunda vía de apertura)
                    IconButton(onClick = { abrirSelectorClientes() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Elegir cliente"
                        )
                    }
                }

                if (localidadesError != null) {
                    Text(
                        "⚠️ ${localidadesError}",
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                } else {
                    Text(
                        "localidades cargadas: ${localidades.size}",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                // Diálogo con la lista (lo dejamos igual a como ya lo tenías, con estados)
                if (showClientesDialog) {
                    AlertDialog(
                        onDismissRequest = { showClientesDialog = false },
                        title = { Text("Selecciona un cliente") },
                        text = {
                            when {
                                loadingClientes -> {
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }

                                clientesError != null -> {
                                    Text("No se pudieron cargar los clientes:\n$clientesError")
                                }

                                clientes.isEmpty() -> {
                                    Text("No hay clientes activos")
                                }

                                else -> {
                                    LazyColumn(Modifier.heightIn(max = 360.dp)) {
                                        items(clientes) { (cidItem, nombreItem) ->
                                            Column(
                                                Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        selectedCid = cidItem
                                                        selectedClientLabel =
                                                            if (nombreItem.isNotBlank()) "$nombreItem ($cidItem)" else cidItem
                                                        showClientesDialog = false
                                                        cargarLocalidades(selectedCid, ctx)
                                                    }
                                                    .padding(vertical = 10.dp)
                                            ) {
                                                Text(
                                                    if (nombreItem.isNotBlank()) nombreItem else cidItem,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                if (nombreItem.isNotBlank())
                                                    Text(
                                                        cidItem,
                                                        fontSize = 12.sp,
                                                        color = Color.Gray
                                                    )
                                            }
                                            HorizontalDivider()
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                showClientesDialog = false
                            }) { Text("Cerrar") }
                        }
                    )
                }
            }


            Spacer(Modifier.height(8.dp))
        }

        Column(modifier = Modifier.fillMaxSize()) {

            ElevatedButton(
                enabled = selectedCid.isNotBlank(),
                onClick = {
                    actualizarActividad(context)
                    codigoInput = ""
                    nombreInput = ""
                    docIdToEdit = ""
                    isEditing = false
                    showDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF001F5B),
                    contentColor = Color.White
                )
            ) {
                Text(
                    "+ ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp,
                    color = Color.Green
                )
                Text("Crear Almacen")
            }

            LazyColumn(
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                items(items = localidades, key = { it.first }) { (codigo, nombre) ->

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        buildAnnotatedString {
                                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                                append(
                                                    "Código: "
                                                )
                                            }
                                            append(codigo)
                                            append("   ")
                                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                                append(
                                                    "Nombre: "
                                                )
                                            }
                                            append(nombre)
                                        }
                                    )
                                }

                                Row {
                                    IconButton(onClick = {
                                        // Editar
                                        isEditing = true
                                        docIdToEdit = codigo
                                        codigoInput = codigo
                                        nombreInput = nombre
                                        showDialog = true
                                    }) {
                                        Icon(
                                            Icons.Filled.Edit,
                                            contentDescription = "Editar"
                                        )
                                    }
                                    // Borrar
                                    IconButton(
                                        enabled = !isDeleting,                 // deshabilitado mientras borra
                                        onClick = {
                                            codeToDelete =
                                                codigo              // ← viene del Pair (codigo, nombre)
                                            showDeleteDialog = true            // ← abre el diálogo
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Eliminar"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- Dialogo Crear/Editar ---
            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text(if (isEditing) "Editar Localidad" else "Crear Localidad") },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = codigoInput,
                                onValueChange = {
                                    codigoInput = it.uppercase().trimStart()
                                },
                                label = { Text("Código* (ej. ALM_REP)") },
                                singleLine = true,
                                enabled = !isEditing
                            )
                            OutlinedTextField(
                                value = nombreInput,
                                onValueChange = { nombreInput = it.trimStart() },
                                label = { Text("Nombre de la Localidad*") },
                                singleLine = true
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (selectedCid.isBlank()) return@Button
                                if (codigoInput.isBlank() || nombreInput.isBlank()) return@Button

                                if (isEditing) {
                                    // EDITAR
                                    val cid = selectedCid.trim().uppercase()
                                    val data = mapOf(
                                        "codigo" to codigoInput.trim().uppercase(),
                                        "nombre" to nombreInput.trim(),
                                        "activo" to true,
                                        "clienteId" to cid
                                    )
                                    android.util.Log.d(
                                        "Localidades",
                                        "SET (EDIT) -> /clientes/$cid/localidades/${
                                            codigoInput.trim().uppercase()
                                        } payload=$data"
                                    )

                                    firestore.collection("clientes").document(cid)
                                        .collection("localidades")
                                        .document(codigoInput.trim().uppercase())
                                        .set(data)
                                        .addOnSuccessListener {
                                            showDialog = false
                                            successMessage = "Localidad actualizada"
                                            showSuccessDialog = true
                                            cargarLocalidades(cid, ctx)
                                            com.eriknivar.firebasedatabase.data.Audit.log(
                                                clienteId = cid,
                                                entidad = "localidad",
                                                entidadId = codigoInput.trim().uppercase(),
                                                accion = "UPDATE",
                                                byUid = Firebase.auth.currentUser?.uid
                                                    ?: (userViewModel.documentId.value ?: ""),
                                                byNombre = userViewModel.nombre.value ?: "",
                                                rol = userViewModel.tipo.value ?: ""
                                            )
                                        }
                                } else {
                                    val cid = selectedCid.trim().uppercase()
                                    val codigo = codigoInput.trim().uppercase()
                                    val nombre = nombreInput.trim()

                                    if (cid.isBlank() || codigo.isBlank() || nombre.isBlank()) return@Button

                                    isSaving = true

                                    LocalidadesRepo.crearLocalidad(
                                        codigoRaw = codigo,
                                        nombreRaw = nombre,
                                        clienteIdDestino = cid
                                    ) { ok, msg ->
                                        Log.d("TEST", "ok=$ok msg=$msg")
                                        isSaving = false
                                        if (ok) {
                                            showDialog = false
                                            successMessage = "Localidad $codigo guardada"
                                            showSuccessDialog = true
                                            cargarLocalidades(cid, ctx)

                                            com.eriknivar.firebasedatabase.data.Audit.log(
                                                clienteId = cid,
                                                entidad = "localidad",
                                                entidadId = codigo,
                                                accion = "CREATE",
                                                byUid = Firebase.auth.currentUser?.uid
                                                    ?: (userViewModel.documentId.value ?: ""),
                                                byNombre = userViewModel.nombre.value ?: "",
                                                rol = userViewModel.tipo.value ?: ""
                                            )
                                        } else {
                                            Log.e("Localidades", "CREATE FAILED: $msg")
                                            Toast.makeText(ctx, "Error: $msg", Toast.LENGTH_LONG)
                                                .show()
                                        }
                                    }
                                }


                            }
                        ) { Text(if (isEditing) "Guardar" else "Crear") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDialog = false }) { Text("Cancelar") }
                    }
                )
            }

            // --- Diálogo de éxito ---
            if (showSuccessDialog) {
                AlertDialog(
                    onDismissRequest = { showSuccessDialog = false },
                    title = { Text("Éxito") },
                    text = { Text(successMessage) },
                    confirmButton = {
                        TextButton(onClick = { showSuccessDialog = false }) { Text("OK") }
                    }
                )
            }

            // --- Diálogo de duplicado ---
            if (showNombreExistenteDialog) {
                AlertDialog(
                    onDismissRequest = { showNombreExistenteDialog = false },
                    title = { Text("Dato duplicado") },
                    text = { Text("Ya existe una localidad con ese código.") },
                    confirmButton = {
                        TextButton(onClick = {
                            showNombreExistenteDialog = false
                        }) { Text("OK") }
                    }
                )
            }

            // --- Diálogo de borrado (soft delete) ---
            if (showDeleteDialog && codeToDelete != null) {
                AlertDialog(
                    onDismissRequest = { if (!isDeleting) showDeleteDialog = false },
                    title = { Text("Eliminar ${codeToDelete}") },
                    text = { Text("¿Seguro que deseas eliminar esta localidad? Esta acción no se puede deshacer.") },
                    confirmButton = {
                        TextButton(
                            enabled = !isDeleting,
                            onClick = {
                                val cid = selectedCid.trim().uppercase()
                                if (cid == "__TODAS__") {
                                    Toast.makeText(
                                        ctx,
                                        "Selecciona un cliente específico",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@TextButton
                                }
                                isDeleting = true
                                LocalidadesRepo.borrarLocalidad(
                                    codigo = codeToDelete!!,
                                    clienteIdDestino = cid
                                ) { ok, msg ->
                                    isDeleting = false
                                    if (ok) {
                                        showDeleteDialog = false
                                        cargarLocalidades(selectedCid, ctx)
                                        Toast.makeText(
                                            ctx,
                                            "Eliminada ${codeToDelete}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        codeToDelete = null
                                    } else {
                                        Toast.makeText(ctx, "Error: $msg", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isDeleting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text("Eliminar")
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(
                            enabled = !isDeleting,
                            onClick = { showDeleteDialog = false; codeToDelete = null }
                        ) { Text("Cancelar") }
                    }
                )
            }

        }
    }
}

