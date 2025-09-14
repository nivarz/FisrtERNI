package com.eriknivar.firebasedatabase.network

import com.eriknivar.firebasedatabase.network.dto.LocalidadDto
import com.eriknivar.firebasedatabase.network.dto.UbicacionDto

class CatalogoRepository {
    suspend fun localidades(): List<LocalidadDto> {
        // Si es superuser, pasa el cliente también por query (además del header)
        val cid: String? = if (SelectedClientStore.isSuperuser) SelectedClientStore.selectedClienteId else null
        return Api.service.getLocalidades(clienteId = cid)
    }

    suspend fun ubicaciones(localidad: String): List<UbicacionDto> {
        val cid: String? = if (SelectedClientStore.isSuperuser) SelectedClientStore.selectedClienteId else null
        return Api.service.getUbicaciones(localidad = localidad, clienteId = cid)
    }
}
