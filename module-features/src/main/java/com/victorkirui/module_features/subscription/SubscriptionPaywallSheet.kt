package com.victorkirui.module_features.subscription

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Devices.FOLDABLE
import androidx.compose.ui.tooling.preview.Devices.TABLET
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.victorkirui.core.billing.BillingManager
import com.victorkirui.core.repository.SubscriptionTier
import com.victorkirui.core.ui.theme.RemindlyTheme
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionPaywallSheet(
    onDismiss: () -> Unit,
    onPlanSelected: (SubscriptionTier) -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val subscriptionRepository: com.victorkirui.core.repository.SubscriptionRepository = koinInject()
    val billingManager: BillingManager = koinInject()

    val currentTier by subscriptionRepository.subscriptionTier.collectAsState(initial = SubscriptionTier.FREE)
    val productDetailsList by billingManager.productDetailsList.collectAsState()
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        contentColor = Color.Black,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.LightGray) },
        modifier = Modifier.fillMaxHeight(0.95f)
    ) {
        SubscriptionPaywallContent(
            currentTier = currentTier,
            onDismiss = onDismiss,
            onSelectPro = { isYearlySelected ->
                val targetProduct = if (isYearlySelected) {
                    productDetailsList.find { it.productId == BillingManager.PRODUCT_PRO_YEARLY }
                } else {
                    productDetailsList.find { it.productId == BillingManager.PRODUCT_PRO_MONTHLY }
                }

                if (activity != null && targetProduct != null) {
                    billingManager.launchBillingFlow(activity, targetProduct)
                } else if (com.victorkirui.module_features.BuildConfig.DEBUG) {
                    // Debug Fallback for local testing without Play Store
                    scope.launch {
                        subscriptionRepository.setSubscriptionTier(SubscriptionTier.PREMIUM)
                        onPlanSelected(SubscriptionTier.PREMIUM)
                        Toast.makeText(context, "Upgraded to Remindly Pro (Debug)", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                } else {
                    billingManager.startConnection()
                    Toast.makeText(context, "Connecting to Google Play Store... Please try again in a moment.", Toast.LENGTH_LONG).show()
                }
            },
            onSelectFree = {
                onPlanSelected(SubscriptionTier.FREE)
                onDismiss()
            }
        )
    }
}

@Composable
fun SubscriptionPaywallContent(
    currentTier: SubscriptionTier = SubscriptionTier.FREE,
    onDismiss: () -> Unit = {},
    onSelectPro: (isYearly: Boolean) -> Unit = {},
    onSelectFree: () -> Unit = {}
) {
    var isYearlySelected by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Close Button Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
            }
        }

        // Header Section
        Surface(
            color = Color(0xFFEAF2EE),
            shape = CircleShape
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFF2D6A4F),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "UNLEASH YOUR AI MEMORY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2D6A4F),
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Choose Your Remindly Plan",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Upgrade to Pro for unlimited visual capture, instant calendar sync, and priority AI processing.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = Color.Gray,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Monthly / Yearly Billing Toggle
        BillingCycleToggle(
            isYearlySelected = isYearlySelected,
            onToggle = { isYearlySelected = it }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Pro / Premium Tier Card (Highlighted)
        ProTierCard(
            isYearlySelected = isYearlySelected,
            onSelectPro = { onSelectPro(isYearlySelected) }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Free Tier Card
        FreeTierCard(
            isCurrent = currentTier == SubscriptionTier.FREE,
            onSelectFree = onSelectFree
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Footer / Skip Action
        TextButton(onClick = onSelectFree) {
            Text(
                "Continue with Free Plan for now",
                color = Color.Gray,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun BillingCycleToggle(
    isYearlySelected: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF4F6F4),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (!isYearlySelected) Color.White else Color.Transparent)
                    .clickable { onToggle(false) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Monthly",
                    fontWeight = if (!isYearlySelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (!isYearlySelected) Color.Black else Color.Gray,
                    fontSize = 14.sp
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isYearlySelected) Color(0xFF2D6A4F) else Color.Transparent)
                    .clickable { onToggle(true) },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Yearly",
                        fontWeight = if (isYearlySelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isYearlySelected) Color.White else Color.Gray,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = if (isYearlySelected) Color(0xFFD4AF37) else Color(0xFFEAF2EE),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            "SAVE 33%",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = if (isYearlySelected) Color.White else Color(0xFF2D6A4F),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProTierCard(
    isYearlySelected: Boolean,
    onSelectPro: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFFF9FAF9),
        border = BorderStroke(2.dp, Color(0xFF2D6A4F))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFD4AF37),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Remindly Pro",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Pricing Display
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    if (isYearlySelected) "$3.33" else "$4.99",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF2D6A4F)
                )
                Text(
                    "/ month",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                )

                if (isYearlySelected) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Billed $39.99/yr",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Perks List
            ProPerkItem("Unlimited AI Visual Analysis (Images & Documents up to 50 pages)")
            ProPerkItem("Instant Auto-Sync to Google Calendar")
            ProPerkItem("Actionable Checklists & Priority Extraction")
            ProPerkItem("10x Higher Rate Limits & Priority Server Queue")
            ProPerkItem("Home Screen Quick Access Widgets")
            ProPerkItem("Cloud Backup & Cross-Device Sync")

            Spacer(modifier = Modifier.height(20.dp))

            // Upgrade Button
            Button(
                onClick = onSelectPro,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D6A4F))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isYearlySelected) "Start Pro (Billed Yearly)" else "Start Pro Monthly",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun FreeTierCard(
    isCurrent: Boolean,
    onSelectFree: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFFFFFFFF),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Free Plan",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.Black
                )

                if (isCurrent) {
                    Surface(
                        color = Color(0xFFF0F0F0),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "CURRENT PLAN",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            color = Color.Gray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "$0 / forever",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(12.dp))

            FreePerkItem("Standard AI extraction & basic reminders", included = true)
            FreePerkItem("PDF analysis up to 2 pages", included = true)
            FreePerkItem("Limited to 5 image captures per month", included = true)
            FreePerkItem("Google Calendar Auto-Sync", included = false)
            FreePerkItem("Quick Widgets & Priority Processing", included = false)

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onSelectFree,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
            ) {
                Text(if (isCurrent) "Current Plan" else "Use Free Plan", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ProPerkItem(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            Icons.Default.Check,
            contentDescription = null,
            tint = Color(0xFF2D6A4F),
            modifier = Modifier.size(18.dp).padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Black,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun FreePerkItem(text: String, included: Boolean) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            if (included) Icons.Default.Check else Icons.Default.Close,
            contentDescription = null,
            tint = if (included) Color(0xFF2D6A4F) else Color.LightGray,
            modifier = Modifier.size(18.dp).padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (included) Color.Black else Color.Gray,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            textDecoration = if (!included) TextDecoration.LineThrough else TextDecoration.None
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun SubscriptionPaywallCompactPreview() {
    RemindlyTheme {
        Surface(color = Color.White) {
            SubscriptionPaywallContent()
        }
    }
}

@Preview(showBackground = true, device = FOLDABLE)
@Composable
fun SubscriptionPaywallMediumPreview() {
    RemindlyTheme {
        Surface(color = Color.White) {
            SubscriptionPaywallContent()
        }
    }
}

@Preview(showBackground = true, device = TABLET)
@Composable
fun SubscriptionPaywallExpandedPreview() {
    RemindlyTheme {
        Surface(color = Color.White) {
            SubscriptionPaywallContent()
        }
    }
}
