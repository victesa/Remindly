package com.victorkirui.core.monitoring

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.Firebase

interface CrashReporter {
    fun log(message: String)
    fun recordException(throwable: Throwable)
    fun setCustomKey(key: String, value: String)
    fun setUserId(id: String)
}

class FirebaseCrashReporterImpl : CrashReporter {
    private val crashlytics: FirebaseCrashlytics = Firebase.crashlytics

    override fun log(message: String) {
        crashlytics.log(message)
    }

    override fun recordException(throwable: Throwable) {
        crashlytics.recordException(throwable)
    }

    override fun setCustomKey(key: String, value: String) {
        crashlytics.setCustomKey(key, value)
    }

    override fun setUserId(id: String) {
        crashlytics.setUserId(id)
    }
}
