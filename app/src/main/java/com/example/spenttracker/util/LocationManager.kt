package com.example.spenttracker.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*
import kotlin.coroutines.resume

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val city: String?,
    val country: String?,
    val displayLocation: String,
    // Detailed address components for precise location
    val streetAddress: String? = null,      // Street number + street name
    val subLocality: String? = null,        // Neighborhood/District/Area within city
    val locality: String? = null,           // City/Town
    val subAdminArea: String? = null,       // County/Local Government Area
    val adminArea: String? = null,          // State/Province
    val postalCode: String? = null,         // ZIP/Postal code
    val premises: String? = null,           // Building name/number
    val thoroughfare: String? = null,       // Street name
    val subThoroughfare: String? = null,    // Street number
    val featureName: String? = null,        // Point of interest name
    // Enhanced location from Google Places
    val nearbyPlaceName: String? = null,    // "Shoprite Mall"
    val nearbyPlaceType: String? = null,    // "shopping_mall"
    val nearbyPlaceAddress: String? = null  // Full address of the place
)

@Singleton
class LocationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "LocationManager"
        private const val LOCATION_TIMEOUT_MS = 5000L
        private const val LAST_LOCATION_MAX_AGE_MS = 300000L // 5 minutes
        private const val PLACES_SEARCH_RADIUS = 150.0 // meters for nearby search
    }

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    private val geocoder: Geocoder? by lazy {
        if (Geocoder.isPresent()) {
            Geocoder(context, Locale.getDefault())
        } else {
            null
        }
    }

    private val placesClient: PlacesClient? by lazy {
        try {
            if (!Places.isInitialized()) {
                Log.w(TAG, "Places API not initialized. Add Google API key to enable enhanced location names.")
                null
            } else {
                Places.createClient(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Places client: ${e.message}", e)
            null
        }
    }

    /**
     * Check if location permissions are granted
     */
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Get current location with timeout
     */
    suspend fun getCurrentLocation(): LocationData? {
        Log.d(TAG, "getCurrentLocation() called")

        if (!hasLocationPermission()) {
            Log.w(TAG, "Location permission not granted")
            return null
        }

        if (!isLocationEnabled()) {
            Log.w(TAG, "Location services not enabled")
            return null
        }

        Log.d(TAG, "Permission granted and location enabled, attempting to get location...")

        return withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
            try {
                val location = getCurrentLocationInternal()
                Log.d(TAG, "getCurrentLocationInternal() returned: $location")

                if (location != null) {
                    Log.d(TAG, "Location received: lat=${location.latitude}, lng=${location.longitude}")

                    // Get detailed address and enhanced place info
                    val locationData = getEnhancedLocationData(location.latitude, location.longitude)
                    Log.d(TAG, "Enhanced location data: $locationData")

                    locationData
                } else {
                    Log.w(TAG, "Unable to get current location - location is null")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting current location: ${e.message}", e)
                null
            }
        } ?: run {
            Log.w(TAG, "Current location request timed out, trying last known location...")
            getLastKnownLocation()
        }
    }

    /**
     * Get enhanced location data with Google Places API
     */
    private suspend fun getEnhancedLocationData(latitude: Double, longitude: Double): LocationData? {
        return withContext(Dispatchers.IO) {
            try {
                // First get basic address from Geocoder
                val basicAddress = getBasicAddressFromLocation(latitude, longitude)

                // Then enhance with Google Places API
                val nearbyPlace = getNearbyPlaceInfo(latitude, longitude)

                // Combine the information
                if (basicAddress != null) {
                    val enhancedLocation = basicAddress.copy(
                        nearbyPlaceName = nearbyPlace?.name,
                        nearbyPlaceType = nearbyPlace?.type,
                        nearbyPlaceAddress = nearbyPlace?.address
                    )

                    // Update display location with nearby place if available
                    if (nearbyPlace != null) {
                        enhancedLocation.copy(
                            displayLocation = createEnhancedDisplayLocation(enhancedLocation, nearbyPlace),
                            subLocality = nearbyPlace.name // Use place name as neighborhood
                        )
                    } else {
                        // Fallback: Try to get a better neighborhood from geocoding
                        Log.d(TAG, "No Places API data available, enhancing with geocoding...")
                        val enhancedNeighborhood = getEnhancedNeighborhoodFromGeocoding(latitude, longitude)
                        if (!enhancedNeighborhood.isNullOrBlank()) {
                            enhancedLocation.copy(
                                subLocality = enhancedNeighborhood,
                                displayLocation = enhancedLocation.displayLocation.replace(
                                    enhancedLocation.city ?: "Unknown", 
                                    "$enhancedNeighborhood, ${enhancedLocation.city ?: "Unknown"}"
                                )
                            )
                        } else {
                            enhancedLocation
                        }
                    }
                } else {
                    // Return basic location if geocoding fails
                    LocationData(
                        latitude = latitude,
                        longitude = longitude,
                        city = null,
                        country = null,
                        displayLocation = "Location (GPS)",
                        nearbyPlaceName = nearbyPlace?.name,
                        nearbyPlaceType = nearbyPlace?.type
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting enhanced location data: ${e.message}", e)
                LocationData(
                    latitude = latitude,
                    longitude = longitude,
                    city = null,
                    country = null,
                    displayLocation = "Location (GPS)"
                )
            }
        }
    }

    /**
     * Get basic address information from Geocoder
     */
    private suspend fun getBasicAddressFromLocation(latitude: Double, longitude: Double): LocationData? {
        return withContext(Dispatchers.IO) {
            try {
                if (geocoder == null) {
                    Log.w(TAG, "Geocoder not available")
                    return@withContext null
                }

                val address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    suspendCancellableCoroutine { continuation ->
                        geocoder!!.getFromLocation(latitude, longitude, 1) { addresses ->
                            continuation.resume(addresses.firstOrNull())
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder!!.getFromLocation(latitude, longitude, 1)
                    addresses?.firstOrNull()
                }

                if (address != null) {
                    Log.d(TAG, "Raw address: ${address.getAddressLine(0)}")

                    val streetNumber = address.subThoroughfare
                    val streetName = address.thoroughfare
                    val streetAddress = if (streetNumber != null && streetName != null) {
                        "$streetNumber $streetName"
                    } else {
                        streetName ?: streetNumber
                    }

                    val city = address.locality ?: address.subAdminArea
                    val state = address.adminArea
                    val country = address.countryName

                    // Don't use subLocality here as it often contains Plus codes
                    // We'll get better neighborhood info from Places API

                    LocationData(
                        latitude = latitude,
                        longitude = longitude,
                        city = city,
                        country = country,
                        displayLocation = createBasicDisplayLocation(city, state, country),
                        streetAddress = streetAddress,
                        subLocality = null, // Will be filled by Places API
                        locality = city,
                        subAdminArea = address.subAdminArea,
                        adminArea = state,
                        postalCode = address.postalCode,
                        premises = address.premises,
                        thoroughfare = streetName,
                        subThoroughfare = streetNumber,
                        featureName = address.featureName
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting address from location: ${e.message}", e)
                null
            }
        }
    }

    /**
     * Data class for nearby place information
     */
    private data class NearbyPlaceInfo(
        val name: String,
        val type: String,
        val address: String?
    )

    /**
     * Get nearby place information using Google Places API
     */
    private suspend fun getNearbyPlaceInfo(latitude: Double, longitude: Double): NearbyPlaceInfo? {
        return withContext(Dispatchers.IO) {
            try {
                if (placesClient == null) {
                    Log.w(TAG, "Places client not available")
                    return@withContext null
                }

                Log.d(TAG, "Getting current place using Places API")

                // Define fields we want to retrieve
                val placeFields = listOf(
                    Place.Field.ID,
                    Place.Field.NAME,
                    Place.Field.ADDRESS,
                    Place.Field.TYPES,
                    Place.Field.LAT_LNG
                )

                // Create current place request (this finds nearby places automatically)
                val request = FindCurrentPlaceRequest.newInstance(placeFields)

                // Execute search with timeout and error handling
                val places = withTimeoutOrNull(3000L) { // Reduced timeout to 3 seconds
                    suspendCancellableCoroutine<List<Place>> { continuation ->
                        placesClient!!.findCurrentPlace(request)
                            .addOnSuccessListener { response ->
                                val placeList = response.placeLikelihoods.map { it.place }
                                Log.d(TAG, "Found ${placeList.size} current places")
                                continuation.resume(placeList)
                            }
                            .addOnFailureListener { exception ->
                                Log.e(TAG, "Places search failed: ${exception.message}")
                                // Check for specific error codes
                                when {
                                    exception.message?.contains("9011") == true -> {
                                        Log.w(TAG, "Places API billing not enabled - continuing with geocoding only")
                                    }
                                    exception.message?.contains("billing") == true -> {
                                        Log.w(TAG, "Places API billing issue - continuing with geocoding only")
                                    }
                                    else -> {
                                        Log.e(TAG, "Places API error: ${exception.message}")
                                    }
                                }
                                continuation.resume(emptyList())
                            }
                    }
                } ?: run {
                    Log.w(TAG, "Places API search timed out after 3s - continuing with geocoding")
                    emptyList()
                }

                // Find the best place for expense tracking context
                val bestPlace = findBestPlaceForExpenseTracking(places, latitude, longitude)

                if (bestPlace != null) {
                    Log.d(TAG, "Best nearby place: ${bestPlace.name}")

                    val placeType = if (bestPlace.types?.isNotEmpty() == true) {
                        getMainPlaceType(bestPlace.types)
                    } else {
                        getPlaceTypeFromName(bestPlace.name)
                    }

                    NearbyPlaceInfo(
                        name = bestPlace.name ?: "Unknown Place",
                        type = placeType,
                        address = bestPlace.address
                    )
                } else {
                    Log.d(TAG, "No suitable nearby places found")
                    null
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error getting nearby places: ${e.message}", e)
                null
            }
        }
    }

    /**
     * Find the best place for expense tracking context
     */
    private fun findBestPlaceForExpenseTracking(places: List<Place>, userLat: Double, userLon: Double): Place? {
        if (places.isEmpty()) return null

        // Priority keywords for expense-relevant places (since we can't filter by types anymore)
        val priorityKeywords = listOf(
            "mall", "market", "supermarket", "store", "shop",
            "restaurant", "cafe", "food", "dining",
            "bank", "atm", "pharmacy", "hospital", "clinic",
            "university", "school", "college", "gas", "station",
            "hotel", "plaza", "centre", "center"
        )

        // Sort places by relevance based on name
        val sortedPlaces = places.sortedWith(compareBy(
            // First by name relevance (contains priority keywords)
            { place ->
                val name = place.name?.lowercase() ?: ""
                val hasKeyword = priorityKeywords.any { keyword -> name.contains(keyword) }
                if (hasKeyword) 0 else 1 // Priority places first
            },
            // Then by distance if location is available
            { place ->
                place.latLng?.let { placeLatLng ->
                    calculateDistance(userLat, userLon, placeLatLng.latitude, placeLatLng.longitude)
                } ?: Double.MAX_VALUE
            }
        ))

        // Return the best place (must have a name to be useful)
        return sortedPlaces.firstOrNull { place ->
            !place.name.isNullOrBlank()
        }
    }

    /**
     * Get the main place type as a string based on place name
     */
    private fun getMainPlaceType(types: List<Place.Type>?): String {
        if (types.isNullOrEmpty()) return "place"

        // Try to map known types
        return when (types.first()) {
            Place.Type.SHOPPING_MALL -> "shopping_mall"
            Place.Type.SUPERMARKET -> "supermarket"
            Place.Type.RESTAURANT -> "restaurant"
            Place.Type.BANK -> "bank"
            Place.Type.ATM -> "atm"
            Place.Type.GAS_STATION -> "gas_station"
            Place.Type.PHARMACY -> "pharmacy"
            Place.Type.HOSPITAL -> "hospital"
            Place.Type.STORE -> "store"
            Place.Type.CAFE -> "cafe"
            Place.Type.UNIVERSITY -> "university"
            Place.Type.SCHOOL -> "school"
            else -> "place"
        }
    }
    
    /**
     * Get place type from place name when types aren't available
     */
    private fun getPlaceTypeFromName(name: String?): String {
        if (name.isNullOrBlank()) return "place"
        
        val lowerName = name.lowercase()
        return when {
            lowerName.contains("mall") -> "shopping_mall"
            lowerName.contains("market") || lowerName.contains("supermarket") -> "supermarket"
            lowerName.contains("restaurant") || lowerName.contains("dining") -> "restaurant"
            lowerName.contains("bank") -> "bank"
            lowerName.contains("atm") -> "atm"
            lowerName.contains("gas") || lowerName.contains("station") -> "gas_station"
            lowerName.contains("pharmacy") -> "pharmacy"
            lowerName.contains("hospital") || lowerName.contains("clinic") -> "hospital"
            lowerName.contains("store") || lowerName.contains("shop") -> "store"
            lowerName.contains("cafe") || lowerName.contains("coffee") -> "cafe"
            lowerName.contains("university") || lowerName.contains("college") -> "university"
            lowerName.contains("school") -> "school"
            lowerName.contains("hotel") -> "hotel"
            else -> "place"
        }
    }

    /**
     * Calculate distance between two points in meters
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    /**
     * Create enhanced display location string
     */
    private fun createEnhancedDisplayLocation(locationData: LocationData, nearbyPlace: NearbyPlaceInfo): String {
        val parts = mutableListOf<String>()

        // Add "Near [Place Name]" if available
        parts.add("Near ${nearbyPlace.name}")

        // Add street if available and different from place
        locationData.streetAddress?.let {
            if (!nearbyPlace.name.contains(it)) {
                parts.add(it)
            }
        }

        // Add city
        locationData.city?.let { parts.add(it) }

        // Add state if different from city
        locationData.adminArea?.let {
            if (it != locationData.city) parts.add(it)
        }

        // Add country
        locationData.country?.let { parts.add(it) }

        return if (parts.isNotEmpty()) {
            "${parts.joinToString(", ")} (GPS)"
        } else {
            "Unknown Location (GPS)"
        }
    }

    /**
     * Create basic display location string
     */
    private fun createBasicDisplayLocation(city: String?, state: String?, country: String?): String {
        val parts = mutableListOf<String>()

        city?.let { parts.add(it) }
        state?.let { if (it != city) parts.add(it) }
        country?.let { parts.add(it) }

        return if (parts.isNotEmpty()) {
            "${parts.joinToString(", ")} (GPS)"
        } else {
            "Unknown Location (GPS)"
        }
    }

    /**
     * Internal method to get location using FusedLocationProviderClient
     */
    private suspend fun getCurrentLocationInternal(): Location? = suspendCancellableCoroutine { continuation ->
        if (!hasLocationPermission()) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        val cancellationTokenSource = CancellationTokenSource()

        continuation.invokeOnCancellation {
            cancellationTokenSource.cancel()
        }

        try {
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { location ->
                Log.d(TAG, "Location obtained: lat=${location?.latitude}, lng=${location?.longitude}")
                continuation.resume(location)
            }.addOnFailureListener { exception ->
                Log.e(TAG, "Failed to get location: ${exception.message}", exception)
                continuation.resume(null)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception getting location: ${e.message}", e)
            continuation.resume(null)
        }
    }

    /**
     * Get last known location as fallback
     */
    private suspend fun getLastKnownLocation(): LocationData? {
        return withContext(Dispatchers.IO) {
            try {
                if (!hasLocationPermission()) {
                    return@withContext null
                }

                val lastLocation = suspendCancellableCoroutine<Location?> { continuation ->
                    try {
                        fusedLocationClient.lastLocation
                            .addOnSuccessListener { location ->
                                continuation.resume(location)
                            }
                            .addOnFailureListener {
                                continuation.resume(null)
                            }
                    } catch (e: SecurityException) {
                        continuation.resume(null)
                    }
                }

                if (lastLocation != null) {
                    val locationAge = System.currentTimeMillis() - lastLocation.time
                    if (locationAge <= LAST_LOCATION_MAX_AGE_MS) {
                        val locationData = getEnhancedLocationData(lastLocation.latitude, lastLocation.longitude)
                        locationData?.copy(
                            displayLocation = locationData.displayLocation.replace("(GPS)", "(cached)")
                        )
                    } else {
                        null
                    }
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting last known location: ${e.message}", e)
                null
            }
        }
    }

    /**
     * Enhanced neighborhood detection using only geocoding as fallback
     */
    private suspend fun getEnhancedNeighborhoodFromGeocoding(latitude: Double, longitude: Double): String? {
        return withContext(Dispatchers.IO) {
            try {
                if (geocoder == null) return@withContext null
                
                Log.d(TAG, "Trying enhanced geocoding for better area names...")
                
                // Try getting multiple addresses with different approach
                val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    suspendCancellableCoroutine { continuation ->
                        geocoder!!.getFromLocation(latitude, longitude, 3) { addresses ->
                            continuation.resume(addresses)
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    geocoder!!.getFromLocation(latitude, longitude, 3)
                }
                
                // Look through results for meaningful neighborhood names
                addresses?.forEach { address ->
                    Log.d(TAG, "Checking enhanced address: ${address.getAddressLine(0)}")
                    
                    // Try different address components in order
                    val candidates = listOf(
                        address.subLocality,
                        address.premises,
                        address.thoroughfare,
                        extractMeaningfulAreaFromAddress(address.getAddressLine(0))
                    )
                    
                    for (candidate in candidates) {
                        if (isValidNeighborhoodName(candidate)) {
                            Log.d(TAG, "Found enhanced neighborhood: $candidate")
                            return@withContext candidate
                        }
                    }
                }
                
                Log.d(TAG, "No enhanced neighborhood found from geocoding")
                return@withContext null
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in enhanced geocoding: ${e.message}", e)
                return@withContext null
            }
        }
    }
    
    /**
     * Extract meaningful area name from address line
     */
    private fun extractMeaningfulAreaFromAddress(addressLine: String?): String? {
        if (addressLine.isNullOrBlank()) return null
        
        val parts = addressLine.split(",").map { it.trim() }
        
        for (part in parts) {
            // Skip Plus codes, postal codes, state/country names
            if (isPlusCode(part) || 
                part.matches(Regex("^\\d+$")) ||
                part.matches(Regex(".*\\s+\\d{5,6}$")) ||
                part.equals("Nigeria", ignoreCase = true) ||
                part.equals("Borno", ignoreCase = true) ||
                part.equals("Maiduguri", ignoreCase = true)) {
                continue
            }
            
            // Look for meaningful location indicators
            val locationKeywords = listOf("area", "district", "ward", "quarter", "estate", "gardens", "market", "mall", "plaza")
            if (locationKeywords.any { part.contains(it, ignoreCase = true) } && part.length > 3) {
                return part
            }
        }
        return null
    }
    
    /**
     * Check if a string is a Plus Code (Google's location code)
     */
    private fun isPlusCode(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        
        // Plus codes typically have format like "R59G+42R" or similar
        val plusCodePattern = Regex("^[A-Z0-9]{4}\\+[A-Z0-9]{2,3}$")
        return plusCodePattern.matches(text.trim())
    }

    /**
     * Validate if a string is a good neighborhood name
     */
    private fun isValidNeighborhoodName(candidate: String?): Boolean {
        if (candidate.isNullOrBlank() || candidate.length < 3) return false
        if (isPlusCode(candidate)) return false
        if (candidate.matches(Regex("^\\d+$"))) return false
        if (candidate.matches(Regex(".*\\s+\\d{5,6}$"))) return false
        
        // Reject common non-neighborhood terms
        val rejectTerms = listOf("unnamed", "road", "street", "nigeria", "borno")
        if (rejectTerms.any { candidate.contains(it, ignoreCase = true) }) return false
        
        return true
    }

    /**
     * Check if GPS is enabled on the device
     */
    fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
    }
}