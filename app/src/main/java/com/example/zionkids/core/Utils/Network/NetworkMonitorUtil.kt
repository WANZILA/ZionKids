package com.example.zionkids.core.Utils.Network

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

// …other imports…

@Singleton
class NetworkMonitorUtil @Inject constructor(
    @ApplicationContext context: Context
) {
    private val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isOnline = MutableStateFlow(checkNow(cm))
    val isOnline: StateFlow<Boolean> = _isOnline

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { _isOnline.value = checkNow(cm) }
        override fun onLost(network: Network) { _isOnline.value = checkNow(cm) }
        override fun onCapabilitiesChanged(network: Network, nc: NetworkCapabilities) { _isOnline.value = checkNow(cm) }
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            cm.registerDefaultNetworkCallback(callback)
        } else {
            cm.registerNetworkCallback(android.net.NetworkRequest.Builder().build(), callback)
        }
        _isOnline.value = checkNow(cm)
    }

    @SuppressLint("MissingPermission")
    private fun checkNow(cm: ConnectivityManager): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val nw = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(nw) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                            || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                            || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
        } else {
            @Suppress("DEPRECATION")
            val info = cm.activeNetworkInfo
            @Suppress("DEPRECATION")
            info != null && info.isConnected
        }
    }
}
