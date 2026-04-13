package com.moneymanager.data.repository

import com.moneymanager.data.dao.CategoryDao
import com.moneymanager.data.dao.TagDao
import com.moneymanager.data.entity.CategoryEntity
import com.moneymanager.data.entity.TagEntity
import com.moneymanager.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao,
    private val tagDao: TagDao
) : CategoryRepository {

    override fun getAllCategories(): Flow<List<CategoryEntity>> =
        categoryDao.getAllCategories()

    override fun getAllTags(): Flow<List<TagEntity>> =
        tagDao.getAllTags()

    override fun getTagsByCategory(categoryId: Long): Flow<List<TagEntity>> =
        tagDao.getTagsByCategory(categoryId)

    override suspend fun getCategoryById(id: Long): CategoryEntity? =
        categoryDao.getCategoryById(id)

    override suspend fun getTagById(id: Long): TagEntity? =
        tagDao.getTagById(id)

    override suspend fun insertCategory(category: CategoryEntity): Long =
        categoryDao.insertCategory(category)

    override suspend fun insertTag(tag: TagEntity): Long =
        tagDao.insertTag(tag)

    override suspend fun updateCategory(category: CategoryEntity) =
        categoryDao.updateCategory(category)

    override suspend fun updateTag(tag: TagEntity) =
        tagDao.updateTag(tag)

    override suspend fun deleteCategory(category: CategoryEntity) =
        categoryDao.deleteCategory(category)

    override suspend fun deleteTag(tag: TagEntity) =
        tagDao.deleteTag(tag)

    // Sub-category methods
    override fun getParentCategories(): Flow<List<CategoryEntity>> =
        categoryDao.getParentCategories()

    override fun getSubCategories(parentId: Long): Flow<List<CategoryEntity>> =
        categoryDao.getSubCategories(parentId)
}
