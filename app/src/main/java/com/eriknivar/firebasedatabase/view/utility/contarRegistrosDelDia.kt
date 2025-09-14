package com.eriknivar.firebasedatabase.view.utility

import android.util.Log
import com.eriknivar.firebasedatabase.view.storagetype.DataFields
import java.time.LocalDate
import java.time.ZoneId

fun contarRegistrosDelDia(data: List<DataFields>, nombre: String): Int {
    val hoy = java.time.LocalDate.now(java.time.ZoneId.systemDefault())
    val objetivo = nombre.trim()
    return data.count { r ->
        val f = r.fechaRegistro
            ?.toDate()
            ?.toInstant()
            ?.atZone(java.time.ZoneId.systemDefault())
            ?.toLocalDate()
        f == hoy && r.usuario.trim().equals(objetivo, ignoreCase = true)
    }
}

