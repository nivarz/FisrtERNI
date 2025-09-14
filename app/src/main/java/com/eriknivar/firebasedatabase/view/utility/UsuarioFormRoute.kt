package com.eriknivar.firebasedatabase.view.utility

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eriknivar.firebasedatabase.viewmodel.UsuarioFormViewModel

@Composable
fun UsuarioFormRoute(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    vm: UsuarioFormViewModel = viewModel()
) {
    val ui by vm.ui.collectAsState()

    LaunchedEffect(ui.saved) {
        if (ui.saved) onSaved()
    }

    UsuarioFormScreen(
        ui = ui,
        onBack = onBack,
        onNombre = vm::setNombre,
        onEmail = vm::setEmail,
        onTipo = vm::setTipo,
        onClienteId = vm::setClienteId,
        onSave = vm::guardar
    )
}
