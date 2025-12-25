package com.example.presonalincome_expenditureaccountingsystem.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.presonalincome_expenditureaccountingsystem.AccountingApplication
import com.example.presonalincome_expenditureaccountingsystem.data.entity.Account
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 账本管理器
 * 
 * 负责管理当前选中的账本，使用 SharedPreferences 持久化存储
 * 所有记账、查询操作都基于当前选中的账本
 */
object AccountManager {
    
    private const val TAG = "AccountManager"
    private const val PREFS_NAME = "account_prefs"
    private const val KEY_CURRENT_ACCOUNT_ID = "current_account_id"
    private const val DEFAULT_ACCOUNT_ID = 1L
    
    private lateinit var prefs: SharedPreferences
    
    // 当前账本
    private val _currentAccount = MutableStateFlow<Account?>(null)
    val currentAccount: StateFlow<Account?> = _currentAccount.asStateFlow()
    
    // 当前账本ID
    private val _currentAccountId = MutableStateFlow(DEFAULT_ACCOUNT_ID)
    val currentAccountId: StateFlow<Long> = _currentAccountId.asStateFlow()
    
    /**
     * 初始化账本管理器
     * 需要在 Application.onCreate() 中调用
     */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _currentAccountId.value = prefs.getLong(KEY_CURRENT_ACCOUNT_ID, DEFAULT_ACCOUNT_ID)
        Log.d(TAG, "初始化账本管理器，当前账本ID: ${_currentAccountId.value}")
    }
    
    /**
     * 切换账本
     */
    suspend fun switchAccount(accountId: Long) {
        Log.d(TAG, "切换账本: $accountId")
        
        // 验证账本是否存在
        val account = AccountingApplication.accountRepository.getById(accountId)
        if (account != null) {
            _currentAccountId.value = accountId
            _currentAccount.value = account
            
            // 持久化存储
            prefs.edit().putLong(KEY_CURRENT_ACCOUNT_ID, accountId).apply()
            
            Log.d(TAG, "✅ 切换到账本: ${account.name} (ID: $accountId)")
        } else {
            Log.e(TAG, "❌ 账本不存在: $accountId，回退到默认账本")
            switchToDefaultAccount()
        }
    }
    
    /**
     * 切换到默认账本
     */
    suspend fun switchToDefaultAccount() {
        val defaultAccount = AccountingApplication.accountRepository.getDefaultAccount()
        if (defaultAccount != null) {
            switchAccount(defaultAccount.id)
        } else {
            Log.e(TAG, "❌ 默认账本不存在！")
        }
    }
    
    /**
     * 加载当前账本信息
     */
    suspend fun loadCurrentAccount() {
        val accountId = _currentAccountId.value
        val account = AccountingApplication.accountRepository.getById(accountId)
        
        if (account != null) {
            _currentAccount.value = account
            Log.d(TAG, "加载当前账本: ${account.name}")
        } else {
            Log.w(TAG, "当前账本不存在，切换到默认账本")
            switchToDefaultAccount()
        }
    }
    
    /**
     * 获取当前账本ID（同步方法）
     */
    fun getCurrentAccountIdSync(): Long {
        return _currentAccountId.value
    }
    
    /**
     * 刷新当前账本信息（账本被修改后调用）
     */
    suspend fun refreshCurrentAccount() {
        val accountId = _currentAccountId.value
        val account = AccountingApplication.accountRepository.getById(accountId)
        if (account != null) {
            _currentAccount.value = account
        }
    }
    
    /**
     * 检查账本是否可以删除
     * 不能删除当前正在使用的账本，也不能删除唯一的账本
     */
    suspend fun canDeleteAccount(accountId: Long): Boolean {
        // 检查是否为当前账本
        if (accountId == _currentAccountId.value) {
            return false
        }
        
        // 检查账本数量
        val count = AccountingApplication.accountRepository.getAccountCount()
        return count > 1
    }
}

