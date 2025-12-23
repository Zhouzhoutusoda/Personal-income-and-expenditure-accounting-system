package com.example.presonalincome_expenditureaccountingsystem.data.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * 收支记录与类别的关联查询结果
 * 
 * 用于在查询记录时同时获取关联的类别信息
 */
data class RecordWithCategory(
    @Embedded
    val record: Record,
    
    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: Category?
)

