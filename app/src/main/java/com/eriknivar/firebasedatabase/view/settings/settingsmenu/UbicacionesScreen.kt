package com.eriknivar.firebasedatabase.view.settings.settingsmenu

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.eriknivar.firebasedatabase.view.NavigationDrawer
import com.eriknivar.firebasedatabase.view.inventoryentry.QRCodeScanner
import com.eriknivar.firebasedatabase.viewmodel.UserViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun UbicacionesScreen(navController: NavHostController, userViewModel: UserViewModel) {

    val tipo = userViewModel.tipo.value ?: ""

    if (tipo.lowercase() != "admin" && tipo.lowercase() != "superuser") {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.TopCenter
        ) {
            Text(
                text = "⛔ Acceso restringido",
                color = Color.Red,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(32.dp)
            )
        }
        return
    }

    val firestore = Firebase.firestore
    val ubicaciones = remember { mutableStateListOf<Pair<String, String?>>() } // código y zona

    val showDialog = remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var docIdToEdit by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    var codigoInput by remember { mutableStateOf("") }
    var zonaInput by remember { mutableStateOf("") }

    val navyBlue = Color(0xFF001F5B)

    val showSuccessDialog = remember { mutableStateOf(false) }
    val showSuccessDeleteDialog = remember { mutableStateOf(false) }
    val successMessage = remember { mutableStateOf("") }
    var ubicacionAEliminar by remember { mutableStateOf<Pair<String, String?>?>(null) }

    // Estados para el Dropdown de Localidades
    val localidadesList = remember { mutableStateListOf<String>() }
    val selectedLocalidad = remember { mutableStateOf("") }
    val expandedLocalidad = remember { mutableStateOf(false) }


    // Cargar localidades desde Firebase (una sola vez)
    LaunchedEffect(Unit) {
        FirebaseFirestore.getInstance().collection("localidades")
            .orderBy("nombre")
            .get()
            .addOnSuccessListener { result ->
                localidadesList.clear()
                for (document in result) {
                    document.getString("nombre")?.let {
                        localidadesList.add(it)
                    }
                }
            }
    }

    // 🔄 Cargar ubicaciones ordenadas

    fun cargarUbicaciones() {
        firestore.collection("ubicaciones")
            .orderBy("codigo_ubi")
            .get()
            .addOnSuccessListener { result ->
                ubicaciones.clear()
                result.forEach { doc ->
                    val codigo = doc.getString("codigo_ubi") ?: ""
                    val zona = doc.getString("zona")
                    val localidad = doc.getString("localidad").orEmpty()
                    ubicaciones.add(doc.id to "$codigo|${zona.orEmpty()}")
                }
            }
    }

    LaunchedEffect(Unit) {
        cargarUbicaciones()
    }

    val dummyLocation = remember { mutableStateOf("") }
    val dummySku = remember { mutableStateOf("") }
    val dummyQuantity = remember { mutableStateOf("") }
    val dummyLot = remember { mutableStateOf("") }
    val dummyDateText = remember { mutableStateOf("") }

    val qrCodeContent = remember { mutableStateOf("") }
    val wasScanned = remember { mutableStateOf(false) }

    val qrScanLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            val intentResult = IntentIntegrator.parseActivityResult(result.resultCode, data)
            if (intentResult != null) {
                qrCodeContent.value = intentResult.contents ?: ""
                wasScanned.value = true
            }
        }

    val qrCodeScanner = remember { QRCodeScanner(qrScanLauncher) }
    val context = LocalContext.current

// Esto dentro de LaunchedEffect
    LaunchedEffect(wasScanned.value) {
        if (wasScanned.value && qrCodeContent.value.isNotBlank()) {
            codigoInput = qrCodeContent.value.trim().uppercase()
            wasScanned.value = false
        }
    }

    NavigationDrawer(
        navController = navController,
        storageType = "Ubicaciones",
        userViewModel = userViewModel,
        location = dummyLocation,
        sku = dummySku,
        quantity = dummyQuantity,
        lot = dummyLot,
        expirationDate = dummyDateText
    ) {

        Column(modifier = Modifier.fillMaxSize()) {
            ElevatedButton(
                colors = ButtonDefaults.buttonColors(
                    containerColor = navyBlue, contentColor = Color.White
                ),
                onClick = {
                    codigoInput = ""
                    zonaInput = ""
                    docIdToEdit = ""
                    isEditing = false
                    showDialog.value = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("+ ", fontWeight = FontWeight.Bold, fontSize = 30.sp, color = Color.Green)

                Text("Crear Ubicación")
            }

            LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
                items(ubicaciones) { (docId, datos) ->
                    val partes = datos?.split("|") ?: listOf()
                    val codigo = partes.getOrNull(0)
                    val zona = partes.getOrNull(1).orEmpty()
                    val localidad = partes.getOrNull(2).orEmpty()


                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Ubicacion: $codigo", fontWeight = FontWeight.Bold)
                                    if (zona.isNotBlank()) {
                                        Text("Zona: $zona")
                                    }
                                    if (localidad.isNotBlank()) {
                                        Text("Localidad: $localidad")
                                    }
                                }

                                Row {
                                    IconButton(onClick = {
                                        // Editar
                                        isEditing = true
                                        docIdToEdit = docId
                                        if (codigo != null) {
                                            codigoInput = codigo
                                        }
                                        zonaInput = zona
                                        showDialog.value = true
                                    }) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Editar",
                                            tint = Color.Blue
                                        )
                                    }
                                    IconButton(onClick = {
                                        ubicacionAEliminar = docId to (codigo ?: "")
                                        showDeleteDialog = true
                                    }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Eliminar",
                                            tint = Color.Red
                                        )
                                    }


                                    if (showDeleteDialog) {
                                        ubicacionAEliminar?.let { (id, codigo) ->
                                            AlertDialog(
                                                onDismissRequest = {
                                                    showDeleteDialog = false
                                                    ubicacionAEliminar = null
                                                },
                                                title = { Text("¿Eliminar ubicación?") },
                                                text = {
                                                    Text(buildAnnotatedString {
                                                        append("¿Estás seguro de que deseas eliminar la ubicación ")
                                                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                                            append("\"$codigo\"")
                                                        }
                                                        append("? Esta acción no se puede deshacer.")
                                                    })
                                                },
                                                confirmButton = {
                                                    TextButton(onClick = {
                                                        firestore.collection("ubicaciones")
                                                            .document(id).delete()
                                                            .addOnSuccessListener {
                                                                showDeleteDialog = false
                                                                ubicacionAEliminar = null
                                                                showSuccessDeleteDialog.value = true

                                                                CoroutineScope(Dispatchers.Main).launch {
                                                                    delay(2000)
                                                                    showSuccessDeleteDialog.value =
                                                                        false
                                                                }

                                                                cargarUbicaciones()
                                                            }
                                                    }) {
                                                        Text("Sí")
                                                    }
                                                },
                                                dismissButton = {
                                                    TextButton(onClick = {
                                                        showDeleteDialog = false
                                                        ubicacionAEliminar = null
                                                    }) {
                                                        Text("Cancelar")
                                                    }
                                                }
                                            )
                                        }
                                    }


                                }
                            }
                        }
                    }
                }
            }
        }

        var showUbicacionExistenteDialog by remember { mutableStateOf(false) }


        if (showDialog.value) {
            AlertDialog(
                onDismissRequest = { showDialog.value = false }, // ✅
                title = { Text(if (isEditing) "Editar Ubicación" else "Nueva Ubicación") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = codigoInput,
                            onValueChange = { codigoInput = it.uppercase().trim() },
                            label = { Text("Código de Ubicación*") },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(
                                    onClick = { qrCodeScanner.startQRCodeScanner(context as Activity) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.QrCodeScanner,
                                        contentDescription = "Escanear Código"
                                    )
                                }
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = zonaInput,
                            singleLine = true,
                            onValueChange = { zonaInput = it.uppercase() },
                            label = { Text("Zona (opcional)") }
                        )

                        Spacer(Modifier.height(8.dp))
                        Text("Localidad*", fontWeight = FontWeight.Bold)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color.Gray, shape = RoundedCornerShape(4.dp))
                                .clickable(onClick = { expandedLocalidad.value = true })
                                .padding(12.dp)
                        )
                        {
                            Text(
                                text = if (selectedLocalidad.value.isNotEmpty()) selectedLocalidad.value else "Seleccionar una localidad",
                                color = if (selectedLocalidad.value.isNotEmpty()) Color.Black else Color.Gray
                            )

                            DropdownMenu(
                                expanded = expandedLocalidad.value,
                                onDismissRequest = { expandedLocalidad.value = false }
                            ) {
                                localidadesList.forEach { localidad ->

                                    DropdownMenuItem(
                                        text = { Text(localidad) },
                                        onClick = {
                                            selectedLocalidad.value = localidad
                                            expandedLocalidad.value = false
                                        }
                                    )
                                }
                            }
                        }


                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (codigoInput.isNotEmpty()) {
                            firestore.collection("ubicaciones")
                                .whereEqualTo("codigo_ubi", codigoInput)
                                .whereEqualTo("zona", zonaInput)
                                .get()
                                .addOnSuccessListener { documents ->
                                    val yaExiste = documents.any { doc ->
                                        !isEditing || doc.id != docIdToEdit
                                    }

                                    if (yaExiste) {
                                        showUbicacionExistenteDialog = true
                                    } else {
                                        val data = mapOf(
                                            "codigo_ubi" to codigoInput,
                                            "zona" to zonaInput.trim(),
                                            "localidad" to selectedLocalidad.value

                                        )

                                        val operacion = if (isEditing) {
                                            firestore.collection("ubicaciones")
                                                .document(docIdToEdit).update(data)
                                        } else {
                                            firestore.collection("ubicaciones").add(data)
                                        }

                                        operacion.addOnSuccessListener {
                                            operacion.addOnSuccessListener {
                                                successMessage.value =
                                                    if (isEditing) "Ubicación actualizada exitosamente" else "Ubicación agregada exitosamente"
                                                showSuccessDialog.value = true
                                                showDialog.value = false
                                                cargarUbicaciones()
                                            }

                                            showDialog.value = false
                                            cargarUbicaciones()
                                        }
                                    }
                                }
                        }
                    }) {
                        Text(if (isEditing) "Actualizar" else "Guardar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog.value = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        if (showUbicacionExistenteDialog) {
            AlertDialog(
                onDismissRequest = { showUbicacionExistenteDialog = false },
                title = { Text("Ubicación existente") },
                text = { Text("Ya existe una ubicación con ese mismo código y zona.") },
                confirmButton = {
                    TextButton(onClick = { showUbicacionExistenteDialog = false }) {
                        Text("Aceptar")
                    }
                }
            )
        }
    }

    if (showSuccessDialog.value) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog.value = false },
            title = { Text("Creada/Actualizada") },
            text = { Text(successMessage.value) },
            confirmButton = {
                TextButton(onClick = { showSuccessDialog.value = false }) {
                    Text("Aceptar")
                }
            }
        )
    }
}


