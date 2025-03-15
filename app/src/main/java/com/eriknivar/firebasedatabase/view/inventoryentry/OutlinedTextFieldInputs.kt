package com.eriknivar.firebasedatabase.view.inventoryentry

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eriknivar.firebasedatabase.view.storagetype.DataFields
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.zxing.integration.android.IntentIntegrator

@Composable
fun OutlinedTextFieldsInputs(productoDescripcion: MutableState<String>) {

    var location by remember { mutableStateOf("") }
    var sku by remember { mutableStateOf("") }
    var lot by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }

    val firestore = Firebase.firestore
    val allData = remember { mutableStateListOf<DataFields>() }
    val dateText = remember { mutableStateOf("") }

    val qrCodeContentLocation = remember { mutableStateOf("") } //esto es para el scanner de QRCode
    val qrCodeContentSku = remember { mutableStateOf("") } //esto es para el scanner de QRCode
    val qrCodeContentLot = remember { mutableStateOf("") } //esto es para el scanner de QRCode
    val qrCodeContentQuantity = remember { mutableStateOf("") } //esto es para el scanner de QRCode

    val db = FirebaseFirestore.getInstance()

    // Para ocultar el teclado val focusManager = LocalFocusManager.current

    // Lista de descripciones de productos obtenidas de Firestore
    var productDescriptions by remember { mutableStateOf(listOf<String>()) }

    // 📌 Estados para los diálogos
    var showProductDialog by remember { mutableStateOf(false) } // 🔥 Para la lista de productos
    var showErrorDialog by remember { mutableStateOf(false) } // 🔥 Para el mensaje de error

    var productList by remember { mutableStateOf(listOf<String>()) }
    var productMap by remember { mutableStateOf(mapOf<String, Pair<String, String>>()) }


    LaunchedEffect(Unit) {
        db.collection("productos").get().addOnSuccessListener { result ->
            productDescriptions = result.documents.mapNotNull { it.getString("descripcion") }
        }.addOnFailureListener { e ->
            Log.e("Firestore", "Error al obtener descripciones: ", e)
        }
    }

    val qrScanLauncherLocation =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            val intentResult = IntentIntegrator.parseActivityResult(result.resultCode, data)
            if (intentResult != null) {
                if (intentResult.contents != null) {
                    qrCodeContentLocation.value = intentResult.contents
                } else {
                    qrCodeContentLocation.value = "Codigo No Encontrado"
                }
            }
        }

    val qrScanLauncherSku =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            val intentResult = IntentIntegrator.parseActivityResult(result.resultCode, data)
            if (intentResult != null) {
                if (intentResult.contents != null) {
                    qrCodeContentSku.value = intentResult.contents
                } else {
                    qrCodeContentSku.value = "Codigo No Encontrado"
                }
            }
        }

    val qrScanLauncherLot =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            val intentResult = IntentIntegrator.parseActivityResult(result.resultCode, data)
            if (intentResult != null) {
                if (intentResult.contents != null) {
                    qrCodeContentLot.value = intentResult.contents
                } else {
                    qrCodeContentLot.value = "Codigo No Encontrado"
                }
            }
        }


    val qrCodeScannerLocation = remember { QRCodeScanner(qrScanLauncherLocation) }
    val qrCodeScannerSku = remember { QRCodeScanner(qrScanLauncherSku) }
    val qrCodeScannerLot = remember { QRCodeScanner(qrScanLauncherLot) }

    val context = LocalContext.current
    var showError by remember { mutableStateOf(false) }// Para validar los campos vacios
    var showError1 by remember { mutableStateOf(false) }
    var showError2 by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) } // Estado para mostrar el cuadro de diálogo
    var showDialog1 by remember { mutableStateOf(false) }
    var showDialog2 by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") } // Mensaje de error para el cuadro de diálogo
    var errorMessage1 by remember { mutableStateOf("") }
    var errorMessage2 by remember { mutableStateOf("") }
    var showErrorQuantity by remember { mutableStateOf(false) }
    var errorMessageQuantity by remember { mutableStateOf("") }

    var unidadMedida by remember { mutableStateOf("") } // ✅ Agrega esto en `OutlinedTextFieldsInputs`


    LaunchedEffect(qrCodeContentLocation.value) {
        location = qrCodeContentLocation.value.uppercase()
    }


    LaunchedEffect(qrCodeContentSku.value) {
        sku = qrCodeContentSku.value.uppercase()

    }


    LaunchedEffect(qrCodeContentLot.value) {
        lot = qrCodeContentLot.value.uppercase()
    }


    LaunchedEffect(qrCodeContentQuantity.value) {
        quantity = qrCodeContentQuantity.value.uppercase()
    }


    LaunchedEffect(Unit) {
        fetchDataFromFirestore(firestore, allData)
    }


    Column(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .padding(8.dp, 0.dp, 40.dp, 0.dp)// 📌 Ajusta el padding, digase la columna donde estan los campos
    ) {

        // 📌 CAMPO DE TEXTO PARA LA UBICACION

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically // 📌 Asegura alineación vertical
        ) {
            OutlinedTextField(modifier = Modifier
                .weight(2f) // 📌 Hace que el campo de texto ocupe el espacio disponible
                .padding(2.dp),
                singleLine = true,
                label = { Text(text = "Ubicación") },
                value = location,
                onValueChange = { newValue ->
                    location = newValue.uppercase()
                    qrCodeContentLocation.value = newValue.uppercase()
                    if (newValue.isNotEmpty()) {
                        showError = false // ✅ Si hay un valor, ocultar el error

                    }
                },
                isError = showError && (location.isEmpty() || location == "CODIGO NO ENCONTRADO"),

                trailingIcon = {
                    Row {
                        IconButton(
                            onClick = { qrCodeScannerLocation.startQRCodeScanner(context as android.app.Activity) },
                            modifier = Modifier.size(50.dp) // 📌 Tamaño del botón
                        ) {
                            Icon(
                                imageVector = Icons.Filled.QrCodeScanner,
                                contentDescription = "Escanear Código",
                            )
                        }
                    }
                })
        }

        // 📌 CAMPO DE TEXTO PARA EL SKU

        Row(
            modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(modifier = Modifier
                .weight(2f)
                .padding(2.dp)
                .onFocusChanged { focusState ->
                    if (!focusState.isFocused && sku.isNotEmpty() && sku != "CODIGO NO ENCONTRADO") {
                        findProductDescription(db, sku) { descripcion ->
                            productoDescripcion.value = descripcion // 🔥 Actualiza la descripción
                        }
                    }
                },
                singleLine = true,
                label = { Text(text = "Código Producto") },
                value = sku,
                onValueChange = { newValue ->
                    sku = newValue.uppercase()
                    qrCodeContentSku.value = newValue.uppercase()
                    showError = false
                },
                isError = showError && (sku.isEmpty() || sku == "CODIGO NO ENCONTRADO"),
                trailingIcon = {
                    Row {
                        // 📌 Botón para abrir la lista de productos
                        IconButton(
                            onClick = {
                                buscarProductos(db) { lista, mapa ->
                                    productList = lista
                                    productMap = mapa
                                    showProductDialog = true // 🔥 Abre el diálogo de productos
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Buscar productos"
                            )

                        }

                        // 📌 Botón para escanear código QR
                        IconButton(onClick = { qrCodeScannerSku.startQRCodeScanner(context as android.app.Activity) }) {
                            Icon(
                                imageVector = Icons.Filled.QrCodeScanner,
                                contentDescription = "Escanear Código"
                            )
                        }
                    }
                })
        }

        // 🔽 🔥 Diálogo de Lista de Productos (Pantalla Completa)

        if (showProductDialog) {
            var searchQuery by remember { mutableStateOf("") } // Estado para la búsqueda
            var isLoading by remember { mutableStateOf(true) } // Estado para mostrar el loading

            // Llamar a la función de búsqueda de productos cuando se abra el diálogo

            LaunchedEffect(Unit) {
                isLoading = true // 🔥 Muestra el indicador de carga antes de obtener los datos
                buscarProductos(db) { lista, mapa ->
                    productList = lista.sorted() // 🔥 Ordena los productos alfabéticamente
                    productMap = mapa
                    isLoading = false // 🔥 Oculta el loading cuando se cargan los datos
                }
            }

            AlertDialog(
                onDismissRequest = { showProductDialog = false },
                confirmButton = {
                    TextButton(onClick = { showProductDialog = false }) {
                        Text("Cerrar")
                    }
                },
                title = { Text("Selecciona un Producto") },
                text = {
                    Column {
                        // 🔍 Campo de búsqueda
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Buscar producto") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        )

                        // 🔥 Mostrar indicador de carga mientras los productos se obtienen
                        if (isLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        } else {
                            // 🔥 Filtrar y ordenar productos por orden alfabético
                            val filteredProducts = productList.filter { it.contains(searchQuery, ignoreCase = true) }

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 0.dp) // 🔥 Reduce espacio vertical general
                            ) {
                                items(filteredProducts) { descripcion ->
                                    TextButton(
                                        onClick = {
                                            val productoSeleccionado = productMap[descripcion]
                                            if (productoSeleccionado != null) {
                                                val (codigoSeleccionado, unidadMedidaSeleccionada) = productoSeleccionado
                                                sku = codigoSeleccionado
                                                qrCodeContentSku.value = codigoSeleccionado
                                                productoDescripcion.value = descripcion
                                                unidadMedida = unidadMedidaSeleccionada // ✅ Actualiza correctamente la unidad de medida
                                            }
                                            showProductDialog = false // 🔥 Cierra el diálogo
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 0.dp, vertical = 0.dp) // 🔥 Espaciado mínimo
                                    ) {
                                        Text(
                                            text = descripcion,
                                            fontSize = 14.sp,
                                            color = Color.Black,
                                            textAlign = TextAlign.Start,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis, // 🔥 Agrega "..." si el texto es muy largo
                                            modifier = Modifier
                                                .padding(vertical = 0.dp)
                                                .fillMaxWidth()
                                        )
                                    }

                                    HorizontalDivider(
                                        color = Color.Gray, // Color de la línea
                                        thickness = 1.dp, // Grosor de la línea
                                        modifier = Modifier.padding(horizontal = 8.dp) // Espaciado lateral
                                    )
                                }
                            }
                        }
                    }
                }
            )
        }

        // 📌 CAMPO DE TEXTO PARA EL LOTE

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically // 📌 Asegura alineación vertical
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .weight(2f) // 📌 Hace que el campo de texto ocupe el espacio disponible
                    .padding(2.dp),
                singleLine = true,
                label = { Text(text = "Lote") },
                value = lot,
                onValueChange = { newValue ->
                    lot = newValue.uppercase()
                    qrCodeContentLot.value = newValue.uppercase()
                },

                trailingIcon = {
                    Row {
                        IconButton(
                            onClick = { qrCodeScannerLot.startQRCodeScanner(context as android.app.Activity) },
                            modifier = Modifier.size(50.dp) // 📌 Tamaño del botón
                        ) {
                            Icon(
                                imageVector = Icons.Filled.QrCodeScanner,
                                contentDescription = "Escanear Código",
                            )
                        }
                    }
                })
        }

        DatePickerTextField(dateText)// FUNCION PARA EL CALENDARIO


        // 📌 CAMPO DE TEXTO PARA LA CANTIDAD

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically // 📌 Asegura alineación vertical
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .size(247.dp, 70.dp)
                    .padding(2.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),// ACTIVA EL TECLADO NUMERICO
                label = { Text(text = "Cantidad") },
                value = quantity,
                onValueChange = { newValue ->
                    if (newValue == ".") { // Si el usuario solo ingresa un punto
                        showErrorQuantity = true
                        errorMessageQuantity = "Ingrese un número válido"
                    } else if (newValue.matches(Regex("^\\d*\\.?\\d*\$"))) { // Permite números y un solo punto
                        showErrorQuantity = false
                        quantity = newValue
                    } else {
                        showErrorQuantity = true
                        errorMessageQuantity = "Formato incorrecto"
                    }
                },
                isError = showError && quantity.isEmpty()
            )

            if (showErrorQuantity) {
                Text(
                    text = errorMessageQuantity,
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                )
            }

        }

        // 🔽 🔥 Diálogo de Campos Obligatorios

        if (showErrorDialog) {
            AlertDialog(onDismissRequest = { showErrorDialog = false },
                confirmButton = {
                    TextButton(onClick = { showErrorDialog = false }) {
                        Text("Aceptar")
                    }
                },
                title = { Text("Error") },
                text = { Text("Campos Obligatorios Vacíos. Completa los datos para continuar.") })
        }


        // val quantityValue =
       // quantity.toDoubleOrNull() ?: 0.00  // ✅ Permite decimales y evita errores


        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                if (location.isEmpty() || sku.isEmpty() || quantity.isEmpty()) {
                    errorMessage = "Campos Obligatorios Vacíos"
                    showDialog = true // 🔴 Activa el cuadro de diálogo si hay campos vacíos
                    showError = true

                } else if (location == "CODIGO NO ENCONTRADO" || sku == "CODIGO NO ENCONTRADO") {
                    errorMessage1 = "Codigo No Encontrado"
                    showDialog1 = true // 🔴 Activa el cuadro de diálogo si hay campos vacíos
                    showError1 = true

                } else if (lot == "CODIGO NO ENCONTRADO" || dateText.value.isEmpty()) {
                    lot = "N/A"
                    dateText.value = "N/A"

                } else if (productoDescripcion.value == "Sin descripción") {
                    errorMessage2 = "Producto No Encontrado"
                    showDialog2 = true // 🔴 Activa el cuadro de diálogo si hay campos vacíos
                    showError2 = true


                } else {
                    showError = false
                    errorMessage = ""
                    showError1 = false
                    errorMessage1 = ""
                    showError2 = false
                    errorMessage2 = ""

                    saveToFirestore(
                        firestore,
                        location,
                        sku,
                        productoDescripcion.value,
                        lot,
                        dateText.value,
                        quantity.toDoubleOrNull() ?: 0.0,
                        unidadMedida,
                        allData
                    )
                    location = ""
                    sku = ""
                    lot = ""
                    dateText.value = ""
                    quantity = ""
                    productoDescripcion.value = ""
                }

            },

            modifier = Modifier.fillMaxHeight(0.18f)

        )

        {
            Text("Grabar Registro")
        }

        if (showDialog) {
            AlertDialog(onDismissRequest = {
                showDialog = true
            }, // No se cierra al tocar fuera del cuadro
                title = { Text("Campos Obligatorios") },
                text = { Text("Por favor, completa todos los campos requeridos antes de continuar.") },
                confirmButton = {
                    Button(onClick = { showDialog = false }) {
                        Text("Aceptar")
                    }
                })
        }
        if (showDialog1) {
            AlertDialog(onDismissRequest = {
                showDialog1 = true
            }, // No se cierra al tocar fuera del cuadro
                title = { Text("Codigo No Encontrado") },
                text = { Text("Por favor, completa todos los campos requeridos antes de continuar.") },
                confirmButton = {
                    Button(onClick = { showDialog1 = false }) {
                        Text("Aceptar")
                    }
                })
        }
        if (showDialog2) {
            AlertDialog(onDismissRequest = {
                showDialog2 = true
            }, // No se cierra al tocar fuera del cuadro
                title = { Text("Producto No Encontrado"); Color.Red },
                text = { Text("Por favor, completa todos los campos requeridos antes de continuar.") },
                confirmButton = {
                    Button(onClick = { showDialog2 = false }) {
                        Text("Aceptar")
                    }
                })
        }

    }
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(allData) { allDataShow ->
            MessageCard(
                allDataShow.documentId,
                allDataShow.location,
                allDataShow.sku,
                allDataShow.lote,
                allDataShow.expirationDate,
                allDataShow.quantity,
                firestore,
                allData
            )
        }
    }
}

