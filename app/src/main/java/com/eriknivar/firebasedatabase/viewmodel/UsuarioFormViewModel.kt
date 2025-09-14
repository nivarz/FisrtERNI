package com.eriknivar.firebasedatabase.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eriknivar.firebasedatabase.view.utility.UsuariosRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class UsuarioFormUi(
    val nombre: String = "",
    val email: String = "",
    val tipo: String = "invitado",  // default
    val clienteId: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false
)

class UsuarioFormViewModel(
    private val repo: UsuariosRepository = UsuariosRepository()
) : ViewModel() {

    private val _ui = MutableStateFlow(UsuarioFormUi())
    val ui: StateFlow<UsuarioFormUi> = _ui

    fun setNombre(v: String) { _ui.value = _ui.value.copy(nombre = v, error = null) }
    fun setEmail(v: String) { _ui.value = _ui.value.copy(email = v, error = null) }
    fun setTipo(v: String) { _ui.value = _ui.value.copy(tipo = v, error = null) }
    fun setClienteId(v: String) { _ui.value = _ui.value.copy(clienteId = v, error = null) }

    fun guardar() {
        val s = _ui.value
        if (s.nombre.isBlank() || s.email.isBlank() || s.clienteId.isBlank()) {
            _ui.value = s.copy(error = "Completa nombre, email y cliente.", loading = false)
            return
        }
        viewModelScope.launch {
            try {
                _ui.value = s.copy(loading = true, error = null)
                val uid = Firebase.auth.currentUser?.uid.orEmpty()

                repo.crearUsuarioDoc(
                    com.eriknivar.firebasedatabase.view.utility.UsuarioCreateInput(
                        nombre = s.nombre,
                        email = s.email,
                        tipo = s.tipo,
                        clienteId = s.clienteId,
                        creadoPorUid = uid
                    )
                )
                _ui.value = _ui.value.copy(loading = false, saved = true)
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(loading = false, error = e.message ?: "Error")
            }
        }
    }

    fun clearError() { _ui.value = _ui.value.copy(error = null) }
}
