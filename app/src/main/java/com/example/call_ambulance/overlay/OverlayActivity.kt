package com.example.call_ambulance.overlay

import android.app.KeyguardManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.call_ambulance.ui.theme.CallSupportTheme

class OverlayActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Turn on screen and show on lock screen
        turnOnScreenAndShowOnLock()
        val notificationId = 1001

        val name = intent.getStringExtra("name") ?: "Unknown"
        val phone = intent.getStringExtra("phoneNumber") ?: "N/A"

        setContent {
            CallSupportTheme {
                Dialog(onDismissRequest = { finish() }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        EmergencyOverlayUI(
                            name = name,
                            phone = phone,
                            notificationId = notificationId,
                            onIgnore = { finish() },
                            onAccept = { finish() }
                        )

                    }
                }
            }
        }
    }

    private fun turnOnScreenAndShowOnLock() {
        // Turn on screen
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )

        // For Android 8.0+ (API 26+), use the newer method
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        // For devices with keyguard
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        // Prevent closing the dialog with back button
        // You can add your own logic here if needed
    }
}