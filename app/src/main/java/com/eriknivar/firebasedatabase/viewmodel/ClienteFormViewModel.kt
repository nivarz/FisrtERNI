package com.eriknivar.firebasedatabase.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eriknivar.firebasedatabase.view.utility.clientes.ClienteCreateInput
import com.eriknivar.firebasedatabase.view.utility.clientes.ClienteUtils
import com.eriknivar.firebasedatabase.view.utility.clientes.ClientesRepository
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

enum class ClienteFormMode { CREATE, EDIT }

data class ClienteFormInput(
    val clienteId: String? = null,
    val nombreComercial: String = "",
    val rncOCedula: String = "",
    val telefono: String = "",
    val email: String = "",
    val direccion: String = "",
    val notas: String = ""
)

data class ClienteFormUiState(
    val mode: ClienteFormMode = ClienteFormMode.CREATE,
    val loading: Boolean = false,
    val error: String? = null,
    val initial: ClienteFormInput = ClienteFormInput(),
    val saved: Boolean = false,
    val savedClienteId: String? = null
)

class ClienteFormViewModel(
    private val repo: ClientesRepository = ClientesRepository(),
    private val savedStateHandle: SavedStateHandle // <- no lo guardamos como propiedad para evitar warning
) : ViewModel() {

    // ✅ Constructor que sí entiende la factory por defecto
    constructor(savedStateHandle: SavedStateHandle) : this(
        ClientesRepository(),
        savedStateHandle
    )

    private val clienteIdArg: String? = savedStateHandle.get<String>("clienteId")

    private val _ui = MutableStateFlow(
        ClienteFormUiState(
            mode = if (clienteIdArg.isNullOrBlank()) ClienteFormMode.CREATE else ClienteFormMode.EDIT
        )
    )
    val ui: StateFlow<ClienteFormUiState> = _ui

    init {
        if (_ui.value.mode == ClienteFormMode.EDIT) {
            cargarInicial()
        }
    }

    private fun cargarInicial() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true, error = null)
            try {
                val id = clienteIdArg ?: throw IllegalStateException("clienteId requerido")
                val data = repo.obtenerClienteFormInput(id)
                if (data == null) {
                    _ui.value = _ui.value.copy(loading = false, error = "Cliente no encontrado.")
                } else {
                    _ui.value = _ui.value.copy(loading = false, initial = data)
                }
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(loading = false, error = e.message ?: "Error")
            }
        }
    }

    fun guardar(input: ClienteFormInput, usuarioUid: String) {
        if (_ui.value.loading) return
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true, error = null)
            try {
                val id: String =
                    if (_ui.value.mode == ClienteFormMode.CREATE) {
                        repo.crearCliente(
                            ClienteCreateInput(
                                nombreComercial = input.nombreComercial,
                                rncOCedula = input.rncOCedula,
                                telefono  = input.telefono.takeIf  { it.isNotBlank() },
                                email     = input.email.takeIf     { it.isNotBlank() },
                                direccion = input.direccion.takeIf { it.isNotBlank() },
                                notas     = input.notas.takeIf     { it.isNotBlank() },
                                creadoPorUid = usuarioUid
                            )
                        )
                    } else {
                        val editId =
                            requireNotNull(input.clienteId) { "clienteId requerido en EDIT" }
                        repo.editarCliente(
                            ClientesRepository.ClienteUpdateInput(
                                clienteId = editId,
                                nombreComercial = input.nombreComercial,
                                rncOCedula = input.rncOCedula,
                                telefono  = input.telefono.takeIf  { it.isNotBlank() },
                                email     = input.email.takeIf     { it.isNotBlank() },
                                direccion = input.direccion.takeIf { it.isNotBlank() },
                                notas     = input.notas.takeIf     { it.isNotBlank() },
                                actualizadoPorUid = usuarioUid
                            )
                        )
                        editId
                    }

                _ui.value = _ui.value.copy(loading = false, saved = true, savedClienteId = id)
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(loading = false, error = e.message ?: "Error")
            }
        }
    }

    fun clearError() {
        _ui.value = _ui.value.copy(error = null)
    }

    fun recargarInicial() {
        if (_ui.value.mode == ClienteFormMode.EDIT) {
            cargarInicial() // usa el private de adentro
        }
    }

    // --- Verificación asíncrona de RNC/Cédula (cliente duplicado) ---
    sealed class RncStatus {
        data object Idle : RncStatus()
        data object Checking : RncStatus()
        data object Libre : RncStatus()
        data object EnUso : RncStatus()
        data class Error(val msg: String) : RncStatus()
    }

    private val _rncStatus = MutableStateFlow<RncStatus>(RncStatus.Idle)
    val rncStatus: StateFlow<RncStatus> = _rncStatus

    fun checkRncDisponible(rncActual: String, rncOriginalDeEdicion: String?) {
        viewModelScope.launch {
            val limpio = ClienteUtils.limpiarRncOCedula(rncActual)
            // Si está vacío o no cambió en EDIT, no chequees
            if (limpio.isBlank() ||
                (!rncOriginalDeEdicion.isNullOrBlank()
                        && ClienteUtils.limpiarRncOCedula(rncOriginalDeEdicion) == limpio)
            ) {
                _rncStatus.value = RncStatus.Idle
                return@launch
            }

            try {
                _rncStatus.value = RncStatus.Checking
                val idxRef = Firebase.firestore
                    .collection("indices_clientes_rnc")
                    .document(limpio)
                val exists = idxRef.get().await().exists()
                _rncStatus.value = if (exists) RncStatus.EnUso else RncStatus.Libre
            } catch (e: Exception) {
                _rncStatus.value = RncStatus.Error(e.message ?: "Error verificando RNC")
            }
        }
    }


}