package com.eriknivar.firebasedatabase.view.settings.settingsmenu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.FloatingActionButtonDefaults.elevation
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eriknivar.firebasedatabase.navigation.NavigationDrawer
import com.eriknivar.firebasedatabase.navigation.Rutas
import com.eriknivar.firebasedatabase.viewmodel.ClientesListViewModel
import com.eriknivar.firebasedatabase.viewmodel.UserViewModel
import com.eriknivar.firebasedatabase.view.utility.ClientesRepository
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClientesListScreen(
    ui: ClientesListViewModel.UiState,
    onNew: () -> Unit,
    onEdit: (String) -> Unit,
    onRetry: () -> Unit,
    onQueryChange: (String) -> Unit = {},
    onLoadMore: () -> Unit = {},
    onToggleRequest: (ClientesRepository.ClienteListItem, Boolean) -> Unit,
    onDeleteRequest: (ClientesRepository.ClienteListItem) -> Unit,
    showDelete: Boolean

) {
    val snackbarHost = remember { SnackbarHostState() }

    val BrandGreen = Color(0xFF527782)

    // Tamaños típicos de FAB + margen
    val fabSize = 56.dp
    val fabMargin = 16.dp
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val extraBottom = fabSize + fabMargin * 2 + navInset

    LaunchedEffect(ui.error) {
        ui.error?.let { snackbarHost.showSnackbar(it, withDismissAction = true) }
    }


    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNew,
                containerColor = BrandGreen,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 8.dp
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo cliente")
            }
        },
        floatingActionButtonPosition = FabPosition.End // 👈 opcional, pero recomendado
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Text(
                text = "Clientes",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            OutlinedTextField(
                value = ui.query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                singleLine = true,
                label = { Text("Buscar por nombre o RNC") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (ui.query.isNotBlank()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpiar")
                        }
                    }
                }
            )

            Spacer(Modifier.height(8.dp))

            when {
                ui.loading && ui.items.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                ui.error != null && ui.items.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Ocurrió un error")
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = onRetry) { Text("Reintentar") }
                    }
                }

                ui.empty -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Sin clientes por el momento")
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = onNew) { Text("Crear cliente") }
                    }
                }

                else -> {
                    val listState = rememberLazyListState()

                    // 👇 1) Calculamos el extra inferior para que el FAB no tape los items
                    val fabSize = 56.dp
                    val fabMargin = 16.dp
                    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    val extraBottom = fabSize + fabMargin * 2 + navInset

                    val shouldLoadMore by remember {
                        derivedStateOf {
                            val lastVisible =
                                listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                            lastVisible >= (ui.items.lastIndex - 3) && !ui.endReached
                        }
                    }
                    LaunchedEffect(shouldLoadMore, ui.loading, ui.endReached) {
                        if (shouldLoadMore && !ui.loading && !ui.endReached) onLoadMore()
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 12.dp,
                            top = 12.dp,
                            end = 12.dp,
                            bottom = 12.dp + extraBottom
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(ui.items, key = { it.clienteId }) { item ->
                            ClienteCard(
                                item = item,
                                onClick = { onEdit(item.clienteId) },
                                onToggleRequest = { target, activar ->
                                    onToggleRequest(
                                        target,
                                        activar
                                    )
                                },
                                onEdit = { onEdit(item.clienteId) },
                                onDeleteRequest = { onDeleteRequest(it) },   // 👈 pasa callback
                                showDelete = showDelete                      // 👈 propaga flag de rol
                            )
                        }


                        if (ui.loading && ui.items.isNotEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) { CircularProgressIndicator() }
                            }
                        }
                    }
                }
            }
        }
    }



}

@Composable
private fun ClienteCard(
    item: ClientesRepository.ClienteListItem,
    onClick: () -> Unit,
    onToggleRequest: (ClientesRepository.ClienteListItem, Boolean) -> Unit,
    onEdit: () -> Unit,
    onDeleteRequest: (ClientesRepository.ClienteListItem) -> Unit,
    showDelete: Boolean
) {
    var menuOpen by remember { mutableStateOf(false) }

    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.nombreComercial,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                EstadoChip(activo = item.activo)
                Spacer(Modifier.width(4.dp))

                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Más acciones"
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Editar") },
                            leadingIcon = { Icon(Icons.Default.Edit, null) },
                            onClick = { menuOpen = false; onEdit() }
                        )
                        DropdownMenuItem(
                            text = { Text(if (item.activo) "Desactivar" else "Activar") },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (item.activo) Icons.Default.Block else Icons.Default.Check,
                                    contentDescription = null
                                )
                            },
                            onClick = { menuOpen = false; onToggleRequest(item, !item.activo) }
                        )
                        if (showDelete) {
                            DropdownMenuItem(
                                text = { Text("Eliminar") },
                                leadingIcon = { Icon(Icons.Default.Delete, null) },
                                onClick = { menuOpen = false; onDeleteRequest(item) }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = "${item.clienteId} • ${item.rncOCedula}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


@Composable
private fun EstadoChip(activo: Boolean) {
    val bg = if (activo) Color(0xFFE6F4EA) else Color(0xFFFFEBEE)
    val fg = if (activo) Color(0xFF137333) else Color(0xFFB00020)
    Box(
        modifier = Modifier
            .background(bg, shape = MaterialTheme.shapes.small)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = if (activo) "ACTIVO" else "INACTIVO",
            color = fg,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
fun ClientesScreen(
    navController: NavHostController,
    userViewModel: UserViewModel,
    vm: ClientesListViewModel = viewModel()
) {
    // Guard de sesión (igual a tus otras pantallas)
    val isLoggedOut = userViewModel.nombre.observeAsState("").value.isEmpty()
    val isInitialized = userViewModel.isInitialized.observeAsState(false).value
    if (isInitialized && isLoggedOut) return

    val esSuper = (userViewModel.tipo.value ?: "").equals("superuser", ignoreCase = true)


    val refreshFlag by navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getLiveData<Boolean>("clientes_refresh")
        ?.observeAsState() ?: mutableStateOf(false)

    LaunchedEffect(refreshFlag) {
        if (refreshFlag == true) {
            vm.retry() // o vm.reload() si creaste ese método
            navController.currentBackStackEntry
                ?.savedStateHandle
                ?.remove<Boolean>("clientes_refresh")
        }
    }

    val tipo = userViewModel.tipo.value.orEmpty()
    if (tipo.lowercase() != "superuser") {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Text(
                text = "⛔ Acceso restringido",
                color = Color.Red,
                fontSize = MaterialTheme.typography.titleMedium.fontSize,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(32.dp)
            )
        }
        return
    }

    // Dummies requeridos por tu NavigationDrawer
    val dummyLocation = remember { mutableStateOf("") }
    val dummySku = remember { mutableStateOf("") }
    val dummyQuantity = remember { mutableStateOf("") }
    val dummyLot = remember { mutableStateOf("") }
    val dummyDateText = remember { mutableStateOf("") }

    var pendingToggle by remember {
        mutableStateOf<Pair<ClientesRepository.ClienteListItem, Boolean>?>(null)
    }
    var pendingDelete by remember { mutableStateOf<ClientesRepository.ClienteListItem?>(null) }


    NavigationDrawer(
        navController = navController,
        storageType = "Clientes",
        userViewModel = userViewModel,
        location = dummyLocation,
        sku = dummySku,
        quantity = dummyQuantity,
        lot = dummyLot,
        expirationDate = dummyDateText
    ) {
        val ui = vm.ui.collectAsState().value
        val esSuper = (userViewModel.tipo.value ?: "")
            .equals("superuser", ignoreCase = true)

        ClientesListScreen(
            ui = ui,
            onNew = { navController.navigate(Rutas.CLIENTE_FORM) },
            onEdit = { id -> navController.navigate("${Rutas.CLIENTE_FORM}?${Rutas.ARG_CLIENTE_ID}=$id") },
            onRetry = { vm.retry() },
            onQueryChange = { vm.onQueryChange(it) },
            onLoadMore = { vm.loadNextPage() },
            onToggleRequest = { item, activar -> pendingToggle = item to activar },
            onDeleteRequest = { item -> pendingDelete = item },
            showDelete = esSuper

        )
    }

    // Diálogo de motivo
    val deleteItem = pendingDelete
    if (deleteItem != null) {
        MotivoDialog(
            visible = true,
            titulo = "Eliminar cliente",
            onDismiss = { pendingDelete = null },
            onConfirm = { motivo ->
                val uid =
                    com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
                vm.eliminar(deleteItem, motivo, uid)   // 👈 llama al ViewModel
                pendingDelete = null
            }
        )
    }

    // Diálogo de motivo
    val togglePair = pendingToggle
    if (togglePair != null) {
        val (item, activar) = togglePair
        MotivoDialog(
            visible = true,
            titulo = if (activar) "Activar cliente" else "Desactivar cliente",
            onDismiss = { pendingToggle = null },
            onConfirm = { motivo ->
                val uid =
                    com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
                vm.cambiarEstado(item, activar, motivo, uid)
                pendingToggle = null
            }
        )
    }
}


@Composable
private fun MotivoDialog(
    visible: Boolean,
    titulo: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    if (!visible) return
    var motivo by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titulo) },
        text = {
            OutlinedTextField(
                value = motivo,
                onValueChange = { motivo = it },
                label = { Text("Motivo") },
                singleLine = false
            )
        },
        confirmButton = {
            TextButton(
                enabled = motivo.isNotBlank(),
                onClick = { onConfirm(motivo.trim()) }
            ) { Text("Aceptar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

