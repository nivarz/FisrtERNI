package com.eriknivar.firebasedatabase.navigation


object Rutas {
    const val CLIENTES = "clientes"
    const val CLIENTE_FORM = "clienteForm"
    const val ARG_CLIENTE_ID = "clienteId"
    const val CLIENTE_FORM_ROUTE = "$CLIENTE_FORM?$ARG_CLIENTE_ID={$ARG_CLIENTE_ID}"

    const val USUARIO_FORM = "usuarioForm"
}
