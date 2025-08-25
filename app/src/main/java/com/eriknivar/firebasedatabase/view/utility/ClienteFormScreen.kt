package com.eriknivar.firebasedatabase.view.utility

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.eriknivar.firebasedatabase.viewmodel.ClienteFormMode
import com.eriknivar.firebasedatabase.viewmodel.ClienteFormInput
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eriknivar.firebasedatabase.viewmodel.ClienteFormViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun ClienteFormScreen(
    mode: com.eriknivar.firebasedatabase.viewmodel.ClienteFormMode,
    initial: ClienteFormInput = ClienteFormInput(),
    loading: Boolean = false,
    errorGlobal: String? = null,
    onBack: () -> Unit,
    onSubmit: (ClienteFormInput) -> Unit,
    // 👇 inyecta el VM para observar el estado del RNC (si ya lo recibes en Route, pásalo aquí)
    vm: ClienteFormViewModel = viewModel()
) {
    // --- Configuración de límites ---
    val maxNombre = 60
    val maxRnc = 13  // admite cédula/RNC con guiones si algún día los permites
    val maxTel = 10
    val maxMail = 64
    val maxDir = 120
    val maxNotas = 200

    var nombre by remember(initial) { mutableStateOf(initial.nombreComercial) }
    var rnc by remember(initial) { mutableStateOf(initial.rncOCedula) }
    var tel by remember(initial) { mutableStateOf(initial.telefono) }
    var mail by remember(initial) { mutableStateOf(initial.email) }
    var dir by remember(initial) { mutableStateOf(initial.direccion) }
    var notas by remember(initial) { mutableStateOf(initial.notas) }

    // Errores por campo
    var eNombre by remember { mutableStateOf<String?>(null) }
    var eRnc by remember { mutableStateOf<String?>(null) }
    var eMail by remember { mutableStateOf<String?>(null) }
    var eTel by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()

    // Focus para hacer scroll al primer error
    val frNombre = remember { FocusRequester() }
    val frRnc = remember { FocusRequester() }
    val frTel = remember { FocusRequester() }
    val frMail = remember { FocusRequester() }

    // índice base (si en EDIT mostramos el chip de ID, los campos inician en 1)
    val baseIndex = remember(mode, initial.clienteId) {
        if (mode == ClienteFormMode.EDIT && !initial.clienteId.isNullOrBlank()) 1 else 0
    }

    // “intenciones” de scroll/focus (se setean en validarCampos y se consumen en un effect)
    var pendingScrollTo by remember { mutableStateOf<Int?>(null) }
    var pendingFocus by remember { mutableStateOf<FocusRequester?>(null) }

    // Estado de verificación asíncrona de RNC (duplicado)
    val rncStatus by vm.rncStatus.collectAsState()

    // Debounce de 500ms para chequear RNC cada vez que cambia
    LaunchedEffect(rnc, mode) {
        snapshotFlow { rnc }
            .debounce(500)
            .collect {
                vm.checkRncDisponible(
                    it,
                    if (mode == ClienteFormMode.EDIT) initial.rncOCedula else null
                )
            }
    }

    LaunchedEffect(pendingScrollTo, pendingFocus) {
        val i = pendingScrollTo
        val f = pendingFocus
        if (i != null) listState.animateScrollToItem(i)
        f?.requestFocus()
        pendingScrollTo = null
        pendingFocus = null
    }


    fun validarCampos(): Boolean {
        eNombre = ClienteUtils.validarNombre(nombre)
        eRnc    = ClienteUtils.validarRnc(rnc)
        eMail   = ClienteUtils.validarEmailBasico(mail)
        eTel    = ClienteUtils.validarTelefonoRD(tel)

        if (rncStatus is ClienteFormViewModel.RncStatus.EnUso) {
            eRnc = "Ya existe un cliente con este RNC/Cédula."
        }

        when {
            eNombre != null -> { pendingScrollTo = baseIndex + 0; pendingFocus = frNombre }
            eRnc    != null -> { pendingScrollTo = baseIndex + 1; pendingFocus = frRnc }
            eTel    != null -> { pendingScrollTo = baseIndex + 2; pendingFocus = frTel }
            eMail   != null -> { pendingScrollTo = baseIndex + 3; pendingFocus = frMail }
        }
        return listOf(eNombre, eRnc, eMail, eTel).all { it == null }
    }


    fun intentarGuardar() {
        if (validarCampos() && !loading) {
            onSubmit(
                ClienteFormInput(
                    clienteId = initial.clienteId,
                    nombreComercial = nombre.trim(),
                    rncOCedula = rnc.trim(),
                    telefono = tel.trim(),
                    email = mail.trim(),
                    direccion = dir.trim(),
                    notas = notas.trim()
                )
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (mode == ClienteFormMode.CREATE) "Nuevo cliente" else "Editar cliente") },
                navigationIcon = {
                    IconButton(enabled = !loading, onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
                    .navigationBarsPadding()
                    .imePadding(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onBack,
                    enabled = !loading,
                    modifier = Modifier.weight(1f)
                ) { Text("Cancelar") }

                Button(
                    onClick = { intentarGuardar() },
                    enabled = !loading &&
                            !(rncStatus is ClienteFormViewModel.RncStatus.Checking), // evita guardar mientras chequea
                    modifier = Modifier.weight(1f)
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier
                                .size(18.dp)
                                .padding(end = 8.dp)
                        )
                    }
                    Text(if (mode == ClienteFormMode.CREATE) "Guardar" else "Guardar cambios")
                }
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
        )
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = innerPadding.calculateTopPadding() + 12.dp,
                bottom = innerPadding.calculateBottomPadding() + 12.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ID en EDIT
            if (mode == ClienteFormMode.EDIT && !initial.clienteId.isNullOrBlank()) {
                item {
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text("ID: ${initial.clienteId}") })
                }
            }

            // Nombre
            item {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = {
                        nombre = it.take(maxNombre)
                        if (eNombre != null) eNombre = null
                    },
                    label = { Text("Nombre comercial *") },
                    singleLine = true,
                    isError = eNombre != null,
                    supportingText = {
                        val cnt = "${nombre.length}/$maxNombre"
                        Text((eNombre ?: cnt))
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(frNombre)
                )
            }

            // RNC / Cédula
            item {
                OutlinedTextField(
                    value = rnc,
                    onValueChange = {
                        rnc = it.take(maxRnc)
                        eRnc = null // limpia error manual si escribe
                    },
                    label = { Text("RNC / Cédula *") },
                    singleLine = true,
                    isError = eRnc != null || rncStatus is ClienteFormViewModel.RncStatus.EnUso,
                    supportingText = {
                        when (rncStatus) {
                            is ClienteFormViewModel.RncStatus.Checking ->
                                Text("Verificando disponibilidad…")

                            is ClienteFormViewModel.RncStatus.EnUso ->
                                Text(
                                    "Ya existe un cliente con este RNC/Cédula.",
                                    color = MaterialTheme.colorScheme.error
                                )

                            is ClienteFormViewModel.RncStatus.Error ->
                                Text(
                                    "No se pudo verificar el RNC.",
                                    color = MaterialTheme.colorScheme.error
                                )

                            else -> {
                                val cnt = "${rnc.length}/$maxRnc"
                                Text(eRnc ?: cnt)
                            }
                        }
                    },
                    trailingIcon = {
                        when (rncStatus) {
                            is ClienteFormViewModel.RncStatus.Checking ->
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )

                            is ClienteFormViewModel.RncStatus.Libre ->
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF137333)
                                )

                            is ClienteFormViewModel.RncStatus.EnUso ->
                                Icon(
                                    Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )

                            else -> {}
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(frRnc)
                )
            }

            // Teléfono
            item {
                OutlinedTextField(
                    value = tel,
                    onValueChange = {
                        // solo dígitos y límite de 10
                        tel = it.filter(Char::isDigit).take(maxTel)
                        if (eTel != null) eTel = null
                    },
                    label = { Text("Teléfono (10 dígitos)") },
                    singleLine = true,
                    isError = eTel != null,
                    supportingText = {
                        val cnt = "${tel.length}/$maxTel"
                        Text(eTel ?: cnt)
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(frTel)
                )
            }

            // Email
            item {
                OutlinedTextField(
                    value = mail,
                    onValueChange = {
                        mail = it.take(maxMail)
                        if (eMail != null) eMail = null
                    },
                    label = { Text("Email") },
                    singleLine = true,
                    isError = eMail != null,
                    supportingText = {
                        val cnt = "${mail.length}/$maxMail"
                        Text(eMail ?: cnt)
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(frMail)
                )
            }

            // Dirección
            item {
                OutlinedTextField(
                    value = dir,
                    onValueChange = { dir = it.take(maxDir) },
                    label = { Text("Dirección") },
                    singleLine = false,
                    minLines = 2,
                    supportingText = { Text("${dir.length}/$maxDir") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Notas
            item {
                OutlinedTextField(
                    value = notas,
                    onValueChange = { notas = it.take(maxNotas) },
                    label = { Text("Notas") },
                    singleLine = false,
                    minLines = 2,
                    supportingText = { Text("${notas.length}/$maxNotas") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (!errorGlobal.isNullOrBlank()) {
                item {
                    Text(
                        text = errorGlobal,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}