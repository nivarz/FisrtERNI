package com.eriknivar.firebasedatabase.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.eriknivar.firebasedatabase.view.inventoryentry.limpiarCampos
import com.eriknivar.firebasedatabase.view.utility.EditableProfileImage
import com.eriknivar.firebasedatabase.viewmodel.UserViewModel
import kotlinx.coroutines.launch
import java.util.Locale
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextAlign
import com.eriknivar.firebasedatabase.view.utility.DrawerMenuItem
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationDrawer(
    navController: NavHostController,
    storageType: String,
    userViewModel: UserViewModel,
    location: MutableState<String>? = null,
    sku: MutableState<String>? = null,
    quantity: MutableState<String>? = null,
    lot: MutableState<String>? = null,
    expirationDate: MutableState<String>? = null,
    content: @Composable () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val userName by userViewModel.nombre.observeAsState("")
    val userType by userViewModel.tipo.observeAsState("")

    var showLogoutDialog by remember { mutableStateOf(false) }

    var isConfigExpanded by remember { mutableStateOf(false) }
    var isConteoExpanded by remember { mutableStateOf(false) }

    val isLoggedOut = userViewModel.nombre.observeAsState("").value.isEmpty()
    val isInitialized = userViewModel.isInitialized.observeAsState(false).value

    LaunchedEffect(drawerState.isClosed) {
        if (drawerState.isClosed) {
            isConfigExpanded = false
        }
    }

    LaunchedEffect(drawerState.isClosed) {
        if (drawerState.isClosed) {
            isConteoExpanded = false
        }
    }

    val scrollState = rememberScrollState()

    ModalNavigationDrawer(drawerState = drawerState, drawerContent = {
        ModalDrawerSheet {
            Column(
                modifier = Modifier
                    .fillMaxWidth(.75f)
                    .padding()
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, end = 16.dp, top = 16.dp)
                )
                {
                    IconButton(onClick = {
                        scope.launch { drawerState.close() }
                    }) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Cerrar menú",
                            tint = Color(0xFF003366),
                            modifier = Modifier.size(65.dp)
                        )
                    }

                    val documentId by userViewModel.documentId.observeAsState("")

                    EditableProfileImage(
                        userName,
                        documentId
                    )


                }

                fun capitalizarNombreCompleto(nombre: String): String {
                    return nombre
                        .lowercase()
                        .split(" ")
                        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                }


                Row(
                    modifier = Modifier
                        .padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Hola, ",
                        fontSize = 20.sp,
                        color = Color(0xFF003366),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = capitalizarNombreCompleto(userName),
                        fontSize = 16.sp,
                        color = Color.Black,
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    modifier = Modifier.padding(start = 16.dp, top = 0.dp, bottom = 8.dp),
                    text = userType.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Gray,
                    fontStyle = FontStyle.Italic
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                ) {

                    DrawerMenuItem(
                        icon = Icons.Default.Inventory, // Puedes usar otro si prefieres
                        label = "Entrada de Inventario",
                        isSubItem = false,
                        trailingIcon = if (isConteoExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        onClick = { isConteoExpanded = !isConteoExpanded }
                    )

                    if (isConteoExpanded) {
                        Spacer(modifier = Modifier.height(4.dp))

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            thickness = 0.5.dp,
                            color = Color.LightGray
                        )

                        DrawerMenuItem(
                            icon = Icons.Default.EditNote,
                            label = "Conteo",
                            isSubItem = true,
                            onClick = {
                                navController.navigate("storagetype")
                                scope.launch { drawerState.close() }
                            }
                        )

                        DrawerMenuItem(
                            icon = Icons.Default.AssignmentTurnedIn,
                            label = "Reconteo",
                            isSubItem = true,
                            onClick = {
                                navController.navigate("reconteoAsignado")
                                scope.launch { drawerState.close() }
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            thickness = 0.5.dp,
                            color = Color.LightGray
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    DrawerMenuItem(
                        icon = Icons.Default.BarChart,
                        label = "Reporte de Inventario",
                        onClick = {
                            navController.navigate("inventoryreports")
                            scope.launch { drawerState.close() }
                        }
                    )

                    DrawerMenuItem(
                        icon = Icons.Default.Settings,
                        label = "Configuración",
                        isSubItem = false,
                        trailingIcon = if (isConfigExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        onClick = { isConfigExpanded = !isConfigExpanded }
                    )

                    if (isConfigExpanded) {

                        Spacer(modifier = Modifier.height(4.dp))

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            thickness = 0.5.dp, // Más fina
                            color = Color.LightGray
                        )
                        DrawerMenuItem(
                            icon = Icons.Filled.Group,     // si no compila, usa Icons.Outlined.Group
                            label = "Clientes",
                            isSubItem = true,
                            onClick = {
                                navController.navigate("clientes")   // <- ruta del listado
                                scope.launch { drawerState.close() }
                            }
                        )
                        DrawerMenuItem(
                            icon = Icons.Default.Person,
                            label = "Usuarios",
                            isSubItem = true,
                            onClick = {
                                navController.navigate("usuarios")
                                scope.launch { drawerState.close() }
                            }
                        )
                        DrawerMenuItem(
                            icon = Icons.Default.LocationOn,
                            label = "Ubicaciones",
                            isSubItem = true,
                            onClick = {
                                navController.navigate("ubicaciones")
                                scope.launch { drawerState.close() }
                            }
                        )
                        DrawerMenuItem(
                            icon = Icons.Default.Map,
                            label = "Localidades",
                            isSubItem = true,
                            onClick = {
                                navController.navigate("localidades")
                                scope.launch { drawerState.close() }
                            }
                        )
                        DrawerMenuItem(
                            icon = Icons.Default.History,
                            label = "Auditoría de Registros",
                            isSubItem = true,
                            onClick = {
                                navController.navigate("auditoria")
                                scope.launch { drawerState.close() }
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            thickness = 0.5.dp, // Más fina
                            color = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                    }

                    DrawerMenuItem(
                        icon = Icons.Default.FolderSpecial,
                        label = "Datos Maestro",
                        onClick = {
                            navController.navigate("masterdata")
                            scope.launch { drawerState.close() }
                        }
                    )

                    DrawerMenuItem(
                        icon = Icons.AutoMirrored.Filled.ExitToApp,
                        label = "Salir",
                        onClick = {
                            showLogoutDialog = true
                        }
                    )

                    if (showLogoutDialog) {
                        val activity = LocalActivity.current

                        AlertDialog(
                            onDismissRequest = { showLogoutDialog = false },
                            title = { Text("Cerrar sesión") },
                            text = { Text("¿Estás seguro de que deseas cerrar sesión?") },
                            confirmButton = {
                                TextButton(
                                    modifier = Modifier.padding(),
                                    onClick = {
                                        showLogoutDialog = false // 🔵 Cerrar el diálogo

                                        val documentId = userViewModel.documentId.value ?: ""

                                        userViewModel.isManualLogout.value = true

                                        Firebase.firestore.collection("usuarios")
                                            .document(documentId)
                                            .update("sessionId", "")
                                            .addOnCompleteListener {
                                                userViewModel.clearUser() // 🧹 Limpiar usuario
                                                userViewModel.isManualLogout.value =
                                                    false // ✅ Restaurar bandera

                                                limpiarCampos(
                                                    location = location ?: mutableStateOf(""),
                                                    sku = sku ?: mutableStateOf(""),
                                                    quantity = quantity ?: mutableStateOf(""),
                                                    lot = lot ?: mutableStateOf(""),
                                                    expirationDate = expirationDate
                                                        ?: mutableStateOf("")
                                                )

                                                userViewModel.activarRecargaUsuarios()

                                                activity?.finishAffinity() // 🔥 Cerrar completamente la app
                                            }
                                    }
                                ) {
                                    Text("Sí")
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    modifier = Modifier.padding(),
                                    onClick = {
                                        showLogoutDialog =
                                            false // Solo cierra el diálogo si presiona "Cancelar"
                                    }
                                ) {
                                    Text("Cancelar")
                                }
                            }
                        )
                    }
                }

            }
        }
    })
    // El ModalNavigationDrawer tiene que contener el Scaffold
    {

        val customColorBackGround = Color(0xFF527782)
        val keyboardController = LocalSoftwareKeyboardController.current

        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = {
                            keyboardController?.hide() // 🔽 Oculta el teclado si está activo

                            scope.launch {
                                if (drawerState.isClosed) {
                                    drawerState.open()
                                } else {
                                    drawerState.close()
                                }
                            }


                        }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                tint = Color.White,
                                modifier = Modifier.size(30.dp),
                                contentDescription = "Menu"

                            )
                        }

                    }, colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = customColorBackGround, titleContentColor = Color.Black,
                    ), title = {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            // Spacer(modifier = Modifier.weight(0.85f)) // Empuja el texto al centro


                            Text(
                                text = storageType,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                fontSize = 18.sp,

                                )


                            //Spacer(modifier = Modifier.weight(1.15f)) // Equilibra el espacio del otro lado
                        }
                    })
            }) { innerPadding ->

            LaunchedEffect(isLoggedOut, isInitialized) {
                if (isInitialized && isLoggedOut && navController.currentDestination?.route != "login" && !userViewModel.isManualLogout.value) {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxWidth()
            ) {
                content()

            }
        }
    }
}

