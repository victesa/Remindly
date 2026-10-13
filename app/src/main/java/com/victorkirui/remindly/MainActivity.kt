package com.victorkirui.remindly

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.victorkirui.module_features.reminder.BriefingWorker
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.victorkirui.core.ShareIntentHandler
import com.victorkirui.core.notification.NotificationHelper
import com.victorkirui.core.repository.ReminderSettingsRepository
import com.victorkirui.module_features.capturing.CapturingDialog
import com.victorkirui.module_features.capturing.CapturingViewModel
import com.victorkirui.module_features.reminder.ReminderScheduler
import com.victorkirui.remindly.nav.RemindlyNavigation
import com.victorkirui.core.ui.theme.RemindlyTheme
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.time.LocalTime

class MainActivity : ComponentActivity() {

    private val shareIntentParser = ShareIntentHandler()
    private val capturingViewModel: CapturingViewModel by viewModel()
    private val settingsRepository: ReminderSettingsRepository by inject()
    private val reminderScheduler: ReminderScheduler by inject()
    
    private var pendingItemId by mutableStateOf<String?>(null)
    private var pendingOpenCapture by mutableStateOf(false)
    private var pendingSelectedDate by mutableStateOf<String?>(null)
    private var pendingOpenPaywall by mutableStateOf(false)


    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted
        }
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        handleIntent(intent)


        MainScope().launch {
            settingsRepository.preferredReminderTime.collect { preferredTimeStr ->
                val preferredTime = try {
                    LocalTime.parse(preferredTimeStr)
                } catch (e: Exception) {
                    LocalTime.of(8, 0)
                }
                reminderScheduler.scheduleDailyBriefing(preferredTime)
                try {
                    BriefingWorker.cancel(applicationContext)
                } catch (_: Exception) {}
            }
        }

        setContent {
            RemindlyTheme {
                val uiState by capturingViewModel.uiState.collectAsState()
                val windowSizeClass = calculateWindowSizeClass(this)
                
                Box(modifier = Modifier.fillMaxSize()) {
                    val source = try {
                        val ref = referrer?.host
                        if (ref != null) {
                            ref
                        } else {
                            // Try to get from Intent extras if referrer is missing
                            val extraRef = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                intent?.getParcelableExtra(Intent.EXTRA_REFERRER, Uri::class.java)
                            } else {
                                @Suppress("DEPRECATION")
                                intent?.getParcelableExtra(Intent.EXTRA_REFERRER)
                            }
                            extraRef?.host
                                ?: intent?.getStringExtra(Intent.EXTRA_REFERRER_NAME)
                                ?: "Unknown"
                        }
                    } catch (e: Exception) { "Unknown" }

                    RemindlyNavigation(
                        shareContent = shareIntentParser.handleIntent(intent, source),
                        initialItemId = pendingItemId,
                        initialOpenCapture = pendingOpenCapture,
                        initialSelectedDate = pendingSelectedDate,
                        initialOpenPaywall = pendingOpenPaywall,
                        windowWidthSizeClass = windowSizeClass.widthSizeClass,
                        onIntentHandled = {
                            if (pendingItemId != null) pendingItemId = null
                            if (pendingOpenCapture) pendingOpenCapture = false
                            if (pendingSelectedDate != null) pendingSelectedDate = null
                            if (pendingOpenPaywall) pendingOpenPaywall = false
                        }
                    )
                    
                    var showPaywallDirectly by remember { mutableStateOf(false) }

                    if (showPaywallDirectly) {
                        com.victorkirui.module_features.subscription.SubscriptionPaywallSheet(
                            onDismiss = { showPaywallDirectly = false },
                            onPlanSelected = { tier ->
                                showPaywallDirectly = false
                            }
                        )
                    }
                    
                    CapturingDialog(
                        uiState = uiState,
                        onDismiss = { 
                            capturingViewModel.resetState()
                        },
                        onViewPlans = {
                            capturingViewModel.resetState()
                            showPaywallDirectly = true
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.getStringExtra(NotificationHelper.EXTRA_ITEM_ID)?.let { itemId ->
            pendingItemId = itemId
        }
        val extras = intent?.extras
        if (extras != null) {

            val directItemId = extras.getString(com.victorkirui.remindly.widget.WidgetIntentConstants.EXTRA_ITEM_ID)
                ?: extras.getString(com.victorkirui.remindly.widget.WidgetIntentConstants.PARAM_ITEM_ID.name)
            if (!directItemId.isNullOrEmpty()) {
                pendingItemId = directItemId
            }

            val openCapture = extras.getBoolean(com.victorkirui.remindly.widget.WidgetIntentConstants.EXTRA_OPEN_CAPTURE, false) ||
                              extras.getBoolean(com.victorkirui.remindly.widget.WidgetIntentConstants.PARAM_OPEN_CAPTURE.name, false)
            if (openCapture) {
                pendingOpenCapture = true
            }

            val openPaywall = extras.getBoolean("OPEN_PAYWALL", false) ||
                              extras.getBoolean("EXTRA_OPEN_PAYWALL", false) ||
                              extras.getBoolean(com.victorkirui.remindly.widget.WidgetIntentConstants.EXTRA_OPEN_PAYWALL, false) ||
                              extras.getBoolean(com.victorkirui.remindly.widget.WidgetIntentConstants.PARAM_OPEN_PAYWALL.name, false)
            if (openPaywall) {
                pendingOpenPaywall = true
            }

            val dateStr = extras.getString(com.victorkirui.remindly.widget.WidgetIntentConstants.EXTRA_SELECTED_DATE)
                ?: extras.getString(com.victorkirui.remindly.widget.WidgetIntentConstants.PARAM_SELECTED_DATE.name)
            if (!dateStr.isNullOrEmpty()) {
                pendingSelectedDate = dateStr
            }
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onStop() {
        super.onStop()
        MainScope().launch {
            try {
                com.victorkirui.remindly.widget.WidgetUpdater.updateWidgets(applicationContext)
            } catch (e: Exception) {
                android.util.Log.w("MainActivity", "Failed to update widgets onStop: ${e.message}")
            }
        }
    }

}
