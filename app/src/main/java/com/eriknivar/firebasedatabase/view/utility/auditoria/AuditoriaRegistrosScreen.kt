package com.eriknivar.firebasedatabase.view.utility.auditoria

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.eriknivar.firebasedatabase.navigation.NavigationDrawer
import com.eriknivar.firebasedatabase.view.utility.SessionUtils
import com.eriknivar.firebasedatabase.view.utility.exportarAuditoriaAExcel
import com.eriknivar.firebasedatabase.viewmodel.UserViewModel
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AuditoriaRegistrosScreen(
    navController: NavHostController,
    userViewModel: UserViewModel
) {
    val isLoggedOut = userViewModel.nombre.observeAsState("").value.isEmpty()
    val isInitialized = userViewModel.isInitialized.observeAsState(false).value
    if (isInitialized && isLoggedOut) return

    val firestore = FirebaseFirestore.getInstance()
    val cid = (userViewModel.clienteId.value ?: "").trim().uppercase()
    val baseAud = firestore.collection("clientes").document(cid).collection("auditoria_conteos")

    val auditorias = remember { mutableStateListOf<DocumentSnapshot>() }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val isExporting = remember { mutableStateOf(false) }
    val tipoFiltro = remember { mutableStateOf("Todos") }

    // Borrar auditoría (ruta correcta)
    fun eliminarAuditoria(doc: DocumentSnapshot) {
        baseAud.document(doc.id).delete()
            .addOnSuccessListener {
                auditorias.remove(doc)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Registro eliminado correctamente")
                }
            }
            .addOnFailureListener {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Error al eliminar el registro")
                }
            }
    }

    // Acceso
    val tipo = userViewModel.tipo.value ?: ""
    if (tipo.lowercase() !in listOf("admin", "superuser")) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.TopCenter
        ) {
            Text(
                text = "⛔ Acceso restringido",
                color = Color.Red,
                fontSize = MaterialTheme.typography.titleMedium.fontSize,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(32.dp)
            )
        }
        return
    }

    // Carga inicial
    LaunchedEffect(cid) {
        baseAud.orderBy("fecha", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener {
                auditorias.clear()
                auditorias.addAll(it.documents)
            }
    }

    // Helper de casteo seguro para mostrar mapas en el card
    fun Any?.safeMapCast(): Map<String, Any?>? {
        return if (this is Map<*, *>) {
            this.entries
                .filter { it.key is String }
                .associate { it.key as String to it.value }
        } else null
    }

    // Control de sesión por inactividad
    val lastInteractionTime =
        remember { mutableLongStateOf(SessionUtils.obtenerUltimaInteraccion(context)) }

    fun actualizarActividad(context: Context) {
        val now = System.currentTimeMillis()
        lastInteractionTime.longValue = now
        SessionUtils.guardarUltimaInteraccion(context, now)
    }

    LaunchedEffect(lastInteractionTime.longValue) {
        while (true) {
            delay(60_000)
            val now = System.currentTimeMillis()
            val inactive = now - lastInteractionTime.longValue
            if (inactive >= 30 * 60_000) {
                val documentId = userViewModel.documentId.value ?: ""
                Firebase.firestore.collection("usuarios")
                    .document(documentId)
                    .update("sessionId", "")
                Toast.makeText(context, "Sesión finalizada por inactividad", Toast.LENGTH_LONG)
                    .show()
                userViewModel.clearUser()
                navController.navigate("login") { popUpTo(0) { inclusive = true } }
                break
            }
        }
    }

    // Dummy refs que usa tu NavigationDrawer
    val dummy = remember { mutableStateOf("") }

    NavigationDrawer(
        navController = navController,
        storageType = "Auditoría",
        userViewModel = userViewModel,
        location = dummy,
        sku = dummy,
        quantity = dummy,
        lot = dummy,
        expirationDate = dummy
    ) {
        SnackbarHost(hostState = snackbarHostState)

        // Exportar a Excel
        Button(
            onClick = {
                actualizarActividad(context)
                exportarAuditoriaAExcel(
                    context = context,
                    snackbarHostState = snackbarHostState,
                    coroutineScope = coroutineScope,
                    onFinish = { isExporting.value = false }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF001F5B),
                contentColor = Color.White
            )
        ) {
            Icon(Icons.Default.FileDownload, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Exportar a Excel")
        }

        // Filtro
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Filtrar por :", modifier = Modifier.padding(end = 8.dp),
                fontWeight = FontWeight.Bold
            )
            var expanded by remember { mutableStateOf(false) }
            Box {
                Button(onClick = { expanded = true }) { Text(tipoFiltro.value) }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    listOf("Todos", "Modificación", "Eliminación").forEach { tipoOpt ->
                        DropdownMenuItem(
                            text = { Text(tipoOpt) },
                            onClick = {
                                tipoFiltro.value = tipoOpt
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        // Lista
        LazyColumn(modifier = Modifier.padding(16.dp)) {
            val auditoriasFiltradas = auditorias.filter { doc ->
                val t = doc.getString("tipo_accion")?.lowercase() ?: ""
                tipoFiltro.value.lowercase() == "todos" || t == tipoFiltro.value.lowercase()
            }

            items(items = auditoriasFiltradas, key = { it.id }) { doc ->
                runCatching {
                    val tipoAccion = doc.getString("tipo_accion") ?: "Desconocido"
                    val registroId = doc.getString("registro_id") ?: "N/A"
                    val usuario = doc.getString("usuarioNombre") ?: "N/A"
                    val fecha = doc.getTimestamp("fecha") ?: Timestamp.now()
                    val valoresAntes = doc.get("valores_antes").safeMapCast()
                    val valoresDespues = doc.get("valores_despues").safeMapCast()

                    AuditoriaCard(
                        tipoAccion = tipoAccion,
                        registroId = registroId,
                        usuario = usuario,
                        fecha = fecha,
                        valoresAntes = valoresAntes,
                        valoresDespues = valoresDespues,
                        tipoUsuario = userViewModel.tipo.value ?: "",
                        onDelete = { eliminarAuditoria(doc) }
                    )
                }.onFailure { e ->
                    Log.e(
                        "Auditoría",
                        "Campo 'fecha' no es Timestamp: ${doc.get("fecha")?.javaClass}",
                        e
                    )
                }
            }
        }
    }
}
