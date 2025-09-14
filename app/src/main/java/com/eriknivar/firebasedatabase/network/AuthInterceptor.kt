package com.eriknivar.firebasedatabase.network

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        // 1ra pasada: usa token en caché (o TTL)
        val req1 = original.newBuilder().apply {
            try {
                val token = TokenProvider.getIdTokenOrThrow(false)
                header("Authorization", "Bearer $token")
            } catch (_: Exception) {
                // sin token → dejamos pasar; el backend devolverá 401 y reintentamos abajo
            }
        }.build()

        var res = chain.proceed(req1)

        // Si vence/expira → forza refresh y reintenta 1 vez
        if (res.code == 401) {
            res.close()
            val req2 = original.newBuilder().apply {
                try {
                    val fresh = TokenProvider.getIdTokenOrThrow(true)
                    header("Authorization", "Bearer $fresh")
                } catch (_: Exception) { /* seguimos sin header */ }
            }.build()
            res = chain.proceed(req2)
        }
        return res
    }
}
