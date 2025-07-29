package com.example.call_ambulance

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.call_ambulance.domain.EmergencyCall
import com.example.call_ambulance.foregroundservice.CallSupportForegroundService
import com.example.call_ambulance.myui.acceptedcall.AcceptedRideCallsScreen
import com.example.call_ambulance.myui.login.LoginScreen
import com.example.call_ambulance.myui.policy.PrivacyPolicyScreen
import com.example.call_ambulance.myui.policy.TermsAndConditionsScreen
import com.example.call_ambulance.myui.profile.MyAccountScreen
import com.example.call_ambulance.myui.splashscreen.SplashScreen
import com.example.call_ambulance.service.preference.AppPreferences
import com.example.call_ambulance.ui.theme.CallSupportTheme
import com.google.firebase.messaging.FirebaseMessaging
import mainScreenNavGraph

class MainActivity : ComponentActivity() {

    private lateinit var nextPermissionStep: () -> Unit

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d("PERMISSION", "Notification: $isGranted")
        nextPermissionStep()
    }

    private val callPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        Log.d("PERMISSION", "Call: $allGranted")
        nextPermissionStep()
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d("PERMISSION", "Location: $isGranted")
        nextPermissionStep()
    }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        Log.d("PERMISSION", "Overlay: ${Settings.canDrawOverlays(this)}")
        startForegroundServiceIfAllowed()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, true)

        startPermissionFlow()

        // FCM
        FirebaseMessaging.getInstance().token.addOnCompleteListener {
            if (it.isSuccessful) Log.d("FCM_TOKEN", it.result ?: "")
            else Log.e("FCM_TOKEN", it.exception?.message ?: "Unknown error")
        }

        setContent {
            CallSupportTheme {
                val navController = rememberNavController()
                val navigateTo = intent.getStringExtra("navigateTo")

                val acceptedCallsState = remember {
                    mutableStateListOf(
                        EmergencyCall(
                            id = "0",
                            patientName = "Accepted Caller",
                            phoneNumber = "0000000000",
                            address = "Unknown",
                            lat = "0.0",
                            lng = "0.0",
                            status = "accepted",
                            createdAt = "2024-01-01T10:00:00"
                        )
                    )
                }

                NavHost(navController = navController, startDestination = "splash") {
                    composable("splash") { SplashScreen(navController) }
                    composable("login") { LoginScreen(navController) }
                    composable("acceptedCalls") { AcceptedRideCallsScreen(navController) }
                    composable("myAccount") { MyAccountScreen(navController) }
                    composable("privacy") { PrivacyPolicyScreen(navController) }
                    composable("terms") { TermsAndConditionsScreen(navController) }

                    mainScreenNavGraph(navController, acceptedCallsState)
                }

                LaunchedEffect(navigateTo) {
                    if (navigateTo == "home") {
                        navController.navigate("home") {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                }
            }
        }
    }

    private fun startPermissionFlow() {
        requestNotificationPermission()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            nextPermissionStep = { requestCallPermissions() }
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            requestCallPermissions()
        }
    }

    private fun requestCallPermissions() {
        val callPermissions = arrayOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE
        )
        val missing = callPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            nextPermissionStep = { requestLocationPermission() }
            callPermissionsLauncher.launch(missing.toTypedArray())
        } else {
            requestLocationPermission()
        }
    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            nextPermissionStep = { requestOverlayPermission() }
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            requestOverlayPermission()
        }
    }

    private fun requestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            overlayPermissionLauncher.launch(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        } else {
            startForegroundServiceIfAllowed()
        }
    }

    private fun startForegroundServiceIfAllowed() {
        val hasCall = ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
        val hasOverlay = Settings.canDrawOverlays(this)
        if (hasCall && hasOverlay) {
            CallSupportForegroundService.start(this)
        }
    }

    override fun onResume() {
        super.onResume()
        AppPreferences.isAppInForeground = true
    }

    override fun onPause() {
        super.onPause()
        AppPreferences.isAppInForeground = false
    }
}

