package com.example.presonalincome_expenditureaccountingsystem.data.dao

import androidx.room.*
import com.example.presonalincome_expenditureaccountingsystem.data.entity.Category
import kotlinx.coroutines.flow.Flow

/**
 * 类别数据访问对象
 */
@Dao
interface CategoryDao {

    // ==================== 插入操作 ====================
    
    /**
     * 插入单个类别
     * @return 插入类别的ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: Category): Long
    
    /**
     * 批量插入类别
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<Category>): List<Long>

    // ==================== 更新操作 ====================
    
    /**
     * 更新类别
     */
    @Update
    suspend fun update(category: Category)

    // ==================== 删除操作 ====================
    
    /**
     * 删除类别
     */
    @Delete
    suspend fun delete(category: Category)
    
    /**
     * 根据ID删除类别（仅限非默认类别）
     */
    @Query("DELETE FROM categories WHERE id = :categoryId AND isDefault = 0")
    suspend fun deleteById(categoryId: Long): Int

    // ==================== 查询操作 ====================
    
    /**
     * 根据ID查询类别
     */
    @Query("SELECT * FROM categories WHERE id = :categoryId")
    suspend fun getById(categoryId: Long): Category?
    
    /**
     * 获取所有类别
     */
    @Query("SELECT * FROM categories ORDER BY type, sortOrder")
    fun getAllCategories(): Flow<List<Category>>
    
    /**
     * 获取支出类别
     */
    @Query("SELECT * FROM categories WHERE type = 0 ORDER BY sortOrder")
    fun getExpenseCategories(): Flow<List<Category>>
    
    /**
     * 获取收入类别
     */
    @Query("SELECT * FROM categories WHERE type = 1 ORDER BY sortOrder")
    fun getIncomeCategories(): Flow<List<Category>>
    
    /**
     * 获取支出类别列表（非Flow，用于初始化选择）
     */
    @Query("SELECT * FROM categories WHERE type = 0 ORDER BY sortOrder")
    suspend fun getExpenseCategoriesList(): List<Category>
    
    /**
     * 获取收入类别列表（非Flow，用于初始化选择）
     */
    @Query("SELECT * FROM categories WHERE type = 1 ORDER BY sortOrder")
    suspend fun getIncomeCategoriesList(): List<Category>
    
    /**
     * 获取类别数量
     */
    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoryCount(): Int
    
    /**
     * 检查类别名称是否已存在
     */
    @Query("SELECT COUNT(*) FROM categories WHERE name = :name AND type = :type")
    suspend fun isCategoryNameExists(name: String, type: Int): Int
    
    /**
     * 获取最大排序值
     */
    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM categories WHERE type = :type")
    suspend fun getMaxSortOrder(type: Int): Int
}

