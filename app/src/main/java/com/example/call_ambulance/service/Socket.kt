package com.example.call_ambulance.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.call_ambulance.service.api.ApiClient
import com.google.android.gms.location.*
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import java.net.URISyntaxException

object SocketHandler {
//    https://2q766kvz-9000.inc1.devtunnels.ms/
    private var mSocket: Socket? = null
    private const val SERVER_URL = ApiClient.BASE_URL

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null

    fun initSocket() {
        try {
            mSocket = IO.socket(SERVER_URL)
        } catch (e: URISyntaxException) {
            e.printStackTrace()
        }
    }

    fun connectSocket() {
        if (mSocket == null) {
            initSocket()
        }
        mSocket?.connect()
    }

    fun emitLiveLocation(userId: String, latitude: Double, longitude: Double, sessionKey: String?) {
        val data = JSONObject().apply {
            put("userId", userId)
            put("latitude", latitude)
            put("longitude", longitude)
            sessionKey?.let {
                put("sessionKey", it)
            }
        }

        Log.d("LiveLocation", "UserId: $userId, Lat: $latitude, Lng: $longitude, SessionKey: $sessionKey")

        if (mSocket?.connected() == true) {
            mSocket?.emit("shareLocation", data)
        }
    }

    fun startLocationUpdates(context: Context, userId: String, sessionKey: String?) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(context, "Location permission not granted", Toast.LENGTH_SHORT).show()
            return
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        val locationRequest = LocationRequest.create().apply {
            interval = 2000
            fastestInterval = 2000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)
                val location = result.lastLocation
                location?.let {
                    emitLiveLocation(userId, it.latitude, it.longitude, sessionKey)
                }
            }
        }

        fusedLocationClient?.requestLocationUpdates(
            locationRequest,
            locationCallback!!,
            Looper.getMainLooper()
        )
    }

    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient?.removeLocationUpdates(it)
        }
        locationCallback = null
    }

    fun closeSocket() {
        mSocket?.disconnect()
        mSocket?.off() // Removes all listeners
        mSocket = null
    }
}
