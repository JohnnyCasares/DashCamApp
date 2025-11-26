package com.kasahirotech.dashcamapp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.kasahirotech.dashcamapp.MainActivity
import com.kasahirotech.dashcamapp.R

object NotificationHelper {
    const val CHANNEL_ID = "recording_channel"
    const val CHANNEL_NAME = "Recording Status"
    
    /**
     * Creates the notification channel for recording notifications.
     * Required for Android O (API 26) and above.
     * Channel is configured with HIGH importance but no sound for silent updates.
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows recording status and controls"
                setSound(null, null) // No sound for silent updates
                enableVibration(false)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Builds a recording notification with status, elapsed time, and action buttons.
     * The notification is ongoing (non-dismissible) and includes:
     * - App icon
     * - Recording status text
     * - Elapsed time
     * - Stop action button
     * - Tap action to open app
     */
    fun buildRecordingNotification(context: Context, elapsedTime: String): Notification {
        val stopIntent = Intent(context, RecordingService::class.java).apply {
            action = RecordingService.ACTION_STOP_RECORDING
        }
        val stopPendingIntent = PendingIntent.getService(
            context,
            0,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Recording")
            .setContentText("Elapsed time: $elapsedTime")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true) // Makes notification non-dismissible
            .setOnlyAlertOnce(true) // Silent updates
            .addAction(R.drawable.ic_stop_recording, "Stop", stopPendingIntent)
            .setContentIntent(openAppPendingIntent)
            .build()
    }
}
