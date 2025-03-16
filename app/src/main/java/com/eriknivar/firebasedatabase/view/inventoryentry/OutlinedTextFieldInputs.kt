package com.eriknivar.firebasedatabase.view.inventoryentry

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eriknivar.firebasedatabase.view.storagetype.DataFields
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.zxing.integration.android.IntentIntegrator

@Composable
fun OutlinedTextFieldsInputs(productoDescripcion: MutableState<String>) {

    //var sku by remember { mutableStateOf("") }
    val sku = remember { mutableStateOf("") }
    val qrCodeContentSku = remember { mutableStateOf("") } //esto es para el scanner de QRCode
    val unidadMedida = remember { mutableStateOf("") } // ✅ Agrega esto en `OutlinedTextFieldsInputs`
    val showProductDialog = remember { mutableStateOf(false) } // 🔥 Para la lista de productos
    val productDescriptions = remember { mutableStateOf(emptyList<String>()) }
    val productList = remember { mutableStateOf(emptyList<String>()) }
    val productMap = remember { mutableStateOf(emptyMap<String, Pair<String, String>>()) }



    var lot by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }

    val firestore = Firebase.firestore
    val allData = remember { mutableStateListOf<DataFields>() }
    val dateText = remember { mutableStateOf("") }
    val location = remember { mutableStateOf("") } // 🔥 Debe ser MutableState<String>


    val qrCodeContentLot = remember { mutableStateOf("") } //esto es para el scanner de QRCode
    val qrCodeContentQuantity = remember { mutableStateOf("") } //esto es para el scanner de QRCode

    val db = FirebaseFirestore.getInstance()

    // Para ocultar el teclado val focusManager = LocalFocusManager.current


    var showErrorDialog by remember { mutableStateOf(false) } // 🔥 Para el mensaje de error


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


    val qrCodeScannerSku = remember { QRCodeScanner(qrScanLauncherSku) }
    val qrCodeScannerLot = remember { QRCodeScanner(qrScanLauncherLot) }


    val showErrorLocation = remember { mutableStateOf(false) }// Para validar los campos vacios

    val context = LocalContext.current
    var showError1 by remember { mutableStateOf(false) }
    var showError2 by remember { mutableStateOf(false) }
    var showError3 by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) } // Estado para mostrar el cuadro de diálogo
    var showDialog1 by remember { mutableStateOf(false) }
    var showDialog2 by remember { mutableStateOf(false) }
    var showDialog3 by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") } // Mensaje de error para el cuadro de diálogo
    var errorMessage1 by remember { mutableStateOf("") }
    var errorMessage2 by remember { mutableStateOf("") }
    var errorMessage3 by remember { mutableStateOf("") }
    var showErrorQuantity by remember { mutableStateOf(false) }
    var errorMessageQuantity by remember { mutableStateOf("") }


    LaunchedEffect(qrCodeContentSku.value) {
        sku.value = qrCodeContentSku.value.uppercase()

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

                OutlinedTextFieldsInputsLocation(location,showErrorLocation) // 📌 FUNCION PARA LA UBICACION

        // 📌 CAMPO DE TEXTO PARA EL SKU

        Row(
            modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(modifier = Modifier
                .weight(2f)
                .padding(2.dp)
                .onFocusChanged { focusState ->
                    if (!focusState.isFocused && sku.value.isNotEmpty() && sku.value != "CODIGO NO ENCONTRADO") {
                        findProductDescription(db, sku.value) { descripcion ->
                            productoDescripcion.value = descripcion // 🔥 Actualiza la descripción
                        }
                    }
                },
                singleLine = true,
                label = { Text(text = "Código Producto") },
                value = sku.value,
                onValueChange = { newValue ->
                    sku.value = newValue.uppercase()
                    qrCodeContentSku.value = newValue.uppercase()
                    showError3 = false
                },
                isError = showError3 && (sku.value.isEmpty() || sku.value == "CODIGO NO ENCONTRADO"),

                trailingIcon = {
                    Row {
                        // 📌 Botón para abrir la lista de productos
                        IconButton(
                            onClick = {
                                buscarProductos(db) { lista, mapa ->
                                    productList.value = lista
                                    productMap.value = mapa
                                    showProductDialog.value = true // 🔥 Abre el diálogo de productos
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

        // 📌 FUNCION PARA EL DIALOGO DE PRODUCTOS, DIGASE EL LISTADO DE PRODUCTOS(DESCRIPCIONES)
        ProductSelectionDialog(
            productList = productList, // Lista de descripciones
            productMap = productMap, // Mapa de descripción -> (Código, UM)
            showProductDialog = showProductDialog, // Estado para mostrar el diálogo
            sku = sku, // Estado del SKU
            qrCodeContentSku = qrCodeContentSku, // Estado del código escaneado
            productoDescripcion = productoDescripcion, // Estado de la descripción
            unidadMedida = unidadMedida, // Pasamos un string vacío o alguna variable que contenga la UM
            productDescriptions = productDescriptions // Estado de la lista de descripciones
        )



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
                    .size(222.dp, 70.dp)
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
                isError = showError3 && quantity.isEmpty()
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


        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (location.value.isEmpty())  {
                    errorMessage = "Campos Obligatorios Vacíos"
                    showDialog = true // 🔴 Activa el cuadro de diálogo si hay campos vacíos
                    showErrorLocation.value = true

                }else if(sku.value.isEmpty() || quantity.isEmpty()){
                    errorMessage3 = "Campos Obligatorios Vacíos"
                    showDialog3 = true // 🔴 Activa el cuadro de diálogo si hay campos vacíos
                    showError3 = true

                } else if (location.value == "CODIGO NO ENCONTRADO" || sku.value == "CODIGO NO ENCONTRADO") {  // Si el valor de la UBICACION y el SKU es "CODIGO NO ENCONTRADO" muestra un mensaje.
                    errorMessage1 = "Codigo No Encontrado"
                    showDialog1 = true // 🔴 Activa el cuadro de diálogo si hay campos vacíos
                    showError1 = true

                } else if (lot == "CODIGO NO ENCONTRADO" || dateText.value.isEmpty()) {
                    lot = "N/A"
                    dateText.value = "N/A"

                } else if (productoDescripcion.value == "Producto No Existe") {
                    errorMessage2 = "Producto No Encontrado"
                    showDialog2 = true // 🔴 Activa el cuadro de diálogo si hay campos vacíos
                    showError2 = true

                } else {
                    showErrorLocation.value = false
                    errorMessage = ""
                    showError1 = false
                    errorMessage1 = ""
                    showError2 = false
                    errorMessage2 = ""
                    showError3 = false
                    errorMessage3 = ""

                    saveToFirestore(
                        firestore,
                        location.value,
                        sku.value,
                        productoDescripcion.value,
                        lot,
                        dateText.value,
                        quantity.toDoubleOrNull() ?: 0.0,
                        unidadMedida.value,
                        allData
                    )
                    location.value = ""
                    sku.value = ""
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
                title = { Text("Campos Obligatorios"); Color.Red },
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
                title = { Text("Codigo No Encontrado"); Color.Red },
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

        Spacer(modifier = Modifier.height(8.dp))
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

