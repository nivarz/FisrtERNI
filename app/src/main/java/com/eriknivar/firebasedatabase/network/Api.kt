package com.eriknivar.firebasedatabase.network

object Api {
    val service: ApiService by lazy {
        Network.retrofit.create(ApiService::class.java)
    }
}
