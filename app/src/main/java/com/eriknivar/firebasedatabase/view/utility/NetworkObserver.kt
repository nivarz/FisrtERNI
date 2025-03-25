package com.eriknivar.firebasedatabase.view.utility

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.compose.runtime.MutableState

class NetworkObserver(private val context: Context, private val isConnected: MutableState<Boolean>) {

    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback

    fun startObserving() {
        connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                isConnected.value = true
            }

            override fun onLost(network: Network) {
                isConnected.value = false
            }

            override fun onUnavailable() {
                isConnected.value = false
            }
        }

        connectivityManager.registerNetworkCallback(request, networkCallback)
    }

    fun stopObserving() {
        if (::connectivityManager.isInitialized && ::networkCallback.isInitialized) {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }
}
