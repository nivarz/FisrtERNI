package com.eriknivar.firebasedatabase.security

object RoleRules {

    enum class Rol { SUPERUSER, ADMIN, INVITADO, OTRO }

    fun parse(tipoRaw: String?): Rol = when (tipoRaw?.trim()?.lowercase()) {
        "superuser", "super", "root" -> Rol.SUPERUSER
        "admin", "administrator"     -> Rol.ADMIN
        "invitado", "guest"          -> Rol.INVITADO
        else                         -> Rol.OTRO
    }

    /** ¿Puede ver/entrar al módulo Maestro? */
    fun canAccessMasterData(tipoRaw: String?): Boolean =
        when (parse(tipoRaw)) {
            Rol.SUPERUSER, Rol.ADMIN -> true
            else -> false
        }

    /** ¿Puede crear/editar/borrar en Maestro? (invitado no puede) */
    fun canMutateMasterData(tipoRaw: String?): Boolean =
        when (parse(tipoRaw)) {
            Rol.SUPERUSER, Rol.ADMIN -> true
            else -> false
        }

    /**
     * ¿Puede un usuario actuar sobre recursos de un cliente?
     * - SUPERUSER: siempre true
     * - ADMIN: solo si es el mismo cliente
     * - INVITADO/OTRO: false (solo lectura o bloqueo total)
     */
    fun canActOnCliente(tipoRaw: String?, userClienteId: String?, targetClienteId: String?): Boolean =
        when (parse(tipoRaw)) {
            Rol.SUPERUSER -> true
            Rol.ADMIN     -> userClienteId?.trim()?.uppercase() == targetClienteId?.trim()?.uppercase()
            else          -> false
        }
}
