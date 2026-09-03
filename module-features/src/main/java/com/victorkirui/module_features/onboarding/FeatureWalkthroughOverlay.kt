package com.victorkirui.module_features.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class WalkthroughStep(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val badge: String
)

val walkthroughSteps = listOf(
    WalkthroughStep(
        title = "Quick Capture Anywhere",
        description = "Tap the '+' button to capture URLs, notes, images, or PDFs—or use the Android Share Sheet to send content straight to Remindly from any app!",
        icon = Icons.Outlined.AddCircleOutline,
        badge = "Capture Content"
    ),
    WalkthroughStep(
        title = "Smart Gemini AI Extraction",
        description = "Remindly's AI automatically extracts deadlines, titles, summaries, organizations, and actionable checklist items so you don't type a thing.",
        icon = Icons.Outlined.AutoAwesome,
        badge = "AI Power"
    ),
    WalkthroughStep(
        title = "Proactive Deadline Reminders",
        description = "Stay ahead of your goals with sequence countdown alerts (7 days, 2 days, and morning of) plus your customizable Daily Morning Briefing.",
        icon = Icons.Outlined.NotificationsActive,
        badge = "Smart Alarms"
    ),
    WalkthroughStep(
        title = "Home Screen Widgets",
        description = "Add Calendar and Upcoming Deadline widgets to your phone's Home Screen for at-a-glance updates without opening the app.",
        icon = Icons.Outlined.Widgets,
        badge = "Home Widgets"
    )
)

@Composable
fun FeatureWalkthroughOverlay(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentStepIndex by remember { mutableIntStateOf(0) }
    val currentStep = walkthroughSteps[currentStepIndex]

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* Block touches to background */ },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Row: Badge & Skip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFEAF2EE)
                    ) {
                        Text(
                            text = currentStep.badge,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2D6A4F)
                            )
                        )
                    }

                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "Skip",
                            color = Color.Gray,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Animated Icon Container
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    color = Color(0xFF2D6A4F)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = currentStep.icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Title
                AnimatedContent(
                    targetState = currentStep.title,
                    label = "titleAnimation"
                ) { title ->
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        ),
                        color = Color(0xFF1B4332),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Description
                AnimatedContent(
                    targetState = currentStep.description,
                    label = "descAnimation"
                ) { desc ->
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 22.sp,
                            color = Color(0xFF52796F)
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(walkthroughSteps.size) { index ->
                        val isSelected = index == currentStepIndex
                        val width by animateDpAsState(
                            targetValue = if (isSelected) 24.dp else 8.dp,
                            label = "dotWidth"
                        )
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(if (isSelected) Color(0xFF2D6A4F) else Color(0xFFD8F3DC))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (currentStepIndex > 0) {
                        OutlinedButton(
                            onClick = { currentStepIndex-- },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF2D6A4F)
                            )
                        ) {
                            Text("Back", fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = {
                            if (currentStepIndex < walkthroughSteps.size - 1) {
                                currentStepIndex++
                            } else {
                                onDismiss()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2D6A4F),
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = if (currentStepIndex == walkthroughSteps.size - 1) "Got It!" else "Next",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
