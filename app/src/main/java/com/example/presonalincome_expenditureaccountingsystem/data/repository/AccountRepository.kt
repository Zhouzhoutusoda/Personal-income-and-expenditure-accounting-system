package com.example.presonalincome_expenditureaccountingsystem.data.repository

import com.example.presonalincome_expenditureaccountingsystem.data.dao.AccountDao
import com.example.presonalincome_expenditureaccountingsystem.data.entity.Account
import kotlinx.coroutines.flow.Flow

/**
 * 账本仓库
 */
class AccountRepository(private val accountDao: AccountDao) {

    // ==================== 基础 CRUD ====================
    
    /**
     * 添加账本
     */
    suspend fun insert(account: Account): Long {
        return accountDao.insert(account)
    }
    
    /**
     * 更新账本
     */
    suspend fun update(account: Account) {
        accountDao.update(account)
    }
    
    /**
     * 删除账本（不能删除默认账本）
     * @return 删除的行数，0表示删除失败（可能是默认账本）
     */
    suspend fun deleteById(accountId: Long): Int {
        return accountDao.deleteById(accountId)
    }
    
    /**
     * 根据ID获取账本
     */
    suspend fun getById(accountId: Long): Account? {
        return accountDao.getById(accountId)
    }

    // ==================== 查询操作 ====================
    
    /**
     * 获取所有账本
     */
    fun getAllAccounts(): Flow<List<Account>> {
        return accountDao.getAllAccounts()
    }
    
    /**
     * 获取所有账本列表
     */
    suspend fun getAllAccountsList(): List<Account> {
        return accountDao.getAllAccountsList()
    }
    
    /**
     * 获取默认账本
     */
    suspend fun getDefaultAccount(): Account? {
        return accountDao.getDefaultAccount()
    }
    
    /**
     * 获取默认账本（Flow）
     */
    fun getDefaultAccountFlow(): Flow<Account?> {
        return accountDao.getDefaultAccountFlow()
    }

    // ==================== 辅助方法 ====================
    
    /**
     * 设置默认账本
     */
    suspend fun setDefaultAccount(accountId: Long) {
        accountDao.clearDefaultAccount()
        accountDao.setDefaultAccount(accountId)
    }
    
    /**
     * 创建新账本
     */
    suspend fun createAccount(name: String, description: String = "", icon: String = "ic_wallet"): Long {
        // 检查名称是否已存在
        if (accountDao.isAccountNameExists(name) > 0) {
            return -1L // 表示名称已存在
        }
        
        val account = Account(
            name = name,
            description = description,
            icon = icon,
            isDefault = false
        )
        
        return accountDao.insert(account)
    }
    
    /**
     * 获取账本数量
     */
    suspend fun getAccountCount(): Int {
        return accountDao.getAccountCount()
    }
}

