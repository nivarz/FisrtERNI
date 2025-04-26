package com.eriknivar.firebasedatabase.view.inventoryentry

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.eriknivar.firebasedatabase.view.storagetype.DataFields
import com.eriknivar.firebasedatabase.viewmodel.UserViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineScope
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.eriknivar.firebasedatabase.view.utility.validarRegistroDuplicado
import kotlinx.coroutines.delay

@Composable
fun OutlinedTextFieldsInputs(
    productoDescripcion: MutableState<String>,
    coroutineScope: CoroutineScope,
    userViewModel: UserViewModel,
    localidad: String,
    onSuccess: () -> Unit
) {
    val sku = remember { mutableStateOf("") }
    val qrCodeContentSku = remember { mutableStateOf("") } //esto es para el scanner de QRCode
    val qrCodeContentLot = remember { mutableStateOf("") } //esto es para el scanner de QRCode
    val unidadMedida =
        remember { mutableStateOf("") } // ✅ Agrega esto en `OutlinedTextFieldsInputs`
    val showProductDialog = remember { mutableStateOf(false) } // 🔥 Para la lista de productos
    val productList = remember { mutableStateOf(emptyList<String>()) }
    val productMap = remember { mutableStateOf(emptyMap<String, Pair<String, String>>()) }

    val lot = remember { mutableStateOf("") }
    val quantity = remember { mutableStateOf("") }
    val showErrorQuantity = remember { mutableStateOf(false) }
    val errorMessageQuantity = remember { mutableStateOf("") }
    var showDialogValueQuantityCero by remember { mutableStateOf(false) }

    val showDialogRegistroDuplicado = remember { mutableStateOf(false) }
    val context = LocalContext.current
    val firestore = Firebase.firestore
    val allData = remember { mutableStateListOf<DataFields>() }
    val dateText = remember { mutableStateOf("") }
    val location = remember { mutableStateOf("") }

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
    val shouldRequestFocusAfterClear = remember { mutableStateOf(false) }
    val focusRequesterSku = remember { FocusRequester() }
    val focusRequesterLot =
        remember { FocusRequester() } // 👈 este sería el que pasas como `nextFocusRequester`
    val focusRequesterFecha = remember { FocusRequester() }
    val focusRequesterCantidad = remember { FocusRequester() }

    val usuario by userViewModel.nombre.observeAsState("")
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // ✅ Estado para asegurarnos que se restaura solo una vez
    val restored = remember { mutableStateOf(false) }

    val showSuccessDialog = remember { mutableStateOf(false) }

    val listState = rememberLazyListState()


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
            try {
                focusRequester.requestFocus()
            } catch (e: Exception) {
                Log.e("FocusError", "Focus no disponible: ${e.message}")
            }
            shouldRequestFocus.value = false
        }
    }

    LaunchedEffect(usuario) {
        if (usuario.isNotEmpty()) {
            fetchDataFromFirestore(
                db = Firebase.firestore,
                allData = allData,
                usuario = usuario,
                listState = listState // 👈 Muy importante
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)// 📌 Ajusta el padding, digase la columna donde estan los campos
    ) {


        // 📌 FUNCION PARA LA UBICACION
        OutlinedTextFieldsInputsLocation(
            location,
            showErrorLocation,
            nextFocusRequester = focusRequesterSku,
            shouldRequestFocusAfterClear = shouldRequestFocusAfterClear

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
            focusRequester = focusRequesterSku,
            nextFocusRequester = focusRequesterLot,
            shouldRequestFocusAfterClear = shouldRequestFocusAfterClear,
            keyboardController = keyboardController

        )

        // 📌 FUNCION PARA EL DIALOGO DE PRODUCTOS, DIGASE EL LISTADO DE PRODUCTOS(DESCRIPCIONES)

        ProductSelectionDialog(
            productList = productList,
            productMap = productMap,
            showProductDialog = showProductDialog,
            sku = sku,
            qrCodeContentSku = qrCodeContentSku,
            productoDescripcion = productoDescripcion,
            unidadMedida = unidadMedida
        )

        // 📌 CAMPO DE TEXTO PARA EL LOTE

        OutlinedTextFieldsInputsLot(
            lot,
            focusRequester = focusRequesterLot,
            nextFocusRequester = focusRequesterFecha,
            keyboardController = keyboardController,
            shouldRequestFocusAfterClear = shouldRequestFocusAfterClear
        )

        // 📌 CAMPO DE TEXTO PARA LA FECHA

        DatePickerTextField(
            dateText,
            focusRequester = focusRequesterFecha,
            nextFocusRequester = focusRequesterCantidad
        )// FUNCION PARA EL CALENDARIO

        // 📌 CAMPO DE TEXTO PARA LA CANTIDAD

        OutlinedTextFieldsInputsQuantity(
            quantity,
            showErrorQuantity,
            errorMessageQuantity,
            lot,
            dateText,
            focusRequester = focusRequesterCantidad,
            keyboardController = LocalSoftwareKeyboardController.current
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
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

                    } else if (productoDescripcion.value == "Producto No Existe" || productoDescripcion.value.isEmpty() || productoDescripcion.value == "Error al obtener datos" || productoDescripcion.value == "Sin descripción") {
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

                        validarRegistroDuplicado(
                            db = firestore,
                            usuario = userViewModel.nombre.value ?: "",
                            ubicacion = location.value,
                            sku = sku.value,
                            lote = lot.value,
                            cantidad = quantity.value.toDoubleOrNull() ?: 0.0,
                            localidad = localidad,
                            onResult = { existeDuplicado ->
                                if (existeDuplicado) {
                                    showDialogRegistroDuplicado.value = true
                                } else {
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
                                        coroutineScope,
                                        localidad = localidad,
                                        userViewModel,
                                        showSuccessDialog,
                                        listState

                                    )

                                    // ✅ Recargar datos y hacer scroll al top
                                    fetchDataFromFirestore(
                                        db = firestore,
                                        allData = allData,
                                        usuario = usuario,
                                        listState = listState
                                    )

                                    sku.value = ""
                                    lot.value = ""
                                    dateText.value = ""
                                    quantity.value = ""
                                    productoDescripcion.value = ""
                                    unidadMedida.value = ""
                                    qrCodeContentSku.value =
                                        "" // 🔥 Esto elimina "Código No Encontrado"
                                    qrCodeContentLot.value =
                                        "" // 🔥 Esto elimina "Código No Encontrado"
                                    userViewModel.limpiarValoresTemporales()

                                    // 👉 Pasar el foco a SKU
                                    try {
                                        focusRequesterSku.requestFocus()
                                    } catch (e: Exception) {
                                        Log.e("FocusError", "Error al pasar foco a SKU: ${e.message}")
                                    }
                                }
                            },
                            onError = {
                                Toast.makeText(
                                    context,
                                    "Error al validar duplicados",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                },

                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF003366), // Azul marino
                    contentColor = Color.White          // Color del texto
                ),

                modifier = Modifier
                    .weight(1f)
                    .height(40.dp),
            )

            {
                Text("Grabar Registro", fontSize = 13.sp)
            }
            // 🔘 Botón Limpiar
            Button(
                onClick = {
                    location.value = ""
                    sku.value = ""
                    lot.value = ""
                    dateText.value = ""
                    quantity.value = ""
                    productoDescripcion.value = ""
                    unidadMedida.value = ""
                    qrCodeContentSku.value = "" // 🔥 Esto elimina "Código No Encontrado"
                    qrCodeContentLot.value = "" // 🔥 Esto elimina "Código No Encontrado"

                    showErrorLocation.value = false
                    showErrorSku.value = false
                    showErrorQuantity.value = false

                    shouldRequestFocusAfterClear.value = true

                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.DarkGray,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp),
            ) {
                Text("Limpiar Campos", fontSize = 13.sp)
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

        if (showDialogRegistroDuplicado.value) {
            AlertDialog(onDismissRequest = { showDialogRegistroDuplicado.value = true },
                title = { Text("Registro Duplicado") },
                text = { Text("Ya existe un registro con los mismos datos. Verifica antes de grabar nuevamente.") },
                confirmButton = {
                    Button(onClick = { showDialogRegistroDuplicado.value = false }) {
                        Text("Aceptar")
                    }
                }
            )
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState, // 👈 asegúrate que esté conectado
        reverseLayout = false // ✅ Esto invierte la lista: el más reciente aparece arriba
    ) {
        items(allData) { item ->
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
                onSuccess = onSuccess,
                listState = listState,
                index = allData.indexOf(item)
            )
        }

    }

    if (showSuccessDialog.value) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog.value = false },
            confirmButton = {},
            title = { Text("✔️ Registro exitoso") },
            text = { Text("El registro se guardó correctamente.") },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        )

        LaunchedEffect(showSuccessDialog.value) {
            delay(2000) // ✅ o el tiempo que prefieras
            showSuccessDialog.value = false
        }
    }

}