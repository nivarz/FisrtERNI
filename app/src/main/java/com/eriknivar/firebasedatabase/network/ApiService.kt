package com.eriknivar.firebasedatabase.network

import retrofit2.http.GET
import retrofit2.http.Query
import com.eriknivar.firebasedatabase.network.dto.LocalidadDto
import com.eriknivar.firebasedatabase.network.dto.UbicacionDto


interface ApiService {

    // Nota: clienteId es opcional. Para admin/invitado deja null.
    // Si eres superuser y pusiste SelectedClientStore, igual viaja X-Cliente-Id.
    @GET("localidades")
    suspend fun getLocalidades(
        @Query("clienteId") clienteId: String? = null
    ): List<LocalidadDto>

    @GET("ubicaciones")
    suspend fun getUbicaciones(
        @Query("localidad") localidad: String,
        @Query("clienteId") clienteId: String? = null
    ): List<UbicacionDto>
}
