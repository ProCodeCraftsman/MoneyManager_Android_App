package com.moneymanager.app.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.text.FontWeight
import androidx.glance.unit.ColorProvider
import androidx.glance.ColorFilter
import com.moneymanager.app.MainActivity
import com.moneymanager.app.R
import androidx.glance.action.Action

class QuickAddWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                WidgetContent(context)
            }
        }
    }

    @Composable
    private fun WidgetContent(context: Context) {
        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ImageProvider(R.drawable.widget_background))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Icon / Home
            WidgetItem(
                modifier = GlanceModifier.defaultWeight(),
                iconRes = R.mipmap.ic_launcher,
                label = "Home",
                color = null,
                onClick = actionStartActivity(
                    Intent(context, MainActivity::class.java).apply {
                        data = "moneymanager://home".toUri()
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            )

            VerticalDivider()

            // Lend
            WidgetItem(
                modifier = GlanceModifier.defaultWeight(),
                iconRes = R.drawable.ic_widget_expense, // Using expense icon for now, 🤝
                label = "Lend",
                color = Color(0xFFE57373),
                onClick = actionStartActivity(
                    Intent(context, MainActivity::class.java).apply {
                        data = "moneymanager://transactions?type=lend".toUri()
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            )

            VerticalDivider()

            // Borrow
            WidgetItem(
                modifier = GlanceModifier.defaultWeight(),
                iconRes = R.drawable.ic_widget_income, // Using income icon for now, 📥
                label = "Borrow",
                color = Color(0xFF81C784),
                onClick = actionStartActivity(
                    Intent(context, MainActivity::class.java).apply {
                        data = "moneymanager://transactions?type=receive".toUri()
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            )

            VerticalDivider()

            // Expense
            WidgetItem(
                modifier = GlanceModifier.defaultWeight(),
                iconRes = R.drawable.ic_widget_expense,
                label = "Expense",
                color = Color(0xFFE57373),
                onClick = actionStartActivity(
                    Intent(context, MainActivity::class.java).apply {
                        data = "moneymanager://transactions?type=expense".toUri()
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            )

            VerticalDivider()

            // Income
            WidgetItem(
                modifier = GlanceModifier.defaultWeight(),
                iconRes = R.drawable.ic_widget_income,
                label = "Income",
                color = Color(0xFF81C784),
                onClick = actionStartActivity(
                    Intent(context, MainActivity::class.java).apply {
                        data = "moneymanager://transactions?type=income".toUri()
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            )

            VerticalDivider()

            // Transfer
            WidgetItem(
                modifier = GlanceModifier.defaultWeight(),
                iconRes = R.drawable.ic_widget_transfer,
                label = "Transfer",
                color = Color(0xFF64B5F6),
                onClick = actionStartActivity(
                    Intent(context, MainActivity::class.java).apply {
                        data = "moneymanager://transfer".toUri()
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            )
        }
    }

    @Composable
    private fun WidgetItem(
        modifier: GlanceModifier = GlanceModifier,
        iconRes: Int,
        label: String,
        color: Color?,
        onClick: Action
    ) {
        Column(
            modifier = modifier
                .fillMaxHeight()
                .padding(4.dp)
                .clickable(onClick),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = GlanceModifier
                    .size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(iconRes),
                    contentDescription = label,
                    modifier = GlanceModifier.size(24.dp),
                    colorFilter = color?.let { ColorFilter.tint(ColorProvider(it)) }
                )
            }
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
                text = label,
                style = TextStyle(
                    fontSize = 10.sp,
                    color = ColorProvider(color ?: Color.White),
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }

    @Composable
    private fun VerticalDivider() {
        Spacer(
            modifier = GlanceModifier
                .width(1.dp)
                .height(24.dp)
                .background(Color.White.copy(alpha = 0.2f))
        )
    }
}
