package com.moneymanager.domain.repository

import com.moneymanager.data.entity.CategoryEntity
import com.moneymanager.data.entity.TagEntity
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getAllCategories(): Flow<List<CategoryEntity>>
    fun getAllCategoriesWithArchived(): Flow<List<CategoryEntity>>
    fun getAllTags(): Flow<List<TagEntity>>
    fun getTagsByCategory(categoryId: Long): Flow<List<TagEntity>>
    // Sub-category methods
    fun getParentCategories(): Flow<List<CategoryEntity>>
    fun getSubCategories(parentId: Long): Flow<List<CategoryEntity>>
    suspend fun getCategoryById(id: Long): CategoryEntity?
    suspend fun getCategoryByName(name: String, type: String): CategoryEntity?
    suspend fun categoryNameExists(name: String, type: String): Boolean
    suspend fun getTagById(id: Long): TagEntity?
    suspend fun insertCategory(category: CategoryEntity): Long
    suspend fun insertTag(tag: TagEntity): Long
    suspend fun updateCategory(category: CategoryEntity)
    suspend fun updateTag(tag: TagEntity)
    suspend fun deleteCategory(category: CategoryEntity)
    suspend fun deleteTag(tag: TagEntity)
}
