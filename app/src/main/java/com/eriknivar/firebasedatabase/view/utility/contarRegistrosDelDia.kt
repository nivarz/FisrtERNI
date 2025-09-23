package com.eriknivar.firebasedatabase.view.utility

import com.eriknivar.firebasedatabase.view.storagetype.DataFields
import java.time.LocalDate
import java.time.ZoneId

fun contarRegistrosDelDia(data: List<DataFields>, nombre: String): Int {
    val hoy: LocalDate = LocalDate.now(ZoneId.systemDefault())
    val objetivo = nombre.trim()

    return data.count { r ->
        val ts = r.fechaRegistro
            ?: r.fecha      // 👈 por si tu DataFields aún mantiene 'fecha' legacy
        val f = ts
            ?.toDate()
            ?.toInstant()
            ?.atZone(ZoneId.systemDefault())
            ?.toLocalDate()

        (f == hoy) && r.usuario.trim().equals(objetivo, ignoreCase = true)
    }
}

