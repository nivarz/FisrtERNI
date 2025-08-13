package com.eriknivar.firebasedatabase.view.utility

data class Cliente(
    val id: String = "",          // igual al documentId (clienteId)
    val nombre: String = "",
    val activo: Boolean = true
)
