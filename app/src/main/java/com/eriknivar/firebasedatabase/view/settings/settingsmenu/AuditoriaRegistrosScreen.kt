package com.eriknivar.firebasedatabase.view.settings.settingsmenu

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
import com.eriknivar.firebasedatabase.view.NavigationDrawer
import com.eriknivar.firebasedatabase.view.utility.AuditoriaCard
import com.eriknivar.firebasedatabase.view.utility.SessionUtils
import com.eriknivar.firebasedatabase.view.utility.exportarAuditoriaAExcel
import com.eriknivar.firebasedatabase.viewmodel.UserViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AuditoriaRegistrosScreen(navController: NavHostController, userViewModel: UserViewModel) {

    val isLoggedOut = userViewModel.nombre.observeAsState("").value.isEmpty()
    val isInitialized = userViewModel.isInitialized.observeAsState(false).value

    if (isInitialized && isLoggedOut) {
        // 🔴 No muestres nada, Compose lo ignora y se cerrará la app correctamente
        return
    }

    val auditorias = remember { mutableStateListOf<DocumentSnapshot>() }
    val firestore = FirebaseFirestore.getInstance()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val context = LocalContext.current
    val isExporting = remember { mutableStateOf(false) }
    val tipoFiltro = remember { mutableStateOf("Todos") }


    fun eliminarAuditoria(doc: DocumentSnapshot) {
        firestore.collection("auditoria_conteos").document(doc.id)
            .delete()
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

    val dummy = remember { mutableStateOf("") }

    val tipo = userViewModel.tipo.value ?: ""
    if (tipo.lowercase() !in listOf("admin", "superuser")) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.TopCenter
        ) {
            Text(
                "⛔ Acceso restringido",
                color = Color.Red,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }
        return
    }

    LaunchedEffect(Unit) {
        firestore.collection("auditoria_conteos").orderBy("fecha", Query.Direction.DESCENDING).get()
            .addOnSuccessListener {
                auditorias.clear()
                auditorias.addAll(it.documents)
            }
    }

    fun Any?.safeMapCast(): Map<String, Any?>? {
        return if (this is Map<*, *>) {
            this.entries
                .filter { it.key is String }
                .associate { it.key as String to it.value }
        } else null
    }

    val lastInteractionTime = remember { mutableLongStateOf(SessionUtils.obtenerUltimaInteraccion(context)) }

    fun actualizarActividad(context: Context) {
        val tiempoActual = System.currentTimeMillis()
        lastInteractionTime.longValue = tiempoActual
        SessionUtils.guardarUltimaInteraccion(context, tiempoActual)
    }

    LaunchedEffect(lastInteractionTime.longValue) {
        while (true) {
            delay(60_000)
            val tiempoActual = System.currentTimeMillis()
            val tiempoInactivo = tiempoActual - lastInteractionTime.longValue

            if (tiempoInactivo >= 30 * 60_000) {
                val documentId = userViewModel.documentId.value ?: ""
                Firebase.firestore.collection("usuarios")
                    .document(documentId)
                    .update("sessionId", "")
                Toast.makeText(context, "Sesión finalizada por inactividad", Toast.LENGTH_LONG).show()

                userViewModel.clearUser()

                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                }

                break
            }
        }
    }

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
        )
        {
            Icon(Icons.Default.FileDownload, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Exportar a Excel")
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Filtrar por :", modifier = Modifier
                .padding(end = 8.dp), fontWeight = FontWeight.Bold)
            var expanded by remember { mutableStateOf(false) }

            Box {
                Button(onClick = { expanded = true }) {
                    Text(tipoFiltro.value)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    listOf("Todos", "Modificación", "Eliminación").forEach { tipo ->
                        DropdownMenuItem(
                            text = { Text(tipo) },
                            onClick = {
                                tipoFiltro.value = tipo
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        val auditoriasFiltradas = auditorias.filter { doc ->
            val tipoAccion = doc.getString("tipo_accion")?.lowercase() ?: ""
            tipoFiltro.value.lowercase() == "todos" || tipoAccion == tipoFiltro.value.lowercase()
        }
        LazyColumn(modifier = Modifier.padding(16.dp)) {
            items(items = auditoriasFiltradas, key = { it.id }) { doc ->
                runCatching {
                    val tipoAccion = doc.getString("tipo_accion") ?: "Desconocido"
                    val registroId = doc.getString("registro_id") ?: "N/A"
                    val usuario = doc.getString("usuario") ?: "N/A"
                    val fecha = (doc.get("fecha") as? Timestamp) ?: Timestamp.now()
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
                        onDelete = {
                            eliminarAuditoria(doc)
                        }
                    )
                }.onFailure {
                    Log.e(
                        "Auditoría",
                        "Campo 'fecha' no es Timestamp: ${doc.get("fecha")?.javaClass}"
                    )

                }
            }
        }
    }
}


