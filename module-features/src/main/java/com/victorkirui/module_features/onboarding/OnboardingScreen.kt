package com.victorkirui.module_features.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.victorkirui.core.R
import com.victorkirui.core.ui.theme.*
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

data class OnboardingPage(
    val title: String,
    val description: String,
    @DrawableRes val imageRes: Int? = null,
    val isNotificationsPage: Boolean = false
)

val onboardingPages = listOf(
    OnboardingPage(
        title = "Stop Losing What Matters",
        description = "Job posts, flyers, or scholarships lost in screenshots and open tabs? Remindly captures and organizes them for you.",
        imageRes = R.drawable.overloaded_onboarding
    ),
    OnboardingPage(
        title = "Share From Any App",
        description = "Tap the share button in your browser, social apps, or messages to send information directly to Remindly.",
        imageRes = R.drawable.capture
    ),
    OnboardingPage(
        title = "Automated in the Background",
        description = "Remindly extracts details, sets smart reminders, and syncs with your calendar automatically.",
        imageRes = R.drawable.automatic
    ),
    OnboardingPage(
        title = "Notifications",
        description = "Customize your preferences to ensure you receive only the notifications that matter to you, avoiding unnecessary disruptions.",
        isNotificationsPage = true
    )
)

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    windowWidthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact,
    viewModel: OnboardingViewModel = koinViewModel()
) {
    OnboardingScreenContent(
        windowWidthSizeClass = windowWidthSizeClass,
        onFinished = onFinished,
        onCompleteOnboarding = { deadlineAlerts, morningBriefing ->
            viewModel.completeOnboarding(deadlineAlerts, morningBriefing)
        }
    )
}

@Composable
fun OnboardingScreenContent(
    windowWidthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact,
    onFinished: () -> Unit,
    onCompleteOnboarding: (Boolean, Boolean) -> Unit
) {
    val context = LocalContext.current
    val isExpanded = windowWidthSizeClass != WindowWidthSizeClass.Compact
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == onboardingPages.size - 1

    var deadlineAlertsEnabled by remember { mutableStateOf(true) }
    var morningBriefingEnabled by remember { mutableStateOf(true) }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        onCompleteOnboarding(deadlineAlertsEnabled, morningBriefingEnabled)
        onFinished()
    }

    val handleFinalCtaClick = {
        val anyNotificationRequested = deadlineAlertsEnabled || morningBriefingEnabled
        if (anyNotificationRequested &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onCompleteOnboarding(deadlineAlertsEnabled, morningBriefingEnabled)
            onFinished()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = FreshSnow
    ) {
        if (isExpanded) {
            // Adaptive 2-Column Split Layout for Tablets and Large Screens
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Column: Hero Illustration / Icon Container
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(12.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = SoftMint
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val currentPage = onboardingPages[pagerState.currentPage]
                        if (currentPage.isNotificationsPage || currentPage.imageRes == null) {
                            NotificationsHeroBadge()
                        } else {
                            Image(
                                painter = painterResource(id = currentPage.imageRes),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Right Column: Header, Pager Content & Actions
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // App Logo Header
                    Text(
                        text = "Remindly",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                        ),
                        color = Evergreen,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Page Content
                    val currentPage = onboardingPages[pagerState.currentPage]
                    if (currentPage.isNotificationsPage) {
                        OnboardingNotificationsContent(
                            deadlineAlertsEnabled = deadlineAlertsEnabled,
                            onDeadlineAlertsChange = { deadlineAlertsEnabled = it },
                            morningBriefingEnabled = morningBriefingEnabled,
                            onMorningBriefingChange = { morningBriefingEnabled = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        OnboardingPageTextContent(currentPage)
                    }

                    // Bottom Controls (Indicators + Action Button)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Page Indicators
                        Row(
                            modifier = Modifier.padding(bottom = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            repeat(onboardingPages.size) { index ->
                                val isSelected = index == pagerState.currentPage
                                val width by animateDpAsState(
                                    targetValue = if (isSelected) 28.dp else 8.dp,
                                    label = "width"
                                )
                                Box(
                                    modifier = Modifier
                                        .height(8.dp)
                                        .width(width)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Evergreen else PaleSage)
                                )
                            }
                        }

                        // Primary Action Button
                        Button(
                            onClick = {
                                if (!isLastPage) {
                                    scope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                } else {
                                    handleFinalCtaClick()
                                }
                            },
                            modifier = Modifier
                                .widthIn(max = 380.dp)
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Evergreen,
                                contentColor = FreshSnow
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                        ) {
                            Text(
                                text = if (isLastPage) "Let's get started!" else "Continue",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
        } else {
            // Standard Single-Column Layout for Phones
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // App Logo Header
                Text(
                    text = "Remindly",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    color = Evergreen,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp, bottom = 4.dp),
                    softWrap = false,
                    maxLines = 1
                )

                // Pager Section
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) { index ->
                    val page = onboardingPages[index]
                    if (page.isNotificationsPage) {
                        OnboardingNotificationsPageContent(
                            deadlineAlertsEnabled = deadlineAlertsEnabled,
                            onDeadlineAlertsChange = { deadlineAlertsEnabled = it },
                            morningBriefingEnabled = morningBriefingEnabled,
                            onMorningBriefingChange = { morningBriefingEnabled = it }
                        )
                    } else {
                        OnboardingStandardPageContent(page)
                    }
                }

                // Bottom Navigation Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 28.dp, top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Page Indicators
                    Row(
                        modifier = Modifier.padding(bottom = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(onboardingPages.size) { index ->
                            val isSelected = index == pagerState.currentPage
                            val width by animateDpAsState(
                                targetValue = if (isSelected) 24.dp else 8.dp,
                                label = "width"
                            )
                            Box(
                                modifier = Modifier
                                    .height(8.dp)
                                    .width(width)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Evergreen else PaleSage)
                            )
                        }
                    }

                    // Primary Button
                    Button(
                        onClick = {
                            if (!isLastPage) {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            } else {
                                handleFinalCtaClick()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Evergreen,
                            contentColor = FreshSnow
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text(
                            text = if (isLastPage) "Let's get started!" else "Continue",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingStandardPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Image Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (page.imageRes != null) {
                Image(
                    painter = painterResource(id = page.imageRes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }

        // Text Content
        OnboardingPageTextContent(page)
    }
}

@Composable
fun OnboardingPageTextContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp)
            .padding(top = 8.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 25.sp,
                lineHeight = 32.sp,
                letterSpacing = (-0.5).sp
            ),
            color = DarkPine,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MutedSage,
                lineHeight = 22.sp,
                fontSize = 15.sp
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun OnboardingNotificationsPageContent(
    deadlineAlertsEnabled: Boolean,
    onDeadlineAlertsChange: (Boolean) -> Unit,
    morningBriefingEnabled: Boolean,
    onMorningBriefingChange: (Boolean) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        NotificationsHeroBadge()

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Notifications",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp
            ),
            color = DarkPine,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Customize your preferences to ensure you receive only the notifications that matter to you, avoiding unnecessary disruptions.",
            style = MaterialTheme.typography.bodyMedium.copy(
                lineHeight = 20.sp,
                fontSize = 14.sp
            ),
            color = MutedSage,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(0.95f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        OnboardingNotificationsContent(
            deadlineAlertsEnabled = deadlineAlertsEnabled,
            onDeadlineAlertsChange = onDeadlineAlertsChange,
            morningBriefingEnabled = morningBriefingEnabled,
            onMorningBriefingChange = onMorningBriefingChange,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun NotificationsHeroBadge() {
    Box(
        contentAlignment = Alignment.TopEnd,
        modifier = Modifier.padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(SoftMint),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = null,
                tint = Evergreen,
                modifier = Modifier.size(44.dp)
            )
        }

        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color(0xFFE74C3C)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "1",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun OnboardingNotificationsContent(
    deadlineAlertsEnabled: Boolean,
    onDeadlineAlertsChange: (Boolean) -> Unit,
    morningBriefingEnabled: Boolean,
    onMorningBriefingChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        NotificationPreferenceCard(
            title = "Deadline alerts",
            subtitle = "Get reminders for captured tasks and deadlines so you don't miss important dates.",
            checked = deadlineAlertsEnabled,
            onCheckedChange = onDeadlineAlertsChange
        )

        NotificationPreferenceCard(
            title = "Daily Briefing",
            subtitle = "Get a morning summary of tasks due today to organize your day.",
            checked = morningBriefingEnabled,
            onCheckedChange = onMorningBriefingChange
        )
    }
}

@Composable
fun NotificationPreferenceCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFF2F5F3),
        border = BorderStroke(1.dp, if (checked) Evergreen.copy(alpha = 0.25f) else Color(0xFFE0E5E2))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = DarkPine
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        lineHeight = 18.sp,
                        fontSize = 13.sp
                    ),
                    color = MutedSage
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = FreshSnow,
                    checkedTrackColor = Evergreen,
                    uncheckedThumbColor = FreshSnow,
                    uncheckedTrackColor = PaleSage
                )
            )
        }
    }
}

@Preview(name = "Phone - Portrait", showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun OnboardingScreenPhonePreview() {
    RemindlyTheme {
        OnboardingScreenContent(
            windowWidthSizeClass = WindowWidthSizeClass.Compact,
            onFinished = {},
            onCompleteOnboarding = { _, _ -> }
        )
    }
}

@Preview(name = "Tablet - Landscape", showBackground = true, device = "spec:width=1280dp,height=800dp")
@Composable
fun OnboardingScreenTabletPreview() {
    RemindlyTheme {
        OnboardingScreenContent(
            windowWidthSizeClass = WindowWidthSizeClass.Expanded,
            onFinished = {},
            onCompleteOnboarding = { _, _ -> }
        )
    }
}
