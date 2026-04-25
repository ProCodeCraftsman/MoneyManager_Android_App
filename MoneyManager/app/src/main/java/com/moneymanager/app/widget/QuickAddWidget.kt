package com.moneymanager.app.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.moneymanager.app.MainActivity
import com.moneymanager.app.R

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
                iconRes = R.mipmap.ic_launcher,
                label = "Home",
                color = null,
                onClick = actionStartActivity(
                    Intent(context, MainActivity::class.java).apply {
                        data = Uri.parse("moneymanager://home")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            )

            VerticalDivider()

            // Expense
            WidgetItem(
                iconRes = R.drawable.ic_widget_expense,
                label = "Expense",
                color = Color(0xFFE57373),
                onClick = actionStartActivity(
                    Intent(context, MainActivity::class.java).apply {
                        data = Uri.parse("moneymanager://transactions?type=expense")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            )

            VerticalDivider()

            // Income
            WidgetItem(
                iconRes = R.drawable.ic_widget_income,
                label = "Income",
                color = Color(0xFF81C784),
                onClick = actionStartActivity(
                    Intent(context, MainActivity::class.java).apply {
                        data = Uri.parse("moneymanager://transactions?type=income")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            )

            VerticalDivider()

            // Transfer
            WidgetItem(
                iconRes = R.drawable.ic_widget_transfer,
                label = "Transfer",
                color = Color(0xFF64B5F6),
                onClick = actionStartActivity(
                    Intent(context, MainActivity::class.java).apply {
                        data = Uri.parse("moneymanager://transactions?type=transfer")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            )
        }
    }

    @Composable
    private fun WidgetItem(
        iconRes: Int,
        label: String,
        color: Color?,
        onClick: androidx.glance.action.Action
    ) {
        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .fillMaxSize()
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
                    modifier = GlanceModifier.size(24.dp)
                )
            }
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
                text = label,
                style = TextStyle(
                    fontSize = 10.sp,
                    color = ColorProvider(color ?: Color.White)
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
                .background(Color.Gray.copy(alpha = 0.3f))
        )
    }
}
