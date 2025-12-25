package com.example.presonalincome_expenditureaccountingsystem.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.presonalincome_expenditureaccountingsystem.AccountingApplication
import com.example.presonalincome_expenditureaccountingsystem.data.entity.Account
import com.example.presonalincome_expenditureaccountingsystem.data.entity.Category
import com.example.presonalincome_expenditureaccountingsystem.data.entity.Record
import com.example.presonalincome_expenditureaccountingsystem.util.AccountManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

/**
 * 数据备份与恢复工具类
 */
object BackupUtils {
    
    private const val TAG = "BackupUtils"
    
    // 备份文件版本号
    private const val BACKUP_VERSION = 1
    
    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()
    
    /**
     * 备份数据模型
     */
    data class BackupData(
        val version: Int = BACKUP_VERSION,
        val timestamp: Long = System.currentTimeMillis(),
        val accounts: List<Account>,
        val categories: List<Category>,
        val records: List<Record>
    )
    
    /**
     * 导出数据到 JSON 文件（导出当前账本）
     */
    suspend fun exportData(context: Context, uri: Uri): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // 获取当前账本ID
                val currentAccountId = AccountManager.getCurrentAccountIdSync()
                
                Log.d(TAG, "========================================")
                Log.d(TAG, "开始导出当前账本数据 (账本ID: $currentAccountId)...")
                
                // 安全检查
                if (!AccountingApplication.isInitialized) {
                    return@withContext Result.failure(Exception("应用未完全初始化"))
                }
                
                // 获取当前账本
                val currentAccount = AccountingApplication.accountRepository.getById(currentAccountId)
                    ?: return@withContext Result.failure(Exception("当前账本不存在"))
                
                // 获取所有类别（类别是共享的）
                val categories = AccountingApplication.categoryRepository.getAllCategories().first()
                
                // 只获取当前账本的记录
                val records = AccountingApplication.recordRepository.getRecordsByAccount(currentAccountId).first()
                    .map { it.record }
                
                Log.d(TAG, "  - 账本: ${currentAccount.name}")
                Log.d(TAG, "  - 类别: ${categories.size} 个")
                Log.d(TAG, "  - 记录: ${records.size} 条")
                
                // 创建备份数据
                val backupData = BackupData(
                    accounts = listOf(currentAccount),
                    categories = categories,
                    records = records
                )
                
                // 转换为 JSON
                val json = gson.toJson(backupData)
                
                // 写入文件
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        writer.write(json)
                    }
                } ?: return@withContext Result.failure(Exception("无法打开输出流"))
                
                Log.d(TAG, "✅ 数据导出成功！")
                Log.d(TAG, "========================================")
                
                Result.success("成功导出「${currentAccount.name}」的 ${records.size} 条记录")
                
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e  // 重新抛出协程取消异常
            } catch (e: Exception) {
                Log.e(TAG, "❌ 数据导出失败: ${e.message}", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * 导出所有账本数据到 JSON 文件
     */
    suspend fun exportAllData(context: Context, uri: Uri): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "========================================")
                Log.d(TAG, "开始导出所有数据...")
                
                // 安全检查
                if (!AccountingApplication.isInitialized) {
                    return@withContext Result.failure(Exception("应用未完全初始化"))
                }
                
                // 获取所有数据
                val accounts = AccountingApplication.accountRepository.getAllAccounts().first()
                val categories = AccountingApplication.categoryRepository.getAllCategories().first()
                val records = AccountingApplication.recordRepository.getAllRecordsWithCategory().first()
                    .map { it.record }
                
                Log.d(TAG, "  - 账本: ${accounts.size} 个")
                Log.d(TAG, "  - 类别: ${categories.size} 个")
                Log.d(TAG, "  - 记录: ${records.size} 条")
                
                // 创建备份数据
                val backupData = BackupData(
                    accounts = accounts,
                    categories = categories,
                    records = records
                )
                
                // 转换为 JSON
                val json = gson.toJson(backupData)
                
                // 写入文件
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        writer.write(json)
                    }
                } ?: return@withContext Result.failure(Exception("无法打开输出流"))
                
                Log.d(TAG, "✅ 数据导出成功！")
                Log.d(TAG, "========================================")
                
                Result.success("成功导出全部 ${records.size} 条记录")
                
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e  // 重新抛出协程取消异常
            } catch (e: Exception) {
                Log.e(TAG, "❌ 数据导出失败: ${e.message}", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * 从 JSON 文件恢复数据
     */
    suspend fun importData(context: Context, uri: Uri, clearExisting: Boolean = false): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "========================================")
                Log.d(TAG, "开始导入数据...")
                
                // 安全检查
                if (!AccountingApplication.isInitialized) {
                    return@withContext Result.failure(Exception("应用未完全初始化"))
                }
                
                // 读取文件内容
                val json = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        reader.readText()
                    }
                } ?: return@withContext Result.failure(Exception("无法读取文件"))
                
                // 解析 JSON
                val backupData = gson.fromJson(json, BackupData::class.java)
                    ?: return@withContext Result.failure(Exception("无法解析备份文件"))
                
                Log.d(TAG, "备份文件版本: ${backupData.version}")
                Log.d(TAG, "备份时间: ${DateUtils.formatDateTime(backupData.timestamp)}")
                Log.d(TAG, "  - 账本: ${backupData.accounts.size} 个")
                Log.d(TAG, "  - 类别: ${backupData.categories.size} 个")
                Log.d(TAG, "  - 记录: ${backupData.records.size} 条")
                
                // 如果需要清空现有数据
                if (clearExisting) {
                    Log.d(TAG, "清空现有数据...")
                    AccountingApplication.database.clearAllTables()
                }
                
                // 导入账本
                backupData.accounts.forEach { account ->
                    try {
                        AccountingApplication.accountRepository.insert(account)
                    } catch (e: Exception) {
                        Log.w(TAG, "跳过已存在的账本: ${account.name}")
                    }
                }
                
                // 导入类别
                backupData.categories.forEach { category ->
                    try {
                        AccountingApplication.categoryRepository.insert(category)
                    } catch (e: Exception) {
                        Log.w(TAG, "跳过已存在的类别: ${category.name}")
                    }
                }
                
                // 导入记录
                var importedCount = 0
                backupData.records.forEach { record ->
                    try {
                        AccountingApplication.recordRepository.insert(record)
                        importedCount++
                    } catch (e: Exception) {
                        Log.w(TAG, "跳过已存在的记录: ID=${record.id}")
                    }
                }
                
                Log.d(TAG, "✅ 数据导入成功！")
                Log.d(TAG, "  - 导入记录: $importedCount 条")
                Log.d(TAG, "========================================")
                
                Result.success("成功导入 $importedCount 条记录")
                
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e  // 重新抛出协程取消异常
            } catch (e: Exception) {
                Log.e(TAG, "❌ 数据导入失败: ${e.message}", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * 清空所有数据
     */
    suspend fun clearAllData(): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "========================================")
                Log.d(TAG, "清空所有数据...")
                
                // 安全检查
                if (!AccountingApplication.isInitialized) {
                    return@withContext Result.failure(Exception("应用未完全初始化"))
                }
                
                AccountingApplication.database.clearAllTables()
                
                // 重新初始化默认数据
                AccountingApplication.database.initializeDefaultData()
                
                Log.d(TAG, "✅ 数据清空成功！")
                Log.d(TAG, "========================================")
                
                Result.success("数据已清空")
                
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e  // 重新抛出协程取消异常
            } catch (e: Exception) {
                Log.e(TAG, "❌ 数据清空失败: ${e.message}", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * 生成备份文件名
     */
    fun generateBackupFileName(): String {
        val timestamp = DateUtils.formatDate(System.currentTimeMillis()).replace("-", "")
        return "accounting_backup_$timestamp.json"
    }
}

