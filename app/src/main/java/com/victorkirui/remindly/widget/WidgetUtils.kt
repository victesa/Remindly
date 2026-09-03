package com.victorkirui.remindly.widget

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import com.victorkirui.core.repository.subscriptionDataStore
import kotlinx.coroutines.flow.first

object WidgetUtils {
    suspend fun isUserPro(context: Context): Boolean {
        return try {
            val prefs = context.subscriptionDataStore.data.first()
            val tierName = prefs[stringPreferencesKey("subscription_tier")] ?: "FREE"
            android.util.Log.d("WidgetUtils", "isUserPro checked: tierName=$tierName")
            tierName == "PREMIUM"
        } catch (e: Exception) {
            android.util.Log.w("WidgetUtils", "Error checking isUserPro: ${e.message}")
            false
        }
    }
}
