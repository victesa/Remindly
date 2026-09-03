package com.victorkirui.core.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.subscriptionDataStore by preferencesDataStore(name = "subscription_settings")

enum class SubscriptionTier {
    FREE, PREMIUM
}

interface SubscriptionRepository : com.victorkirui.core.data.DataClearer {
    val subscriptionTier: Flow<SubscriptionTier>
    suspend fun setSubscriptionTier(tier: SubscriptionTier)
    
    // Limits
    val freePdfPageLimit: Int get() = 2
    val premiumPdfPageLimit: Int get() = 50 // Much higher for premium
    
    val freeImageLimit: Int get() = 5 // Example limit
    val premiumImageLimit: Int get() = Int.MAX_VALUE

    // Quota info from backend
    val remainingQuota: Flow<Int>
    val quotaLimit: Flow<Int>
    suspend fun updateQuota(limit: Int, remaining: Int)
    suspend fun updateTier(tier: SubscriptionTier)
    
    val imageCaptureCount: Flow<Int>
    suspend fun incrementImageCaptureCount()

    val isCalendarSyncEnabled: Flow<Boolean>
    suspend fun setCalendarSyncEnabled(enabled: Boolean)

    val hasPendingProCelebration: Flow<Boolean>
    suspend fun consumeProCelebration()
}

class SubscriptionRepositoryImpl(private val context: Context) : SubscriptionRepository {
    private val TIER_KEY = stringPreferencesKey("subscription_tier")
    private val QUOTA_REMAINING_KEY = androidx.datastore.preferences.core.intPreferencesKey("quota_remaining")
    private val QUOTA_LIMIT_KEY = androidx.datastore.preferences.core.intPreferencesKey("quota_limit")
    private val IMAGE_COUNT_KEY = androidx.datastore.preferences.core.intPreferencesKey("image_capture_count")
    private val CALENDAR_SYNC_ENABLED_KEY = androidx.datastore.preferences.core.booleanPreferencesKey("calendar_sync_enabled")
    private val PRO_CELEBRATION_KEY = androidx.datastore.preferences.core.booleanPreferencesKey("pending_pro_celebration")

    override val subscriptionTier: Flow<SubscriptionTier> = context.subscriptionDataStore.data.map { preferences ->
        val tierName = preferences[TIER_KEY] ?: SubscriptionTier.FREE.name
        try {
            SubscriptionTier.valueOf(tierName)
        } catch (e: Exception) {
            SubscriptionTier.FREE
        }
    }

    override val remainingQuota: Flow<Int> = context.subscriptionDataStore.data.map { it[QUOTA_REMAINING_KEY] ?: -1 }
    override val quotaLimit: Flow<Int> = context.subscriptionDataStore.data.map { it[QUOTA_LIMIT_KEY] ?: -1 }
    override val imageCaptureCount: Flow<Int> = context.subscriptionDataStore.data.map { it[IMAGE_COUNT_KEY] ?: 0 }
    override val isCalendarSyncEnabled: Flow<Boolean> = context.subscriptionDataStore.data.map { it[CALENDAR_SYNC_ENABLED_KEY] ?: true }
    override val hasPendingProCelebration: Flow<Boolean> = context.subscriptionDataStore.data.map { it[PRO_CELEBRATION_KEY] ?: false }

    override suspend fun setSubscriptionTier(tier: SubscriptionTier) {
        context.subscriptionDataStore.edit { preferences ->
            val currentTierName = preferences[TIER_KEY] ?: SubscriptionTier.FREE.name
            if (currentTierName == SubscriptionTier.FREE.name && tier == SubscriptionTier.PREMIUM) {
                preferences[PRO_CELEBRATION_KEY] = true
            }
            preferences[TIER_KEY] = tier.name
        }
        notifyWidgetUpdate(context)
    }

    override suspend fun consumeProCelebration() {
        context.subscriptionDataStore.edit { preferences ->
            preferences[PRO_CELEBRATION_KEY] = false
        }
    }

    override suspend fun setCalendarSyncEnabled(enabled: Boolean) {
        context.subscriptionDataStore.edit { preferences ->
            preferences[CALENDAR_SYNC_ENABLED_KEY] = enabled
        }
    }

    override suspend fun updateQuota(limit: Int, remaining: Int) {
        context.subscriptionDataStore.edit { preferences ->
            preferences[QUOTA_LIMIT_KEY] = limit
            preferences[QUOTA_REMAINING_KEY] = remaining
        }
    }

    override suspend fun updateTier(tier: SubscriptionTier) {
        context.subscriptionDataStore.edit { preferences ->
            preferences[TIER_KEY] = tier.name
        }
        notifyWidgetUpdate(context)
    }

    override suspend fun incrementImageCaptureCount() {
        context.subscriptionDataStore.edit { preferences ->
            val current = preferences[IMAGE_COUNT_KEY] ?: 0
            preferences[IMAGE_COUNT_KEY] = current + 1
        }
    }

    override suspend fun clearUserData(userId: String) {
        context.subscriptionDataStore.edit { it.clear() }
        notifyWidgetUpdate(context)
    }

    private fun notifyWidgetUpdate(context: Context) {
        try {
            val isPro = kotlinx.coroutines.runBlocking {
                try {
                    val prefs = context.subscriptionDataStore.data.first()
                    val tierName = prefs[TIER_KEY] ?: SubscriptionTier.FREE.name
                    tierName == SubscriptionTier.PREMIUM.name
                } catch (e: Exception) { false }
            }

            val newState = if (isPro) {
                android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }

            val pm = context.packageManager
            val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
            val receivers = listOf(
                "com.victorkirui.remindly.widget.SmallCalendarWidgetReceiver",
                "com.victorkirui.remindly.widget.MediumCalendarWidgetReceiver",
                "com.victorkirui.remindly.widget.LargeCalendarWidgetReceiver",
                "com.victorkirui.remindly.widget.SmallUpcomingWidgetReceiver",
                "com.victorkirui.remindly.widget.MediumUpcomingWidgetReceiver",
                "com.victorkirui.remindly.widget.LargeUpcomingWidgetReceiver"
            )

            receivers.forEach { receiverClassName ->
                try {
                    val component = android.content.ComponentName(context.packageName, receiverClassName)
                    if (pm.getComponentEnabledSetting(component) != newState) {
                        pm.setComponentEnabledSetting(
                            component,
                            newState,
                            android.content.pm.PackageManager.DONT_KILL_APP
                        )
                    }

                    if (isPro) {
                        val ids = appWidgetManager.getAppWidgetIds(component)
                        if (ids != null && ids.isNotEmpty()) {
                            val intent = android.content.Intent().apply {
                                this.component = component
                                action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
                                putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                            }
                            context.sendBroadcast(intent)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("SubscriptionRepo", "Error updating $receiverClassName: ${e.message}")
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("SubscriptionRepo", "Failed to update widgets: ${e.message}")
        }
    }
}
