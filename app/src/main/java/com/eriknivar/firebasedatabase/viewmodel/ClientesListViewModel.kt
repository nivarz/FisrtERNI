package com.eriknivar.firebasedatabase.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eriknivar.firebasedatabase.view.utility.ClientesRepository
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ClientesListViewModel(
    private val repo: ClientesRepository = ClientesRepository()
) : ViewModel() {

    companion object {
        private const val DEFAULT_QUERY = ""
        private const val QUERY_DEBOUNCE_MS = 0L // si luego quieres debouce, súbelo
        private const val ERROR_GENERIC = "Ocurrió un error. Intenta de nuevo."
        private const val ERROR_EMPTY = "No se encontraron clientes."
        private const val ERROR_LOADING = "No se pudo cargar la lista."
        private const val ERROR_LOADING_MORE = "No se pudo cargar más resultados."
        private const val ERROR_SEARCH = "No se pudo realizar la búsqueda."
        private const val ERROR_RETRY = "No se pudo reintentar."
        private const val ERROR_REFRESH = "No se pudo refrescar."
        private const val ERROR_PARAMS = "Parámetros inválidos."
        private const val ERROR_STATE = "Estado inválido."
        private const val ERROR_UNKNOWN = "Error desconocido."
        private val PAGE_SIZE: Long = 20L      // 👈 IMPORTANTE: Long
    }

    data class UiState(
        val items: List<ClientesRepository.ClienteListItem> = emptyList(),
        val loading: Boolean = false,
        val error: String? = null,
        val query: String = DEFAULT_QUERY,
        val endReached: Boolean = false,
        val empty: Boolean = false
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    private var lastSnapshot: DocumentSnapshot? = null
    private var loadJob: Job? = null

    init {
        // Carga inicial
        loadFirstPage()
    }

    fun onQueryChange(newQuery: String) {
        val q = newQuery.trim()
        if (q == _ui.value.query) return
        _ui.value = _ui.value.copy(query = q)
        loadFirstPage()
    }

    fun retry() {
        loadFirstPage()
    }

    fun loadNextPage() {
        val state = _ui.value
        if (state.loading || state.endReached) return

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _ui.value = state.copy(loading = true, error = null)
            try {
                val result = if (state.query.isBlank()) {
                    repo.listarClientesActivos(
                        limit = PAGE_SIZE,                    // 👈 Long
                        startAfter = lastSnapshot
                    )
                } else {
                    repo.buscarClientes(
                        term = state.query,
                        limit = PAGE_SIZE,                    // 👈 Long
                        startAfter = lastSnapshot
                    )
                }

                val newList = state.items + result.items
                lastSnapshot = result.lastSnapshot
                val reached = result.items.isEmpty()

                _ui.value = state.copy(
                    items = newList,
                    loading = false,
                    error = null,
                    endReached = reached,
                    empty = newList.isEmpty()
                )
            } catch (e: Exception) {
                _ui.value = state.copy(
                    loading = false,
                    error = ERROR_LOADING_MORE
                )
            }
        }
    }

    private fun loadFirstPage() {
        loadJob?.cancel()
        lastSnapshot = null

        val current = _ui.value
        _ui.value = current.copy(loading = true, error = null, endReached = false)

        loadJob = viewModelScope.launch {
            try {
                val result = if (current.query.isBlank()) {
                    repo.listarClientesActivos(
                        limit = PAGE_SIZE,                    // 👈 Long
                        startAfter = null
                    )
                } else {
                    repo.buscarClientes(
                        term = current.query,
                        limit = PAGE_SIZE,                    // 👈 Long
                        startAfter = null
                    )
                }

                lastSnapshot = result.lastSnapshot
                _ui.value = current.copy(
                    items = result.items,
                    loading = false,
                    error = null,
                    endReached = result.items.isEmpty(),
                    empty = result.items.isEmpty()
                )
            } catch (e: Exception) {
                _ui.value = current.copy(
                    loading = false,
                    error = ERROR_LOADING
                )
            }
        }
    }
}


