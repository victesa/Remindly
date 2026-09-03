package com.victorkirui.module_features.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.victorkirui.local.repository.LocalRepository
import com.victorkirui.core.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class BootReceiver : BroadcastReceiver(), KoinComponent {
    private val localRepository: LocalRepository by inject()
    private val authRepository: AuthRepository by inject()
    private val reminderScheduler: ReminderScheduler by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            Log.d("BootReceiver", "Rescheduling alarms after boot")
            
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val user = authRepository.currentUser.value
                    if (user != null) {
                        val allItems = localRepository.getAllItems(user.uid).first()
                        allItems.filter { it.status != "DONE" }.forEach { item ->
                            reminderScheduler.scheduleRemindersForItem(item)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Failed to reschedule alarms", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
