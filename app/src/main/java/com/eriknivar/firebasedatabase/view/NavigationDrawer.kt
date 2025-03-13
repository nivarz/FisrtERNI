package com.eriknivar.firebasedatabase.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AddHome
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Report
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.eriknivar.firebasedatabase.R
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationDrawer(
    navController: NavHostController,
    content: @Composable () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(.7f)
                        .padding()
                ) {

                    Image(
                        modifier = Modifier
                            .padding(16.dp)
                            .size(70.dp)
                            .clip(CircleShape),
                        painter = painterResource(id = R.drawable.erik),
                        contentDescription = "",
                        contentScale = ContentScale.Fit
                    )

                    Text(
                        modifier = Modifier.padding(16.dp, 8.dp, 0.dp, 0.dp),
                        text = "Hola, Erik",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Blue,
                        fontStyle = FontStyle.Normal,
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    TextButton(
                        onClick = {
                            navController.navigate("storagetype")
                            scope.launch { drawerState.close() }
                        }
                    ) {
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

                    TextButton(
                        onClick = {
                            navController.navigate("inventoryreports")
                            scope.launch { drawerState.close() }
                        }
                    ) {
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

                    TextButton(
                        onClick = {
                            navController.navigate("editscounts")
                            scope.launch { drawerState.close() }
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(),
                        ) {
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = "",
                                tint = Color.Black
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Editar Conteo", color = Color.Black)
                        }
                    }

                    TextButton(
                        onClick = {
                            navController.navigate("masterdata")
                            scope.launch { drawerState.close() }
                        }
                    ) {
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
                        onClick = { navController.navigate("login") }
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
                }
            }
        }
    )// El ModalNavigationDrawer tiene que contener el Scaffold


    {

        val customColorBackGround = Color(0xFF527782)

        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = {

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
                        Text(
                            text = "ERNI-Inventory", modifier = Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
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

