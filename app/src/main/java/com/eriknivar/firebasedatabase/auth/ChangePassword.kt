package com.eriknivar.firebasedatabase.auth

import android.util.Patterns
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException

/**
 * Cambia la contraseña del usuario actual.
 *
 * Flujo:
 *  1) Valida entradas localmente.
 *  2) Reautentica con la contraseña ACTUAL.
 *  3) Actualiza a la contraseña NUEVA.
 *  4) Hace signOut() al finalizar con éxito (UI debe navegar a LOGIN).
 *
 * @param actual Contraseña actual del usuario.
 * @param nueva  Nueva contraseña deseada.
 * @param onResult Callback con (ok, mensaje). Si ok=true, ya se hizo signOut().
 */
fun cambiarPasswordFirebase(
    actual: String,
    nueva: String,
    onResult: (ok: Boolean, msg: String) -> Unit
) {
    // === 0) Validaciones locales rápidas ===
    val actualTrim = actual.trim()
    val nuevaTrim  = nueva.trim()

    if (actualTrim.isEmpty()) {
        onResult(false, "La contraseña actual está vacía.")
        return
    }
    if (nuevaTrim.isEmpty()) {
        onResult(false, "La nueva contraseña está vacía.")
        return
    }
    if (nuevaTrim.length < 6) {
        onResult(false, "La nueva contraseña debe tener al menos 6 caracteres.")
        return
    }
    if (nuevaTrim == actualTrim) {
        onResult(false, "La nueva contraseña no puede ser igual a la actual.")
        return
    }

    val user = FirebaseAuth.getInstance().currentUser
        ?: run {
            onResult(false, "Sesión expirada. Inicia sesión nuevamente.")
            return
        }

    val email = user.email
        ?: run {
            onResult(false, "La cuenta no tiene email asociado.")
            return
        }

    // (Opcional) Evitar contraseñas que sean literalmente el email
    if (Patterns.EMAIL_ADDRESS.matcher(nuevaTrim).matches() && nuevaTrim == email) {
        onResult(false, "La contraseña no debe ser igual al correo.")
        return
    }

    val cred = EmailAuthProvider.getCredential(email, actualTrim)

    // === 1) Reautenticar ===
    user.reauthenticate(cred)
        .addOnSuccessListener {
            // === 2) Actualizar contraseña ===
            user.updatePassword(nuevaTrim)
                .addOnSuccessListener {
                    // === 3) Sign out para forzar re-login seguro ===
                    onResult(true, "Contraseña actualizada correctamente.")
                }
                .addOnFailureListener { e ->
                    onResult(false, mapUpdateError(e))
                }
        }
        .addOnFailureListener { e ->
            onResult(false, mapReauthError(e))
        }
}

/* ===================== Helpers de mapeo de errores ===================== */

private fun mapReauthError(e: Exception): String {
    val fe = e as? FirebaseAuthException
    return when (fe?.errorCode) {
        "ERROR_WRONG_PASSWORD"           -> "La contraseña actual es incorrecta."
        "ERROR_USER_MISMATCH"            -> "Usuario no coincide. Inicia sesión otra vez."
        "ERROR_REQUIRES_RECENT_LOGIN"    -> "Por seguridad, inicia sesión nuevamente."
        else                             -> fe?.message ?: "No se pudo reautenticar."
    }
}

private fun mapUpdateError(e: Exception): String {
    val fe = e as? FirebaseAuthException
    return when (fe?.errorCode) {
        "ERROR_WEAK_PASSWORD"            -> "La nueva contraseña es muy débil."
        "ERROR_OPERATION_NOT_ALLOWED"    -> "Operación no permitida por seguridad."
        "ERROR_REQUIRES_RECENT_LOGIN"    -> "Por seguridad, inicia sesión nuevamente."
        else                             -> fe?.message ?: "No se pudo actualizar la contraseña."
    }
}