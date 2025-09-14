package com.eriknivar.firebasedatabase.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eriknivar.firebasedatabase.network.dto.LocalidadDto
import com.eriknivar.firebasedatabase.network.dto.UbicacionDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CatalogoViewModel : ViewModel() {

    private val repo = CatalogoRepository()

    private val _localidades = MutableStateFlow<List<LocalidadDto>>(emptyList())
    val localidades: StateFlow<List<LocalidadDto>> = _localidades

    private val _ubicaciones = MutableStateFlow<List<UbicacionDto>>(emptyList())
    val ubicaciones: StateFlow<List<UbicacionDto>> = _ubicaciones

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    /** True si: (no es superuser) OR (es superuser y ya eligió cliente) */
    fun isClientReady(): Boolean =
        !SelectedClientStore.isSuperuser || SelectedClientStore.selectedClienteId != null

    fun cargarLocalidades() {
        if (!isClientReady()) {
            _localidades.value = emptyList()
            _ubicaciones.value = emptyList()
            return
        }
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                _localidades.value = repo.localidades()
            } catch (e: Exception) {
                _error.value = e.message ?: "Error al cargar localidades"
                _localidades.value = emptyList()
            } finally {
                _loading.value = false
            }
        }
    }

    fun cargarUbicaciones(localidad: String) {
        if (!isClientReady()) {
            _ubicaciones.value = emptyList()
            return
        }
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                _ubicaciones.value = repo.ubicaciones(localidad)
            } catch (e: Exception) {
                _error.value = e.message ?: "Error al cargar ubicaciones"
                _ubicaciones.value = emptyList()
            } finally {
                _loading.value = false
            }
        }
    }

    fun limpiarUbicaciones() {
        _ubicaciones.value = emptyList()
    }
}
