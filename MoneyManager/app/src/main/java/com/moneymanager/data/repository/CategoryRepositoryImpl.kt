package com.moneymanager.data.repository

import com.moneymanager.data.dao.CategoryDao
import com.moneymanager.data.dao.TagDao
import com.moneymanager.data.entity.CategoryEntity
import com.moneymanager.data.entity.TagEntity
import com.moneymanager.domain.repository.CategoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao,
    private val tagDao: TagDao
) : CategoryRepository {

    override fun getAllCategories(): Flow<List<CategoryEntity>> =
        categoryDao.getAllCategories()

    override fun getAllCategoriesWithArchived(): Flow<List<CategoryEntity>> =
        categoryDao.getAllCategoriesWithArchived()

    override fun getAllTags(): Flow<List<TagEntity>> =
        tagDao.getAllTags()

    override suspend fun getCategoryById(id: Long): CategoryEntity? =
        categoryDao.getCategoryById(id)

    override suspend fun getCategoryByName(name: String, type: String): CategoryEntity? =
        categoryDao.getCategoryByName(name, type)

    override suspend fun categoryNameExists(name: String, type: String): Boolean =
        categoryDao.categoryNameExists(name, type)

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

    override suspend fun reassignCategoryColors(shuffle: Boolean) = withContext(Dispatchers.IO) {
        val allCategories = categoryDao.getAllCategoriesWithArchived().first()
        val parents = allCategories.filter { it.parentId == null }.let {
            if (shuffle) it.shuffled() else it
        }

        parents.forEachIndexed { index, parent ->
            val colorIndex = index % 40
            // Update parent
            categoryDao.updateCategory(parent.copy(colorIndex = colorIndex))
            
            // Update children
            allCategories.filter { it.parentId == parent.id }.forEach { child ->
                categoryDao.updateCategory(child.copy(colorIndex = colorIndex))
            }
        }
    }

    // Sub-category methods
    override fun getParentCategories(): Flow<List<CategoryEntity>> =
        categoryDao.getParentCategories()

    override fun getSubCategories(parentId: Long): Flow<List<CategoryEntity>> =
        categoryDao.getSubCategories(parentId)
}
