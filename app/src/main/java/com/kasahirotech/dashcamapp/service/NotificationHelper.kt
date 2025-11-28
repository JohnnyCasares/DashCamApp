package com.kasahirotech.dashcamapp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
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
     * Builds a MediaStyle recording notification with status, elapsed time, and action button.
     * The notification is ongoing (non-dismissible) and includes:
     * - Large app icon for better visibility
     * - Recording status text with elapsed time
     * - Stop action button (synced with MainActivity button)
     * - MediaStyle presentation for enhanced UI
     * - Low storage warning (if applicable)
     * 
     * @param context Application context
     * @param elapsedTime Formatted elapsed recording time (e.g., "01:23")
     * @param isLowStorage Whether storage space is low
     * @return Configured notification ready for display
     */
    fun buildRecordingNotification(
        context: Context, 
        elapsedTime: String,
        isLowStorage: Boolean = false
    ): Notification {
        // Create Stop Recording action
        val stopIntent = Intent(context, RecordingService::class.java).apply {
            action = RecordingService.ACTION_STOP_RECORDING
        }
        val stopPendingIntent = PendingIntent.getService(
            context,
            0,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        // Build notification content text with optional low storage warning
        val contentText = if (isLowStorage) {
            "⚠️ Low Storage • $elapsedTime"
        } else {
            "Recording • $elapsedTime"
        }
        
        // Create large icon from launcher icon
        val largeIcon = BitmapFactory.decodeResource(
            context.resources,
            R.mipmap.ic_launcher
        )
        
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("DashCam Recording")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setLargeIcon(largeIcon)
            .setOngoing(true) // Makes notification non-dismissible
            .setOnlyAlertOnce(true) // Silent updates
            .setShowWhen(true)
            .setUsesChronometer(false)
            // Add stop action button (synced with MainActivity)
            .addAction(R.drawable.ic_stop_recording, "Stop", stopPendingIntent)
            // Apply MediaStyle for enhanced presentation
            .setStyle(
                MediaStyle()
                    .setShowActionsInCompactView(0) // Show stop action in compact view
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
    
    /**
     * Legacy method for backward compatibility.
     * Calls the enhanced version with default parameters.
     */
    @Deprecated(
        message = "Use buildRecordingNotification with isLowStorage parameter",
        replaceWith = ReplaceWith("buildRecordingNotification(context, elapsedTime, false)")
    )
    fun buildRecordingNotification(context: Context, elapsedTime: String): Notification {
        return buildRecordingNotification(context, elapsedTime, false)
    }
}
