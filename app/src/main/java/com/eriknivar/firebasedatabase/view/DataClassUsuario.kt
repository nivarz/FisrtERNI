package com.eriknivar.firebasedatabase.view

data class Usuario(
    val id: String,
    val nombre: String,
    val usuario: String,
    val contrasena: String,
    val tipo: String,
    val clienteId: String = "",
    val requiereCambioPassword: Boolean = false,
    val sessionId: String? = null
)