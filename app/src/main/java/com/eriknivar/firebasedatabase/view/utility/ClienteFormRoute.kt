package com.eriknivar.firebasedatabase.view.utility

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.eriknivar.firebasedatabase.viewmodel.ClienteFormMode
import com.eriknivar.firebasedatabase.viewmodel.ClienteFormViewModel
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClienteFormRoute(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: ClienteFormViewModel = viewModel()
) {
    val ui by viewModel.ui.collectAsState()
    val context = LocalContext.current
    val currentUid = remember { FirebaseAuth.getInstance().currentUser?.uid.orEmpty() }

    // ✅ Si estamos en EDIT y aún no se ha cargado el doc, muestra loader
    val isPreloading = ui.mode == ClienteFormMode.EDIT &&
            ui.initial.clienteId == null &&
            ui.loading

    // ❌ Si hubo error antes de tener datos iniciales, muestra pantalla de error
    val isPreloadError = ui.mode == ClienteFormMode.EDIT &&
            ui.initial.clienteId == null &&
            ui.error != null

    val keyboard = LocalSoftwareKeyboardController.current

    // Navegar al guardar
    LaunchedEffect(ui.saved) {
        if (ui.saved) {
            keyboard?.hide()
            onSaved()
        }
    }

    when {
        isPreloading -> {
            // Pantalla simple con loader
            Scaffold(topBar = {
                TopAppBar(title = { Text("Editar cliente") }, navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                })
            }) { padding ->
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }

        isPreloadError -> {
            // Pantalla de error con reintentar
            Scaffold(topBar = {
                TopAppBar(title = { Text("Editar cliente") }, navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                })
            }) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No se pudo cargar el cliente.", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    Text(ui.error ?: "", color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = { viewModel.recargarInicial() }) {
                        Text("Reintentar")
                    }
                }
            }
        }

        else -> {
            // Formulario normal (CREATE o EDIT con datos ya cargados)
            ClienteFormScreen(
                mode = ui.mode,
                initial = ui.initial,
                loading = ui.loading,
                errorGlobal = ui.error,
                onBack = {
                    keyboard?.hide()   // opcional: también al salir con “Cancelar” o back
                    onBack()
                },
                onSubmit = { input ->
                    // tu guardar normal
                    viewModel.guardar(input, usuarioUid = /* uid actual */ "")
                }
            )
        }
    }
}