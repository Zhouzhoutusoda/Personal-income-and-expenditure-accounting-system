package com.example.presonalincome_expenditureaccountingsystem.data.repository

import com.example.presonalincome_expenditureaccountingsystem.data.dao.CategoryDao
import com.example.presonalincome_expenditureaccountingsystem.data.entity.Category
import kotlinx.coroutines.flow.Flow

/**
 * 类别仓库
 */
class CategoryRepository(private val categoryDao: CategoryDao) {

    // ==================== 基础 CRUD ====================
    
    /**
     * 添加类别
     */
    suspend fun insert(category: Category): Long {
        return categoryDao.insert(category)
    }
    
    /**
     * 更新类别
     */
    suspend fun update(category: Category) {
        categoryDao.update(category)
    }
    
    /**
     * 删除类别（仅限非默认类别）
     * @return 删除的行数，0表示删除失败（可能是默认类别）
     */
    suspend fun deleteById(categoryId: Long): Int {
        return categoryDao.deleteById(categoryId)
    }
    
    /**
     * 根据ID获取类别
     */
    suspend fun getById(categoryId: Long): Category? {
        return categoryDao.getById(categoryId)
    }

    // ==================== 查询操作 ====================
    
    /**
     * 获取所有类别
     */
    fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories()
    }
    
    /**
     * 获取支出类别
     */
    fun getExpenseCategories(): Flow<List<Category>> {
        return categoryDao.getExpenseCategories()
    }
    
    /**
     * 获取收入类别
     */
    fun getIncomeCategories(): Flow<List<Category>> {
        return categoryDao.getIncomeCategories()
    }
    
    /**
     * 获取支出类别列表
     */
    suspend fun getExpenseCategoriesList(): List<Category> {
        return categoryDao.getExpenseCategoriesList()
    }
    
    /**
     * 获取收入类别列表
     */
    suspend fun getIncomeCategoriesList(): List<Category> {
        return categoryDao.getIncomeCategoriesList()
    }

    // ==================== 辅助方法 ====================
    
    /**
     * 添加自定义类别
     */
    suspend fun addCustomCategory(name: String, icon: String, type: Int): Long {
        // 检查名称是否已存在
        if (categoryDao.isCategoryNameExists(name, type) > 0) {
            return -1L // 表示名称已存在
        }
        
        // 获取最大排序值
        val maxSortOrder = categoryDao.getMaxSortOrder(type)
        
        val category = Category(
            name = name,
            icon = icon,
            type = type,
            isDefault = false,
            sortOrder = maxSortOrder + 1
        )
        
        return categoryDao.insert(category)
    }
    
    /**
     * 获取类别数量
     */
    suspend fun getCategoryCount(): Int {
        return categoryDao.getCategoryCount()
    }
}

