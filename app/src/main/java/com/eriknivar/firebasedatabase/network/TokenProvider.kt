package com.eriknivar.firebasedatabase.network

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth

object TokenProvider {
    private val auth get() = FirebaseAuth.getInstance()

    @Volatile private var cachedToken: String? = null
    @Volatile private var lastRefreshMs: Long = 0
    private const val TTL_MS = 55 * 60 * 1000L // 55 min

    @Synchronized
    fun getIdTokenOrThrow(forceRefresh: Boolean = false): String {
        val user = auth.currentUser ?: throw IllegalStateException("No hay usuario autenticado")
        val now = System.currentTimeMillis()
        val shouldRefresh = forceRefresh || cachedToken == null || (now - lastRefreshMs) > TTL_MS
        return try {
            val result = Tasks.await(user.getIdToken(shouldRefresh))
            val token = result?.token ?: throw IllegalStateException("ID Token nulo")
            cachedToken = token
            lastRefreshMs = now
            token
        } catch (e: Exception) {
            cachedToken = null
            lastRefreshMs = 0
            throw e
        }
    }

    fun clear() {
        cachedToken = null
        lastRefreshMs = 0
    }
}
