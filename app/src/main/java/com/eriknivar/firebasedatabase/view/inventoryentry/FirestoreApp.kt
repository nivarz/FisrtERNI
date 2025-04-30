package com.eriknivar.firebasedatabase.view.inventoryentry

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import com.eriknivar.firebasedatabase.view.NavigationDrawer
import com.eriknivar.firebasedatabase.view.storagetype.DataFields
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

    LaunchedEffect(usuario) {
        if (usuario.isNotEmpty()) {
            fetchDataFromFirestore(
                db = Firebase.firestore,
                allData = allData,
                usuario = usuario,
                listState = listState
            )
        }
    }

    val lastInteractionTime = remember { mutableLongStateOf(System.currentTimeMillis()) }

    fun actualizarActividad() {
        lastInteractionTime.longValue = System.currentTimeMillis()

    }

    LaunchedEffect(lastInteractionTime.longValue) {
        while (true) {
            delay(60_000)
            val tiempoActual = System.currentTimeMillis()
            val tiempoInactivo = tiempoActual - lastInteractionTime.longValue

            if (tiempoInactivo >= 1 * 60_000) {
                // 🧹 Limpiar los campos ANTES de salir
                sku.value = ""
                location.value = ""
                lot.value = ""
                dateText.value = ""
                quantity.value = ""
                productoDescripcion.value = ""
                unidadMedida.value = ""

                userViewModel.clearUser()

                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                }

                break
            }
        }
    }


    NavigationDrawer(navController, storageType, userViewModel, location, sku, quantity, lot, dateText) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = { }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    /*
                    BackHandler(true) {
                        Log.i("LOG_TAG", "Clicked back")
                    }
                    */

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedForm.value = !expandedForm.value }
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Captura de Inventario",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF003366),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )

                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = if (expandedForm.value) "Ocultar formulario" else "Mostrar formulario",
                            modifier = Modifier
                                .size(28.dp)
                                .graphicsLayer { rotationZ = if (expandedForm.value) 0f else 180f },
                            tint = Color.Blue
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 1.dp))

                    // 🔵 Esto sí depende de expandedForm
                    if (expandedForm.value && productoDescripcion.value.isNotBlank()) {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                            .animateContentSize(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
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
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }

                    // 🔵 El formulario SIEMPRE debe estar montado, no depende de expandedForm
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
                        isVisible = expandedForm.value,
                        onUserInteraction = { actualizarActividad() }

                    )


                    // 🧩 Lista de Cards de Inventario
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                        .background(Color.White),
                        state = listState
                    ) {
                        itemsIndexed(
                            items = allData,
                            key = { _, item -> item.documentId }
                        ) { index, item ->
                            MessageCard(
                                documentId = item.documentId,
                                location = item.location,
                                sku = item.sku,
                                lote = item.lote,
                                expirationDate = item.expirationDate,
                                quantity = item.quantity,
                                unidadMedida = item.unidadMedida,
                                firestore = Firebase.firestore,
                                allData = allData,
                                fechaRegistro = item.fechaRegistro,
                                descripcion = item.description,
                                onSuccess = { showSuccessDialog = true },
                                listState = listState,
                                index = index,
                                expandedStates = expandedStates
                            )
                        }
                    }
                }
            }


                // ✅ Snackbar centrado en pantalla
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 500.dp), // 🔼 Ajusta la altura según lo que necesites
                    contentAlignment = Alignment.BottomCenter // Centrado pero más arriba
                )
                {
                }
            }
        }
    }
