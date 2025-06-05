package com.eriknivar.firebasedatabase.view.inventoryentry

import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.eriknivar.firebasedatabase.view.utility.validarRegistroDuplicado
import com.google.firebase.storage.storage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.UUID
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.Icon


@Composable
fun FormEntradaDeInventario(
    location: MutableState<String>,
    sku: MutableState<String>,
    lot: MutableState<String>,
    dateText: MutableState<String>,
    quantity: MutableState<String>,
    productoDescripcion: MutableState<String>,
    unidadMedida: MutableState<String>,
    coroutineScope: CoroutineScope,
    userViewModel: UserViewModel,
    localidad: String,
    allData: SnapshotStateList<DataFields>,
    listState: LazyListState,
    isVisible: Boolean,
    onUserInteraction: () -> Unit

) {

    val qrCodeContentSku = remember { mutableStateOf("") } //esto es para el scanner de QRCode
    val qrCodeContentLot = remember { mutableStateOf("") } //esto es para el scanner de QRCode

    val showErrorQuantity = remember { mutableStateOf(false) }
    val showErrorLocation = remember { mutableStateOf(false) }
    val showErrorSku = remember { mutableStateOf(false) }

    val errorMessageQuantity = remember { mutableStateOf("") }
    var showDialogValueQuantityCero by remember { mutableStateOf(false) }
    val showDialogRegistroDuplicado = remember { mutableStateOf(false) }
    val showConfirmDialog = remember { mutableStateOf(false) }


    val showProductDialog = remember { mutableStateOf(false) } // 🔥 Para la lista de productos
    val productList = remember { mutableStateOf(emptyList<String>()) }
    val productMap = remember { mutableStateOf(emptyMap<String, Pair<String, String>>()) }

    val shouldRequestFocus = remember { mutableStateOf(false) }
    val focusRequesterSku = remember { FocusRequester() }
    val focusRequesterLot = remember { FocusRequester() }
    val focusRequesterFecha = remember { FocusRequester() }
    val focusRequesterCantidad = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequesterLocation = remember { FocusRequester() }
    val openUbicacionInvalidaDialog = remember { mutableStateOf(false) }
    val tempLocationInput = remember { mutableStateOf("") }

    //NO VEO ESTOS ESTADOS EN LA NUEVA MODIFICACION

    val context = LocalContext.current
    val firestore = Firebase.firestore

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

    val focusRequester = remember { FocusRequester() }
    val shouldRequestFocusAfterClear = remember { mutableStateOf(false) }
    val usuario by userViewModel.nombre.observeAsState("")
    val restored = remember { mutableStateOf(false) }
    val showSuccessDialog = remember { mutableStateOf(false) }
    var usuarioDuplicado by remember { mutableStateOf("Desconocido") }

    val imagenBitmap = remember { mutableStateOf<Bitmap?>(null) }
    val tomarFotoLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            imagenBitmap.value = bitmap
        }


    LaunchedEffect(Unit) {
        userViewModel.nombre.observeForever { nuevoNombre ->
            if (nuevoNombre.isEmpty()) {
                if (sku.value.isNotBlank() || lot.value.isNotBlank() || quantity.value.isNotBlank() || location.value.isNotBlank() || dateText.value.isNotBlank()) {
                    userViewModel.guardarValoresTemporalmente(
                        sku.value, lot.value, quantity.value, location.value, dateText.value
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
                listState = listState,
                localidad = localidad
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isVisible) Dp.Unspecified else 0.dp) // 🔥 Oculta visualmente
            .padding(horizontal = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // 📌 FUNCION PARA LA UBICACION
            OutlinedTextFieldsInputsLocation(
                location,
                showErrorLocation,
                focusRequester = focusRequesterLocation, // 🔥 Este es el inicial
                nextFocusRequester = focusRequesterSku,
                shouldRequestFocusAfterClear = shouldRequestFocusAfterClear,
                tempLocationInput = tempLocationInput

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
                unidadMedida = unidadMedida,
                focusRequesterLote = focusRequesterLot
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

            fun continuarGuardadoConFoto(fotoUrl: String?) {
                validarRegistroDuplicado(
                    db = firestore,
                    ubicacion = location.value,
                    sku = sku.value,
                    lote = lot.value,
                    cantidad = quantity.value.toDoubleOrNull() ?: 0.0,
                    localidad = localidad,
                    onResult = { existeDuplicado, usuarioEncontrado ->
                        if (existeDuplicado) {
                            usuarioDuplicado = usuarioEncontrado ?: "Desconocido"
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
                                listState,
                                fotoUrl = fotoUrl
                            )

                            fetchDataFromFirestore(
                                db = firestore,
                                allData = allData,
                                usuario = usuario,
                                listState = listState,
                                localidad = localidad
                            )

                            // limpiar campos
                            sku.value = ""
                            lot.value = ""
                            dateText.value = ""
                            quantity.value = ""
                            productoDescripcion.value = ""
                            unidadMedida.value = ""
                            qrCodeContentSku.value = ""
                            qrCodeContentLot.value = ""
                            imagenBitmap.value = null
                            userViewModel.limpiarValoresTemporales()
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

            fun subirImagenAFirebase(bitmap: Bitmap, onUrlLista: (String) -> Unit) {
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                val data = baos.toByteArray()

                val storageRef =
                    Firebase.storage.reference.child("fotos_registro/${UUID.randomUUID()}.jpg")

                storageRef.putBytes(data)
                    .addOnSuccessListener {
                        storageRef.downloadUrl.addOnSuccessListener { uri ->
                            onUrlLista(uri.toString())
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Error al subir imagen", Toast.LENGTH_SHORT).show()
                        onUrlLista("") // en caso de fallo se puede pasar vacío o null
                    }
            }


            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Button(
                    onClick = { tomarFotoLauncher.launch(null) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                ) {
                    Text("📷 Foto", fontSize = 13.sp, color = Color.White)
                }

                Button(
                    onClick = {
                        onUserInteraction()
                        focusManager.clearFocus()
                        keyboardController?.hide()

                        coroutineScope.launch {
                            delay(300)
                            try {
                                focusRequesterSku.requestFocus()
                            } catch (e: Exception) {
                                Log.e("FocusError", "Error al solicitar foco en SKU: ${e.message}")
                            }

                            // 🟥 1. Validación: ubicación no existe
                            if (location.value.isEmpty() && showErrorLocation.value) {
                                showErrorLocation.value = true
                                delay(150)
                                openUbicacionInvalidaDialog.value = true
                                return@launch
                            }

                            // 🟥 2. Validación general de campos vacíos
                            if (location.value.isEmpty() || sku.value.isEmpty() || quantity.value.isEmpty()) {
                                showErrorLocation.value = location.value.isEmpty()
                                showErrorSku.value = sku.value.isEmpty()
                                showErrorQuantity.value = quantity.value.isEmpty()
                                delay(150)
                                showDialog = true
                                return@launch
                            }

                            // 🟥 3. Validación: "CÓDIGO NO ENCONTRADO"
                            if (location.value == "CÓDIGO NO ENCONTRADO" || sku.value == "CÓDIGO NO ENCONTRADO") {
                                showErrorLocation.value = true
                                showErrorSku.value = true
                                delay(150)
                                showDialog1 = true
                                return@launch
                            }

                            // 🟡 4. Lote vacío o no encontrado → colocar -
                            if (lot.value == "CÓDIGO NO ENCONTRADO" || lot.value.isEmpty()) {
                                lot.value = "-"
                            }

                            // 🟡 5. Fecha vacía → colocar -
                            if (dateText.value.isEmpty()) {
                                dateText.value = "-"
                            }

                            // 🟥 6. Validación: producto no existe o sin descripción válida
                            if (productoDescripcion.value == "Sin descripción" ||
                                productoDescripcion.value.isEmpty() ||
                                productoDescripcion.value == "Error al obtener datos"
                            ) {
                                errorMessage2 = "Código No Existe"
                                delay(150)
                                showDialog2 = true
                                return@launch
                            }

                            // 🟥 7. Validación: cantidad igual a 0
                            if (quantity.value == "0" || quantity.value.isEmpty()) {
                                errorMessage = "No Admite cantidades 0"
                                showDialogValueQuantityCero = true
                                showErrorQuantity.value = true
                                return@launch
                            }

                            // ✅ 8. Si todas las validaciones pasaron, mostrar AlertDialog de confirmación
                            delay(150)
                            showConfirmDialog.value = true

                            showErrorLocation.value = false
                            showErrorSku.value = false
                            errorMessage = ""
                            showError1 = false
                            errorMessage1 = ""
                            showError2 = false
                            errorMessage2 = ""
                            showError3 = false
                            errorMessage3 = ""

                        }
                    },

                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF003366), contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Grabar", fontSize = 13.sp, color = Color.White)
                }

                // 🔘 Botón Limpiar
                Button(
                    onClick = {
                        onUserInteraction()
                        focusManager.clearFocus()

                        location.value = ""
                        tempLocationInput.value = ""
                        sku.value = ""
                        lot.value = ""
                        dateText.value = ""
                        quantity.value = ""
                        productoDescripcion.value = ""
                        unidadMedida.value = ""
                        qrCodeContentSku.value = ""
                        qrCodeContentLot.value = ""

                        showErrorLocation.value = false
                        showErrorSku.value = false
                        showErrorQuantity.value = false

                        coroutineScope.launch {
                            delay(200)
                            try {
                                focusRequesterLocation.requestFocus()
                                keyboardController?.show()
                            } catch (e: Exception) {
                                Log.e("FocusError", "Error al solicitar foco: ${e.message}")
                            }
                        }


                    }, colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50), contentColor = Color.White
                    ), modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Limpiar", fontSize = 13.sp, color = Color.White)
                }
            }

            HorizontalDivider(
                thickness = 2.dp,
                color = Color.Gray,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )

            if (showConfirmDialog.value) {
                AlertDialog(
                    onDismissRequest = { showConfirmDialog.value = false },
                    title = { Text("Confirmar Registro") },
                    text = { Text("¿Estás seguro de que deseas grabar este registro?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showConfirmDialog.value = false

                                if (imagenBitmap.value != null) {
                                    subirImagenAFirebase(imagenBitmap.value!!) { urlFoto ->
                                        continuarGuardadoConFoto(urlFoto)
                                    }
                                } else {
                                    continuarGuardadoConFoto(null)
                                }

                            }
                        ) {
                            Text("Sí, grabar", color = Color(0xFF003366))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showConfirmDialog.value = false }) {
                            Text("Cancelar", color = Color(0xFF003366))
                        }
                    }
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
                AlertDialog(
                    onDismissRequest = {
                        showDialog2 = true
                    },
                    title = { Text("Codigo No Existe") },
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
                AlertDialog(
                    onDismissRequest = { showDialogRegistroDuplicado.value = false },
                    title = { Text("Registro Duplicado") },
                    text = {
                        Text(
                            buildAnnotatedString {
                                append("Ya existe un registro con los mismos datos realizado por: ")
                                withStyle(
                                    style = SpanStyle(
                                        fontWeight = FontWeight.Bold,
                                        fontStyle = FontStyle.Italic,
                                        color = Color.DarkGray,
                                        fontFamily = FontFamily.SansSerif
                                    )
                                ) {
                                    append("\"$usuarioDuplicado\"")
                                }
                                append(". Verifica antes de grabar nuevamente.")
                            },
                            fontSize = 14.sp // opcional, ajusta tamaño a gusto
                        )
                    },
                    confirmButton = {
                        Button(onClick = { showDialogRegistroDuplicado.value = false }) {
                            Text("Aceptar")
                        }
                    }
                )
            }

            if (showSuccessDialog.value) {
                AlertDialog(
                    onDismissRequest = { showSuccessDialog.value = false },
                    confirmButton = {},
                    title = { Text("✔️ Registro exitoso") },
                    text = { Text("El registro se guardó correctamente.") },
                    properties = DialogProperties(
                        dismissOnBackPress = false, dismissOnClickOutside = false
                    )
                )

                LaunchedEffect(showSuccessDialog.value) {
                    delay(1000) // ✅ o el tiempo que prefieras
                    showSuccessDialog.value = false
                }
            }

            if (openUbicacionInvalidaDialog.value) {
                AlertDialog(onDismissRequest = { openUbicacionInvalidaDialog.value = false },
                    title = { Text("Ubicación no válida") },
                    text = { Text("La ubicación ingresada no existe en el maestro. Verifícala antes de continuar.") },
                    confirmButton = {
                        TextButton(onClick = { openUbicacionInvalidaDialog.value = false }) {
                            Text("Aceptar")
                        }
                    })
            }
        }
    }


}
