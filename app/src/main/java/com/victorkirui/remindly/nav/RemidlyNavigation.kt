package com.victorkirui.remindly.nav

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.victorkirui.core.repository.AuthRepository
import androidx.compose.runtime.collectAsState
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RemindlyNavigation(
    shareContent: ShareContent,
    windowWidthSizeClass: WindowWidthSizeClass
) {
    val authRepository: AuthRepository = org.koin.compose.koinInject()
    val currentUser by authRepository.currentUser.collectAsState()
    val scope = rememberCoroutineScope()

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // Determine start destination based on authentication status and shared content
    val startDestination = when {
        currentUser == null -> "signUp"
        shareContent is ShareContent.Unknown -> "home"
        else -> "capture"
    }

    val isLargeScreen = windowWidthSizeClass == WindowWidthSizeClass.Expanded

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
                onAddClick = { /* Global Capture Action */ }
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
                    val capturingViewModel: com.victorkirui.module_features.capturing.CapturingViewModel = org.koin.androidx.compose.koinViewModel()
                    FloatingActionButton(
                        onClick = { 
                            capturingViewModel.capture(com.victorkirui.core.model.ShareContent.Text("Test capture from FAB at ${java.util.Date()}"))
                        },
                        containerColor = Color(0xFF2D6A4F),
                        contentColor = Color.White,
                        shape = CircleShape,
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Capture")
                    }
                }
            }
        ) { paddingValues ->
            NavHost(
                navController = navController, 
                startDestination = startDestination,
                modifier = Modifier
                    .padding(paddingValues)
                    .consumeWindowInsets(paddingValues)
            ) {
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
                    onNavigateToItem = { itemId -> navController.navigate("details/$itemId") }
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
}
}
