package com.moneymanager.app.widget

import android.content.Context
import android.content.Intent
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
import androidx.glance.ColorFilter
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.color.ColorProvider
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
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LogoSection(context)
            VerticalDivider()
            IncomeButton(context)
            VerticalDivider()
            ExpenseButton(context)
        }
    }

    @Composable
    private fun RowScope.LogoSection(context: Context) {
        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .fillMaxHeight()
                .clickable(onClick = actionStartActivity(
                    Intent(context, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                ))
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = GlanceModifier.size(30.dp)
            )
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
                text = "Summary",
                style = TextStyle(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    color = GlanceTheme.colors.onSurface
                )
            )
        }
    }

    @Composable
    private fun RowScope.IncomeButton(context: Context) {
        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .fillMaxHeight()
                .clickable(onClick = actionStartActivity(
                    Intent(context, MainActivity::class.java).apply {
                        data = "moneymanager://add_transaction?type=income".toUri()
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                ))
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_widget_income),
                contentDescription = null,
                modifier = GlanceModifier.size(20.dp),
                colorFilter = ColorFilter.tint(
                    ColorProvider(day = Color(0xFF2E7D32), night = Color(0xFF81C784))
                )
            )
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
                text = "Income",
                style = TextStyle(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    color = ColorProvider(day = Color(0xFF2E7D32), night = Color(0xFF81C784))
                )
            )
        }
    }

    @Composable
    private fun RowScope.ExpenseButton(context: Context) {
        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .fillMaxHeight()
                .clickable(onClick = actionStartActivity(
                    Intent(context, MainActivity::class.java).apply {
                        data = "moneymanager://add_transaction?type=expense".toUri()
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                ))
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_widget_expense),
                contentDescription = null,
                modifier = GlanceModifier.size(20.dp),
                colorFilter = ColorFilter.tint(
                    ColorProvider(day = Color(0xFFC62828), night = Color(0xFFEF9A9A))
                )
            )
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
                text = "Expense",
                style = TextStyle(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    color = ColorProvider(day = Color(0xFFC62828), night = Color(0xFFEF9A9A))
                )
            )
        }
    }

    @Composable
    private fun VerticalDivider() {
        Box(
            modifier = GlanceModifier
                .width(1.dp)
                .height(26.dp)
                .background(GlanceTheme.colors.outline)
        ) {}
    }
}
