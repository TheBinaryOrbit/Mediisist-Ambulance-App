package com.example.call_ambulance.foregroundservice

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.PermissionChecker
import com.example.call_ambulance.R
import android.content.pm.ServiceInfo

class CallSupportForegroundService : Service() {

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "call_support_channel"
        private const val FOREGROUND_SERVICE_ID = 1010

        fun start(context: Context) {
            val intent = Intent(context, CallSupportForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, CallSupportForegroundService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceSafely()
    }

    private fun startForegroundServiceSafely() {
        val hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionChecker.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                    PermissionChecker.PERMISSION_GRANTED
        } else true

        if (!hasNotificationPermission) {
            stopSelf()
            return
        }

        createNotificationChannel()

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Call Support Active")
            .setContentText("Listening for support notifications")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                FOREGROUND_SERVICE_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
            )
        } else {
            startForeground(FOREGROUND_SERVICE_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Call Support Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Channel for call support service"
            }

            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
