package com.eriknivar.firebasedatabase.network

object SelectedClientStore {
    @Volatile var isSuperuser: Boolean = false
    @Volatile var selectedClienteId: String? = null

    fun setCliente(id: String?) {
        selectedClienteId = id?.trim()?.uppercase()?.ifEmpty { null }
    }

    fun setRolSuperuser(isSuper: Boolean) {
        isSuperuser = isSuper
    }

    fun clear() {
        isSuperuser = false
        selectedClienteId = null
    }
}
