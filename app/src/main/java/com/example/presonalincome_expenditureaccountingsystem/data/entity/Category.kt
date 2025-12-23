package com.example.presonalincome_expenditureaccountingsystem.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 类别实体类
 * 
 * 用于存储收支记录的分类信息，如餐饮、交通、工资等
 * 
 * @property id 主键，自增
 * @property name 类别名称
 * @property icon 图标资源名称
 * @property type 类型：0=支出类别，1=收入类别
 * @property isDefault 是否为系统默认类别（不可删除）
 * @property sortOrder 排序顺序（数值越小越靠前）
 * @property createdAt 创建时间
 */
@Entity(
    tableName = "categories",
    indices = [
        Index(value = ["type"]),
        Index(value = ["sortOrder"])
    ]
)
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /** 类别名称 */
    val name: String,
    
    /** 图标资源名称 */
    val icon: String,
    
    /** 类型：0=支出类别，1=收入类别 */
    val type: Int,
    
    /** 是否为系统默认类别 */
    val isDefault: Boolean = false,
    
    /** 排序顺序 */
    val sortOrder: Int = 0,
    
    /** 创建时间 */
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        /** 支出类别 */
        const val TYPE_EXPENSE = 0
        
        /** 收入类别 */
        const val TYPE_INCOME = 1
        
        /**
         * 获取默认支出类别列表
         */
        fun getDefaultExpenseCategories(): List<Category> = listOf(
            Category(name = "餐饮", icon = "ic_food", type = TYPE_EXPENSE, isDefault = true, sortOrder = 1),
            Category(name = "交通", icon = "ic_transport", type = TYPE_EXPENSE, isDefault = true, sortOrder = 2),
            Category(name = "购物", icon = "ic_shopping", type = TYPE_EXPENSE, isDefault = true, sortOrder = 3),
            Category(name = "娱乐", icon = "ic_entertainment", type = TYPE_EXPENSE, isDefault = true, sortOrder = 4),
            Category(name = "居住", icon = "ic_home", type = TYPE_EXPENSE, isDefault = true, sortOrder = 5),
            Category(name = "通讯", icon = "ic_phone", type = TYPE_EXPENSE, isDefault = true, sortOrder = 6),
            Category(name = "医疗", icon = "ic_medical", type = TYPE_EXPENSE, isDefault = true, sortOrder = 7),
            Category(name = "教育", icon = "ic_education", type = TYPE_EXPENSE, isDefault = true, sortOrder = 8),
            Category(name = "人情", icon = "ic_gift", type = TYPE_EXPENSE, isDefault = true, sortOrder = 9),
            Category(name = "其他", icon = "ic_other", type = TYPE_EXPENSE, isDefault = true, sortOrder = 99)
        )
        
        /**
         * 获取默认收入类别列表
         */
        fun getDefaultIncomeCategories(): List<Category> = listOf(
            Category(name = "工资", icon = "ic_salary", type = TYPE_INCOME, isDefault = true, sortOrder = 1),
            Category(name = "奖金", icon = "ic_bonus", type = TYPE_INCOME, isDefault = true, sortOrder = 2),
            Category(name = "投资", icon = "ic_investment", type = TYPE_INCOME, isDefault = true, sortOrder = 3),
            Category(name = "兼职", icon = "ic_parttime", type = TYPE_INCOME, isDefault = true, sortOrder = 4),
            Category(name = "理财", icon = "ic_finance", type = TYPE_INCOME, isDefault = true, sortOrder = 5),
            Category(name = "红包", icon = "ic_redpacket", type = TYPE_INCOME, isDefault = true, sortOrder = 6),
            Category(name = "其他", icon = "ic_other", type = TYPE_INCOME, isDefault = true, sortOrder = 99)
        )
        
        /**
         * 获取所有默认类别
         */
        fun getAllDefaultCategories(): List<Category> = 
            getDefaultExpenseCategories() + getDefaultIncomeCategories()
    }
    
    /** 是否为支出类别 */
    val isExpenseCategory: Boolean get() = type == TYPE_EXPENSE
    
    /** 是否为收入类别 */
    val isIncomeCategory: Boolean get() = type == TYPE_INCOME
}

