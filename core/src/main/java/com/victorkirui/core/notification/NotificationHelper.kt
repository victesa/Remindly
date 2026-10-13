package com.victorkirui.core.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.victorkirui.core.R

class NotificationHelper(private val context: Context) {
    private val channelId = "capture_channel"

    companion object {
        const val EXTRA_ITEM_ID = "NAVIGATE_TO_ITEM_ID"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d("NotificationHelper", "Creating notification channel: $channelId")
            val name = "Capture Processing"
            val descriptionText = "Notifications for media capture processing"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                setShowBadge(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    @android.annotation.SuppressLint("MissingPermission")
    fun showNotification(title: String, message: String, itemId: String? = null, notificationId: Int? = null) {
        Log.d("NotificationHelper", "Request to show notification: $title - $message, itemId: $itemId")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.e("NotificationHelper", "Missing POST_NOTIFICATIONS permission")
                return
            }
        }

        // Create Intent for clicking the notification
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            if (itemId != null) {
                putExtra(EXTRA_ITEM_ID, itemId)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 
            itemId?.hashCode() ?: 0, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (itemId != null) {
            builder.addAction(
                android.R.drawable.ic_menu_view,
                "View Details",
                pendingIntent
            )
        }

        // Use a unique ID to prevent notifications from overwriting each other
        val finalId = notificationId ?: (itemId?.hashCode()?.plus(title.hashCode()) ?: System.currentTimeMillis().toInt())

        with(NotificationManagerCompat.from(context)) {
            notify(finalId, builder.build())
        }
    }



    @android.annotation.SuppressLint("MissingPermission")
    fun showQuotaExhaustedNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.e("NotificationHelper", "Missing POST_NOTIFICATIONS permission")
                return
            }
        }

        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("OPEN_PAYWALL", true)
            putExtra("EXTRA_OPEN_PAYWALL", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            9991,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Monthly Capture Limit Reached")
            .setContentText("You've reached your monthly AI extraction limit. Upgrade to Remindly Pro for 250 monthly extractions.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        with(NotificationManagerCompat.from(context)) {
            notify(9991, builder.build())
        }
    }
}
