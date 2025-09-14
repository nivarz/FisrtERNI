package com.eriknivar.firebasedatabase.network.dto

import com.google.gson.annotations.SerializedName

data class UbicacionDto(
    @SerializedName("codigo_ubi") val codigoUbi: String,
    @SerializedName("localidad")  val localidad: String,
    @SerializedName("zona")       val zona: String? = null,
    @SerializedName("descripcion")val descripcion: String? = null
)