package com.example.call_ambulance.foregroundservice

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class CallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            "DECLINE_CALL" -> {
                // Just cancel the notification
                (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                    .cancelAll()
            }
        }
    }
}