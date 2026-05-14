package com.moneymanager.domain.repository

import com.moneymanager.data.entity.TemplateEntity
import kotlinx.coroutines.flow.Flow

interface TemplateRepository {
    fun getAllTemplates(): Flow<List<TemplateEntity>>
    suspend fun getTemplateById(id: Long): TemplateEntity?
    suspend fun insertTemplate(template: TemplateEntity): Long
    suspend fun updateTemplate(template: TemplateEntity)
    suspend fun deleteTemplate(template: TemplateEntity)
}
