package com.eriknivar.firebasedatabase.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eriknivar.firebasedatabase.view.utility.clientes.ClientesRepository
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private const val PAGE_SIZE = 20L
private const val SEARCH_DEBOUNCE_MS = 350L

enum class ClientesMode { LIST, SEARCH }

data class ClientesUiState(
    val mode: ClientesMode = ClientesMode.LIST,
    val query: String = "",
    val items: List<ClientesRepository.ClienteListItem> = emptyList(),
    val lastSnapshot: DocumentSnapshot? = null,
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val endReached: Boolean = false
)

class ClientesViewModel(
    private val repo: ClientesRepository = ClientesRepository() // inyecta por Hilt si lo usas
) : ViewModel() {

    private val _ui = MutableStateFlow(ClientesUiState())
    val ui: StateFlow<ClientesUiState> = _ui

    private var searchJob: Job? = null

    init {
        // Carga inicial (activos)
        refresh()
    }

    private fun refresh() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(
                loading = true, error = null, endReached = false, lastSnapshot = null
            )
            runCatching {
                repo.listarClientesActivos(limit = PAGE_SIZE)
            }.onSuccess { page ->
                _ui.value = _ui.value.copy(
                    mode = ClientesMode.LIST,
                    items = page.items,
                    lastSnapshot = page.lastSnapshot,
                    loading = false,
                    endReached = page.lastSnapshot == null
                )
            }.onFailure { e ->
                _ui.value = _ui.value.copy(loading = false, error = e.message ?: "Error")
            }
        }
    }

    fun onQueryChange(text: String) {
        _ui.value = _ui.value.copy(query = text)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)

            if (text.isBlank()) {
                // Volver a modo LIST
                refresh()
                return@launch
            }

            _ui.value = _ui.value.copy(
                mode = ClientesMode.SEARCH,
                loading = true, error = null, lastSnapshot = null, endReached = false
            )

            runCatching {
                repo.buscarClientes(term = text, limit = PAGE_SIZE)
            }.onSuccess { page ->
                _ui.value = _ui.value.copy(
                    items = page.items,
                    lastSnapshot = page.lastSnapshot,
                    loading = false,
                    endReached = page.lastSnapshot == null
                )
            }.onFailure { e ->
                _ui.value = _ui.value.copy(loading = false, error = e.message ?: "Error")
            }
        }
    }

    fun loadMore() {
        val state = _ui.value
        if (state.loading || state.loadingMore || state.endReached) return

        viewModelScope.launch {
            _ui.value = _ui.value.copy(loadingMore = true, error = null)

            val result = runCatching {
                when (state.mode) {
                    ClientesMode.LIST -> repo.listarClientesActivos(
                        limit = PAGE_SIZE, startAfter = state.lastSnapshot
                    )
                    ClientesMode.SEARCH -> repo.buscarClientes(
                        term = state.query, limit = PAGE_SIZE, startAfter = state.lastSnapshot
                    )
                }
            }

            result.onSuccess { page ->
                _ui.value = _ui.value.copy(
                    items = state.items + page.items,
                    lastSnapshot = page.lastSnapshot,
                    loadingMore = false,
                    endReached = page.lastSnapshot == null
                )
            }.onFailure { e ->
                _ui.value = _ui.value.copy(loadingMore = false, error = e.message ?: "Error")
            }
        }
    }

    fun clearError() {
        _ui.value = _ui.value.copy(error = null)
    }
}
