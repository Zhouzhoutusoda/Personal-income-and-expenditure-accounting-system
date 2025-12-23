package com.example.presonalincome_expenditureaccountingsystem.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 收支记录实体类
 * 
 * 用于存储用户的每一笔收入或支出记录
 * 
 * @property id 主键，自增
 * @property amount 金额（正数，单位：元）
 * @property type 类型：0=支出，1=收入
 * @property categoryId 关联的类别ID
 * @property accountId 关联的账本ID
 * @property date 记录日期（时间戳，精确到天）
 * @property note 备注说明
 * @property createdAt 创建时间（时间戳）
 * @property updatedAt 更新时间（时间戳）
 */
@Entity(
    tableName = "records",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["accountId"]),
        Index(value = ["date"]),
        Index(value = ["type"])
    ]
)
data class Record(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /** 金额（正数） */
    val amount: Double,
    
    /** 类型：0=支出，1=收入 */
    val type: Int,
    
    /** 关联的类别ID，可为空（类别被删除时） */
    val categoryId: Long? = null,
    
    /** 关联的账本ID */
    val accountId: Long,
    
    /** 记录日期（时间戳，毫秒） */
    val date: Long,
    
    /** 备注说明 */
    val note: String = "",
    
    /** 创建时间 */
    val createdAt: Long = System.currentTimeMillis(),
    
    /** 更新时间 */
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        /** 支出类型 */
        const val TYPE_EXPENSE = 0
        
        /** 收入类型 */
        const val TYPE_INCOME = 1
    }
    
    /** 是否为支出 */
    val isExpense: Boolean get() = type == TYPE_EXPENSE
    
    /** 是否为收入 */
    val isIncome: Boolean get() = type == TYPE_INCOME
    
    /** 获取带符号的金额（支出为负，收入为正） */
    val signedAmount: Double get() = if (isExpense) -amount else amount
}

