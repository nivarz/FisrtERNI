package com.eriknivar.firebasedatabase.view.settings.settingsmenu

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.eriknivar.firebasedatabase.view.NavigationDrawer
import com.eriknivar.firebasedatabase.view.utility.AuditoriaCard
import com.eriknivar.firebasedatabase.viewmodel.UserViewModel
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp

@Composable
fun AuditoriaRegistrosScreen(navController: NavHostController, userViewModel: UserViewModel) {

    val auditorias = remember { mutableStateListOf<DocumentSnapshot>() }
    val firestore = FirebaseFirestore.getInstance()

    val dummy = remember { mutableStateOf("") }

    val tipo = userViewModel.tipo.value ?: ""
    if (tipo.lowercase() !in listOf("admin", "superuser")) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.Center
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
        LazyColumn(modifier = Modifier.padding(16.dp)) {

            items(items = auditorias, key = { it.id }) { doc ->
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
                        valoresDespues = valoresDespues
                    )
                }.onFailure {
                    Log.e("Auditoría", "Campo 'fecha' no es Timestamp: ${doc.get("fecha")?.javaClass}")

                }
            }






        }
    }
}


