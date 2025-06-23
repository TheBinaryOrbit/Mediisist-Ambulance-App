package com.example.call_ambulance

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
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

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d("MainActivity", "Notification permission granted: $isGranted")
    }

    private val callPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        Log.d("MainActivity", "Call permissions all granted: $allGranted")
    }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(this)) {
            Log.d("MainActivity", "Overlay permission granted")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestAllPermissions()

        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, true)

        // Get FCM token
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d("FCM_TOKEN", "Token: $token")
            } else {
                Log.e("FCM_TOKEN", "Token fetch failed: ${task.exception?.message}")
            }
        }

        setContent {
            CallSupportTheme {
                val context = LocalContext.current
                val navController = rememberNavController()

                // ✅ Accepted calls list (from accepted ride API or mock for now)
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

                // Check for deep-link navigation
                val navigateTo = intent.getStringExtra("navigateTo")

                NavHost(navController = navController, startDestination = "splash") {
                    composable("splash") { SplashScreen(navController) }
                    composable("login") { LoginScreen(navController) }
                    composable("acceptedCalls") {
                        AcceptedRideCallsScreen(
                            navController = navController,
                        )
                    }
                    composable("myAccount") { MyAccountScreen(navController = navController) }
                    composable("privacy") { PrivacyPolicyScreen(navController = navController) }
                    composable("terms") { TermsAndConditionsScreen(navController = navController) }

                    mainScreenNavGraph(
                        navController = navController,
                        acceptedCalls = acceptedCallsState
                    )
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

    private fun checkAndRequestAllPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val callPermissions = arrayOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE
        )

        val missingCallPermissions = callPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingCallPermissions.isNotEmpty()) {
            callPermissionsLauncher.launch(missingCallPermissions.toTypedArray())
        }

        if (!Settings.canDrawOverlays(this)) {
            overlayPermissionLauncher.launch(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:$packageName".toUri())
            )
        }

        if (hasMinimumPermissions()) {
            CallSupportForegroundService.start(this)
        }
    }

    private fun hasMinimumPermissions(): Boolean {
        val hasCallPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        val hasOverlayPermission = Settings.canDrawOverlays(this)

        return hasCallPermission && hasOverlayPermission
    }

    override fun onResume() {
        super.onResume()
        AppPreferences.isAppInForeground = true
        if (!hasMinimumPermissions()) {
            checkAndRequestAllPermissions()
        }
    }

    override fun onPause() {
        super.onPause()
        AppPreferences.isAppInForeground = false
    }
}
