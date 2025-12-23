package com.example.presonalincome_expenditureaccountingsystem.data.dao

import androidx.room.*
import com.example.presonalincome_expenditureaccountingsystem.data.entity.CategoryStatistics
import com.example.presonalincome_expenditureaccountingsystem.data.entity.Record
import com.example.presonalincome_expenditureaccountingsystem.data.entity.RecordWithCategory
import kotlinx.coroutines.flow.Flow

/**
 * 收支记录数据访问对象
 */
@Dao
interface RecordDao {

    // ==================== 插入操作 ====================
    
    /**
     * 插入单条记录
     * @return 插入记录的ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: Record): Long
    
    /**
     * 批量插入记录
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<Record>): List<Long>

    // ==================== 更新操作 ====================
    
    /**
     * 更新记录
     */
    @Update
    suspend fun update(record: Record)

    // ==================== 删除操作 ====================
    
    /**
     * 删除记录
     */
    @Delete
    suspend fun delete(record: Record)
    
    /**
     * 根据ID删除记录
     */
    @Query("DELETE FROM records WHERE id = :recordId")
    suspend fun deleteById(recordId: Long)
    
    /**
     * 删除账本下所有记录
     */
    @Query("DELETE FROM records WHERE accountId = :accountId")
    suspend fun deleteByAccountId(accountId: Long)
    
    /**
     * 清空所有记录
     */
    @Query("DELETE FROM records")
    suspend fun deleteAll()

    // ==================== 查询操作 ====================
    
    /**
     * 根据ID查询记录
     */
    @Query("SELECT * FROM records WHERE id = :recordId")
    suspend fun getById(recordId: Long): Record?
    
    /**
     * 获取所有记录（按日期降序）
     */
    @Transaction
    @Query("SELECT * FROM records ORDER BY date DESC, createdAt DESC")
    fun getAllRecordsWithCategory(): Flow<List<RecordWithCategory>>
    
    /**
     * 获取指定账本的所有记录
     */
    @Transaction
    @Query("SELECT * FROM records WHERE accountId = :accountId ORDER BY date DESC, createdAt DESC")
    fun getRecordsByAccount(accountId: Long): Flow<List<RecordWithCategory>>
    
    /**
     * 获取指定日期范围内的记录
     */
    @Transaction
    @Query("""
        SELECT * FROM records 
        WHERE date >= :startDate AND date <= :endDate 
        ORDER BY date DESC, createdAt DESC
    """)
    fun getRecordsByDateRange(startDate: Long, endDate: Long): Flow<List<RecordWithCategory>>
    
    /**
     * 获取指定账本和日期范围内的记录
     */
    @Transaction
    @Query("""
        SELECT * FROM records 
        WHERE accountId = :accountId AND date >= :startDate AND date <= :endDate 
        ORDER BY date DESC, createdAt DESC
    """)
    fun getRecordsByAccountAndDateRange(
        accountId: Long,
        startDate: Long,
        endDate: Long
    ): Flow<List<RecordWithCategory>>
    
    /**
     * 搜索记录（按备注模糊搜索）
     */
    @Transaction
    @Query("""
        SELECT * FROM records 
        WHERE note LIKE '%' || :keyword || '%' 
        ORDER BY date DESC, createdAt DESC
    """)
    fun searchRecords(keyword: String): Flow<List<RecordWithCategory>>

    // ==================== 统计查询 ====================
    
    /**
     * 获取指定日期范围内的总收入
     */
    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM records 
        WHERE type = 1 AND date >= :startDate AND date <= :endDate
    """)
    suspend fun getTotalIncome(startDate: Long, endDate: Long): Double
    
    /**
     * 获取指定日期范围内的总支出
     */
    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM records 
        WHERE type = 0 AND date >= :startDate AND date <= :endDate
    """)
    suspend fun getTotalExpense(startDate: Long, endDate: Long): Double
    
    /**
     * 获取全部总收入
     */
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM records WHERE type = 1")
    suspend fun getTotalIncomeAll(): Double
    
    /**
     * 获取全部总支出
     */
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM records WHERE type = 0")
    suspend fun getTotalExpenseAll(): Double
    
    /**
     * 获取指定账本在日期范围内的总收入
     */
    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM records 
        WHERE accountId = :accountId AND type = 1 AND date >= :startDate AND date <= :endDate
    """)
    suspend fun getTotalIncomeByAccount(accountId: Long, startDate: Long, endDate: Long): Double
    
    /**
     * 获取指定账本在日期范围内的总支出
     */
    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM records 
        WHERE accountId = :accountId AND type = 0 AND date >= :startDate AND date <= :endDate
    """)
    suspend fun getTotalExpenseByAccount(accountId: Long, startDate: Long, endDate: Long): Double
    
    /**
     * 获取类别统计（支出）
     */
    @Query("""
        SELECT 
            c.id as categoryId,
            c.name as categoryName,
            c.icon as categoryIcon,
            COALESCE(SUM(r.amount), 0.0) as totalAmount,
            COUNT(r.id) as recordCount,
            0.0 as percentage
        FROM categories c
        LEFT JOIN records r ON c.id = r.categoryId 
            AND r.type = 0 
            AND r.date >= :startDate 
            AND r.date <= :endDate
        WHERE c.type = 0
        GROUP BY c.id
        HAVING totalAmount > 0
        ORDER BY totalAmount DESC
    """)
    suspend fun getExpenseCategoryStatistics(startDate: Long, endDate: Long): List<CategoryStatistics>
    
    /**
     * 获取类别统计（收入）
     */
    @Query("""
        SELECT 
            c.id as categoryId,
            c.name as categoryName,
            c.icon as categoryIcon,
            COALESCE(SUM(r.amount), 0.0) as totalAmount,
            COUNT(r.id) as recordCount,
            0.0 as percentage
        FROM categories c
        LEFT JOIN records r ON c.id = r.categoryId 
            AND r.type = 1 
            AND r.date >= :startDate 
            AND r.date <= :endDate
        WHERE c.type = 1
        GROUP BY c.id
        HAVING totalAmount > 0
        ORDER BY totalAmount DESC
    """)
    suspend fun getIncomeCategoryStatistics(startDate: Long, endDate: Long): List<CategoryStatistics>
    
    /**
     * 获取记录总数
     */
    @Query("SELECT COUNT(*) FROM records")
    suspend fun getRecordCount(): Int
    
    /**
     * 获取指定账本的记录总数
     */
    @Query("SELECT COUNT(*) FROM records WHERE accountId = :accountId")
    suspend fun getRecordCountByAccount(accountId: Long): Int
}

