package com.eriknivar.firebasedatabase.view.settings.settingsmenu

// Cliente.kt

data class Cliente(
    val clienteId: String = "",              // "000001"
    val nombreComercial: String = "",
    val rncOCedula: String = "",
    val telefono: String? = null,
    val email: String? = null,
    val direccion: String? = null,
    val notas: String? = null,
    val activo: Boolean = true,
    // Derivados / normalizados
    val nombreNormalizado: String = "",
    val rncLimpio: String = "",
    // Auditoría (timestamps se ponen con serverTimestamp en el Map)
    val creadoPorUid: String = "",
    val actualizadoPorUid: String? = null
)
