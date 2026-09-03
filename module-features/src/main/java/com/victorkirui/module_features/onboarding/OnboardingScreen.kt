package com.victorkirui.module_features.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.victorkirui.core.R
import com.victorkirui.core.ui.theme.*
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

data class OnboardingPage(
    val title: String,
    val description: String,
    @DrawableRes val imageRes: Int,
)

val onboardingPages = listOf(
    OnboardingPage(
        title = "Capture Anything",
        description = "Share links, job posts, or text from any app directly to Remindly. We'll handle the extraction for you.",
        imageRes = R.drawable.capture_anything
    ),
    OnboardingPage(
        title = "AI Handles Anything",
        description = "Our intelligent AI automatically identifies deadlines, companies, and summaries so you don't have to type a thing.",
        imageRes = R.drawable.ai
    ),
    OnboardingPage(
        title = "Never Miss a Schedule",
        description = "Receive proactive notifications and countdown alerts. Stay ahead of your deadlines with smart scheduling.",
        imageRes = R.drawable.deadline
    ),
    OnboardingPage(
        title = "Your Opportunity Pipeline",
        description = "A centralized hub for all your captured opportunities. Organize, track, and never let a chance slip away.",
        imageRes = R.drawable.pipeline
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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = FreshSnow
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // App Logo
            Text(
                text = "Remindly",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                ),
                color = Evergreen,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
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
                OnboardingPageContent(onboardingPages[index])
            }

            // Bottom Navigation Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Indicators
                Row(
                    modifier = Modifier.padding(bottom = 32.dp),
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
                        if (pagerState.currentPage < onboardingPages.size - 1) {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            onCompleteOnboarding()
                            onFinished()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Evergreen,
                        contentColor = FreshSnow
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text(
                        text = if (pagerState.currentPage == onboardingPages.size - 1) "Get Started" else "Continue",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Image Container - Large and centered at the top area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp) 
                .height(350.dp), 
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = page.imageRes),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        // Push text down - This fulfills "Push down the texts" while keeping image at top
        Spacer(modifier = Modifier.weight(1f))

        // Text Content - Stretched with alignment
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = page.title,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 30.sp,
                    lineHeight = 38.sp,
                    letterSpacing = (-0.5).sp
                ),
                color = DarkPine,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = page.description,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MutedSage,
                    lineHeight = 24.sp,
                    fontSize = 16.sp
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
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
