package com.example.spenttracker.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Network connectivity observer - Like Laravel's connection health monitoring
 * Observes network state changes and provides real-time connectivity status
 * 
 * Kotlin Syntax Explanations:
 * - sealed interface: Like Laravel's enum but more powerful (type-safe states)
 * - callbackFlow: Creates Flow from callback-based APIs (like Laravel's events)
 * - distinctUntilChanged(): Only emit when value actually changes (like Laravel's remember)
 */
sealed interface ConnectivityStatus {
    object Available : ConnectivityStatus      // Like Laravel's 'connected' status
    object Unavailable : ConnectivityStatus    // Like Laravel's 'disconnected' status  
    object Losing : ConnectivityStatus         // Like Laravel's 'degraded' status
    object Lost : ConnectivityStatus           // Like Laravel's 'lost' status
}

@Singleton
class NetworkConnectivityObserver @Inject constructor(
    private val context: Context
) {
    
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    /**
     * Observe network connectivity changes
     * Like Laravel's Event::listen('connection.changed', $callback)
     */
    fun observe(): Flow<ConnectivityStatus> {
        return callbackFlow {
            val callback = object : ConnectivityManager.NetworkCallback() {
                
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    // Like Laravel's cache('connection_status', 'available')
                    trySend(ConnectivityStatus.Available)
                }
                
                override fun onLosing(network: Network, maxMsToLive: Int) {
                    super.onLosing(network, maxMsToLive)
                    // Like Laravel's Log::warning('Connection degrading')
                    trySend(ConnectivityStatus.Losing)
                }
                
                override fun onLost(network: Network) {
                    super.onLost(network)
                    // Like Laravel's cache('connection_status', 'lost')
                    trySend(ConnectivityStatus.Lost)
                }
                
                override fun onUnavailable() {
                    super.onUnavailable()
                    // Like Laravel's cache('connection_status', 'unavailable')
                    trySend(ConnectivityStatus.Unavailable)
                }
            }
            
            // Register network callback - like Laravel's event listener registration
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build()
            
            connectivityManager.registerNetworkCallback(request, callback)
            
            // Clean up when Flow is cancelled - like Laravel's event cleanup
            awaitClose {
                connectivityManager.unregisterNetworkCallback(callback)
            }
        }.distinctUntilChanged() // Only emit when status actually changes
    }
    
    /**
     * Check current connectivity status synchronously
     * Like Laravel's Cache::get('connection_status')
     */
    fun getCurrentConnectivityStatus(): ConnectivityStatus {
        return if (isNetworkAvailable()) {
            ConnectivityStatus.Available
        } else {
            ConnectivityStatus.Unavailable
        }
    }
    
    /**
     * Simple boolean check for network availability
     * Like Laravel's helper function is_online()
     */
    private fun isNetworkAvailable(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }
}