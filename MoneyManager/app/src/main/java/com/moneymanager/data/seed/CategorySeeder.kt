package com.moneymanager.data.seed

import com.moneymanager.data.entity.CategoryEntity
import com.moneymanager.domain.repository.CategoryRepository

object CategorySeeder {
    suspend fun seed(categoryRepository: CategoryRepository) {
        // *********************** EXPENSE CATEGORIES ***********************
        val expenseCategories = listOf(
            CategorySeed("Food & Dining", "restaurant", listOf(
                SubCategory("Provision", "local_grocery_store"),
                SubCategory("Meat & Fish", "egg_alt"),
                SubCategory("Fruits", "forest"),
                SubCategory("Veggies", "grass"),
                SubCategory("Restaurants/Dining Out", "flatware"),
                SubCategory("Online Delivery", "takeout"),
                SubCategory("Take Away", "fastfood"),
                SubCategory("Snacks & Bakery", "bakery")
            )),
            CategorySeed("Transport", "directions_car", listOf(
                SubCategory("Fuel (Petrol/Diesel)", "local_gas_station"),
                SubCategory("Cab & Uber", "local_taxi"),
                SubCategory("Auto Rickshaw", "electric_moped"),
                SubCategory("Public Transit (Train/Bus)", "directions_bus"),
                SubCategory("Parking & Tolls", "local_parking"),
                SubCategory("Vehicle Maintenance", "build"),
                SubCategory("Bike / Scooter", "motorcycle"),
                SubCategory("Rentals", "airport_shuttle")
            )),
            CategorySeed("Bills & Utilities", "lightbulb", listOf(
                SubCategory("Electricity", "flash_on"),
                SubCategory("Water", "water_drop"),
                SubCategory("Gas", "local_fire_department"),
                SubCategory("Mobile & Internet", "phone_android"),
                SubCategory("Rent", "home"),
                SubCategory("Subscriptions (OTT/Music)", "tv"),
                SubCategory("Garbage & Sewer", "delete")
            )),
            CategorySeed("Shopping", "shopping_bag", listOf(
                SubCategory("Clothing & Shoes", "checkroom"),
                SubCategory("Electronics", "devices"),
                SubCategory("Home Goods & Appliances", "chair"),
                SubCategory("Online Shopping", "inventory")
            )),
            CategorySeed("Health", "medical_services", listOf(
                SubCategory("Doctor & Hospital", "local_hospital"),
                SubCategory("Pharmacy / Medicine", "local_pharmacy"),
                SubCategory("Dental", "medical_services"),
                SubCategory("Mental Health", "psychology"),
                SubCategory("Fitness & Gym", "fitness_center"),
                SubCategory("Lab Tests / Diagnostics", "science")
            )),
            CategorySeed("Entertainment", "movie", listOf(
                SubCategory("Movies", "theater_comedy"),
                SubCategory("Games", "sports_esports"),
                SubCategory("Events / Concerts", "music_note"),
                SubCategory("Streaming Services", "live_tv")
            )),
            CategorySeed("Travel", "flight", listOf(
                SubCategory("Flights", "airplane_ticket"),
                SubCategory("Hotel / Accommodation", "hotel"),
                SubCategory("Holiday Activities", "beach_access"),
                SubCategory("Luggage & Gear", "luggage")
            )),
            CategorySeed("Family & Social", "people", listOf(
                SubCategory("Sent to Family", "favorite"),
                SubCategory("Gifts", "card_giftcard"),
                SubCategory("Social Outings", "celebration"),
                SubCategory("Charity / Donation", "volunteer_activism"),
                SubCategory("Childcare (Daycare/Babysitting)", "child_care")
            )),
            CategorySeed("Home", "house", listOf(
                SubCategory("Maintenance / Repairs", "handyman"),
                SubCategory("Furniture & Decor", "chair"),
                SubCategory("Cleaning Supplies", "cleaning_services"),
                SubCategory("Gardening", "yard")
            )),
            CategorySeed("Personal Care", "content_cut", listOf(
                SubCategory("Salon & Spa", "spa"),
                SubCategory("Cosmetics / Toiletries", "face"),
                SubCategory("Laundry", "local_laundry"),
                SubCategory("Haircut", "content_cut")
            )),
            CategorySeed("Education", "school", listOf(
                SubCategory("Tuition / Fees", "school"),
                SubCategory("Books & Supplies", "book"),
                SubCategory("Courses & Workshops", "laptop"),
                SubCategory("Student Loan EMI", "account_balance")
            )),
            CategorySeed("Pets", "pets", listOf(
                SubCategory("Pet Food", "pets"),
                SubCategory("Veterinary", "local_hospital"),
                SubCategory("Grooming", "content_cut"),
                SubCategory("Pet Supplies", "toys")
            )),
            CategorySeed("Insurance", "verified_user", listOf(
                SubCategory("Life Insurance", "favorite"),
                SubCategory("Health Insurance", "health_and_safety"),
                SubCategory("Vehicle Insurance", "directions_car"),
                SubCategory("Home Insurance", "home"),
                SubCategory("Travel Insurance", "flight")
            )),
            CategorySeed("Financial & Legal", "account_balance", listOf(
                SubCategory("Credit Card EMI", "credit_card"),
                SubCategory("Personal Loan EMI", "receipt_long"),
                SubCategory("Bank Fees & Charges", "local_atm"),
                SubCategory("Taxes", "description"),
                SubCategory("Legal Fees", "gavel")
            )),
            CategorySeed("One time Purchases", "shopping_cart_checkout", listOf(
                SubCategory("Storable", "inventory"),
                SubCategory("Consumable", "local_mall")
            )),
            CategorySeed("Other Expense", "category", listOf(
                SubCategory("Miscellaneous", "more_horiz"),
                SubCategory("Uncategorized", "category")
            ))
        )

        expenseCategories.forEach { seedCategory(it, "expense", categoryRepository) }

        // *********************** INCOME CATEGORIES ***********************
        val incomeCategories = listOf(
            CategorySeed("Salary", "work", listOf(
                SubCategory("Base Salary", "attach_money"),
                SubCategory("Bonus / Incentives", "redeem"),
                SubCategory("Overtime", "schedule")
            )),
            CategorySeed("Freelance / Business", "computer", listOf(
                SubCategory("Consulting", "description"),
                SubCategory("Product Sales", "sell")
            )),
            CategorySeed("Rental Income", "real_estate_agent", listOf(
                SubCategory("Residential Rent", "home"),
                SubCategory("Commercial Rent", "corporate_fare")
            )),
            CategorySeed("Investment Returns", "trending_up", listOf(
                SubCategory("Dividends", "monetization_on"),
                SubCategory("Interest", "account_balance"),
                SubCategory("Capital Gains", "show_chart")
            )),
            CategorySeed("Refunds & Cashbacks", "payments", listOf(
                SubCategory("Tax Refund", "description"),
                SubCategory("Shopping Refund", "shopping_bag"),
                SubCategory("Cashback / Rewards", "emoji_events")
            )),
            CategorySeed("Other Income", "account_balance_wallet", listOf(
                SubCategory("Gift Received", "card_giftcard"),
                SubCategory("Loan Received", "account_balance"),
                SubCategory("Sale of Asset", "sell")
            ))
        )

        incomeCategories.forEach { seedCategory(it, "income", categoryRepository) }

        // *********************** SAVINGS / INVESTMENT CATEGORIES ***********************
        val savingsCategories = listOf(
            CategorySeed("Fixed Deposit", "account_balance", listOf(
                SubCategory("Bank FD", "account_balance"),
                SubCategory("Corporate FD", "corporate_fare")
            )),
            CategorySeed("Mutual Funds", "analytics", listOf(
                SubCategory("Equity", "trending_up"),
                SubCategory("Debt", "trending_down"),
                SubCategory("ELSS", "bar_chart"),
                SubCategory("Index", "show_chart"),
                SubCategory("Hybrid", "compare_arrows")
            )),
            CategorySeed("Stocks", "show_chart", listOf(
                SubCategory("Equity (Long Term)", "trending_up"),
                SubCategory("F&O", "analytics"),
                SubCategory("IPO", "flag")
            )),
            CategorySeed("Bonds", "description", listOf(
                SubCategory("Govt Bonds", "account_balance"),
                SubCategory("Corporate Bonds", "corporate_fare"),
                SubCategory("Municipal Bonds", "apartment")
            )),
            CategorySeed("Gold & Silver", "workspace_premium", listOf(
                SubCategory("Gold (Physical)", "workspace_premium"),
                SubCategory("Gold ETF", "analytics"),
                SubCategory("Silver", "military_tech")
            )),
            CategorySeed("Real Estate", "real_estate_agent", listOf(
                SubCategory("Residential", "home"),
                SubCategory("Commercial", "corporate_fare"),
                SubCategory("REIT", "show_chart")
            )),
            CategorySeed("NPS", "account_balance", listOf(
                SubCategory("Tier I", "looks_one"),
                SubCategory("Tier II", "looks_two")
            )),
            CategorySeed("PPF", "folder", listOf(
                SubCategory("Public Provident Fund", "flag")
            )),
            CategorySeed("Crypto", "currency_bitcoin", listOf(
                SubCategory("Bitcoin", "currency_bitcoin"),
                SubCategory("Ethereum", "currency_exchange"),
                SubCategory("Altcoins", "category")
            )),
            CategorySeed("Other Savings", "savings", listOf(
                SubCategory("Savings Account", "account_balance"),
                SubCategory("Emergency Fund", "health_and_safety"),
                SubCategory("Cash", "attach_money")
            ))
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
                    emoji = seed.icon,
                    iconType = "material",
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
                        emoji = sub.icon,
                        iconType = "material",
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
        val icon: String,
        val subCategories: List<SubCategory> = emptyList()
    )

    private data class SubCategory(
        val name: String,
        val icon: String
    )
}
