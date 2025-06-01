package com.eriknivar.firebasedatabase.view.masterdata

import android.content.Context
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.runtime.mutableLongStateOf
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
import com.eriknivar.firebasedatabase.view.NavigationDrawer
import com.eriknivar.firebasedatabase.view.utility.ScreenWithNetworkBanner
import com.eriknivar.firebasedatabase.view.utility.SessionUtils
import com.eriknivar.firebasedatabase.viewmodel.UserViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val dummyLocation = remember { mutableStateOf("") }
    val dummySku = remember { mutableStateOf("") }
    val dummyQuantity = remember { mutableStateOf("") }
    val dummyLot = remember { mutableStateOf("") }
    val dummyDateText = remember { mutableStateOf("") }

    val navyBlue = Color(0xFF001F5B)

    val lastInteractionTime = remember { mutableLongStateOf(SessionUtils.obtenerUltimaInteraccion(context)) }

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
                Toast.makeText(context, "Sesión finalizada por inactividad", Toast.LENGTH_LONG).show()

                userViewModel.clearUser()

                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                }

                break
            }
        }
    }

    val currentUserId = userViewModel.documentId.value ?: ""
    val currentSessionId = userViewModel.sessionId.value

    DisposableEffect(currentUserId, currentSessionId) {

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

    // Solo permitir acceso a admin o superuser
    val tipo = userViewModel.tipo.value ?: ""

    if (tipo.isNotBlank() && tipo.lowercase() != "admin" && tipo.lowercase() != "superuser") {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.White),
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

    fun cargarProductos() {
        isLoading = true
        firestore.collection("productos")
            .get()
            .addOnSuccessListener { result ->
                productos.clear()
                for (document in result) {
                    val codigo = document.id
                    val descripcion = document.getString("descripcion") ?: ""
                    val unidad = document.getString("UM") ?: ""
                    productos.add(Producto(codigo, codigo, descripcion, unidad))
                }
                productos.sortBy { it.descripcion }
                isLoading = false
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error al cargar productos", Toast.LENGTH_SHORT).show()
                isLoading = false
            }
    }

    ScreenWithNetworkBanner(
        showDisconnectedBanner = false,
        showRestoredBanner = false,
        onCloseDisconnected = {},
        onCloseRestored = {}
    ) {
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
                ElevatedButton(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = navyBlue, contentColor = Color.White
                    ),
                    onClick = {
                        actualizarActividad(context)
                        selectedProduct = null
                        codigoInput = ""
                        descripcionInput = ""
                        unidadInput = ""
                        showDialog = true
                    }, modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Agregar Producto")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = busqueda.uppercase(),
                    singleLine = true,
                    onValueChange = { busqueda = it },
                    label = { Text("Buscar por descripción") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                ElevatedButton(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = navyBlue, contentColor = Color.White
                    ),
                    onClick = {
                        actualizarActividad(context)
                        cargarProductos()
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) {
                        Text("Cargando...")
                    } else {
                        Text("Cargar Datos Maestro")
                    }
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
                    items(productosFiltrados) { producto ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(buildAnnotatedString {
                                    withStyle(style = SpanStyle(color = Color.Blue)) { append("Código: ") }
                                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                        append(
                                            producto.codigo
                                        )
                                    }
                                })
                                Text(buildAnnotatedString {
                                    withStyle(style = SpanStyle(color = Color.Blue)) { append("Descripción: ") }
                                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                        append(
                                            producto.descripcion
                                        )
                                    }
                                })
                                Text(buildAnnotatedString {
                                    withStyle(style = SpanStyle(color = Color.Blue)) { append("Unidad: ") }
                                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                        append(
                                            producto.unidad
                                        )
                                    }
                                })

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    IconButton(onClick = {
                                        selectedProduct = producto
                                        codigoInput = producto.codigo
                                        descripcionInput = producto.descripcion
                                        unidadInput = producto.unidad
                                        showDialog = true
                                    }) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Editar",
                                            tint = Color.Blue
                                        )
                                    }
                                    IconButton(onClick = {
                                        productoAEliminar = producto
                                        showDeleteDialog = true

                                    }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Eliminar",
                                            tint = Color.Red
                                        )
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
                                OutlinedTextField(
                                    value = codigoInput.uppercase(),
                                    singleLine = true,
                                    onValueChange = { codigoInput = it },
                                    label = { Text("Código") },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = selectedProduct == null
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
                            TextButton(onClick = {
                                if (codigoInput.isBlank() || descripcionInput.isBlank() || unidadInput.isBlank()) {
                                    Toast.makeText(
                                        context,
                                        "Completa todos los campos",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@TextButton
                                }

                                if (selectedProduct == null) {
                                    val existe = productos.any {
                                        it.codigo.equals(codigoInput, ignoreCase = true) ||
                                                it.descripcion.equals(
                                                    descripcionInput,
                                                    ignoreCase = true
                                                )
                                    }
                                    if (existe) {
                                        Toast.makeText(
                                            context,
                                            "Este producto ya existe",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        return@TextButton
                                    }
                                    firestore.collection("productos")
                                        .document(codigoInput)
                                        .set(
                                            mapOf(
                                                "descripcion" to descripcionInput.uppercase(),
                                                "UM" to unidadInput.uppercase()
                                            )
                                        )
                                        .addOnSuccessListener {
                                            productos.add(
                                                Producto(
                                                    codigoInput,
                                                    codigoInput,
                                                    descripcionInput.uppercase(),
                                                    unidadInput.uppercase()
                                                )
                                            )
                                            showDialog = false
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Producto agregado")
                                            }
                                        }
                                } else {
                                    val duplicado = productos.any {
                                        it.codigo != selectedProduct!!.codigo &&
                                                it.descripcion.equals(
                                                    descripcionInput,
                                                    ignoreCase = true
                                                ) &&
                                                it.unidad.equals(unidadInput, ignoreCase = true)
                                    }
                                    if (duplicado) {
                                        Toast.makeText(
                                            context,
                                            "Ya existe otro producto con esta descripción y unidad",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        return@TextButton
                                    }
                                    firestore.collection("productos")
                                        .document(selectedProduct!!.codigo)
                                        .update(
                                            mapOf(
                                                "descripcion" to descripcionInput.uppercase(),
                                                "UM" to unidadInput.uppercase()
                                            )
                                        )
                                        .addOnSuccessListener {
                                            val index =
                                                productos.indexOfFirst { it.codigo == selectedProduct!!.codigo }
                                            if (index != -1) {
                                                productos[index] = Producto(
                                                    selectedProduct!!.codigo,
                                                    selectedProduct!!.codigo,
                                                    descripcionInput,
                                                    unidadInput
                                                )
                                            }
                                            showDialog = false
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Producto actualizado")
                                            }
                                        }
                                }
                            }) {
                                Text("Guardar")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDialog = false }) {
                                Text("Cancelar")
                            }
                        }
                    )
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
                                }
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                firestore.collection("productos")
                                    .document(productoAEliminar!!.codigo).delete()
                                productos.remove(productoAEliminar)
                                showDeleteDialog = false
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Producto eliminado")
                                }
                            }) {
                                Text("Sí")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteDialog = false }) {
                                Text("Cancelar")
                            }
                        }
                    )
                }
            }
        }
    }
}








