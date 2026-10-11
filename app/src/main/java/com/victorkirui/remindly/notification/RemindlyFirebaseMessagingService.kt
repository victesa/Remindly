package com.victorkirui.remindly.notification

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.victorkirui.core.notification.NotificationHelper
import com.victorkirui.module_features.capturing.SyncDownWorker
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class RemindlyFirebaseMessagingService : FirebaseMessagingService(), KoinComponent {

    private val notificationHelper: NotificationHelper by inject()

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i("FCMService", "Refreshed FCM registration token: $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCMService", "FCM message received from: ${remoteMessage.from}, data: ${remoteMessage.data}")

        val data = remoteMessage.data
        val action = data["action"]
        val grantPro = data["grant_pro"]
        val promoCode = data["promo_code"] ?: data["code"] ?: "ANDROID254"

        if (action == "REFRESH_PRO_TIER" || grantPro == "true") {
            Log.d("FCMService", "Received Pro tier refresh signal via FCM. Scheduling SyncDownWorker...")
            SyncDownWorker.schedule(applicationContext)
        }

        val title = remoteMessage.notification?.title
            ?: data["title"]
            ?: data["heading"]

        val body = remoteMessage.notification?.body
            ?: data["body"]
            ?: data["message"]

        if (!title.isNullOrBlank() || !body.isNullOrBlank()) {
            val finalTitle = title ?: "Special Offer Unlocked! 🎁"
            val finalBody = body ?: "Claim your exclusive Remindly Pro upgrade!"
            Log.d("FCMService", "Displaying FCM promo notification: $finalTitle - $finalBody, code: $promoCode")

            notificationHelper.showPromoNotification(
                title = finalTitle,
                message = finalBody,
                promoCode = promoCode,
                showDialog = true
            )
        }
    }
}
