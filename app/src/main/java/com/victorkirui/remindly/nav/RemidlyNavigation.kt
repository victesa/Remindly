package com.victorkirui.remindly.nav

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.google.firebase.auth.FirebaseUser
import com.victorkirui.module_features.capturing.CaptureOptionsBottomSheet
import com.victorkirui.module_features.capturing.CapturingDialog
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
    val authRepository: AuthRepository = org.koin.compose.koinInject()
    val onboardingRepository: OnboardingRepository = org.koin.compose.koinInject()
    
    val currentUser by authRepository.currentUser.collectAsState()
    val hasSeenOnboarding by onboardingRepository.hasSeenOnboarding.collectAsState(initial = null)
    val scope = rememberCoroutineScope()

    val navController = rememberNavController()
    
    // Handle initial navigation if itemId or date or capture or paywall is provided
    androidx.compose.runtime.LaunchedEffect(initialItemId, currentUser, hasSeenOnboarding) {
        if (initialItemId != null && currentUser != null && hasSeenOnboarding == true) {
            navController.navigate("details/$initialItemId")
            onIntentHandled()
        }
    }

    var showCaptureOptions by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(initialOpenCapture) {
        if (initialOpenCapture) {
            showCaptureOptions = true
            onIntentHandled()
        }
    }

    androidx.compose.runtime.LaunchedEffect(initialOpenPaywall, currentUser, hasSeenOnboarding) {
        if (initialOpenPaywall && currentUser != null && hasSeenOnboarding == true) {
            navController.navigate("paywall")
            onIntentHandled()
        }
    }

    androidx.compose.runtime.LaunchedEffect(initialSelectedDate, currentUser, hasSeenOnboarding) {
        if (initialSelectedDate != null && currentUser != null && hasSeenOnboarding == true) {
            navController.navigate("reminders")
            onIntentHandled()
        }
    }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // Determine start destination based on onboarding, authentication status and shared content
    val startDestination = when {
        hasSeenOnboarding == null -> "loading" // Wait for DataStore
        hasSeenOnboarding == false -> "onboarding"
        currentUser == null -> "signIn"
        shareContent is ShareContent.Unknown -> "home"
        else -> "capture"
    }

    val isLargeScreen = windowWidthSizeClass == WindowWidthSizeClass.Expanded

    val capturingViewModel: CapturingViewModel = org.koin.androidx.compose.koinViewModel()

    // Map routes to display names for the navigation components
    val screenName = when (currentRoute) {
        "home" -> "Home"
        "inbox" -> "Inbox"
        "reminders" -> "Reminders"
        "profile" -> "Profile"
        else -> null
    }

    val subscriptionRepository: com.victorkirui.core.repository.SubscriptionRepository = org.koin.compose.koinInject()
    val hasPendingCelebration by subscriptionRepository.hasPendingProCelebration.collectAsState(initial = false)
    var showProCelebration by remember { mutableStateOf(false) }

    val hasSeenHomeWalkthrough by onboardingRepository.hasSeenHomeWalkthrough.collectAsState(initial = true)
    var showWalkthroughOverlay by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(hasSeenHomeWalkthrough, currentUser, currentRoute) {
        if (hasSeenHomeWalkthrough == false && currentUser != null && (currentRoute == "home" || currentRoute == null)) {
            showWalkthroughOverlay = true
        }
    }

    androidx.compose.runtime.LaunchedEffect(hasPendingCelebration, currentRoute) {
        if (hasPendingCelebration && currentRoute == "home") {
            showProCelebration = true
            kotlinx.coroutines.delay(6000L) // Show celebration for 6 seconds total
            showProCelebration = false
            subscriptionRepository.consumeProCelebration()
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

    Row(
        modifier = Modifier.fillMaxSize()
    ) {
        if (isLargeScreen && screenName != null) {
            RemindlyNavigationRail(
                currentScreen = screenName,
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
                    val isPremium = currentUser != null && currentUser!!.uid.isNotEmpty() // TODO: Actually fetch tier here if needed, or just keep it simple
                    
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
            if (hasSeenOnboarding == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF2D6A4F))
                }
            } else {
                NavHost(
                    navController = navController, 
                    startDestination = startDestination,
                    modifier = Modifier
                        .padding(paddingValues)
                        .consumeWindowInsets(paddingValues)
                ) {
                composable("onboarding") {
                    OnboardingScreen(
                        onFinished = {
                            val destination = if (currentUser == null) "signIn" else "home"
                            navController.navigate(destination) {
                                popUpTo("onboarding") { inclusive = true }
                            }
                        }
                    )
                }
                composable("signUp") {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    SignUpScreen(
                        onNavigateToSignIn = { navController.popBackStack() },
                        onSignUpSuccess = { 
                            SyncDownWorker.schedule(context)
                            navController.navigate("paywall") {
                                popUpTo("signUp") { inclusive = true }
                            }
                        },
                        windowWidthSizeClass = windowWidthSizeClass
                    )
                }
                composable("paywall") {
                    SubscriptionPaywallSheet(
                        onDismiss = {
                            navController.navigate("home") {
                                popUpTo("paywall") { inclusive = true }
                            }
                        },
                        onPlanSelected = {
                            navController.navigate("home") {
                                popUpTo("paywall") { inclusive = true }
                            }
                        }
                    )
                }
            composable("signIn") {
                val context = androidx.compose.ui.platform.LocalContext.current
                SignInScreen(
                    onNavigateToSignUp = { navController.navigate("signUp") },
                    onSignInSuccess = {
                        SyncDownWorker.schedule(context)
                        navController.navigate("home") {
                            popUpTo("signIn") { inclusive = true }
                        }
                    },
                    windowWidthSizeClass = windowWidthSizeClass
                )
            }
            composable("home") {
                HomeScreen(
                    windowWidthSizeClass = windowWidthSizeClass,
                    onNavigateToInbox = { navController.navigate("inbox") },
                    onNavigateToReminders = { navController.navigate("reminders") },
                    onNavigateToProfile = { navController.navigate("profile") },
                    onNavigateToItem = { itemId -> navController.navigate("details/$itemId") },
                    onCaptureClick = { showCaptureOptions = true }
                )
            }
            composable("inbox") {
                InboxScreen(
                    windowWidthSizeClass = windowWidthSizeClass,
                    onNavigateToHome = { navController.navigate("home") },
                    onNavigateToReminders = { navController.navigate("reminders") },
                    onNavigateToProfile = { navController.navigate("profile") },
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
                    onNavigateToHome = { navController.navigate("home") },
                    onNavigateToInbox = { navController.navigate("inbox") },
                    onNavigateToProfile = { navController.navigate("profile") }
                )
            }
            composable("profile") {
                ProfileScreen(
                    windowWidthSizeClass = windowWidthSizeClass,
                    onSignOut = { 
                        scope.launch {
                            authRepository.signOut(clearData = true)
                            navController.navigate("signIn") {
                                popUpTo(0)
                            }
                        }
                    },
                    onShowWalkthrough = {
                        showWalkthroughOverlay = true
                    }
                )
            }
            composable("capture") {
                // Capture Screen/Dialog is managed by MainActivity for now
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

    if (showProCelebration) {
        com.victorkirui.module_features.home.ProCelebrationOverlay(
            modifier = Modifier.fillMaxSize()
        )
    }

    if (showWalkthroughOverlay) {
        com.victorkirui.module_features.onboarding.FeatureWalkthroughOverlay(
            onDismiss = {
                showWalkthroughOverlay = false
                scope.launch {
                    onboardingRepository.setHasSeenHomeWalkthrough(true)
                }
                navController.navigate("paywall")
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
}
