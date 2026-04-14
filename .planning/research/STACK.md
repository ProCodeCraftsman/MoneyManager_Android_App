# Stack Research

**Domain:** Android Finance App - Default Categories & Dashboard Features
**Researched:** April 14, 2026
**Confidence:** HIGH

## Recommended Stack

### Core Technologies (Already in Project)

| Technology | Current Version | Purpose | Status |
|------------|-----------------|---------|--------|
| Kotlin | 1.9.x (implied) | Language | Already validated |
| Jetpack Compose | BOM 2024.12.01 | UI Framework | Already validated |
| Room | 2.6.1 | Local database | Already validated |
| Hilt | 2.52 | DI | Already validated |
| MPAndroidChart | v3.1.0 | Charts | Already validated |
| Firebase Auth | BOM 33.1.2 | Authentication | Already validated |
| WorkManager | 2.9.1 | Background scheduling | Already validated |

### New Additions Required

| Technology | Version | Purpose | Why Recommended |
|------------|---------|---------|-----------------|
| Room Relationships | 2.6.1 (existing) | Hierarchical categories | Self-referential foreign keys enable category/subcategory tree structure |
| YCharts | 2.1.0+ | Compose-native doughnut charts | Native Compose support, better integration than MPAndroidChart for Compose UI |

### Supporting Libraries

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Room (self-referential) | 2.6.1 | Category with parentId | Category/subcategory hierarchy with foreign key to self |
| YCharts | 2.1.0 | Pie/Donut charts | Dashboard doughnut chart visualization |
| WorkManager | 2.9.1 | Periodic reminders | Recurring budget reminders with minimum 15-min intervals |

## Installation

```kotlin
// build.gradle.kts (app module)

// Room - Already at 2.6.1, no change needed
// Self-referential entity pattern:
// @Entity(tableName = "categories")
// data class Category(
//     @PrimaryKey val id: String,
//     val name: String,
//     val parentId: String? = null,  // Foreign key to self
//     @ForeignKey(
//         entity = Category::class,
//         parentColumns = ["id"],
//         childColumns = ["parentId"],
//         onDelete = CASCADE
//     )
// )

// Add YCharts for Compose-native charts (optional - MPAndroidChart already present)
implementation("co.yml:ycharts:2.1.0")

// WorkManager - Already at 2.9.1, no change needed
```

## Alternatives Considered

| Feature | Recommended | Alternative | When to Use Alternative |
|---------|-------------|-------------|-------------------------|
| Charts | MPAndroidChart (existing) | YCharts | Keep MPAndroidChart if VueView usage works; add YCharts only if specific Compose benefits needed |
| Category hierarchy | Room self-referential | Separate junction table | Self-referential is simpler for tree depth 1-2; use junction if deeper nesting needed |
| Reminders | WorkManager (existing) | AlarmManager | WorkManager is correct for recurring - AlarmManager only for precise time (calendar events) |

## What NOT to Use

| Avoid | Why | Use Instead |
|-------|-----|-------------|
| Room 3.x (alpha) | Breaking changes (new package `androidx.room3`), KMP-focused, not stable | Stay on Room 2.8.4 stable for production |
| Custom chart implementation | Time-consuming to build from scratch | Use MPAndroidChart (VueView wrapper) or YCharts |
| AlarmManager for recurring reminders | Wakes from Doze, battery-draining, not efficient | WorkManager PeriodicWorkRequest with constraints |

## Stack Patterns by Feature

### Default Categories with Sub-Categories
- **Pattern:** Self-referential Room entity with `@ForeignKey` to self
- **Implementation:**
  ```kotlin
  @Entity(tableName = "categories")
  data class Category(
      @PrimaryKey val id: String,
      val name: String,
      val icon: String,
      val type: CategoryType, // INCOME or EXPENSE
      val parentId: String? = null,
      val isArchived: Boolean = false,
      val isDefault: Boolean = true //区分默认/自定义
  )
  ```
- **Rationale:** Standard Room pattern for tree structures (documented in Android developer docs)

### Archive Functionality
- **Pattern:** Boolean flag on entity (`isArchived`)
- **Implementation:** Soft delete via flag, exclude from normal queries with `WHERE isArchived = 0`
- **Rationale:** Preserves historical data for transactions while hiding from active UI

### Dashboard with Time Filters
- **Pattern:** Room queries with date range parameters
- **Implementation:** DAO methods with `@Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate")`
- **Rationale:** Standard Room query pattern, no additional library needed

### Doughnut Charts
- **Pattern:** YCharts with `DonutPieChart` or MPAndroidChart with `PieChart`
- **Current:** MPAndroidChart already integrated with VueView
- **Recommendation:** Continue with MPAndroidChart (VueView wrapper) for consistency
- **If issues arise:** YCharts provides native Compose implementation with Material 3 support

### Budget Widget
- **Pattern:** Glance (Jetpack Compose runtime for App Widgets)
- **Implementation:** `androidx.glance` library for Compose-based home screen widgets
- **Note:** Not in current stack - requires evaluation if widget is critical for Phase 1

### Recurring Reminders
- **Pattern:** WorkManager `PeriodicWorkRequest`
- **Implementation:**
  ```kotlin
  val reminderRequest = PeriodicWorkRequestBuilder<ReminderWorker>(
      repeatInterval = 1,
      TimeUnit.DAYS
  )
  .setConstraints(Constraints.Builder()
      .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
      .build())
  .build()
  ```
- **Rationale:** Minimum 15-minute interval, persists across reboots, respects battery optimization

## Version Compatibility

| Package | Current | Compatible With | Notes |
|---------|---------|------------------|-------|
| Room | 2.6.1 | KSP 2.0.21+ | Current KSP version supports Room 2.6.1 |
| Room | 2.6.1 | Kotlin 1.9.x | Compatible with current setup |
| WorkManager | 2.9.1 | Hilt 2.52 | Using hilt-work integration |
| MPAndroidChart | v3.1.0 | Compose BOM 2024.12.01 | Works via VueView wrapper |
| YCharts | 2.1.0 | Compose BOM 2024.12.01 | Native Compose, no wrapper needed |

## Sources

- [Android Developers - Room Relationships](https://developer.android.com/training/data-storage/room/relationships/nested) — Self-referential relations for tree structures
- [Commonsware - Room Tree Structures](https://commonsware.com/AndroidArch/pages/chap-roomrelations-005.html) — Self-referential entity pattern
- [Android Developers - WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) — Official documentation for recurring work
- [Android Developers - Room Releases](https://developer.android.com/jetpack/androidx/releases/room) — Stable version 2.8.4 (current)
- [YCharts GitHub](https://github.com/codeandtheory/YCharts) — Jetpack Compose chart library
- [WorkManager 2025 Patterns](https://medium.com/@hiren6997/workmanager-in-2025-5-patterns-that-actually-work-in-production-fde952c0d095) — Production best practices

---

*Stack research for: Android Finance App - Default Categories & Dashboard*
*Researched: April 14, 2026*
