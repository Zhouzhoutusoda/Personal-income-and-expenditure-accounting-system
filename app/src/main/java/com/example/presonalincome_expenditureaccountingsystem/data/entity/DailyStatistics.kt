package com.example.presonalincome_expenditureaccountingsystem.data.entity

import androidx.room.Ignore

/**
 * 每日统计数据
 * 
 * 用于按日期分组显示记录列表
 * 
 * @property date 日期（时间戳）
 * @property totalIncome 当日总收入
 * @property totalExpense 当日总支出
 * @property records 当日记录列表
 */
data class DailyStatistics(
    val date: Long,
    val totalIncome: Double,
    val totalExpense: Double,
    val records: List<RecordWithCategory>
) {
    /** 当日结余 */
    val balance: Double get() = totalIncome - totalExpense
}

/**
 * 月度统计数据
 * 
 * 用于显示月度收支统计
 * 
 * @property year 年份
 * @property month 月份（1-12）
 * @property totalIncome 月度总收入
 * @property totalExpense 月度总支出
 * @property recordCount 记录数量
 */
data class MonthlyStatistics(
    val year: Int,
    val month: Int,
    val totalIncome: Double,
    val totalExpense: Double,
    val recordCount: Int
) {
    /** 月度结余 */
    val balance: Double get() = totalIncome - totalExpense
}

/**
 * 类别统计数据
 * 
 * 用于图表展示各类别的收支占比
 * 
 * @property categoryId 类别ID
 * @property categoryName 类别名称
 * @property categoryIcon 类别图标
 * @property totalAmount 该类别总金额
 * @property recordCount 该类别记录数量
 * @property percentage 占比（0-100）
 */
data class CategoryStatistics(
    val categoryId: Long,
    val categoryName: String,
    val categoryIcon: String,
    val totalAmount: Double,
    val recordCount: Int,
    val percentage: Float = 0f
) {
    /** 显示颜色（不从数据库读取，在代码中动态赋值） */
    @Ignore
    var color: Int = 0
}

