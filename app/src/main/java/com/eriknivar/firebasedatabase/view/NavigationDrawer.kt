package com.eriknivar.firebasedatabase.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AddHome
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.platform.LocalSoftwareKeyboardController


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationDrawer(
    navController: NavHostController,
    storageType: String,
    userViewModel: UserViewModel,
    location: MutableState<String>,
    sku: MutableState<String>,
    quantity: MutableState<String>,
    lot: MutableState<String>,
    expirationDate: MutableState<String>,
    content: @Composable () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val userName by userViewModel.nombre.observeAsState("")
    val userType by userViewModel.tipo.observeAsState("")

    var showLogoutDialog by remember { mutableStateOf(false) }


    ModalNavigationDrawer(drawerState = drawerState, drawerContent = {
        ModalDrawerSheet {
            Column(
                modifier = Modifier
                    .fillMaxWidth(.7f)
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
                            tint = Color.Blue,
                            modifier = Modifier.size(65.dp)
                        )
                    }

                    EditableProfileImage(userName = userName)

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
                        color = Color.Blue,
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

                TextButton(onClick = {
                    navController.navigate("storagetype")
                    scope.launch { drawerState.close() }
                }) {
                    Row(
                        modifier = Modifier.padding(),
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddHome,
                            contentDescription = "",
                            tint = Color.Black,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Entrada de Inventario", color = Color.Black)
                    }
                }

                TextButton(onClick = {
                    navController.navigate("inventoryreports")
                    scope.launch { drawerState.close() }
                }) {
                    Row(
                        modifier = Modifier.padding(),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Report,
                            contentDescription = "",
                            tint = Color.Black
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reporte de Inventario", color = Color.Black)
                    }
                }

                TextButton(onClick = {
                    navController.navigate("settings") // o "configuracion", según tu ruta real
                    scope.launch { drawerState.close() }
                }) {
                    Row(
                        modifier = Modifier.padding(),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "",
                            tint = Color.Black
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Configuración", color = Color.Black)
                    }
                }
                // }


                TextButton(onClick = {
                    navController.navigate("masterdata")
                    scope.launch { drawerState.close() }
                }) {
                    Row(
                        modifier = Modifier.padding(),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Archive,
                            contentDescription = "",
                            tint = Color.Black
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Datos Maestro", color = Color.Black)
                    }
                }

                TextButton(
                    modifier = Modifier.padding(),
                    onClick = {
                        showLogoutDialog = true // 🔔 Solo mostrar el diálogo, nada más aquí
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "",
                            tint = Color.Black
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Salir", color = Color.Black)
                    }
                }

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

                                    limpiarCampos(
                                        location,
                                        sku,
                                        quantity,
                                        lot,
                                        expirationDate
                                    ) // 🧹 Limpiar campos
                                    userViewModel.clearUser() // 🧹 Limpiar usuario

                                    activity?.finishAffinity() // 🔥 Cerrar completamente la app
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
    })
    // El ModalNavigationDrawer tiene que contener el Scaffold
    {

        val customColorBackGround = Color(0xFF527782)
        val keyboardController = LocalSoftwareKeyboardController.current

        Scaffold(
            topBar = {
                TopAppBar(navigationIcon = {
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.weight(0.85f)) // Empuja el texto al centro

                        Text(
                            text = storageType,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )

                        Spacer(modifier = Modifier.weight(1.15f)) // Equilibra el espacio del otro lado
                    }
                })
            }) { innerPadding ->
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

