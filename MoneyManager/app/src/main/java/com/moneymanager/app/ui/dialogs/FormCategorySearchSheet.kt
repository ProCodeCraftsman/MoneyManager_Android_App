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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FormCategorySearchSheet(
    query: String,
    onQueryChange: (String) -> Unit,
    categories: List<CategoryEntity>,
    categoryFilter: String,
    categoryUsageCounts: Map<Long, Int> = emptyMap(),
    accentColor: Color,
    accentContainer: Color,
    onCategorySelected: (CategoryEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val byType = remember(categories, categoryFilter) {
        if (categoryFilter == "all") categories else categories.filter { it.type == categoryFilter }
    }

    val subsByParent = remember(byType) {
        byType.filter { it.parentId != null }.groupBy { it.parentId!! }
    }

    val allParents = remember(byType, subsByParent, categoryUsageCounts) {
        byType.filter { it.parentId == null }.sortedByDescending { cat ->
            val direct = categoryUsageCounts[cat.id] ?: 0
            val subCounts = subsByParent[cat.id]?.sumOf { categoryUsageCounts[it.id] ?: 0 } ?: 0
            direct + subCounts
        }
    }

    val parentIds = remember(allParents) { allParents.map { it.id }.toSet() }
    val orphanSubs = remember(byType, parentIds) {
        byType.filter { it.parentId != null && !parentIds.contains(it.parentId) }
    }

    var expandedParentIds by remember(allParents) {
        mutableStateOf(emptySet<Long>())
    }

    val (displayParents, displaySubsMap, displayOrphans) = remember(allParents, subsByParent, orphanSubs, query) {
        if (query.isBlank()) {
            Triple(allParents, subsByParent, orphanSubs)
        } else {
            val matchedParents = mutableListOf<CategoryEntity>()
            val matchedSubsMap = mutableMapOf<Long, List<CategoryEntity>>()
            allParents.forEach { parent ->
                val subs = subsByParent[parent.id] ?: emptyList()
                val parentMatches = parent.name.contains(query, ignoreCase = true)
                val matchingSubs = subs.filter { it.name.contains(query, ignoreCase = true) }
                if (parentMatches || matchingSubs.isNotEmpty()) {
                    matchedParents.add(parent)
                    matchedSubsMap[parent.id] = if (parentMatches && matchingSubs.isEmpty()) subs else matchingSubs
                }
            }
            val matchedOrphans = orphanSubs.filter { it.name.contains(query, ignoreCase = true) }
            Triple(matchedParents, matchedSubsMap, matchedOrphans)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = accentColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Category, null, tint = accentColor, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("Select Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Search categories...") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                displayParents.forEach { parent ->
                    val subCategories = displaySubsMap[parent.id] ?: emptyList()
                    val hasSubCategories = subCategories.isNotEmpty()
                    val isExpanded = parent.id in expandedParentIds || query.isNotBlank()

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            onClick = { onCategorySelected(parent) },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CategoryIcon(
                                    emoji = parent.emoji,
                                    iconType = parent.iconType,
                                    colorIndex = parent.colorIndex,
                                    fontSize = 22.sp
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        parent.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (hasSubCategories) {
                                        Text(
                                            "${subCategories.size} sub-categories",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                if (hasSubCategories) {
                                    IconButton(
                                        onClick = {
                                            expandedParentIds = if (isExpanded && query.isBlank()) {
                                                expandedParentIds - parent.id
                                            } else {
                                                expandedParentIds + parent.id
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        if (isExpanded && hasSubCategories) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 32.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                subCategories.forEach { subCat ->
                                    Surface(
                                        onClick = { onCategorySelected(subCat) },
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.20f),
                                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CategoryIcon(
                                                emoji = subCat.emoji,
                                                iconType = subCat.iconType,
                                                colorIndex = subCat.colorIndex,
                                                fontSize = 18.sp
                                            )
                                            Spacer(Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    subCat.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (displayOrphans.isNotEmpty()) {
                    Text(
                        "Other Sub-categories",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    displayOrphans.forEach { subCat ->
                        Surface(
                            onClick = { onCategorySelected(subCat) },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.20f),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CategoryIcon(
                                    emoji = subCat.emoji,
                                    iconType = subCat.iconType,
                                    colorIndex = subCat.colorIndex,
                                    fontSize = 18.sp
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    subCat.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
