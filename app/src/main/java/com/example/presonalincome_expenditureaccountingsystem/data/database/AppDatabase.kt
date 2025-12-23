package com.example.presonalincome_expenditureaccountingsystem.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.presonalincome_expenditureaccountingsystem.data.dao.AccountDao
import com.example.presonalincome_expenditureaccountingsystem.data.dao.CategoryDao
import com.example.presonalincome_expenditureaccountingsystem.data.dao.RecordDao
import com.example.presonalincome_expenditureaccountingsystem.data.entity.Account
import com.example.presonalincome_expenditureaccountingsystem.data.entity.Category
import com.example.presonalincome_expenditureaccountingsystem.data.entity.Record
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 应用数据库
 * 
 * 使用 Room 持久化库管理 SQLite 数据库
 */
@Database(
    entities = [
        Record::class,
        Category::class,
        Account::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    /** 收支记录 DAO */
    abstract fun recordDao(): RecordDao
    
    /** 类别 DAO */
    abstract fun categoryDao(): CategoryDao
    
    /** 账本 DAO */
    abstract fun accountDao(): AccountDao
    
    /**
     * 重新初始化默认数据
     * 用于清空数据后恢复默认类别和账本
     */
    suspend fun initializeDefaultData() {
        // 创建默认账本
        val defaultAccount = Account.createDefault()
        accountDao().insert(defaultAccount)
        
        // 创建默认类别
        val defaultCategories = Category.getAllDefaultCategories()
        categoryDao().insertAll(defaultCategories)
    }
    
    companion object {
        private const val DATABASE_NAME = "personal_accounting.db"
        
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        /**
         * 获取数据库实例（单例模式）
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }
        
        /**
         * 构建数据库
         */
        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .addCallback(DatabaseCallback())
                .build()
        }
    }
    
    /**
     * 数据库创建回调
     * 用于初始化默认数据
     */
    private class DatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // 在后台线程初始化默认数据
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    populateDatabase(database)
                }
            }
        }
        
        /**
         * 填充初始数据
         */
        private suspend fun populateDatabase(database: AppDatabase) {
            // 创建默认账本
            val defaultAccount = Account.createDefault()
            database.accountDao().insert(defaultAccount)
            
            // 创建默认类别
            val defaultCategories = Category.getAllDefaultCategories()
            database.categoryDao().insertAll(defaultCategories)
        }
    }
}

