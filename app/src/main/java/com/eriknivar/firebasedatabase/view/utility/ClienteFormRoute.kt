package com.eriknivar.firebasedatabase.view.utility

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.eriknivar.firebasedatabase.viewmodel.ClienteFormViewModel
import com.google.firebase.auth.FirebaseAuth

@Composable
fun ClienteFormRoute(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: ClienteFormViewModel = viewModel()
) {
    val ui by viewModel.ui.collectAsState()
    val context = LocalContext.current

    // ✅ Toma el UID del usuario logueado
    val currentUid = remember { FirebaseAuth.getInstance().currentUser?.uid.orEmpty() }

    LaunchedEffect(ui.saved) {
        if (ui.saved) onSaved()
    }

    ClienteFormScreen(
        mode = ui.mode,
        initial = ui.initial,
        loading = ui.loading,
        errorGlobal = ui.error,
        onBack = onBack,
        onSubmit = { input ->
            if (currentUid.isBlank()) {
                Toast.makeText(context, "No hay sesión válida", Toast.LENGTH_SHORT).show()
                return@ClienteFormScreen
            }
            viewModel.guardar(input, usuarioUid = currentUid)
        }
    )
}
