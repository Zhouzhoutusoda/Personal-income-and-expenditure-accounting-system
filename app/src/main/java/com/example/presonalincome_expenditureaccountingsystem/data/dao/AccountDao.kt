package com.example.presonalincome_expenditureaccountingsystem.data.dao

import androidx.room.*
import com.example.presonalincome_expenditureaccountingsystem.data.entity.Account
import kotlinx.coroutines.flow.Flow

/**
 * 账本数据访问对象
 */
@Dao
interface AccountDao {

    // ==================== 插入操作 ====================
    
    /**
     * 插入账本
     * @return 插入账本的ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: Account): Long

    // ==================== 更新操作 ====================
    
    /**
     * 更新账本
     */
    @Update
    suspend fun update(account: Account)
    
    /**
     * 设置默认账本（先清除其他账本的默认状态）
     */
    @Query("UPDATE accounts SET isDefault = 0 WHERE isDefault = 1")
    suspend fun clearDefaultAccount()
    
    /**
     * 设置指定账本为默认账本
     */
    @Query("UPDATE accounts SET isDefault = 1 WHERE id = :accountId")
    suspend fun setDefaultAccount(accountId: Long)

    // ==================== 删除操作 ====================
    
    /**
     * 删除账本
     */
    @Delete
    suspend fun delete(account: Account)
    
    /**
     * 根据ID删除账本（不能删除默认账本）
     */
    @Query("DELETE FROM accounts WHERE id = :accountId AND isDefault = 0")
    suspend fun deleteById(accountId: Long): Int

    // ==================== 查询操作 ====================
    
    /**
     * 根据ID查询账本
     */
    @Query("SELECT * FROM accounts WHERE id = :accountId")
    suspend fun getById(accountId: Long): Account?
    
    /**
     * 获取所有账本
     */
    @Query("SELECT * FROM accounts ORDER BY isDefault DESC, createdAt ASC")
    fun getAllAccounts(): Flow<List<Account>>
    
    /**
     * 获取所有账本列表（非Flow）
     */
    @Query("SELECT * FROM accounts ORDER BY isDefault DESC, createdAt ASC")
    suspend fun getAllAccountsList(): List<Account>
    
    /**
     * 获取默认账本
     */
    @Query("SELECT * FROM accounts WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultAccount(): Account?
    
    /**
     * 获取默认账本（Flow）
     */
    @Query("SELECT * FROM accounts WHERE isDefault = 1 LIMIT 1")
    fun getDefaultAccountFlow(): Flow<Account?>
    
    /**
     * 获取账本数量
     */
    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun getAccountCount(): Int
    
    /**
     * 检查账本名称是否已存在
     */
    @Query("SELECT COUNT(*) FROM accounts WHERE name = :name")
    suspend fun isAccountNameExists(name: String): Int
}

