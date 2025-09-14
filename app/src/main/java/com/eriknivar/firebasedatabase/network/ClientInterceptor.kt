package com.eriknivar.firebasedatabase.network

import okhttp3.Interceptor
import okhttp3.Response

class ClientInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        val reqBuilder = original.newBuilder()

        // Solo superuser + cliente elegido
        if (SelectedClientStore.isSuperuser) {
            val cid = SelectedClientStore.selectedClienteId
            if (!cid.isNullOrBlank()) {
                reqBuilder.header("X-Cliente-Id", cid)
            }
        }

        return chain.proceed(reqBuilder.build())
    }
}
