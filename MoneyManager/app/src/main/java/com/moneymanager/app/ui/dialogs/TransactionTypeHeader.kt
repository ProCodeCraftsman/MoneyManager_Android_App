package com.moneymanager.app.ui.dialogs

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import java.io.File
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.moneymanager.app.ui.components.CategoryIcon
import com.moneymanager.app.ui.components.NumericKeypad
import com.moneymanager.app.ui.components.SplitRowCard
import com.moneymanager.app.ui.theme.LocalCategoryColors
import com.moneymanager.app.ui.util.CurrencyUtils
import com.moneymanager.app.ui.util.FileHelper
import com.moneymanager.domain.ai.TransactionDraft
import com.moneymanager.app.ui.util.accountTypeIcon
import com.moneymanager.app.ui.util.evaluateExpression
import com.moneymanager.data.entity.*

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

internal fun getNextCircularTypeId(
    currentTypeId: String,
    swipeDirection: Int,
    types: List<FormTypeConfig> = TransactionFormConfig.allTypes
): String {
    if (types.isEmpty()) return currentTypeId
    val currentIndex = types.indexOfFirst { it.id == currentTypeId }.let { if (it < 0) 0 else it }
    val count = types.size
    val targetIndex = (currentIndex + swipeDirection % count + count) % count
    return types[targetIndex].id
}

internal fun Modifier.circularTypeSwipeable(
    selectedType: String,
    onTypeSelected: (String) -> Unit
): Modifier = this.pointerInput(selectedType) {
    var totalDragX = 0f
    val swipeThresholdPx = 40.dp.toPx()

    detectHorizontalDragGestures(
        onDragStart = { totalDragX = 0f },
        onDragEnd = {
            if (totalDragX < -swipeThresholdPx) {
                val nextType = getNextCircularTypeId(selectedType, 1)
                onTypeSelected(nextType)
            } else if (totalDragX > swipeThresholdPx) {
                val prevType = getNextCircularTypeId(selectedType, -1)
                onTypeSelected(prevType)
            }
            totalDragX = 0f
        },
        onDragCancel = { totalDragX = 0f },
        onHorizontalDrag = { change, dragAmount ->
            change.consume()
            totalDragX += dragAmount
        }
    )
}

// ═══════════════════════════════════════════════════════════════
//  Transaction Type Tabs Header
// ═══════════════════════════════════════════════════════════════

@Composable
internal fun TransactionTypeHeader(
    selectedType: String,
    onTypeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val types = TransactionFormConfig.allTypes
    val categoryColors = LocalCategoryColors.current
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .circularTypeSwipeable(selectedType, onTypeSelected),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        types.forEach { item ->
            val isSelected = selectedType == item.id
            val typeAccent = when (item.id) {
                "expense"  -> Color(0xFFE53935)
                "income"   -> colorScheme.primary
                "savings"  -> colorScheme.tertiary
                "transfer" -> colorScheme.secondary
                "lend", "borrow" -> categoryColors.lending
                else       -> colorScheme.primary
            }

            val bgColor by animateColorAsState(
                if (isSelected) typeAccent else Color.Transparent,
                animationSpec = tween(220),
                label = "tabBg"
            )
            val contentColor by animateColorAsState(
                if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                animationSpec = tween(220),
                label = "tabContent"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgColor)
                    .clickable { onTypeSelected(item.id) }
                    .padding(vertical = 8.dp, horizontal = 2.dp)
            ) {
                Icon(
                    item.icon,
                    contentDescription = item.label,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    item.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
