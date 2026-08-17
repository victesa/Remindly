package com.victorkirui.remindly

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.victorkirui.core.ShareIntentHandler
import com.victorkirui.core.model.ShareContent
import com.victorkirui.core.repository.ReminderSettingsRepository
import com.victorkirui.module_features.capturing.CapturingDialog
import com.victorkirui.module_features.capturing.CapturingViewModel
import com.victorkirui.module_features.reminder.BriefingWorker
import com.victorkirui.remindly.nav.RemindlyNavigation
import com.victorkirui.core.ui.theme.RemindlyTheme
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.time.LocalTime

class MainActivity : ComponentActivity() {

    private val shareIntentParser = ShareIntentHandler()
    private val capturingViewModel: CapturingViewModel by viewModel()
    private val settingsRepository: ReminderSettingsRepository by inject()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted, notifications will now work
        }
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        checkNotificationPermission()
        handleShareIntent(intent)

        MainScope().launch {
            val preferredTimeStr = settingsRepository.preferredReminderTime.first()
            val preferredTime = try {
                LocalTime.parse(preferredTimeStr)
            } catch (e: Exception) {
                LocalTime.of(8, 0)
            }
            BriefingWorker.schedule(this@MainActivity, preferredTime)
        }

        setContent {
            val themeRepository: com.victorkirui.core.repository.ThemeRepository = org.koin.compose.koinInject()
            val isDarkThemePref by themeRepository.isDarkTheme.collectAsState(initial = null)
            val useDarkTheme = isDarkThemePref ?: androidx.compose.foundation.isSystemInDarkTheme()

            RemindlyTheme(darkTheme = useDarkTheme) {
                val uiState by capturingViewModel.uiState.collectAsState()
                val windowSizeClass = calculateWindowSizeClass(this)
                
                Box(modifier = Modifier.fillMaxSize()) {
                    val source = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                        referrer?.host ?: "Unknown"
                    } else {
                        "Unknown"
                    }
                    RemindlyNavigation(
                        shareContent = shareIntentParser.handleIntent(intent, source),
                        windowWidthSizeClass = windowSizeClass.widthSizeClass
                    )
                    
                    CapturingDialog(
                        uiState = uiState,
                        onDismiss = { 
                            capturingViewModel.resetState()
                            // Move task to back to return to previous app
                            moveTaskToBack(true)
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        val source = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            referrer?.host ?: "Unknown"
        } else {
            "Unknown"
        }
        val sharedContent = shareIntentParser.handleIntent(intent, source)
        if (sharedContent !is ShareContent.Unknown) {
            capturingViewModel.capture(sharedContent)
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun hideSystemBars() {
        val windowInsetsController =
            WindowCompat.getInsetsController(window, window.decorView)
        // Configure the behavior of the system bars.
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        // Hide both the status bar and the navigation bar.
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }
}
