package com.victorkirui.remindly.nav

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    windowWidthSizeClass: WindowWidthSizeClass
) {
    val authRepository: AuthRepository = org.koin.compose.koinInject()
    val onboardingRepository: OnboardingRepository = org.koin.compose.koinInject()
    
    val currentUser by authRepository.currentUser.collectAsState()
    val hasSeenOnboarding by onboardingRepository.hasSeenOnboarding.collectAsState(initial = null)
    val scope = rememberCoroutineScope()

    val navController = rememberNavController()
    
    // Handle initial navigation if itemId is provided
    androidx.compose.runtime.LaunchedEffect(initialItemId) {
        if (initialItemId != null && currentUser != null && hasSeenOnboarding == true) {
            navController.navigate("details/$initialItemId")
        }
    }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // Determine start destination based on onboarding, authentication status and shared content
    val startDestination = when {
        hasSeenOnboarding == null -> "loading" // Wait for DataStore
        hasSeenOnboarding == false -> "onboarding"
        currentUser == null -> "signUp"
        shareContent is ShareContent.Unknown -> "home"
        else -> "capture"
    }

    val isLargeScreen = windowWidthSizeClass == WindowWidthSizeClass.Expanded

    var showCaptureOptions by remember { mutableStateOf(false) }
    val capturingViewModel: CapturingViewModel = org.koin.androidx.compose.koinViewModel()

    // Map routes to display names for the navigation components
    val screenName = when (currentRoute) {
        "home" -> "Home"
        "inbox" -> "Inbox"
        "reminders" -> "Reminders"
        "profile" -> "Profile"
        else -> null
    }

    Row(
        modifier = Modifier.fillMaxSize()
    ) {
        if (isLargeScreen && screenName != null) {
            RemindlyNavigationRail(
                currentScreen = screenName,
                onHomeClick = { navController.navigate("home") {
                    popUpTo("home") { inclusive = true }
                } },
                onInboxClick = { navController.navigate("inbox") },
                onRemindersClick = { navController.navigate("reminders") },
                onProfileClick = { navController.navigate("profile") },
                onAddClick = { showCaptureOptions = true }
            )
        }

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (!isLargeScreen && screenName != null) {
                    RemindlyBottomNavigation(
                        currentScreen = screenName,
                        onHomeClick = { navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                        } },
                        onInboxClick = { navController.navigate("inbox") },
                        onRemindersClick = { navController.navigate("reminders") },
                        onProfileClick = { navController.navigate("profile") }
                    )
                }
            },
            floatingActionButton = {
                // Show FAB on main screens if not on large screen (where it's in the rail)
                val mainScreens = listOf("home", "inbox", "reminders", "profile")
                val showFab = currentRoute in mainScreens && !isLargeScreen
                if (showFab) {
                    FloatingActionButton(
                        onClick = { showCaptureOptions = true },
                        containerColor = Color(0xFF2D6A4F),
                        contentColor = Color.White,
                        shape = CircleShape,
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Capture")
                    }
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
                            val destination = if (currentUser == null) "signUp" else "home"
                            navController.navigate(destination) {
                                popUpTo("onboarding") { inclusive = true }
                            }
                        }
                    )
                }
                composable("signUp") {
                SignUpScreen(
                    onNavigateToSignIn = { navController.navigate("signIn") },
                    onSignUpSuccess = { 
                        navController.navigate("home") {
                            popUpTo("signUp") { inclusive = true }
                        }
                    },
                    windowWidthSizeClass = windowWidthSizeClass
                )
            }
            composable("signIn") {
                SignInScreen(
                    onNavigateToSignUp = { navController.popBackStack() },
                    onSignInSuccess = {
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
                CategoryDetailScreen(
                    categoryName = categoryName,
                    onBack = { navController.popBackStack() },
                    onItemClick = { itemId -> navController.navigate("details/$itemId") }
                )
            }
            composable("reminders") {
                RemindersScreen(
                    windowWidthSizeClass = windowWidthSizeClass,
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
                            authRepository.signOut()
                            navController.navigate("signUp") {
                                popUpTo(0)
                            }
                        }
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
}
}
}
