package com.example.presonalincome_expenditureaccountingsystem

import android.app.Application
import android.util.Log
import com.example.presonalincome_expenditureaccountingsystem.data.database.AppDatabase
import com.example.presonalincome_expenditureaccountingsystem.data.repository.AccountRepository
import com.example.presonalincome_expenditureaccountingsystem.data.repository.CategoryRepository
import com.example.presonalincome_expenditureaccountingsystem.data.repository.RecordRepository
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 应用程序类
 * 
 * 负责初始化全局资源，如数据库、Repository 等
 */
class AccountingApplication : Application() {
    
    companion object {
        private const val TAG = "AccountingApp"
        
        // 数据库实例
        lateinit var database: AppDatabase
            private set
        
        // Repository 实例
        lateinit var recordRepository: RecordRepository
            private set
        
        lateinit var categoryRepository: CategoryRepository
            private set
        
        lateinit var accountRepository: AccountRepository
            private set
        
        // 检查是否已初始化
        val isInitialized: Boolean
            get() = ::database.isInitialized && 
                    ::recordRepository.isInitialized && 
                    ::categoryRepository.isInitialized && 
                    ::accountRepository.isInitialized
    }
    
    // 应用级别的协程作用域，使用 SupervisorJob 防止子协程失败影响其他协程
    private val applicationScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, throwable ->
            Log.e(TAG, "协程异常: ${throwable.message}", throwable)
        }
    )
    
    override fun onCreate() {
        super.onCreate()
        
        Log.d(TAG, "========================================")
        Log.d(TAG, "应用启动，初始化数据库...")
        Log.d(TAG, "========================================")
        
        // 初始化数据库
        database = AppDatabase.getInstance(this)
        
        // 初始化 Repository
        recordRepository = RecordRepository(database.recordDao())
        categoryRepository = CategoryRepository(database.categoryDao())
        accountRepository = AccountRepository(database.accountDao())
        
        // 验证数据库初始化（使用协程延迟代替线程阻塞）
        applicationScope.launch {
            try {
                // 使用协程延迟代替 Thread.sleep，这样不会阻塞线程
                delay(500)
                
                val categoryCount = categoryRepository.getCategoryCount()
                val accountCount = accountRepository.getAccountCount()
                val recordCount = recordRepository.getRecordCount()
                
                Log.d(TAG, "----------------------------------------")
                Log.d(TAG, "数据库初始化完成！")
                Log.d(TAG, "  - 类别数量: $categoryCount")
                Log.d(TAG, "  - 账本数量: $accountCount")
                Log.d(TAG, "  - 记录数量: $recordCount")
                Log.d(TAG, "----------------------------------------")
                
                // 输出默认账本信息
                val defaultAccount = accountRepository.getDefaultAccount()
                if (defaultAccount != null) {
                    Log.d(TAG, "默认账本: ${defaultAccount.name} (ID: ${defaultAccount.id})")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "数据库初始化验证失败: ${e.message}", e)
            }
        }
    }
}

