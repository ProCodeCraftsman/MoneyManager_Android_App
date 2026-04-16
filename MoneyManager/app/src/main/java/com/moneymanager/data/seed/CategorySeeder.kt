package com.moneymanager.data.seed

import com.moneymanager.data.entity.CategoryEntity
import com.moneymanager.domain.repository.CategoryRepository

object CategorySeeder {
    suspend fun seed(categoryRepository: CategoryRepository) {
        // Expense Categories
        val expenseCategories = listOf(
            CategorySeed("Food & Dining", "🍔", listOf("Restaurants", "Online Delivery", "Bakery", "Groceries")),
            CategorySeed("Transport", "🚗", listOf("Fuel", "Cab/Ride Share", "Public Transit")),
            CategorySeed("Shopping", "🛍️", listOf("Clothing", "Electronics", "Home Goods")),
            CategorySeed("Bills & Utilities", "💡", listOf("Electricity", "Internet", "Rent", "Insurance")),
            CategorySeed("Health", "💊", listOf("Doctor", "Pharmacy", "Health Insurance")),
            CategorySeed("Entertainment", "🎬", listOf("Streaming", "Movies", "Games")),
            CategorySeed("Travel", "✈️", listOf("Flights", "Hotel", "Activities")),
            CategorySeed("Education", "📚"),
            CategorySeed("Home", "🏠"),
            CategorySeed("Personal Care", "✂️"),
            CategorySeed("Other Expense", "📦")
        )

        expenseCategories.forEach { seedCategory(it, "expense", categoryRepository) }

        // Income Categories
        val incomeCategories = listOf(
            CategorySeed("Salary", "💼"),
            CategorySeed("Freelance", "💻"),
            CategorySeed("Investment", "📈"),
            CategorySeed("Gift", "🎁"),
            CategorySeed("Other Income", "💰")
        )

        incomeCategories.forEach { seedCategory(it, "income", categoryRepository) }

        // Savings & Investment Categories
        val savingsCategories = listOf(
            CategorySeed("Mutual Funds", "📊", listOf("Equity", "Debt", "ELSS", "Index")),
            CategorySeed("Fixed Deposit", "🏦", listOf("Bank FD", "Corporate FD")),
            CategorySeed("NPS", "🏛️", listOf("Tier I", "Tier II")),
            CategorySeed("Stocks", "📈", listOf("Equity", "F&O")),
            CategorySeed("Bonds", "📜", listOf("Govt Bonds", "Corporate Bonds")),
            CategorySeed("Gold", "🥇", listOf("SGB", "Physical")),
            CategorySeed("PPF", "🏦"),
            CategorySeed("EPF", "💼"),
            CategorySeed("Real Estate", "🏠"),
            CategorySeed("Crypto", "₿"),
            CategorySeed("Other Savings", "💰")
        )

        savingsCategories.forEach { seedCategory(it, "savings", categoryRepository) }
    }

    private suspend fun seedCategory(
        seed: CategorySeed,
        type: String,
        categoryRepository: CategoryRepository
    ) {
        val existing = categoryRepository.getCategoryByName(seed.name, type)
        val categoryId = if (existing == null) {
            categoryRepository.insertCategory(
                CategoryEntity(
                    name = seed.name,
                    emoji = seed.emoji,
                    type = type,
                    isCustom = false
                )
            )
        } else {
            existing.id
        }

        seed.subCategories.forEach { subName ->
            if (categoryRepository.getCategoryByName(subName, type) == null) {
                categoryRepository.insertCategory(
                    CategoryEntity(
                        name = subName,
                        emoji = seed.emoji, // Using parent emoji for sub-categories as they are not specified separately
                        type = type,
                        parentId = categoryId,
                        isCustom = false
                    )
                )
            }
        }
    }

    private data class CategorySeed(
        val name: String,
        val emoji: String,
        val subCategories: List<String> = emptyList()
    )
}
