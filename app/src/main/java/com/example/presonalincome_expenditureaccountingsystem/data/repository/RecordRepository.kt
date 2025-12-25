package com.example.presonalincome_expenditureaccountingsystem.data.repository

import com.example.presonalincome_expenditureaccountingsystem.data.dao.RecordDao
import com.example.presonalincome_expenditureaccountingsystem.data.entity.CategoryStatistics
import com.example.presonalincome_expenditureaccountingsystem.data.entity.Record
import com.example.presonalincome_expenditureaccountingsystem.data.entity.RecordWithCategory
import kotlinx.coroutines.flow.Flow

/**
 * 收支记录仓库
 * 
 * 作为数据层的统一入口，封装数据访问逻辑
 */
class RecordRepository(private val recordDao: RecordDao) {

    // ==================== 基础 CRUD ====================
    
    /**
     * 添加记录
     */
    suspend fun insert(record: Record): Long {
        return recordDao.insert(record)
    }
    
    /**
     * 更新记录
     */
    suspend fun update(record: Record) {
        recordDao.update(record.copy(updatedAt = System.currentTimeMillis()))
    }
    
    /**
     * 删除记录
     */
    suspend fun delete(record: Record) {
        recordDao.delete(record)
    }
    
    /**
     * 根据ID删除记录
     */
    suspend fun deleteById(recordId: Long) {
        recordDao.deleteById(recordId)
    }
    
    /**
     * 根据ID获取记录
     */
    suspend fun getById(recordId: Long): Record? {
        return recordDao.getById(recordId)
    }

    // ==================== 查询操作 ====================
    
    /**
     * 获取所有记录（带类别信息）
     */
    fun getAllRecordsWithCategory(): Flow<List<RecordWithCategory>> {
        return recordDao.getAllRecordsWithCategory()
    }
    
    /**
     * 获取指定账本的记录
     */
    fun getRecordsByAccount(accountId: Long): Flow<List<RecordWithCategory>> {
        return recordDao.getRecordsByAccount(accountId)
    }
    
    /**
     * 获取指定日期范围的记录
     */
    fun getRecordsByDateRange(startDate: Long, endDate: Long): Flow<List<RecordWithCategory>> {
        return recordDao.getRecordsByDateRange(startDate, endDate)
    }
    
    /**
     * 获取指定账本和日期范围的记录
     */
    fun getRecordsByAccountAndDateRange(
        accountId: Long,
        startDate: Long,
        endDate: Long
    ): Flow<List<RecordWithCategory>> {
        return recordDao.getRecordsByAccountAndDateRange(accountId, startDate, endDate)
    }
    
    /**
     * 搜索记录
     */
    fun searchRecords(keyword: String): Flow<List<RecordWithCategory>> {
        return recordDao.searchRecords(keyword)
    }

    // ==================== 统计操作 ====================
    
    /**
     * 获取日期范围内的总收入
     */
    suspend fun getTotalIncome(startDate: Long, endDate: Long): Double {
        return recordDao.getTotalIncome(startDate, endDate)
    }
    
    /**
     * 获取日期范围内的总支出
     */
    suspend fun getTotalExpense(startDate: Long, endDate: Long): Double {
        return recordDao.getTotalExpense(startDate, endDate)
    }
    
    /**
     * 获取全部总收入
     */
    suspend fun getTotalIncomeAll(): Double {
        return recordDao.getTotalIncomeAll()
    }
    
    /**
     * 获取全部总支出
     */
    suspend fun getTotalExpenseAll(): Double {
        return recordDao.getTotalExpenseAll()
    }
    
    /**
     * 获取指定账本在日期范围内的收支统计
     */
    suspend fun getAccountStatistics(
        accountId: Long,
        startDate: Long,
        endDate: Long
    ): Pair<Double, Double> {
        val income = recordDao.getTotalIncomeByAccount(accountId, startDate, endDate)
        val expense = recordDao.getTotalExpenseByAccount(accountId, startDate, endDate)
        return Pair(income, expense)
    }
    
    /**
     * 获取指定账本在日期范围内的总收入
     */
    suspend fun getTotalIncomeByAccount(accountId: Long, startDate: Long, endDate: Long): Double {
        return recordDao.getTotalIncomeByAccount(accountId, startDate, endDate)
    }
    
    /**
     * 获取指定账本在日期范围内的总支出
     */
    suspend fun getTotalExpenseByAccount(accountId: Long, startDate: Long, endDate: Long): Double {
        return recordDao.getTotalExpenseByAccount(accountId, startDate, endDate)
    }
    
    /**
     * 获取指定账本的全部总收入
     */
    suspend fun getTotalIncomeByAccountAll(accountId: Long): Double {
        return recordDao.getTotalIncomeByAccountAll(accountId)
    }
    
    /**
     * 获取指定账本的全部总支出
     */
    suspend fun getTotalExpenseByAccountAll(accountId: Long): Double {
        return recordDao.getTotalExpenseByAccountAll(accountId)
    }
    
    /**
     * 获取支出类别统计
     */
    suspend fun getExpenseCategoryStatistics(
        startDate: Long,
        endDate: Long
    ): List<CategoryStatistics> {
        val stats = recordDao.getExpenseCategoryStatistics(startDate, endDate)
        val total = stats.sumOf { it.totalAmount }
        
        return if (total > 0) {
            stats.map { 
                it.copy(percentage = (it.totalAmount / total * 100).toFloat())
            }
        } else {
            stats
        }
    }
    
    /**
     * 获取收入类别统计
     */
    suspend fun getIncomeCategoryStatistics(
        startDate: Long,
        endDate: Long
    ): List<CategoryStatistics> {
        val stats = recordDao.getIncomeCategoryStatistics(startDate, endDate)
        val total = stats.sumOf { it.totalAmount }
        
        return if (total > 0) {
            stats.map { 
                it.copy(percentage = (it.totalAmount / total * 100).toFloat())
            }
        } else {
            stats
        }
    }
    
    /**
     * 获取指定账本的支出类别统计
     */
    suspend fun getExpenseCategoryStatisticsByAccount(
        accountId: Long,
        startDate: Long,
        endDate: Long
    ): List<CategoryStatistics> {
        val stats = recordDao.getExpenseCategoryStatisticsByAccount(accountId, startDate, endDate)
        val total = stats.sumOf { it.totalAmount }
        
        return if (total > 0) {
            stats.map { 
                it.copy(percentage = (it.totalAmount / total * 100).toFloat())
            }
        } else {
            stats
        }
    }
    
    /**
     * 获取指定账本的收入类别统计
     */
    suspend fun getIncomeCategoryStatisticsByAccount(
        accountId: Long,
        startDate: Long,
        endDate: Long
    ): List<CategoryStatistics> {
        val stats = recordDao.getIncomeCategoryStatisticsByAccount(accountId, startDate, endDate)
        val total = stats.sumOf { it.totalAmount }
        
        return if (total > 0) {
            stats.map { 
                it.copy(percentage = (it.totalAmount / total * 100).toFloat())
            }
        } else {
            stats
        }
    }
    
    /**
     * 获取记录总数
     */
    suspend fun getRecordCount(): Int {
        return recordDao.getRecordCount()
    }
    
    /**
     * 获取指定账本的记录数量
     */
    suspend fun getRecordCountByAccount(accountId: Long): Int {
        return recordDao.getRecordCountByAccount(accountId)
    }
}

