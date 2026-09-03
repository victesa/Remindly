@file:Suppress("RestrictedApi")

package com.victorkirui.remindly.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.victorkirui.core.R
import com.victorkirui.remindly.MainActivity

@Composable
fun ProWidgetPlaceholder(
    title: String = "Remindly Pro Widget"
) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(R.color.widget_bg_white)
            .cornerRadius(24.dp)
            .padding(12.dp)
            .clickable(
                actionStartActivity<MainActivity>(
                    actionParametersOf(WidgetIntentConstants.PARAM_OPEN_PAYWALL to true)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_notification),
                contentDescription = null,
                modifier = GlanceModifier.size(20.dp)
            )

            Spacer(modifier = GlanceModifier.height(4.dp))

            Text(
                text = title,
                style = TextStyle(
                    color = ColorProvider(R.color.widget_green),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            )

            Spacer(modifier = GlanceModifier.height(2.dp))

            Text(
                text = "Tap to unlock Pro widgets",
                style = TextStyle(
                    color = ColorProvider(R.color.widget_gray_label),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}
