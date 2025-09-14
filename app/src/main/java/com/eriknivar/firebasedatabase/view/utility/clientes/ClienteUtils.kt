package com.eriknivar.firebasedatabase.view.utility.clientes

// ClienteUtils.kt


import java.text.Normalizer
import java.util.Locale

object ClienteUtils {

    fun zeroPad6(n: Int): String = String.format(Locale.US, "%06d", n)

    fun normalizarNombre(input: String): String {
        val sinAcentos = Normalizer.normalize(input, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        return sinAcentos.uppercase(Locale.US).trim()
    }

    public fun limpiarRncOCedula(input: String): String {
        // Mantiene solo letras y dígitos (sin espacios ni guiones)
        return input.uppercase(Locale.US)
            .replace("[^A-Z0-9]".toRegex(), "")
            .trim()
    }

    fun validarNombre(nombre: String): String? =
        if (nombre.isBlank()) "El nombre comercial es obligatorio." else null

    fun validarRnc(rnc: String): String? =
        if (limpiarRncOCedula(rnc).isBlank()) "El RNC/Cédula es obligatorio." else null

    fun validarEmailBasico(email: String?): String? {
        if (email.isNullOrBlank()) return null
        val ok = Regex("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", RegexOption.IGNORE_CASE)
            .matches(email)
        return if (!ok) "Email inválido." else null
    }

    fun validarTelefonoRD(telefono: String?): String? {
        if (telefono.isNullOrBlank()) return null
        val soloDigitos = telefono.replace("\\D".toRegex(), "")
        // RD típico: 10 dígitos
        return if (soloDigitos.length != 10) "Teléfono RD debe tener 10 dígitos." else null
    }
}
