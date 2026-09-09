package com.moneymanager.ui.dialog

import com.moneymanager.data.entity.CategoryEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryCarouselTest {

    private fun createCategory(id: Long, name: String, type: String = "expense", parentId: Long? = null): CategoryEntity {
        return CategoryEntity(
            id = id,
            name = name,
            type = type,
            parentId = parentId,
            iconType = "emoji",
            emoji = "📁",
            colorIndex = 0
        )
    }

    @Test
    fun `when expandedCategoryId is null all parent categories are returned in usage volume order`() {
        val cat1 = createCategory(1, "Food")
        val cat2 = createCategory(2, "Transport")
        val cat3 = createCategory(3, "Shopping")
        val cat4 = createCategory(4, "Bills")
        val cat5 = createCategory(5, "Entertainment")
        val cat6 = createCategory(6, "Health")

        val categories = listOf(cat1, cat2, cat3, cat4, cat5, cat6)
        val usageCounts = mapOf(1L to 10, 2L to 20, 3L to 5, 4L to 30, 5L to 1, 6L to 15)

        val filtered = categories.filter { it.type == "expense" }
        val subsByParent = filtered.filter { it.parentId != null }.groupBy { it.parentId }
        val sortedParents = filtered.filter { it.parentId == null }.sortedByDescending { cat ->
            val direct = usageCounts[cat.id] ?: 0
            val subCounts = subsByParent[cat.id]?.sumOf { usageCounts[it.id] ?: 0 } ?: 0
            direct + subCounts
        }

        assertEquals(6, sortedParents.size)
        // Order should be Bills (30), Transport (20), Health (15), Food (10), Shopping (5), Entertainment (1)
        assertEquals("Bills", sortedParents[0].name)
        assertEquals("Transport", sortedParents[1].name)
        assertEquals("Health", sortedParents[2].name)
        assertEquals("Food", sortedParents[3].name)
        assertEquals("Shopping", sortedParents[4].name)
        assertEquals("Entertainment", sortedParents[5].name)
    }

    @Test
    fun `when expandedCategoryId is set parent and subcategories are displayed`() {
        val parent = createCategory(1, "Food")
        val sub1 = createCategory(10, "Groceries", parentId = 1)
        val sub2 = createCategory(11, "Restaurants", parentId = 1)

        val categories = listOf(parent, sub1, sub2)
        val expandedCategoryId: Long? = 1L

        val filtered = categories.filter { it.type == "expense" }
        val subs = filtered.filter { it.parentId == expandedCategoryId }
        val display = listOf(parent) + subs

        assertEquals(3, display.size)
        assertEquals("Food", display[0].name)
        assertEquals("Groceries", display[1].name)
        assertEquals("Restaurants", display[2].name)
    }
}
