package com.victorkirui.remindly.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.Mic
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.victorkirui.core.model.ShareContent
import com.victorkirui.core.ui.component.RemindlyBottomNavigation
import com.victorkirui.core.ui.component.RemindlyNavigationRail
import com.victorkirui.module_features.details.ItemDetailsScreen
import com.victorkirui.module_features.details.EditItemScreen
import com.victorkirui.module_features.home.HomeScreen
import com.victorkirui.module_features.inbox.InboxScreen
import com.victorkirui.module_features.profile.ProfileScreen
import com.victorkirui.module_features.details.CategoryDetailScreen
import com.victorkirui.module_features.reminders.RemindersScreen
import com.victorkirui.module_features.auth.SignUpScreen
import com.victorkirui.module_features.auth.SignInScreen
import com.victorkirui.module_features.onboarding.OnboardingScreen
import com.victorkirui.module_features.subscription.SubscriptionPaywallSheet
import com.victorkirui.module_features.capturing.SyncDownWorker
import com.victorkirui.core.repository.AuthRepository
import com.victorkirui.core.repository.OnboardingRepository
import com.victorkirui.core.repository.SubscriptionRepository
import com.victorkirui.core.repository.SubscriptionTier
import com.victorkirui.module_features.nav.AppViewModel
import com.victorkirui.module_features.nav.AppUiState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import com.victorkirui.module_features.capturing.CaptureOptionsBottomSheet
import com.victorkirui.module_features.capturing.CapturingViewModel
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RemindlyNavigation(
    shareContent: ShareContent,
    initialItemId: String? = null,
    initialOpenCapture: Boolean = false,
    initialSelectedDate: String? = null,
    initialOpenPaywall: Boolean = false,
    windowWidthSizeClass: WindowWidthSizeClass,
    onIntentHandled: () -> Unit = {}
) {
    val appViewModel: AppViewModel = org.koin.androidx.compose.koinViewModel()
    val appUiState by appViewModel.appUiState.collectAsState()

    val authRepository: AuthRepository = org.koin.compose.koinInject()
    val onboardingRepository: OnboardingRepository = org.koin.compose.koinInject()
    val subscriptionRepository: SubscriptionRepository = org.koin.compose.koinInject()

    val currentUser by authRepository.currentUser.collectAsState()
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Reactive single source of truth for session & auth navigation
    LaunchedEffect(appUiState) {
        val dest = navController.currentDestination?.route
        when (appUiState) {
            is AppUiState.Authenticated -> {
                if (dest == "onboarding" || dest == "signIn" || dest == "signUp" || dest == null) {
                    val targetRoute = when {
                        initialItemId != null -> "details/$initialItemId"
                        initialOpenPaywall -> "paywall"
                        initialSelectedDate != null -> "reminders"
                        shareContent !is ShareContent.Unknown -> "capture"
                        else -> "home"
                    }
                    navController.navigate(targetRoute) {
                        popUpTo(0) { inclusive = true }
                    }
                    onIntentHandled()
                }
            }
            AppUiState.Unauthenticated -> {
                if (dest != "signIn" && dest != "signUp") {
                    navController.navigate("signIn") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            AppUiState.Onboarding -> {
                if (dest != "onboarding") {
                    navController.navigate("onboarding") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            AppUiState.Loading -> { /* Do nothing while resolving initial state */ }
        }
    }

    var showCaptureOptions by remember { mutableStateOf(false) }

    LaunchedEffect(initialOpenCapture) {
        if (initialOpenCapture) {
            showCaptureOptions = true
            onIntentHandled()
        }
    }

    val isLargeScreen = windowWidthSizeClass == WindowWidthSizeClass.Expanded
    val capturingViewModel: CapturingViewModel = org.koin.androidx.compose.koinViewModel()

    val screenName = when (currentRoute) {
        "home" -> "Home"
        "inbox" -> "Inbox"
        "reminders" -> "Reminders"
        "profile" -> "Profile"
        else -> null
    }

    val subscriptionTier by subscriptionRepository.subscriptionTier.collectAsState(initial = SubscriptionTier.FREE)
    val isPremiumUser = subscriptionTier == SubscriptionTier.PREMIUM
    val hasPendingCelebration by subscriptionRepository.hasPendingProCelebration.collectAsState(initial = false)
    var showProCelebrationOverlay by remember { mutableStateOf(false) }
    var showProPerksDialog by remember { mutableStateOf(false) }

    val hasPendingExpiration by subscriptionRepository.hasPendingProExpiration.collectAsState(initial = false)
    var showProExpiration by remember { mutableStateOf(false) }
    var showPaywallSheet by remember { mutableStateOf(false) }

    val hasSeenHomeWalkthrough by onboardingRepository.hasSeenHomeWalkthrough.collectAsState(initial = true)
    var showWalkthroughOverlay by remember { mutableStateOf(false) }

    LaunchedEffect(hasSeenHomeWalkthrough, currentUser, currentRoute) {
        if (!hasSeenHomeWalkthrough && currentUser != null && currentRoute == "home") {
            showWalkthroughOverlay = true
        }
    }

    LaunchedEffect(hasPendingCelebration, currentRoute, currentUser) {
        if (hasPendingCelebration && currentUser != null && currentRoute == "home") {
            showProCelebrationOverlay = true
            showProPerksDialog = true
        }
    }

    LaunchedEffect(showProCelebrationOverlay) {
        if (showProCelebrationOverlay) {
            kotlinx.coroutines.delay(4500)
            showProCelebrationOverlay = false
        }
    }

    LaunchedEffect(hasPendingExpiration, currentRoute, currentUser) {
        if (hasPendingExpiration && currentUser != null && currentRoute == "home") {
            showProExpiration = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        fun navigateToTab(route: String) {
            navController.navigate(route) {
                popUpTo("home") {
                    saveState = true
                    inclusive = false
                }
                launchSingleTop = true
                restoreState = true
            }
        }

        Row(modifier = Modifier.fillMaxSize()) {
            if (isLargeScreen && screenName != null) {
                RemindlyNavigationRail(
                    currentScreen = screenName,
                    isPremium = isPremiumUser,
                    onHomeClick = { navigateToTab("home") },
                    onInboxClick = { navigateToTab("inbox") },
                    onRemindersClick = { navigateToTab("reminders") },
                    onProfileClick = { navigateToTab("profile") },
                    onAddClick = { showCaptureOptions = true }
                )
            }

            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    if (!isLargeScreen && screenName != null) {
                        RemindlyBottomNavigation(
                            currentScreen = screenName,
                            onHomeClick = { navigateToTab("home") },
                            onInboxClick = { navigateToTab("inbox") },
                            onRemindersClick = { navigateToTab("reminders") },
                            onProfileClick = { navigateToTab("profile") }
                        )
                    }
                },
                floatingActionButton = {
                    val mainScreens = listOf("home", "inbox", "reminders", "profile")
                    val showFab = currentRoute in mainScreens && !isLargeScreen
                    if (showFab) {
                        ExtendedFloatingActionButton(
                            onClick = { showCaptureOptions = true },
                            containerColor = Color(0xFF2D6A4F),
                            contentColor = Color.White,
                            shape = RoundedCornerShape(16.dp),
                            icon = { Icon(Icons.Default.Add, contentDescription = null) },
                            text = { Text("Capture", fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            ) { paddingValues ->
                if (appUiState == AppUiState.Loading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFF7FBF8)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF2D6A4F))
                    }
                } else {
                    val startDestination = when (appUiState) {
                        AppUiState.Onboarding -> "onboarding"
                        is AppUiState.Authenticated -> "home"
                        else -> "signIn"
                    }

                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
                        modifier = Modifier
                            .padding(paddingValues)
                            .consumeWindowInsets(paddingValues)
                    ) {
                    composable("onboarding") {
                        OnboardingScreen(
                            windowWidthSizeClass = windowWidthSizeClass,
                            onFinished = {
                                scope.launch {
                                    onboardingRepository.setHasSeenOnboarding(true)
                                }
                            }
                        )
                    }

                    composable("signUp") {
                        SignUpScreen(
                            onNavigateToSignIn = { navController.popBackStack() },
                            onSignUpSuccess = {
                                scope.launch {
                                    onboardingRepository.setHasSeenOnboarding(true)
                                    onboardingRepository.setHasSeenHomeWalkthrough(false)
                                }
                            },
                            windowWidthSizeClass = windowWidthSizeClass
                        )
                    }

                    composable("paywall") {
                        SubscriptionPaywallSheet(
                            onDismiss = {
                                navController.navigate("home") {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            onPlanSelected = {
                                navController.navigate("home") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("signIn") {
                        SignInScreen(
                            onNavigateToSignUp = { navController.navigate("signUp") },
                            onSignInSuccess = {
                                scope.launch {
                                    onboardingRepository.setHasSeenOnboarding(true)
                                    onboardingRepository.setHasSeenHomeWalkthrough(true)
                                }
                            },
                            windowWidthSizeClass = windowWidthSizeClass
                        )
                    }

                    composable("home") {
                        HomeScreen(
                            windowWidthSizeClass = windowWidthSizeClass,
                            onNavigateToInbox = { navigateToTab("inbox") },
                            onNavigateToReminders = { navigateToTab("reminders") },
                            onNavigateToProfile = { navigateToTab("profile") },
                            onNavigateToItem = { itemId -> navController.navigate("details/$itemId") },
                            onNavigateToCategory = { categoryName ->
                                val encoded = java.net.URLEncoder.encode(categoryName, "UTF-8")
                                navController.navigate("category_details/$encoded")
                            }
                        )
                    }

                    composable("inbox") {
                        InboxScreen(
                            windowWidthSizeClass = windowWidthSizeClass,
                            onNavigateToHome = { navigateToTab("home") },
                            onNavigateToReminders = { navigateToTab("reminders") },
                            onNavigateToProfile = { navigateToTab("profile") },
                            onNavigateToCategory = { categoryName -> navController.navigate("category_details/$categoryName") },
                            onNavigateToItem = { itemId -> navController.navigate("details/$itemId") }
                        )
                    }

                    composable(
                        route = "category_details/{categoryName}",
                        arguments = listOf(navArgument("categoryName") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val categoryName = backStackEntry.arguments?.getString("categoryName") ?: ""
                        val decodedName = try {
                            java.net.URLDecoder.decode(categoryName, "UTF-8")
                        } catch (_: Exception) {
                            categoryName
                        }
                        CategoryDetailScreen(
                            categoryName = decodedName,
                            onBack = { navController.popBackStack() },
                            onItemClick = { itemId -> navController.navigate("details/$itemId") }
                        )
                    }

                    composable("reminders") {
                        RemindersScreen(
                            windowWidthSizeClass = windowWidthSizeClass,
                            initialSelectedDate = initialSelectedDate,
                            onNavigateToHome = { navigateToTab("home") },
                            onNavigateToInbox = { navigateToTab("inbox") },
                            onNavigateToProfile = { navigateToTab("profile") }
                        )
                    }

                    composable("profile") {
                        ProfileScreen(
                            windowWidthSizeClass = windowWidthSizeClass,
                            onSignOut = {
                                scope.launch {
                                    authRepository.signOut(clearData = true)
                                }
                            },
                            onShowWalkthrough = {
                                showWalkthroughOverlay = true
                            },
                            onNavigateToHome = {
                                navController.navigate("home") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("capture") {
                        // Managed by MainActivity
                    }

                    composable(
                        route = "details/{itemId}",
                        arguments = listOf(navArgument("itemId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val itemId = backStackEntry.arguments?.getString("itemId") ?: return@composable
                        ItemDetailsScreen(
                            itemId = itemId,
                            onBack = { navController.popBackStack() },
                            onEdit = { navController.navigate("edit/$itemId") }
                        )
                    }

                    composable(
                        route = "edit/{itemId}",
                        arguments = listOf(navArgument("itemId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val itemId = backStackEntry.arguments?.getString("itemId") ?: return@composable
                        EditItemScreen(
                            itemId = itemId,
                            onBack = { navController.popBackStack() },
                            onNavigateToHome = { navController.navigate("home") },
                            onNavigateToInbox = { navController.navigate("inbox") },
                            onNavigateToReminders = { navController.navigate("reminders") },
                            onNavigateToProfile = { navController.navigate("profile") },
                            windowWidthSizeClass = windowWidthSizeClass
                        )
                    }
                }
            }
            }
        }

        if (showCaptureOptions) {
            CaptureOptionsBottomSheet(
                onDismiss = { showCaptureOptions = false },
                onCaptureText = { text ->
                    capturingViewModel.capture(com.victorkirui.core.model.ShareContent.Text(text))
                    showCaptureOptions = false
                },
                onCaptureUri = { uri, type ->
                    val content = if (type == "IMAGE") {
                        com.victorkirui.core.model.ShareContent.Image(uri.toString())
                    } else {
                        com.victorkirui.core.model.ShareContent.Pdf(uri.toString())
                    }
                    capturingViewModel.capture(content)
                    showCaptureOptions = false
                }
            )
        }

        if (showProCelebrationOverlay) {
            com.victorkirui.module_features.home.ProCelebrationOverlay(
                modifier = Modifier.fillMaxSize()
            )
        }

        if (showProPerksDialog) {
            ProPerksCelebrationDialog(
                onDismiss = {
                    showProPerksDialog = false
                    scope.launch {
                        subscriptionRepository.consumeProCelebration()
                    }
                }
            )
        }

        if (showProExpiration) {
            ProExpirationDialog(
                onReSubscribe = {
                    showProExpiration = false
                    scope.launch {
                        subscriptionRepository.consumeProExpiration()
                    }
                    showPaywallSheet = true
                },
                onDismiss = {
                    showProExpiration = false
                    scope.launch {
                        subscriptionRepository.consumeProExpiration()
                    }
                }
            )
        }

        if (showPaywallSheet) {
            SubscriptionPaywallSheet(
                onDismiss = { showPaywallSheet = false }
            )
        }

        if (showWalkthroughOverlay) {
            com.victorkirui.module_features.onboarding.FeatureWalkthroughOverlay(
                onDismiss = {
                    showWalkthroughOverlay = false
                    scope.launch {
                        onboardingRepository.setHasSeenHomeWalkthrough(true)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun ProExpirationDialog(
    onReSubscribe: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color(0xFF2D6A4F),
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Your Pro Subscription Ended",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = "Your account is now on the Free plan. Upgrade back to Remindly Pro to restore 250 monthly AI extractions, 50-page PDF processing, Google Calendar auto-sync, and home screen widgets.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(
                onClick = onReSubscribe,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D6A4F)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Get Pro Back", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Maybe Later", color = Color.Gray)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun ProPerksCelebrationDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(28.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2D6A4F).copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFF2D6A4F),
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        title = {
            Text(
                "Welcome to Remindly Pro! 🎉",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                color = Color.Black
            )
        },
        text = {
            Column(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "Your Pro upgrade is active! Here are the perks you now enjoy:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                ProPerkRow(
                    icon = Icons.Default.AutoAwesome,
                    title = "Expanded AI Extractions (250/mo)",
                    subtitle = "Analyze job postings, events, multi-page PDFs (up to 50 pages) & images with a generous Pro quota."
                )

                ProPerkRow(
                    icon = Icons.Default.Mic,
                    title = "Gemini AI & Assistant Integration",
                    subtitle = "Create reminders and search your saved captures hands-free using Gemini AI voice commands."
                )

                ProPerkRow(
                    icon = Icons.Default.Sync,
                    title = "Multi-Device Auto-Sync",
                    subtitle = "Your captures and deadlines sync seamlessly across phones, foldables & tablets."
                )

                ProPerkRow(
                    icon = Icons.Default.CalendarToday,
                    title = "Google Calendar Auto-Sync",
                    subtitle = "Extracted deadlines & events auto-sync directly to your Google Calendar."
                )

                ProPerkRow(
                    icon = Icons.Default.Widgets,
                    title = "Interactive Home Widgets",
                    subtitle = "Add small, medium & large widgets for instant access from your home screen."
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2D6A4F),
                    contentColor = Color.White
                )
            ) {
                Text("Explore Remindly Pro", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    )
}

@Composable
fun ProPerkRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(0xFFEAF2EE)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF2D6A4F),
                modifier = Modifier.size(18.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color.Black
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}
