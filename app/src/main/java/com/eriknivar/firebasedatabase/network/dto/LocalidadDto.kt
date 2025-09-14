package com.eriknivar.firebasedatabase.network.dto

import com.google.gson.annotations.SerializedName

data class LocalidadDto(
    @SerializedName("codigo") val codigo: String,
    @SerializedName("nombre") val nombre: String? = null
)