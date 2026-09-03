package com.victorkirui.core.monitoring

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.Firebase

interface Analytics {
    fun logEvent(name: String, params: Map<String, Any>? = null)
    fun setUserProperty(name: String, value: String)
    fun setUserId(id: String)
}

class FirebaseAnalyticsImpl : Analytics {
    private val firebaseAnalytics: FirebaseAnalytics = Firebase.analytics

    override fun logEvent(name: String, params: Map<String, Any>?) {
        val bundle = params?.let {
            Bundle().apply {
                it.forEach { (key, value) ->
                    when (value) {
                        is String -> putString(key, value)
                        is Int -> putInt(key, value)
                        is Long -> putLong(key, value)
                        is Double -> putDouble(key, value)
                        is Boolean -> putBoolean(key, value)
                    }
                }
            }
        }
        firebaseAnalytics.logEvent(name, bundle)
    }

    override fun setUserProperty(name: String, value: String) {
        firebaseAnalytics.setUserProperty(name, value)
    }

    override fun setUserId(id: String) {
        firebaseAnalytics.setUserId(id)
    }
}
