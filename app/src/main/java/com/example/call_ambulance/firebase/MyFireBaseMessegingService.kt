package com.example.call_ambulance.firebase

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.call_ambulance.overlay.OverlayActivity
import com.example.call_ambulance.R
import com.example.call_ambulance.foregroundservice.CallReceiver
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.json.JSONException
import org.json.JSONObject

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCM_Service"
        const val CALL_CHANNEL_ID = "incoming_calls_channel_v2"
        const val PERSISTENT_CHANNEL_ID = "persistent_service_channel"
        private var isChannelCreated = false
        private var wakeLock: PowerManager.WakeLock? = null
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed FCM token: $token")
        // Send to your server here
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "Message received from: ${remoteMessage.from}")

        remoteMessage.notification?.let {
            Log.d(TAG, "Notification Title: ${it.title}, Body: ${it.body}")
        }

        try {
            remoteMessage.data.let { data ->
                if (data.isNotEmpty()) {
                    Log.d(TAG, "Data payload: $data")

                    val messageJson = data["message"] ?: run {
                        Log.e(TAG, "Empty message field")
                        return
                    }

                    val json = JSONObject(messageJson)
                    val name = json.getString("name")
                    val phone = json.getString("phoneNumber")
                    val address = json.getString("address")

                    Log.d(TAG, "Processing call from $name ($phone)")

                    // 1. Turn on screen and keep it on
                    acquireWakeLock()

                    // 2. Show notification (optional)
//                    showCallNotification(name, phone)

                    // 3. Launch overlay activity
                    launchOverlayActivity(name, phone, address)
                   // launchOverlayActivity(name, phone, address)
                }
            }
        } catch (e: JSONException) {
            Log.e(TAG, "JSON parsing error", e)
        } catch (e: Exception) {
            Log.e(TAG, "Message processing failed", e)
        }
    }

    private fun showCallNotification(name: String, phone: String) {
        ensureNotificationChannelExists()

        val fullScreenIntent = Intent(this, OverlayActivity::class.java).apply {
            putExtra("name", name)
            putExtra("phoneNumber", phone)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CALL_CHANNEL_ID)
            .setContentTitle("Incoming Call: $name")
            .setContentText(phone)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE))
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setAutoCancel(true)
            .setOngoing(true)
            .setTimeoutAfter(30000)
            .addAction(
                NotificationCompat.Action.Builder(
                    R.drawable.ic_launcher_foreground,
                    "Answer",
                    fullScreenPendingIntent
                ).build()
            )
            .addAction(
                NotificationCompat.Action.Builder(
                    R.drawable.ic_launcher_foreground,
                    "Decline",
                    createDeclinePendingIntent()
                ).build()
            )
            .build()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        try {
            notificationManager.notify(
                System.currentTimeMillis().toInt(),
                notification
            )
            Log.d(TAG, "Call notification shown")
        } catch (e: Exception) {
            Log.e(TAG, "Notification failed", e)
        }
    }

    private fun createDeclinePendingIntent(): PendingIntent {
        val intent = Intent(this, CallReceiver::class.java).apply {
            action = "DECLINE_CALL"
        }
        return PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun launchOverlayActivity(name: String, phone: String, address: String) {
        try {
            // Dismiss keyguard if needed
            dismissKeyguard()


            val intent = Intent(this, OverlayActivity::class.java).apply {
                putExtra("name", name)
                putExtra("phoneNumber", phone)
                putExtra("Address", address)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP

                // Add flags to show on lock screen
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch overlay", e)
        }
    }

    private fun dismissKeyguard() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        }
    }

    private fun acquireWakeLock() {
        try {
            releaseWakeLock() // Release any existing wakelock

            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.FULL_WAKE_LOCK or
                        PowerManager.ACQUIRE_CAUSES_WAKEUP or
                        PowerManager.ON_AFTER_RELEASE,
                "CallSupport:IncomingCallWakeLock"
            )
            wakeLock?.acquire(60 * 1000L /*1 minute*/)
            Log.d(TAG, "WakeLock acquired")
        } catch (e: Exception) {
            Log.e(TAG, "WakeLock failed", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    Log.d(TAG, "WakeLock released")
                }
            }
            wakeLock = null
        } catch (e: Exception) {
            Log.e(TAG, "WakeLock release failed", e)
        }
    }

    private fun ensureNotificationChannelExists() {
        if (!isChannelCreated) {
            createIncomingCallChannel()
            isChannelCreated = true
        }
    }

    private fun createIncomingCallChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CALL_CHANNEL_ID,
                "Incoming Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority call notifications"
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .build()
                )
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Call channel created")
        }
    }

    override fun onDestroy() {
        releaseWakeLock()
        super.onDestroy()
    }
}

private fun KeyguardManager.requestDismissKeyguard(
    service: MyFirebaseMessagingService,
    nothing: Nothing?
) {
}
