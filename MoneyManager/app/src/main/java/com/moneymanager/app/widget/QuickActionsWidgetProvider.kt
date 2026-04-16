package com.moneymanager.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.net.toUri
import com.moneymanager.app.R

class QuickActionsWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
    ) {
        
        val views = RemoteViews(context.packageName, R.layout.widget_quick_actions)

        // Expense
        val expenseIntent = Intent(Intent.ACTION_VIEW, "moneymanager://transactions?type=expense".toUri())
        val expensePendingIntent = PendingIntent.getActivity(
            context, 1, expenseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_expense, expensePendingIntent)

        // Income
        val incomeIntent = Intent(Intent.ACTION_VIEW, "moneymanager://transactions?type=income".toUri())
        val incomePendingIntent = PendingIntent.getActivity(
            context, 2, incomeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_income, incomePendingIntent)

        // Transfer
        val transferIntent = Intent(Intent.ACTION_VIEW, "moneymanager://transfer".toUri())
        val transferPendingIntent = PendingIntent.getActivity(
            context, 3, transferIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_transfer, transferPendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
