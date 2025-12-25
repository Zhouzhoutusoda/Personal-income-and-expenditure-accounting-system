package com.example.presonalincome_expenditureaccountingsystem.ui.record

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.presonalincome_expenditureaccountingsystem.AccountingApplication
import com.example.presonalincome_expenditureaccountingsystem.data.entity.Account
import com.example.presonalincome_expenditureaccountingsystem.data.entity.Category
import com.example.presonalincome_expenditureaccountingsystem.data.entity.Record
import com.example.presonalincome_expenditureaccountingsystem.util.AccountManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 记账页面 ViewModel
 */
class RecordViewModel : ViewModel() {
    
    companion object {
        private const val TAG = "RecordViewModel"
    }
    
    private val recordRepository = AccountingApplication.recordRepository
    private val categoryRepository = AccountingApplication.categoryRepository
    private val accountRepository = AccountingApplication.accountRepository
    
    // 当前记录类型：0=支出，1=收入
    private val _currentType = MutableStateFlow(Record.TYPE_EXPENSE)
    val currentType: StateFlow<Int> = _currentType.asStateFlow()
    
    // 支出类别列表
    private val _expenseCategories = MutableStateFlow<List<Category>>(emptyList())
    val expenseCategories: StateFlow<List<Category>> = _expenseCategories.asStateFlow()
    
    // 收入类别列表
    private val _incomeCategories = MutableStateFlow<List<Category>>(emptyList())
    val incomeCategories: StateFlow<List<Category>> = _incomeCategories.asStateFlow()
    
    // 当前选中的类别
    private val _selectedCategory = MutableStateFlow<Category?>(null)
    val selectedCategory: StateFlow<Category?> = _selectedCategory.asStateFlow()
    
    // 当前账本
    private val _currentAccount = MutableStateFlow<Account?>(null)
    val currentAccount: StateFlow<Account?> = _currentAccount.asStateFlow()
    
    // 兼容旧代码
    val defaultAccount: StateFlow<Account?> = _currentAccount
    
    // 保存状态
    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()
    
    init {
        loadCategories()
        loadCurrentAccount()
        observeCurrentAccount()
    }
    
    /**
     * 观察当前账本变化
     */
    private fun observeCurrentAccount() {
        viewModelScope.launch {
            AccountManager.currentAccount.collect { account ->
                if (account != null) {
                    _currentAccount.value = account
                    Log.d(TAG, "当前账本更新: ${account.name}")
                }
            }
        }
    }
    
    /**
     * 加载类别列表
     */
    private fun loadCategories() {
        viewModelScope.launch {
            try {
                val expense = categoryRepository.getExpenseCategoriesList()
                val income = categoryRepository.getIncomeCategoriesList()
                
                _expenseCategories.value = expense
                _incomeCategories.value = income
                
                // 默认选中第一个支出类别
                if (expense.isNotEmpty()) {
                    _selectedCategory.value = expense[0]
                }
                
                Log.d(TAG, "加载类别完成: 支出${expense.size}个, 收入${income.size}个")
                
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e  // 重新抛出协程取消异常
            } catch (e: Exception) {
                Log.e(TAG, "加载类别失败: ${e.message}", e)
            }
        }
    }
    
    /**
     * 加载当前账本
     */
    private fun loadCurrentAccount() {
        viewModelScope.launch {
            try {
                // 从 AccountManager 获取当前账本
                val accountId = AccountManager.getCurrentAccountIdSync()
                val account = accountRepository.getById(accountId)
                    ?: accountRepository.getDefaultAccount()
                
                _currentAccount.value = account
                Log.d(TAG, "加载当前账本: ${account?.name ?: "无"}")
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e  // 重新抛出协程取消异常
            } catch (e: Exception) {
                Log.e(TAG, "加载当前账本失败: ${e.message}", e)
            }
        }
    }
    
    /**
     * 切换记录类型
     */
    fun setType(type: Int) {
        _currentType.value = type
        
        // 切换类型时更新选中的类别
        val categories = if (type == Record.TYPE_EXPENSE) {
            _expenseCategories.value
        } else {
            _incomeCategories.value
        }
        
        if (categories.isNotEmpty()) {
            _selectedCategory.value = categories[0]
        }
    }
    
    /**
     * 选择类别
     */
    fun selectCategory(category: Category) {
        _selectedCategory.value = category
        Log.d(TAG, "选中类别: ${category.name}")
    }
    
    /**
     * 保存记录
     */
    fun saveRecord(amount: Double, date: Long, note: String) {
        val category = _selectedCategory.value
        val account = _currentAccount.value
        
        if (category == null) {
            _saveState.value = SaveState.Error("请选择类别")
            return
        }
        
        if (account == null) {
            _saveState.value = SaveState.Error("请先选择账本")
            return
        }
        
        if (amount <= 0) {
            _saveState.value = SaveState.Error("请输入有效金额")
            return
        }
        
        viewModelScope.launch {
            try {
                _saveState.value = SaveState.Loading
                
                val record = Record(
                    amount = amount,
                    type = _currentType.value,
                    categoryId = category.id,
                    accountId = account.id,
                    date = date,
                    note = note
                )
                
                val recordId = recordRepository.insert(record)
                
                Log.d(TAG, "========================================")
                Log.d(TAG, "✅ 记录保存成功！")
                Log.d(TAG, "  - ID: $recordId")
                Log.d(TAG, "  - 类型: ${if (record.isExpense) "支出" else "收入"}")
                Log.d(TAG, "  - 金额: ¥$amount")
                Log.d(TAG, "  - 类别: ${category.name}")
                Log.d(TAG, "  - 账本: ${account.name}")
                Log.d(TAG, "  - 备注: ${note.ifEmpty { "无" }}")
                Log.d(TAG, "========================================")
                
                // 查询当前记录总数
                val totalCount = recordRepository.getRecordCount()
                Log.d(TAG, "当前记录总数: $totalCount")
                
                _saveState.value = SaveState.Success(
                    if (record.isExpense) "支出" else "收入",
                    amount
                )
                
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e  // 重新抛出协程取消异常
            } catch (e: Exception) {
                Log.e(TAG, "保存记录失败: ${e.message}", e)
                _saveState.value = SaveState.Error("保存失败: ${e.message}")
            }
        }
    }
    
    /**
     * 重置保存状态
     */
    fun resetSaveState() {
        _saveState.value = SaveState.Idle
    }
    
    /**
     * 保存状态密封类
     */
    sealed class SaveState {
        object Idle : SaveState()
        object Loading : SaveState()
        data class Success(val type: String, val amount: Double) : SaveState()
        data class Error(val message: String) : SaveState()
    }
}

