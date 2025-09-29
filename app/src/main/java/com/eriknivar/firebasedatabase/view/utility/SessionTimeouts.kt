package com.eriknivar.firebasedatabase.view.utility

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Configuración de timeouts por rol.
 * - idle: tiempo de inactividad permitido antes de cerrar sesión.
 * - absolute: tiempo máximo absoluto de sesión, aun con actividad.
 *
 * @Idle timeout (inactividad):
 *
 * * Superuser: 90 min
 * * Admin: 60 min
 * * Invitado/otros: 30 min
 *
 * @Absolute timeout (tiempo máximo absoluto):
 * * Superuser: 24 h
 * * Admin: 12 h
 * * Invitado/otros: 8 h
 *
 * Diferencia rápida
 *
 * Idle = “sin usar la app”.
 * Se reinicia cada vez que hay interacción (tap, scroll, etc.). Si pasa más del límite sin actividad, cierra sesión.
 *
 * Absolute = “vida máxima de la sesión”.
 * No se reinicia con la actividad. Cuenta desde que comenzamos la sesión (o desde resetSessionStart()); al llegar al tope, cierra sesión aunque el usuario esté activo.
 *
 */

data class Timeouts(
    val idle: Duration,
    val absolute: Duration
)

object SessionTimeouts {
    fun forRole(role: String): Timeouts = when (role.lowercase()) {
        "superuser" -> Timeouts(
            idle = 90.minutes,    // recomendado
            absolute = 24.hours
        )
        "admin" -> Timeouts(
            idle = 60.minutes,    // recomendado (45–60)
            absolute = 12.hours
        )
        else -> Timeouts(         // invitado / otros
            idle = 30.minutes,
            absolute = 8.hours
        )
    }
}
