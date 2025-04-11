package com.eriknivar.firebasedatabase.view.inventoryentry

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.eriknivar.firebasedatabase.view.storagetype.DataFields
import com.eriknivar.firebasedatabase.viewmodel.UserViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineScope
import androidx.compose.ui.platform.LocalSoftwareKeyboardController


@Composable
fun OutlinedTextFieldsInputs(
    productoDescripcion: MutableState<String>,
    snackbarHostState: SnackbarHostState,
    coroutineScope: CoroutineScope,
    userViewModel: UserViewModel,
    localidad: String
) {

    val sku = remember { mutableStateOf("") }
    val qrCodeContentSku = remember { mutableStateOf("") } //esto es para el scanner de QRCode
    val unidadMedida = remember { mutableStateOf("") } // ✅ Agrega esto en `OutlinedTextFieldsInputs`
    val showProductDialog = remember { mutableStateOf(false) } // 🔥 Para la lista de productos
    val productList = remember { mutableStateOf(emptyList<String>()) }
    val productMap = remember { mutableStateOf(emptyMap<String, Pair<String, String>>()) }

    val lot = remember { mutableStateOf("") }
    val quantity = remember { mutableStateOf("") }
    val showErrorQuantity = remember { mutableStateOf(false) }
    val errorMessageQuantity = remember { mutableStateOf("") }
    var showDialogValueQuantityCero by remember { mutableStateOf(false) }


    val firestore = Firebase.firestore
    val allData = remember { mutableStateListOf<DataFields>() }
    val dateText = remember { mutableStateOf("") }
    val location = remember { mutableStateOf("") } // 🔥 Debe ser MutableState<String>

    // Para ocultar el teclado val focusManager = LocalFocusManager.current

    val showErrorLocation = remember { mutableStateOf(false) }// Para validar los campos vacios
    val showErrorSku = remember { mutableStateOf(false) }

    var showError1 by remember { mutableStateOf(false) }
    var showError2 by remember { mutableStateOf(false) }
    var showError3 by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) } // Estado para mostrar el cuadro de diálogo
    var showDialog1 by remember { mutableStateOf(false) }
    var showDialog2 by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") } // Mensaje de error para el cuadro de diálogo
    var errorMessage1 by remember { mutableStateOf("") }
    var errorMessage2 by remember { mutableStateOf("") }
    var errorMessage3 by remember { mutableStateOf("") }

    val shouldRequestFocus = remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusRequesterSku = remember { FocusRequester() }

    val usuario by userViewModel.nombre.observeAsState("")
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // ✅ Estado para asegurarnos que se restaura solo una vez
    val restored = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        userViewModel.nombre.observeForever { nuevoNombre ->
            if (nuevoNombre.isEmpty()) {
                if (
                    sku.value.isNotBlank() ||
                    lot.value.isNotBlank() ||
                    quantity.value.isNotBlank() ||
                    location.value.isNotBlank() ||
                    dateText.value.isNotBlank()
                ) {
                    userViewModel.guardarValoresTemporalmente(
                        sku.value,
                        lot.value,
                        quantity.value,
                        location.value,
                        dateText.value
                    )

                    Log.d("TEMPORAL", "✅ Guardado CORRECTO antes de logout")
                } else {
                    Log.d("TEMPORAL", "⚠️ Evitado guardado de campos vacíos")
                }
            }
        }
    }


    LaunchedEffect(Unit) {
        if (!restored.value) {
            sku.value = userViewModel.tempSku
            lot.value = userViewModel.tempLote
            quantity.value = userViewModel.tempCantidad
            location.value = userViewModel.tempUbicacion
            dateText.value = userViewModel.tempFecha
            restored.value = true

            Log.d("TEMPORAL", "✅ Restauración visual aplicada")
        }
    }


    LaunchedEffect(shouldRequestFocus.value) {
        if (shouldRequestFocus.value) {
            focusRequester.requestFocus()
            shouldRequestFocus.value = false
        }
    }


    LaunchedEffect(usuario) {
        if (usuario.isNotEmpty()) {
            fetchDataFromFirestore(firestore, allData, usuario)
        }
    }


    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)// 📌 Ajusta el padding, digase la columna donde estan los campos
    ) {


        // 📌 FUNCION PARA LA UBICACION
        OutlinedTextFieldsInputsLocation(
            location, showErrorLocation, nextFocusRequester = focusRequesterSku

        )

        // 📌 CAMPO DE TEXTO PARA EL SKU

        OutlinedTextFieldsInputsSku(
            sku,
            showErrorSku,
            productoDescripcion,
            productList,
            productMap,
            showProductDialog,
            unidadMedida,
            focusRequester = focusRequester,
            keyboardController = keyboardController

        )

        // 📌 FUNCION PARA EL DIALOGO DE PRODUCTOS, DIGASE EL LISTADO DE PRODUCTOS(DESCRIPCIONES)

        ProductSelectionDialog(
            productList = productList, // Lista de descripciones
            productMap = productMap, // Mapa de descripción -> (Código, UM)
            showProductDialog = showProductDialog, // Estado para mostrar el diálogo
            sku = sku, // Estado del SKU
            qrCodeContentSku = qrCodeContentSku, // Estado del código escaneado
            productoDescripcion = productoDescripcion, // Estado de la descripción
            unidadMedida = unidadMedida // Pasamos un string vacío o alguna variable que contenga la UM
        )

        // 📌 CAMPO DE TEXTO PARA EL LOTE

        OutlinedTextFieldsInputsLot(lot)

        // 📌 CAMPO DE TEXTO PARA LA FECHA

        DatePickerTextField(dateText)// FUNCION PARA EL CALENDARIO

        // 📌 CAMPO DE TEXTO PARA LA CANTIDAD

        OutlinedTextFieldsInputsQuantity(
            quantity, showErrorQuantity, errorMessageQuantity, lot, dateText
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {

        Button(
            onClick = {

                focusManager.clearFocus()
                keyboardController?.hide()

                if (location.value.isEmpty() || sku.value.isEmpty() || quantity.value.isEmpty()) {
                    showDialog = true // 🔴 Activa el cuadro de diálogo si hay campos vacíos
                    showErrorLocation.value = true
                    showErrorSku.value = true
                    showErrorQuantity.value = true

                } else if (location.value == "CÓDIGO NO ENCONTRADO" || sku.value == "CÓDIGO NO ENCONTRADO") {  // Si el valor de la UBICACION y el SKU es "CODIGO NO ENCONTRADO" muestra un mensaje.
                    showDialog1 = true // 🔴 Activa el cuadro de diálogo si hay campos vacíos
                    showErrorLocation.value = true
                    showErrorSku.value = true

                } else if (lot.value == "CÓDIGO NO ENCONTRADO" || lot.value.isEmpty()) {
                    lot.value = "N/A"

                } else if (dateText.value.isEmpty()) {
                    dateText.value = "N/A"

                } else if (productoDescripcion.value == "Producto No Existe" || productoDescripcion.value.isEmpty() || productoDescripcion.value == "Error al obtener datos") {
                    errorMessage2 = "Producto No Encontrado"
                    showDialog2 = true // 🔴 Activa el cuadro de diálogo si hay campos vacíos
                    showErrorSku.value = true

                } else if (quantity.value == "0") {
                    errorMessage = "No Admite cantidades 0"
                    showDialogValueQuantityCero = true

                } else {
                    showErrorLocation.value = false
                    showErrorSku.value = false
                    errorMessage = ""
                    showError1 = false
                    errorMessage1 = ""
                    showError2 = false
                    errorMessage2 = ""
                    showError3 = false
                    errorMessage3 = ""
                    shouldRequestFocus.value = true


                    saveToFirestore(
                        firestore,
                        location.value,
                        sku.value,
                        productoDescripcion.value,
                        lot.value,
                        dateText.value,
                        quantity.value.toDoubleOrNull() ?: 0.0,
                        unidadMedida.value,
                        allData,
                        usuario = userViewModel.nombre.value ?: "",
                        snackbarHostState,
                        coroutineScope,
                        localidad = localidad

                    )

                    sku.value = ""
                    lot.value = ""
                    dateText.value = ""
                    quantity.value = ""
                    productoDescripcion.value = ""
                    unidadMedida.value = ""

                    // ✅ Solo si se grabó exitosamente
                    userViewModel.limpiarValoresTemporales()
                }


            },


            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF003366), // Azul marino
                contentColor = Color.White          // Color del texto
            ),

            modifier = Modifier.fillMaxHeight(0.16f)
        )

        {
            Text("Grabar Registro")
        }
// 🔘 Botón Limpiar
            Button(
                onClick = {
                    sku.value = ""
                    lot.value = ""
                    dateText.value = ""
                    quantity.value = ""
                    productoDescripcion.value = ""
                    unidadMedida.value = ""

                    showErrorLocation.value = false
                    showErrorSku.value = false
                    showErrorQuantity.value = false

                    focusRequester.requestFocus()           // ✅ Solicita el foco al campo SKU

                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.DarkGray,
                    contentColor = Color.White
                ),
                modifier = Modifier.fillMaxHeight(0.16f)
            ) {
                Text("Limpiar Campos")
            }
        }

        Box(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(
                thickness = 2.dp,
                color = Color.Gray,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )
        }


        if (showDialog) {
            AlertDialog(onDismissRequest = {
                showDialog = true
            }, // No se cierra al tocar fuera del cuadro
                title = { Text("Campos Obligatorios Vacios") },
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
                title = { Text("Producto No Encontrado") },
                text = { Text("Por favor, completa todos los campos requeridos antes de continuar.") },
                confirmButton = {
                    Button(onClick = { showDialog2 = false }) {
                        Text("Aceptar")
                    }
                })
        }
        if (showDialogValueQuantityCero) {
            AlertDialog(onDismissRequest = {
                showDialogValueQuantityCero = true
            }, // No se cierra al tocar fuera del cuadro
                title = { Text("No Admite cantidades 0") },
                text = { Text("Por favor, completa todos los campos requeridos antes de continuar.") },
                confirmButton = {
                    Button(onClick = { showDialogValueQuantityCero = false }) {
                        Text("Aceptar")
                    }
                })
        }

    }

    val listState = rememberLazyListState()

    LaunchedEffect(allData.size) {
        if (allData.isNotEmpty()) {
            listState.animateScrollToItem(allData.size - 1)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState
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

