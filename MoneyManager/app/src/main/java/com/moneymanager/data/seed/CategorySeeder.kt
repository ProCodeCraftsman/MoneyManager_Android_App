package com.moneymanager.data.seed

import com.moneymanager.data.entity.CategoryEntity
import com.moneymanager.domain.repository.CategoryRepository

object CategorySeeder {
    suspend fun seed(categoryRepository: CategoryRepository) {
        // Expense Categories
        val expenseCategories = listOf(
            CategorySeed("Food & Dining", "🍔", listOf(
                SubCategory("Groceries & Provision", "🛒"),
                SubCategory("Meat & Fish", "🥩"),
                SubCategory("Fruits", "🍎"),
                SubCategory("Veggies", "🥬"),
                SubCategory("Restaurants/Dining Out", "🍽️"),
                SubCategory("Takeaway & Delivery", "🥡"),
                SubCategory("Snacks & Tea", "🍪")
            )),
            CategorySeed("Transport", "🚗", listOf(
                SubCategory("Fuel (Petrol/Diesel)", "⛽"),
                SubCategory("Cab & Uber", "🚕"),
                SubCategory("Auto Rickshaw", "🛺"),
                SubCategory("Public Transit (Train/Bus)", "🚌"),
                SubCategory("Parking & Tolls", "🅿️"),
                SubCategory("Vehicle Maintenance", "🔧")
            )),
            CategorySeed("Family & Social", "👨‍👩‍👧‍👦", listOf(
                SubCategory("Sent to Family", "💝"),
                SubCategory("Gifts", "🎁"),
                SubCategory("Social Outings", "🎉"),
                SubCategory("Charity/Donation", "🤝")
            )),
            CategorySeed("Financial & Debt", "🏦", listOf(
                SubCategory("Credit Card EMI", "💳"),
                SubCategory("Bank Fees & Charges", "🏧"),
                SubCategory("Taxes", "📋")
            )),
            CategorySeed("Bills & Utilities", "💡", listOf(
                SubCategory("Electricity", "⚡"),
                SubCategory("Mobile & Internet", "📱"),
                SubCategory("Rent", "🏠"),
                SubCategory("Subscriptions (OTT/Music)", "📺"),
                SubCategory("Water & Gas", "💧")
            )),
            CategorySeed("Health", "💊", listOf(
                SubCategory("Doctor & Hospital", "🏥"),
                SubCategory("Pharmacy/Medicine", "💊"),
                SubCategory("Fitness", "🏋️"),
                SubCategory("Health Insurance", "🛡️")
            )),
            CategorySeed("Shopping", "🛍️", listOf(
                SubCategory("Clothing & Shoes", "👕"),
                SubCategory("Electronics", "📱"),
                SubCategory("Home Goods & Appliances", "🏺")
            )),
            CategorySeed("Entertainment", "🎬", listOf(
                SubCategory("Movies", "🎬"),
                SubCategory("Games", "🎮"),
                SubCategory("Events/Concerts", "🎵")
            )),
            CategorySeed("Travel", "✈️", listOf(
                SubCategory("Flights", "✈️"),
                SubCategory("Hotel/Accommodation", "🏨"),
                SubCategory("Holiday/Activities", "🏖️")
            )),
            CategorySeed("Education", "📚", listOf(
                SubCategory("Tuition/Fees", "🎓"),
                SubCategory("Books & Supplies", "📖"),
                SubCategory("Courses", "💻")
            )),
            CategorySeed("Home", "🏠", listOf(
                SubCategory("Maintenance/Repairs", "🔨"),
                SubCategory("Furniture/Decor", "🛋️"),
                SubCategory("Cleaning Supplies", "🧹")
            )),
            CategorySeed("Personal Care", "✂️", listOf(
                SubCategory("Salon & Spa", "💆"),
                SubCategory("Cosmetics/Toiletries", "💄"),
                SubCategory("Laundry", "👕")
            )),
            CategorySeed("Other Expense", "📦")
        )

        expenseCategories.forEach { seedCategory(it, "expense", categoryRepository) }

        // 🟢 INCOME CATEGORIES

        val incomeCategories = listOf(
            CategorySeed("Salary", "💼", listOf(
                SubCategory("Base Salary", "💵"),
                SubCategory("Bonus/Incentives", "🎁")
            )),
            CategorySeed("Freelance/Business", "💻"),
            CategorySeed("Refunds & Cashbacks", "💸", listOf(
                SubCategory("Tax Refund", "📋"),
                SubCategory("Shopping Refund", "🛍️"),
                SubCategory("Cashback/Rewards", "🏆")
            )),
            CategorySeed("Investment Returns", "📈", listOf(
                SubCategory("Dividends", "💰"),
                SubCategory("Interest", "🏦"),
                SubCategory("Capital Gains", "📈")
            )),
            CategorySeed("Other Income", "💰", listOf(
                SubCategory("Gift Received", "🎁"),
                SubCategory("Loan Received", "🏦")
            ))
        )

        incomeCategories.forEach { seedCategory(it, "income", categoryRepository) }

        // Savings & Investment Categories
        val savingsCategories = listOf(
            CategorySeed("Mutual Funds", "📊", listOf(
                SubCategory("Equity", "📈"),
                SubCategory("Debt", "📉"),
                SubCategory("ELSS", "📊"),
                SubCategory("Index", "📉")
            )),
            CategorySeed("Fixed Deposit", "🏦", listOf(
                SubCategory("Bank FD", "🏛️"),
                SubCategory("Corporate FD", "🏢")
            )),
            CategorySeed("NPS", "🏛️", listOf(
                SubCategory("Tier I", "1️⃣"),
                SubCategory("Tier II", "2️⃣")
            )),
            CategorySeed("Stocks", "📈", listOf(
                SubCategory("Equity", "📈"),
                SubCategory("F&O", "📊")
            )),
            CategorySeed("Bonds", "📜", listOf(
                SubCategory("Govt Bonds", "🏛️"),
                SubCategory("Corporate Bonds", "🏢")
            )),
            CategorySeed("Gold & Silver", "🥇", listOf(
                SubCategory("Gold", "🥇"),
                SubCategory("Silver", "🥈")
            )),
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

        seed.subCategories.forEach { sub ->
            if (categoryRepository.getCategoryByName(sub.name, type) == null) {
                categoryRepository.insertCategory(
                    CategoryEntity(
                        name = sub.name,
                        emoji = sub.emoji,
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
        val subCategories: List<SubCategory> = emptyList()
    )

    private data class SubCategory(
        val name: String,
        val emoji: String
    )
}
