package com.eriknivar.firebasedatabase.view.utility

import android.util.Log
import com.eriknivar.firebasedatabase.view.storagetype.DataFields
import java.time.LocalDate
import java.time.ZoneId

fun contarRegistrosDelDia(data: List<DataFields>, nombre: String): Int {
    val hoy = LocalDate.now()
    return data.count { registro ->
        val fechaRegistro = registro.fechaRegistro
            ?.toDate()
            ?.toInstant()
            ?.atZone(ZoneId.systemDefault())
            ?.toLocalDate()

        fechaRegistro == hoy && registro.usuario.equals(nombre, ignoreCase = true)
    }
}





