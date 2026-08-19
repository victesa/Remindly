package com.victorkirui.module_features.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.victorkirui.core.ui.theme.RemindlyTheme
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color
)

val onboardingPages = listOf(
    OnboardingPage(
        title = "Capture Anything",
        description = "Share job posts, screenshots, or PDFs directly to Remindly from any app. We'll handle the rest.",
        icon = Icons.Outlined.Share,
        color = Color(0xFF2D6A4F)
    ),
    OnboardingPage(
        title = "AI-Powered Analysis",
        description = "Our AI automatically extracts deadlines, companies, and summaries so you don't have to type a thing.",
        icon = Icons.Outlined.AutoAwesome,
        color = Color(0xFF40916C)
    ),
    OnboardingPage(
        title = "Smart Reminders",
        description = "Get intelligent countdown alerts (7 days, 2 days, and morning of) so you never miss an application again.",
        icon = Icons.Outlined.NotificationsActive,
        color = Color(0xFF52B788)
    ),
    OnboardingPage(
        title = "Offline First",
        description = "Save captures even without internet. Remindly will sync everything once you're back online.",
        icon = Icons.Outlined.CloudSync,
        color = Color(0xFF74C69D)
    )
)

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = koinViewModel()
) {
    OnboardingScreenContent(
        onFinished = onFinished,
        onCompleteOnboarding = { viewModel.completeOnboarding() }
    )
}

@Composable
fun OnboardingScreenContent(
    onFinished: () -> Unit,
    onCompleteOnboarding: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = Color.White,
        bottomBar = {
            OnboardingBottomBar(
                currentPage = pagerState.currentPage,
                pageCount = onboardingPages.size,
                onNext = {
                    if (pagerState.currentPage < onboardingPages.size - 1) {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        onCompleteOnboarding()
                        onFinished()
                    }
                },
                onSkip = {
                    onCompleteOnboarding()
                    onFinished()
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { index ->
                OnboardingPageContent(onboardingPages[index])
            }
        }
    }
}

@Composable
fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(200.dp),
            shape = CircleShape,
            color = page.color.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = page.icon,
                    contentDescription = null,
                    modifier = Modifier.size(100.dp),
                    tint = page.color
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
    }
}

@Composable
fun OnboardingBottomBar(
    currentPage: Int,
    pageCount: Int,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .navigationBarsPadding(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Pager Indicator
        Row {
            repeat(pageCount) { index ->
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(width = if (index == currentPage) 24.dp else 8.dp, height = 8.dp)
                        .clip(CircleShape)
                        .background(if (index == currentPage) Color(0xFF2D6A4F) else Color.LightGray)
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (currentPage < pageCount - 1) {
                TextButton(onClick = onSkip) {
                    Text("Skip", color = Color.Gray)
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            Button(
                onClick = onNext,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D6A4F))
            ) {
                Text(
                    text = if (currentPage == pageCount - 1) "Get Started" else "Next",
                    fontWeight = FontWeight.Bold
                )
                if (currentPage < pageCount - 1) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OnboardingScreenPreview() {
    RemindlyTheme {
        OnboardingScreenContent(
            onFinished = {},
            onCompleteOnboarding = {}
        )
    }
}
