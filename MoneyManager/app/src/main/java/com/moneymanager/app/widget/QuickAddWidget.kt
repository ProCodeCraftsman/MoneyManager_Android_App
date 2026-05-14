package com.moneymanager.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
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
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxHeight()
                    .clickable(onClick = actionStartActivity(
                        Intent(context, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )),
                contentAlignment = Alignment.Center
            ) {
                AppIconContent()
            }

            Box(
                modifier = GlanceModifier
                    .width(1.dp)
                    .height(28.dp)
                    .background(GlanceTheme.colors.outline)
            ) {}

            Box(
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxHeight()
                    .clickable(onClick = actionStartActivity(
                        Intent(context, MainActivity::class.java).apply {
                            data = "moneymanager://add_transaction?type=expense".toUri()
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )),
                contentAlignment = Alignment.Center
            ) {
                AddIconContent()
            }
        }
    }

    @Composable
    private fun AppIconContent() {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                provider = ImageProvider(R.mipmap.ic_app_icon),
                contentDescription = "Summary",
                modifier = GlanceModifier.size(26.dp)
            )
            Spacer(modifier = GlanceModifier.width(6.dp))
            Text(
                text = "Summary",
                style = TextStyle(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = GlanceTheme.colors.onSurface
                )
            )
        }
    }

    @Composable
    private fun AddIconContent() {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_widget_add),
                contentDescription = "Add Expense",
                modifier = GlanceModifier.size(28.dp),
                colorFilter = ColorFilter.tint(GlanceTheme.colors.primary)
            )
            Spacer(modifier = GlanceModifier.width(6.dp))
            Text(
                text = "Add",
                style = TextStyle(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.primary
                )
            )
        }
    }
}
