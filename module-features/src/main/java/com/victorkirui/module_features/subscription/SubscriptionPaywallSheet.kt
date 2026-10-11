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
import androidx.compose.ui.draw.scale
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
    val billingState by billingManager.billingState.collectAsState()

    var errorDialogMessage by remember { mutableStateOf<String?>(null) }
    var pendingDialogMessage by remember { mutableStateOf<String?>(null) }


    LaunchedEffect(Unit) {
        billingManager.resetBillingState()
        billingManager.querySubscriptions()
    }

    LaunchedEffect(billingState) {
        when (val state = billingState) {
            is com.victorkirui.core.billing.BillingState.PurchaseSuccess -> {
                billingManager.resetBillingState()
                onDismiss()
                onPlanSelected(SubscriptionTier.PREMIUM)
            }
            is com.victorkirui.core.billing.BillingState.PurchasePending -> {
                pendingDialogMessage = "Your payment requires additional processing time (e.g. bank transfer or delayed payment). Your Remindly Pro features will activate automatically as soon as Google Play confirms payment completion."
            }
            is com.victorkirui.core.billing.BillingState.Error -> {
                errorDialogMessage = state.message
            }
            else -> {}
        }
    }

    if (errorDialogMessage != null) {
        AlertDialog(
            onDismissRequest = {
                errorDialogMessage = null
                billingManager.resetBillingState()
            },
            title = { Text("Payment Unsuccessful", fontWeight = FontWeight.Bold) },
            text = { Text(errorDialogMessage ?: "") },
            confirmButton = {
                TextButton(onClick = {
                    errorDialogMessage = null
                    billingManager.resetBillingState()
                }) {
                    Text("OK", fontWeight = FontWeight.Bold, color = Color(0xFF2D6A4F))
                }
            }
        )
    }

    if (pendingDialogMessage != null) {
        AlertDialog(
            onDismissRequest = {
                pendingDialogMessage = null
                billingManager.resetBillingState()
            },
            title = { Text("Payment Processing", fontWeight = FontWeight.Bold) },
            text = { Text(pendingDialogMessage ?: "") },
            confirmButton = {
                TextButton(onClick = {
                    pendingDialogMessage = null
                    billingManager.resetBillingState()
                }) {
                    Text("Understood", fontWeight = FontWeight.Bold, color = Color(0xFF2D6A4F))
                }
            }
        )
    }

    if (billingState is com.victorkirui.core.billing.BillingState.PurchaseVerifying) {
        androidx.compose.ui.window.Dialog(onDismissRequest = {}) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF2D6A4F),
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Activating Remindly Pro...", fontWeight = FontWeight.Bold, color = Color.Black)
                        Text("Verifying entitlement...", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }
        }
    }


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
            productDetailsList = productDetailsList,
            onDismiss = onDismiss,
            onSelectPro = { isYearlySelected ->
                val parentProduct = productDetailsList.find { it.productId == BillingManager.PRODUCT_PRO_SUBSCRIPTION }
                val targetBasePlan = if (isYearlySelected) BillingManager.BASE_PLAN_YEARLY else BillingManager.BASE_PLAN_MONTHLY

                if (activity != null && parentProduct != null) {
                    billingManager.launchBillingFlow(activity, parentProduct, targetBasePlan)
                } else {
                    billingManager.querySubscriptions()
                    errorDialogMessage = "Subscription plans are currently loading from Play Store. Please check your network connection and try again."
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
    productDetailsList: List<com.android.billingclient.api.ProductDetails> = emptyList(),
    onDismiss: () -> Unit = {},
    onSelectPro: (isYearly: Boolean) -> Unit = {},
    onSelectFree: () -> Unit = {}
) {
    var isYearlySelected by remember { mutableStateOf(true) }

    val subscriptionProduct = productDetailsList.find { it.productId == BillingManager.PRODUCT_PRO_SUBSCRIPTION }

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
            "Upgrade to Pro for 250 monthly AI extractions, instant calendar sync, and priority AI processing.",
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
            subscriptionProduct = subscriptionProduct,
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
    subscriptionProduct: com.android.billingclient.api.ProductDetails? = null,
    onSelectPro: () -> Unit
) {
    val displayPrice = if (isYearlySelected) {
        subscriptionProduct?.getMonthlyEquivalentFormattedPrice(BillingManager.BASE_PLAN_YEARLY)
    } else {
        subscriptionProduct?.getFormattedPrice(BillingManager.BASE_PLAN_MONTHLY)
    }

    val totalYearlyText = if (isYearlySelected && subscriptionProduct != null) {
        val total = subscriptionProduct.getFormattedPrice(BillingManager.BASE_PLAN_YEARLY)
        if (total != null) "Billed $total/yr" else null
    } else null

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
            if (displayPrice != null) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        displayPrice,
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

                    if (totalYearlyText != null) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            totalYearlyText,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 6.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color(0xFF2D6A4F),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Loading Play Store pricing...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Perks List
            ProPerkItem("250 Monthly AI Extractions (Images & Documents up to 50 pages)")
            ProPerkItem("Gemini AI & Google Assistant Voice Actions")
            ProPerkItem("Instant Auto-Sync to Google Calendar")
            ProPerkItem("Actionable Checklists & Priority Extraction")
            ProPerkItem("25x Higher Rate Limits & Priority Server Queue")

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
            FreePerkItem("10 AI extractions per month", included = true)

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

private fun com.android.billingclient.api.ProductDetails.getOfferForBasePlan(basePlanId: String): com.android.billingclient.api.ProductDetails.SubscriptionOfferDetails? {
    return subscriptionOfferDetails?.find { it.basePlanId == basePlanId }
        ?: subscriptionOfferDetails?.firstOrNull()
}

private fun com.android.billingclient.api.ProductDetails.getFormattedPrice(basePlanId: String): String? {
    return getOfferForBasePlan(basePlanId)
        ?.pricingPhases
        ?.pricingPhaseList
        ?.firstOrNull()
        ?.formattedPrice
}

private fun com.android.billingclient.api.ProductDetails.getMonthlyEquivalentFormattedPrice(basePlanId: String): String? {
    val offer = getOfferForBasePlan(basePlanId) ?: return null
    val phase = offer.pricingPhases.pricingPhaseList.firstOrNull() ?: return null

    val amountMicros = phase.priceAmountMicros
    val currencyCode = phase.priceCurrencyCode

    if (basePlanId == BillingManager.BASE_PLAN_YEARLY && amountMicros > 0) {
        val monthlyMicros = amountMicros / 12
        val doubleAmount = monthlyMicros / 1_000_000.0
        return try {
            val format = java.text.NumberFormat.getCurrencyInstance().apply {
                currency = java.util.Currency.getInstance(currencyCode)
            }
            format.format(doubleAmount)
        } catch (e: Exception) {
            phase.formattedPrice
        }
    }
    return phase.formattedPrice
}

@Composable
fun AnimatedSuccessDialog(
    message: String,
    onDismiss: () -> Unit
) {
    var animateIn by remember { mutableStateOf(false) }
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (animateIn) 1f else 0.2f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        ),
        label = "scale"
    )

    LaunchedEffect(Unit) {
        animateIn = true
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .scale(scale)
                        .background(Color(0xFFEAF2EE), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color(0xFF2D6A4F), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Success",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Welcome to Remindly Pro!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D6A4F))
                ) {
                    Text(
                        "Start Using Pro",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}
