package com.example.presonalincome_expenditureaccountingsystem.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 账本实体类
 * 
 * 用于支持多账本管理功能，用户可以创建不同的账本来分类记账
 * 如：日常开销、旅游账本、家庭账本等
 * 
 * @property id 主键，自增
 * @property name 账本名称
 * @property description 账本描述
 * @property icon 图标资源名称
 * @property isDefault 是否为默认账本（有且仅有一个）
 * @property createdAt 创建时间
 */
@Entity(
    tableName = "accounts",
    indices = [
        Index(value = ["isDefault"])
    ]
)
data class Account(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /** 账本名称 */
    val name: String,
    
    /** 账本描述 */
    val description: String = "",
    
    /** 图标资源名称 */
    val icon: String = "ic_wallet",
    
    /** 是否为默认账本 */
    val isDefault: Boolean = false,
    
    /** 创建时间 */
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        /**
         * 创建默认账本
         */
        fun createDefault(): Account = Account(
            name = "默认账本",
            description = "我的日常收支",
            icon = "ic_wallet",
            isDefault = true
        )
    }
}

