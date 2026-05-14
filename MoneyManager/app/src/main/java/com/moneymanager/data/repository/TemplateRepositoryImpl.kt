package com.moneymanager.data.repository

import com.moneymanager.data.dao.TemplateDao
import com.moneymanager.data.entity.TemplateEntity
import com.moneymanager.domain.repository.TemplateRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TemplateRepositoryImpl @Inject constructor(
    private val templateDao: TemplateDao
) : TemplateRepository {

    override fun getAllTemplates(): Flow<List<TemplateEntity>> =
        templateDao.getAllTemplates()

    override suspend fun getTemplateById(id: Long): TemplateEntity? =
        templateDao.getTemplateById(id)

    override suspend fun insertTemplate(template: TemplateEntity): Long =
        templateDao.insertTemplate(template)

    override suspend fun updateTemplate(template: TemplateEntity) =
        templateDao.updateTemplate(template)

    override suspend fun deleteTemplate(template: TemplateEntity) =
        templateDao.deleteTemplate(template)
}
