# Design Template — Settings Submenu Pages

Use this guide to ensure every settings submenu (Tags, Budgets, Goals, Peers, Recurring, Templates, etc.) shares a single consistent design language derived from `SettingsScreen.kt`.

---

## 1. Scaffold Template

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XxxScreen(
    viewModel: XxxViewModel,
    onNavigateBack: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lazyListState = rememberLazyListState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Screen Title", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { padding ->
        ScrollToTopBox(lazyListState = lazyListState, modifier = Modifier.padding(padding)) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // items here
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}
```

### Key constants

| Property | Value |
|----------|-------|
| LazyColumn horizontal padding | `20.dp` |
| LazyColumn vertical padding | `4.dp` |
| LazyColumn item spacing | `0.dp` (tight rows, use dividers) |
| FAB spacer (bottom) | `80.dp` |

---

## 2. Section Header

```kotlin
@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
    )
}
```

Gap between sections: `Spacer(modifier = Modifier.height(7.dp))`

---

## 3. List Row (clickable, with icon + title + subtitle + trailing)

```kotlin
@Composable
private fun ListRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    showDivider: Boolean = true,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon container
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            // Title + subtitle
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (trailing != null) {
                Spacer(modifier = Modifier.width(8.dp))
                trailing()
            }
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 50.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }
    }
}
```

### Row spacing breakdown

| Element | Value |
|---------|-------|
| Row vertical padding | `5.dp` |
| Icon container size | `36.dp` |
| Icon container shape | `RoundedCornerShape(10.dp)` |
| Icon container bg | `surfaceVariant` |
| Icon tint | `onSurfaceVariant` |
| Icon size | `20.dp` |
| Spacer after icon | `14.dp` |
| Title style | `bodyLarge, Medium` |
| Subtitle style | `bodySmall, onSurfaceVariant` |
| Spacer before trailing | `8.dp` |
| Divider start padding | `50.dp` |
| Divider thickness | `0.5.dp` |
| Divider color | `outlineVariant.copy(alpha = 0.5f)` |

---

## 4. Card Tile (for grid-style quick actions)

```kotlin
@Composable
private fun ActionTile(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
```

Use `Arrangement.spacedBy(8.dp)` for the parent Row of tiles. Tiles use `Modifier.weight(1f)`.

---

## 5. Empty State

```kotlin
if (items.isEmpty()) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "No items yet",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Create items to organize your data",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

---

## 6. Dialog Pattern

```kotlin
AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Dialog Title") },
    text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // fields here
        }
    },
    confirmButton = {
        TextButton(onClick = { ... }, enabled = ...) {
            Text("Save")
        }
    },
    dismissButton = {
        TextButton(onClick = onDismiss) {
            Text("Cancel")
        }
    }
)
```

### Delete confirmation

```kotlin
confirmButton = {
    TextButton(onClick = { viewModel.delete() }) {
        Text("Delete", color = MaterialTheme.colorScheme.error)
    }
}
```

---

## 7. Trailing Widgets

| Widget | When |
|--------|------|
| `Icon(Icons.Default.ChevronRight, ...)` | Navigation to another screen |
| `Switch(checked = ..., onCheckedChange = ...)` | Toggle settings |
| `Text(..., style = bodySmall, color = onSurfaceVariant) + Icon(ChevronRight)` | Current value + drill-down |

---

## 8. Quick Reference: Screens That Need Alignment

| Screen | Current deviations from this guide |
|--------|-----------------------------------|
| TagsScreen.kt | Uses `padding(16.dp)` → should be `padding(horizontal = 20.dp, vertical = 4.dp)`, uses `spacedBy(8.dp)` → should be `spacedBy(0.dp)` with dividers, uses Card per tag item → should use ListRow pattern |
| BudgetsScreen.kt | Uses `padding(horizontal = 16.dp)` → should be `horizontal = 20.dp`, uses `spacedBy(12.dp)` → should use divider rows |
| GoalsScreen.kt | Same deviations as Budgets |
| CategoriesScreen.kt | Already aligned (see `CategoriesScreen.kt` for reference implementation) |
