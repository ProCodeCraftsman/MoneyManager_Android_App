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

// ═══════════════════════════════════════════════════════════════
//  Top Bar & Amount/Date/Account Components
// ═══════════════════════════════════════════════════════════════

@Composable
internal fun DialogTopBar(
    title: String,
    accentColor: Color,
    onBackClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
        }
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        IconButton(onClick = {}) {
            Icon(Icons.Default.MoreVert, "Options", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun FormAmountDateAccountCard(
    modifier: Modifier = Modifier,
    amount: String,
    currency: String,
    selectedDate: Long,
    selectedAccountId: Long?,
    selectedToAccountId: Long?,
    selectedPeerId: Long?,
    type: String,
    accounts: List<AccountEntity>,
    peers: List<PeerContact>,
    expectedReturnDate: Long?,
    showExpectedReturnDate: Boolean,
    accentColor: Color,
    onDateClick: () -> Unit,
    onAccountClick: () -> Unit,
    onToAccountClick: () -> Unit,
    onPeerClick: () -> Unit,
    onExpectedReturnDateClick: () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Amount — left, prominent
            Column(
                modifier = Modifier.weight(1.3f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "AMOUNT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                    fontSize = 9.sp
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        CurrencyUtils.getCurrencySymbol(currency),
                        style = MaterialTheme.typography.titleLarge,
                        color = if (amount.isEmpty())
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        else accentColor,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = amount.ifEmpty { "0" },
                        fontWeight = FontWeight.ExtraBold,
                        color = if (amount.isEmpty())
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 32.sp,
                        lineHeight = 36.sp
                    )
                    Text(
                        "|",
                        color = accentColor,
                        fontWeight = FontWeight.Light,
                        fontSize = 30.sp
                    )
                }
            }

            VerticalDivider(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 2.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
            )

            // Date & Account — right, compact stacked tiles
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CompactInfoTile(
                    icon = Icons.Default.CalendarToday,
                    label = "DATE",
                    value = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(selectedDate)),
                    accentColor = accentColor,
                    onClick = onDateClick
                )
                CompactInfoTile(
                    icon = Icons.Default.AccountBalance,
                    label = if (type == "transfer" || type == "savings") "FROM ACCOUNT" else "ACCOUNT",
                    value = "${accounts.find { it.id == selectedAccountId }?.name ?: "Select Account"} v",
                    accentColor = accentColor,
                    onClick = onAccountClick
                )
                if (type == "transfer" || type == "savings") {
                    CompactInfoTile(
                        icon = Icons.Default.SwapHoriz,
                        label = if (type == "savings") "INVESTMENT ACCOUNT" else "TO ACCOUNT",
                        value = "${accounts.find { it.id == selectedToAccountId }?.name ?: "Select Account"} v",
                        accentColor = accentColor,
                        onClick = onToAccountClick
                    )
                }
                if (type == "lend" || type == "borrow") {
                    CompactInfoTile(
                        icon = Icons.Default.Person,
                        label = "PERSON",
                        value = peers.find { it.id == selectedPeerId }?.effectiveDisplayName ?: "Select Person",
                        accentColor = accentColor,
                        onClick = onPeerClick
                    )
                }
                if (showExpectedReturnDate && (type == "lend" || type == "borrow")) {
                    CompactInfoTile(
                        icon = Icons.AutoMirrored.Filled.EventNote,
                        label = "RETURN DATE",
                        value = expectedReturnDate?.let {
                            SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(it))
                        } ?: "Not set",
                        accentColor = accentColor,
                        onClick = onExpectedReturnDateClick
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactInfoTile(
    icon: ImageVector,
    label: String,
    value: String,
    accentColor: Color,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, modifier = Modifier.size(13.dp), tint = accentColor)
        Spacer(Modifier.width(6.dp))
        Column {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 8.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.5.sp
            )
            Text(
                value,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  Category Section & Shortcuts Carousel
// ═══════════════════════════════════════════════════════════════

@Composable
private fun CategoryTabPill(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    hasValue: Boolean,
    accentColor: Color,
    accentContainer: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) accentColor else if (hasValue) accentContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = if (isSelected) BorderStroke(1.dp, accentColor) else if (hasValue) BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (isSelected) Color.White else if (hasValue) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected || hasValue) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else if (hasValue) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
internal fun FormCategorySection(
    categories: List<CategoryEntity>,
    categoryFilter: String,
    selectedCategoryId: Long?,
    expandedCategoryId: Long?,
    categoryUsageCounts: Map<Long, Int> = emptyMap(),
    hasActiveSpecialFeatures: Boolean,
    accentColor: Color,
    accentContainer: Color,
    features: Set<TransactionFeature>,
    transactionType: String,
    note: String,
    onNoteChange: (String) -> Unit,
    selectedTagIds: Set<Long>,
    tags: List<TagEntity>,
    onToggleTag: (Long) -> Unit,
    isEmiEnabled: Boolean,
    onEmiToggle: (Boolean) -> Unit,
    emiTenure: String,
    onTenureChange: (String) -> Unit,
    receiptData: String?,
    imageAttachmentsEnabled: Boolean,
    onAddReceipt: () -> Unit,
    onPreviewReceipt: () -> Unit,
    splitEnabled: Boolean,
    onOpenSplit: () -> Unit,
    selectedGoalId: Long?,
    goals: List<GoalEntity>,
    onOpenGoal: () -> Unit,
    selectedPeerId: Long?,
    peers: List<PeerContact>,
    onOpenPeer: () -> Unit,
    expectedReturnDate: Long?,
    onOpenReturnDate: () -> Unit,
    activeFeatureTab: String,
    onTabSelected: (String) -> Unit,
    onOpenTagsManager: () -> Unit,
    onCategoryClick: (CategoryEntity) -> Unit,
    onBackClick: () -> Unit,
    onMoreClick: () -> Unit,
    onOpenSpecialFeatures: () -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Tab Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Category Tab
            CategoryTabPill(
                label = "Category",
                icon = Icons.Default.Category,
                isSelected = activeFeatureTab == "category",
                hasValue = selectedCategoryId != null,
                accentColor = accentColor,
                accentContainer = accentContainer,
                onClick = { onTabSelected("category") }
            )

            // 2. Note Tab
            if (TransactionFeature.NOTE in features) {
                CategoryTabPill(
                    label = if (note.isNotBlank()) "Note ✓" else "Note",
                    icon = Icons.AutoMirrored.Filled.Notes,
                    isSelected = activeFeatureTab == "note",
                    hasValue = note.isNotBlank(),
                    accentColor = accentColor,
                    accentContainer = accentContainer,
                    onClick = { onTabSelected("note") }
                )
            }

            // 3. Tags Tab
            if (TransactionFeature.TAGS in features) {
                CategoryTabPill(
                    label = if (selectedTagIds.isNotEmpty()) "Tags (${selectedTagIds.size})" else "Tags",
                    icon = Icons.Default.LocalOffer,
                    isSelected = activeFeatureTab == "tags",
                    hasValue = selectedTagIds.isNotEmpty(),
                    accentColor = accentColor,
                    accentContainer = accentContainer,
                    onClick = { onTabSelected("tags") }
                )
            }

            // 4. Pay via EMI Tab
            if (transactionType == "expense") {
                CategoryTabPill(
                    label = if (isEmiEnabled) "EMI (${emiTenure}m)" else "Pay EMI",
                    icon = Icons.Default.CreditCard,
                    isSelected = activeFeatureTab == "emi",
                    hasValue = isEmiEnabled,
                    accentColor = accentColor,
                    accentContainer = accentContainer,
                    onClick = {
                        onTabSelected("emi")
                        onEmiToggle(true)
                    }
                )
            }

            // 5. Receipt Tab
            if (imageAttachmentsEnabled && TransactionFeature.RECEIPT in features) {
                CategoryTabPill(
                    label = if (receiptData != null) "Receipt ✓" else "Receipt",
                    icon = Icons.Default.Receipt,
                    isSelected = activeFeatureTab == "receipt",
                    hasValue = receiptData != null,
                    accentColor = accentColor,
                    accentContainer = accentContainer,
                    onClick = {
                        onTabSelected("receipt")
                        if (receiptData == null) onAddReceipt()
                    }
                )
            }

            // 6. Split Tab
            if (TransactionFeature.SPLIT in features) {
                CategoryTabPill(
                    label = if (splitEnabled) "Split ✓" else "Split",
                    icon = Icons.Default.CallSplit,
                    isSelected = activeFeatureTab == "split",
                    hasValue = splitEnabled,
                    accentColor = accentColor,
                    accentContainer = accentContainer,
                    onClick = {
                        onTabSelected("split")
                        onOpenSplit()
                    }
                )
            }

            // 7. Goal Tab
            if (TransactionFeature.GOAL in features) {
                val goalName = goals.firstOrNull { it.id == selectedGoalId }?.name
                CategoryTabPill(
                    label = goalName ?: "Goal",
                    icon = Icons.Default.Flag,
                    isSelected = activeFeatureTab == "goal",
                    hasValue = selectedGoalId != null,
                    accentColor = accentColor,
                    accentContainer = accentContainer,
                    onClick = {
                        onTabSelected("goal")
                        onOpenGoal()
                    }
                )
            }

            // 8. Person Tab
            if (TransactionFeature.PEER in features) {
                val peerName = peers.firstOrNull { it.id == selectedPeerId }?.effectiveDisplayName
                CategoryTabPill(
                    label = peerName ?: "Person",
                    icon = Icons.Default.Person,
                    isSelected = activeFeatureTab == "peer",
                    hasValue = selectedPeerId != null,
                    accentColor = accentColor,
                    accentContainer = accentContainer,
                    onClick = {
                        onTabSelected("peer")
                        onOpenPeer()
                    }
                )
            }

            // 9. Return Date Tab
            if (TransactionFeature.RETURN_DATE in features) {
                CategoryTabPill(
                    label = if (expectedReturnDate != null) "Return ✓" else "Return Date",
                    icon = Icons.Default.CalendarToday,
                    isSelected = activeFeatureTab == "return_date",
                    hasValue = expectedReturnDate != null,
                    accentColor = accentColor,
                    accentContainer = accentContainer,
                    onClick = {
                        onTabSelected("return_date")
                        onOpenReturnDate()
                    }
                )
            }
        }

        // Active Tab Content View
        when (activeFeatureTab) {
            "category" -> {
                if (expandedCategoryId != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = onBackClick,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, Modifier.size(12.dp), tint = accentColor)
                            Spacer(Modifier.width(2.dp))
                            Text("Back", style = MaterialTheme.typography.labelSmall, color = accentColor)
                        }
                    }
                }
                CategoryCarousel(
                    categories = categories,
                    type = categoryFilter,
                    selectedCategoryId = selectedCategoryId,
                    expandedCategoryId = expandedCategoryId,
                    categoryUsageCounts = categoryUsageCounts,
                    accentColor = accentColor,
                    accentContainer = accentContainer,
                    onCategoryClick = onCategoryClick,
                    onMoreClick = onMoreClick
                )
            }

            "note" -> {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(0.5.dp, accentColor.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Note & Description", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = note,
                            onValueChange = onNoteChange,
                            placeholder = { Text("Add receipt note or comment...", style = MaterialTheme.typography.bodyMedium) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            maxLines = 3
                        )
                    }
                }
            }

            "tags" -> {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(0.5.dp, accentColor.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Select Tags", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            TextButton(onClick = onOpenTagsManager) {
                                Text("Manage Tags >", style = MaterialTheme.typography.labelSmall, color = accentColor)
                            }
                        }
                        if (tags.isEmpty()) {
                            Text("No tags created yet.", style = MaterialTheme.typography.bodySmall)
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                tags.forEach { tag ->
                                    val isSelected = tag.id in selectedTagIds
                                    Surface(
                                        onClick = { onToggleTag(tag.id) },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) accentColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(0.5.dp, if (isSelected) accentColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                    ) {
                                        Text(
                                            "#${tag.name}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }



            "receipt" -> {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(0.5.dp, accentColor.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (receiptData != null) "Receipt photo attached" else "Attach receipt photo",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(
                            onClick = {
                                if (receiptData != null) onPreviewReceipt() else onAddReceipt()
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                        ) {
                            Text(if (receiptData != null) "Preview / Change" else "Attach Photo")
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun CategoryCarousel(
    categories: List<CategoryEntity>,
    type: String,
    selectedCategoryId: Long?,
    expandedCategoryId: Long?,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    accentContainer: Color = MaterialTheme.colorScheme.primaryContainer,
    categoryUsageCounts: Map<Long, Int> = emptyMap(),
    onCategoryClick: (CategoryEntity) -> Unit,
    onMoreClick: () -> Unit,
) {
    val filtered = remember(categories, type) { categories.filter { it.type == type } }
    val sortedParents = remember(filtered, categoryUsageCounts) {
        val subsByParent = filtered.filter { it.parentId != null }.groupBy { it.parentId }
        filtered.filter { it.parentId == null }.sortedByDescending { cat ->
            val direct = categoryUsageCounts[cat.id] ?: 0
            val subCounts = subsByParent[cat.id]?.sumOf { categoryUsageCounts[it.id] ?: 0 } ?: 0
            direct + subCounts
        }
    }

    val displayCats = remember(sortedParents, expandedCategoryId, filtered) {
        if (expandedCategoryId != null) {
            val subs = filtered.filter { it.parentId == expandedCategoryId }
                .sortedByDescending { categoryUsageCounts[it.id] ?: 0 }
            val parent = sortedParents.firstOrNull { it.id == expandedCategoryId }
            if (parent != null) listOf(parent) + subs else subs
        } else {
            sortedParents
        }
    }

    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        displayCats.forEach { cat ->
            val isSelected = selectedCategoryId == cat.id || expandedCategoryId == cat.id
            val itemBg by animateColorAsState(
                if (isSelected) accentContainer.copy(alpha = 0.35f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                animationSpec = tween(200),
                label = "catBg"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(64.dp)
                    .clickable { onCategoryClick(cat) }
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = itemBg,
                    modifier = Modifier.size(52.dp),
                    border = if (isSelected) BorderStroke(1.5.dp, accentColor) else null,
                    shadowElevation = if (isSelected) 2.dp else 0.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CategoryIcon(
                            emoji = cat.emoji,
                            iconType = cat.iconType,
                            colorIndex = cat.colorIndex,
                            fontSize = 22.sp,
                            tint = if (isSelected) accentColor else Color.Unspecified
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    cat.name,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    fontSize = 10.sp,
                    color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }

        if (expandedCategoryId == null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(64.dp)
                    .clickable { onMoreClick() }
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = "More",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "More",
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


