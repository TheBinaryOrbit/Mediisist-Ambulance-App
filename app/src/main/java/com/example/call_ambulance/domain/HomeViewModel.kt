package com.example.call_ambulance.domain

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.call_ambulance.foregroundservice.ForegroundLocationService
import com.example.call_ambulance.service.SocketHandler
import com.example.call_ambulance.service.SocketHandler.emitLiveLocation
import com.example.call_ambulance.service.api.ApiClient
import com.example.call_ambulance.service.api.AmbulancePartnerResponse
import com.example.call_ambulance.service.preference.AppPreferences
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.Boolean


class HomeViewModel : ViewModel() {

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name

    private val _isOnline = MutableStateFlow<Boolean?>(null)
    val isOnline: StateFlow<Boolean?> = _isOnline

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _pendingCalls = MutableStateFlow<List<EmergencyCall>>(emptyList())
    val pendingCalls: StateFlow<List<EmergencyCall>> = _pendingCalls

    private val _acceptedCalls = MutableStateFlow<List<EmergencyCall>>(emptyList())
    val acceptedCalls: StateFlow<List<EmergencyCall>> = _acceptedCalls

    private var hasFetched = false

    var isSwipeRefreshing by mutableStateOf(false)
        private set



    fun refreshAllCalls(userId: String, context: Context) {
        viewModelScope.launch {
            isSwipeRefreshing = true
            fetchPendingRides(context)
            fetchActiveRides(userId, context)
            delay(1500L)
            isSwipeRefreshing = false
        }
    }


    fun fetchUserData(userId: String, context: Context) {
        if (hasFetched) return
        hasFetched = true
        val sharedPref = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val userIdFromPref = sharedPref.getString("user_id", null) ?: return

        ApiClient.apiService.getAmbulancePartnerById(userIdFromPref)
            .enqueue(object : Callback<AmbulancePartnerResponse> {
                override fun onResponse(
                    call: Call<AmbulancePartnerResponse>,
                    response: Response<AmbulancePartnerResponse>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.partner?.let { cs ->
                            _name.value = cs.name
                            _isOnline.value = cs.isOnline

                            sharedPref.edit()
                                .putString("name", cs.name)
                                .putString("userId", userIdFromPref)
                                .apply()

                            fetchActiveRides(userIdFromPref, context) // 🔄 now this will decide to call pending
                        }
                    } else {
                        Toast.makeText(context, "Failed to load profile", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<AmbulancePartnerResponse>, t: Throwable) {
                    Toast.makeText(context, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }




    fun fetchPendingRides(context: Context) {
        viewModelScope.launch {
            try {
                val response = ApiClient.apiService.getPendingRideList()
                if (response.isSuccessful) {
                    val body = response.body()
                    val rideList = body?.ride ?: emptyList()
                    Log.d("Active", response.toString())

                    val list = rideList.map { ride ->
                        EmergencyCall(
                            id = ride.id,
                            patientName = ride.name,
                            phoneNumber = ride.phoneNumber,
                            address = ride.address,
                            lat = "",
                            lng = "",
                            status = "pending",
                            createdAt = ride.createdAt
                        )
                    }

                    _pendingCalls.value = list
                } else {
                    Toast.makeText(context, "Failed to fetch pending calls", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error: ${e.message}", e)
            }
        }
    }

    fun fetchActiveRides(userId: String, context: Context) {
        viewModelScope.launch {
            try {
                Log.d("useractive", "Fetching active calls for userId: $userId")
                val response = ApiClient.apiService.getRidesByAmbulancePartner(userId, status = "active")
                if (response.isSuccessful) {
                    val rideList = response.body()?.ride ?: emptyList()
                 //   Log.d("Active", response.toString())
                    val acceptedCallList = rideList.map { ride ->
                        EmergencyCall(
                            id = ride.id,
                            patientName = ride.name,
                            phoneNumber = ride.phoneNumber,
                            address = ride.address,
                            lat = ride.lat,
                            lng = ride.lng,
                            status = "accepted",
                            createdAt = ride.createdAt,
                            isLocationAvail = ride.isLocationAvail
                        )
                    }

                    _acceptedCalls.value = acceptedCallList

                    // 🧠 Only fetch pending calls if no active call exists
                    if (acceptedCallList.isEmpty()) {
                        fetchPendingRides(context)
                    } else {
                        _pendingCalls.value = emptyList() // Clear pending if active exists
                    }

                } else {
                    Log.e("HomeVM", "Failed to fetch accepted ride: ${response.body()}")
                }
            } catch (e: Exception) {
                Log.e("HomeVM", "Error: ${e.message}", e)
            }
        }
    }






    fun updateStatus(
        newStatus: Boolean,
        lat: Double,
        lng: Double,
        userId: String,
        context: Context
    ) {
        _isLoading.value = true

        val updateRequest = StatusUpdateRequest(
            isOnline = newStatus,
            lat = lat,
            lng = lng
        )

        ApiClient.apiService.updateOnlineStatus(userId, updateRequest)
            .enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    _isLoading.value = false
                    if (response.isSuccessful) {
                        _isOnline.value = newStatus
                        Toast.makeText(context, "Status and location updated", Toast.LENGTH_SHORT).show()
                    } else {
                        val error = response.errorBody()?.string()
                        Log.e("API_ERROR", "Response failed: $error")
                        Toast.makeText(context, "Failed to update status", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    _isLoading.value = false
                    Toast.makeText(context, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }




    @SuppressLint("MissingPermission")
    fun fetchLocationAndUpdateStatus(
        context: Context,
        newStatus: Boolean,
        userId: String
    ) {
        _isLoading.value = true
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    val lat = location.latitude
                    val lng = location.longitude
                    updateStatus(newStatus, lat, lng, userId, context)
                } else {
                    _isLoading.value = false
                    Toast.makeText(context, "Unable to fetch location", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                _isLoading.value = false
                Toast.makeText(context, "Failed to get location: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }


    fun declineCall(callId: String?, context: Context) {
        if (callId.isNullOrEmpty()) return

        val sharedPref = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val userId = sharedPref.getString("user_id", null) ?: return

        viewModelScope.launch {
            try {
                val response = ApiClient.apiService.declineRide(callId)
                if (response.isSuccessful) {
                    Toast.makeText(context, "Call Declined", Toast.LENGTH_SHORT).show()
                    // Refresh both lists
                    fetchPendingRides(context)

                    fetchActiveRides(userId, context)
                } else {
                    Toast.makeText(context, "Decline failed: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    fun acceptCall(callId: String, context: Context) {
        _isLoading.value = true
        val userId = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)
            .getString("userId", null) ?: return

        viewModelScope.launch {
            try {
                val requestBody = mapOf("ambulancePartnerId" to userId)
                val response = ApiClient.apiService.AcceptRidebyAmbulnacePartner(callId, requestBody)

                if (response.isSuccessful) {
                    val responseBody = response.body()
                    var sessionKeyFromResponse: String? = null

                    if (responseBody is Map<*, *>) {
                        sessionKeyFromResponse = responseBody["sessionKey"] as? String
                    }

                    if (!sessionKeyFromResponse.isNullOrEmpty()) {
                        // ✅ Save only response-wala session key
                        AppPreferences.saveSessionKey(context, sessionKeyFromResponse)

                        // ✅ Connect socket AFTER saving session
                        SocketHandler.connectSocket()
                        SocketHandler.startLocationUpdates(context, userId, sessionKeyFromResponse)


                        // ✅ Fetch updated lists
                        fetchPendingRides(context)
                        fetchActiveRides(userId, context)

                        Toast.makeText(context, "Call accepted", Toast.LENGTH_SHORT).show()

                        // ✅ Start foreground service
                        val intent = Intent(context, ForegroundLocationService::class.java)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(intent)
                        } else {
                            context.startService(intent)
                        }

                    } else {
                        Toast.makeText(context, "Session key missing in response", Toast.LENGTH_SHORT).show()
                    }

                } else {
                    Toast.makeText(context, "Failed to accept call", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                _isLoading.value = false
            }
        }
    }






    fun completeCall(callId: String, context: Context) {
        _isLoading.value = true

        val sharedPrefs = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val userId = sharedPrefs.getString("userId", null) ?: return

        viewModelScope.launch {
            try {
                val requestBody = mapOf("ambulancePartnerId" to userId)
                Log.d("Complete Call", "rideId: $callId")
                Log.d("Complete Call", "requestBody: $requestBody")
                val response = ApiClient.apiService.CompletedByAmbulancePartner(callId, requestBody)

                if (response.isSuccessful) {
                    // ✅ Clear session key
                    sharedPrefs.edit().remove("sessionKey").apply()

                    // ✅ Disconnect socket
                    SocketHandler.closeSocket()

                    // ✅ Refresh ride data
                    fetchPendingRides(context)
                    fetchActiveRides(userId, context)

                    Toast.makeText(context, "Ride Completed", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to Completed Ride Call", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                _isLoading.value = false
            }
        }
    }

}


data class EmergencyCall(
    val id: String,
    val patientName: String,
    val phoneNumber: String,
    val address: String,
    val lat: String? = "0.0",
    val lng: String? = "0.0",
    val status: String,
    val createdAt: String,
    val isLocationAvail: Boolean? = null
)




data class StatusUpdateRequest(
    val isOnline: Boolean,
    val lat: Double,
    val lng: Double
)




