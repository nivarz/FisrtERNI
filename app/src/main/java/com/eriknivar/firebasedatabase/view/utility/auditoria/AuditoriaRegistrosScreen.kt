// AuditoriaRegistrosScreen.kt
package com.eriknivar.firebasedatabase.view.auditoria

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.eriknivar.firebasedatabase.navigation.NavigationDrawer
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.ktx.firestore
import kotlinx.coroutines.tasks.await
import com.eriknivar.firebasedatabase.view.utility.auditoria.AuditoriaCard
import com.eriknivar.firebasedatabase.viewmodel.UserViewModel
import com.google.firebase.Timestamp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.ListenerRegistration
import java.text.Normalizer.normalize

data class ClienteLite(val id: String, val nombre: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditoriaRegistrosScreen(
    userViewModel: UserViewModel,
    navController: NavHostController,
    onMenuClick: () -> Unit = {}
) {
    val db = Firebase.firestore
    val snackbarHostState = remember { SnackbarHostState() }

    // Rol/Cliente del usuario actual
    val tipoUsuario = (userViewModel.tipo.value ?: "").lowercase()
    val clienteIdVM = (userViewModel.clienteId.value ?: "").uppercase()

    // Estado UI
    val isSuper = tipoUsuario == "superuser"
    var selectedCid by remember { mutableStateOf(if (isSuper) "" else clienteIdVM) }
    var clientes by remember { mutableStateOf<List<ClienteLite>>(emptyList()) }
    var auditorias by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val tipo by userViewModel.tipo.observeAsState("")

    if (!tipo.equals("admin", true) && !isSuper) {
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

    // 1) Cargar lista de clientes (solo SUPERUSER)
    LaunchedEffect(isSuper) {
        if (!isSuper) return@LaunchedEffect
        try {
            val snap = db.collection("clientes")
                .orderBy(com.google.firebase.firestore.FieldPath.documentId())
                .get()
                .await()

            clientes = snap.documents.map { d ->
                ClienteLite(id = d.id.uppercase(), nombre = d.getString("nombre") ?: d.id)
            }.sortedBy { it.id }

        } catch (e: Exception) {
            Log.e("Auditoria", "No se pudieron cargar clientes", e)
            errorMsg = "No se pudieron cargar los clientes"
        }
    }

    // 2) Escuchar auditoría en TIEMPO REAL cuando cambie el cliente
    var auditListener by remember { mutableStateOf<ListenerRegistration?>(null) }

    DisposableEffect(selectedCid) {
        // Limpia el listener anterior
        auditListener?.remove()
        auditListener = null
        auditorias = emptyList()

        if (selectedCid.isNotBlank()) {
            loading = true
            errorMsg = null
            val q = db.collection("clientes")
                .document(selectedCid)
                .collection("auditoria_registros")
                .orderBy("fecha", Query.Direction.DESCENDING)
                .limit(200)

            auditListener = q.addSnapshotListener { qs, e ->
                if (e != null) {
                    Log.e("Auditoria", "Error escuchando auditorías", e)
                    errorMsg = "Error cargando auditorías"
                    loading = false
                    return@addSnapshotListener
                }
                val docs = qs?.documents ?: emptyList()
                auditorias = docs.map { it.data ?: emptyMap() }
                loading = false
            }
        }

        onDispose {
            auditListener?.remove()
            auditListener = null
        }
    }

    // ===== Buscador por usuario (nombre/email/uid) =====
    val keyboard = LocalSoftwareKeyboardController.current
    var query by rememberSaveable { mutableStateOf("") }

    // Normalizador (acentos y mayúsculas) local al composable
    fun norm(s: String?): String =
        java.text.Normalizer.normalize(s.orEmpty(), java.text.Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
            .lowercase()
            .trim()

    // Lista filtrada derivada
    val filtered = remember(query, auditorias) {
        val q = norm(query)
        if (q.isBlank()) auditorias else auditorias.filter { audit ->
            val nombre = norm(audit["usuarioNombre"] as? String)
            val email  = norm(audit["usuarioEmail"] as? String)
            val uid    = norm(audit["usuarioUid"] as? String)
            nombre.contains(q) || email.contains(q) || uid.contains(q)
        }
    }

    // Dummy refs que usa tu NavigationDrawer
    val dummy = remember { mutableStateOf("") }

    NavigationDrawer(
        navController = navController,
        storageType = "Auditoría de Registros",
        userViewModel = userViewModel,
        location = dummy,
        sku = dummy,
        quantity = dummy,
        lot = dummy,
        expirationDate = dummy
    ) {

        // Selector de cliente (solo SUPERUSER)
        if (isSuper) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                ClienteSelector(
                    clientes = clientes,
                    selected = selectedCid,
                    onSelect = { selectedCid = it }
                )
            }
            Spacer(Modifier.height(16.dp))
        } else {
            // Admin/User: cliente fijo
            Text("Cliente: $clienteIdVM", style = MaterialTheme.typography.bodyMedium)
            if (selectedCid.isBlank()) selectedCid = clienteIdVM
            Spacer(Modifier.height(16.dp))
        }

        // 🔎 Barra de búsqueda (debajo del selector de cliente)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            singleLine = true,
            label = { Text("Buscar por usuario") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Limpiar")
                    }
                }
            },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() })
        )

        if (loading) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
        }

        errorMsg?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
        }

        // (opcional) Conteo cuando hay filtro activo
        if (query.isNotBlank()) {
            Text(
                text = "Resultados: ${filtered.size}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
            )
        }

        // Lista de auditorías (usa la lista filtrada)
        LazyColumn(Modifier.fillMaxSize()) {
            items(filtered) { audit ->
                val tipoAccion = (audit["tipo_accion"] as? String).orEmpty()
                val registroId = (audit["registro_id"] as? String).orEmpty()
                val fechaTs = audit["fecha"] as? Timestamp
                val usuarioTexto = (audit["usuarioNombre"] as? String)
                    ?: (audit["usuarioUid"] as? String).orEmpty()

                val valoresAntesMap =
                    (audit["valores_antes"] as? Map<String, Any?>).orEmpty()
                val valoresDespuesMap =
                    (audit["valores_despues"] as? Map<String, Any?>).orEmpty()

                val usuarioEmail = audit["usuarioEmail"] as? String

                AuditoriaCard(
                    tipoAccion = tipoAccion,
                    registroId = registroId,
                    fecha = fechaTs,
                    usuario = usuarioTexto,
                    tipoUsuario = tipoUsuario,
                    onDelete = { /* handler real opcional aquí */ },
                    valoresAntes = valoresAntesMap,
                    valoresDespues = valoresDespuesMap,
                    usuarioEmail = usuarioEmail
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}


/** Selector simple para SUPERUSER */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClienteSelector(
    clientes: List<ClienteLite>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var buttonWidth by remember { mutableStateOf(0) }

    val keyboard = LocalSoftwareKeyboardController.current
    var query by rememberSaveable { mutableStateOf("") }

    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier
                .fillMaxWidth(0.9f)             // ⬅️ ocupa el 90% del ancho disponible
                .widthIn(min = 320.dp, max = 560.dp) // ⬅️ límites razonables
                .onGloballyPositioned { coords ->
                    buttonWidth = coords.size.width
                },
            shape = RoundedCornerShape(50),
            border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                text = if (selected.isBlank()) "Seleccione un Cliente..." else "Cliente: $selected",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = null
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(with(LocalDensity.current) { buttonWidth.toDp() })
                .heightIn(max = 320.dp)
        ) {
            clientes.forEachIndexed { index, c ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "${c.id} — ${c.nombre}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    onClick = {
                        onSelect(c.id)
                        expanded = false
                    }
                )
                if (index < clientes.lastIndex) {
                    HorizontalDivider()
                }
            }
        }
    }
}
