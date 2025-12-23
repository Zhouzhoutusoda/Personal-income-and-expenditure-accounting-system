package com.example.presonalincome_expenditureaccountingsystem.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 日期工具类
 * 
 * 提供日期相关的转换和计算方法
 */
object DateUtils {
    
    // ==================== 日期格式化器 ====================
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
    private val monthFormat = SimpleDateFormat("yyyy年MM月", Locale.CHINA)
    private val dayFormat = SimpleDateFormat("MM月dd日", Locale.CHINA)
    private val fullDayFormat = SimpleDateFormat("yyyy年MM月dd日", Locale.CHINA)
    private val weekDayFormat = SimpleDateFormat("EEEE", Locale.CHINA)
    private val dayOnlyFormat = SimpleDateFormat("d", Locale.CHINA)
    
    // ==================== 格式化方法 ====================
    
    /**
     * 格式化为日期字符串：yyyy-MM-dd
     */
    fun formatDate(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }
    
    /**
     * 格式化为日期时间字符串：yyyy-MM-dd HH:mm:ss
     */
    fun formatDateTime(timestamp: Long): String {
        return dateTimeFormat.format(Date(timestamp))
    }
    
    /**
     * 格式化为月份字符串：yyyy年MM月
     */
    fun formatMonth(timestamp: Long): String {
        return monthFormat.format(Date(timestamp))
    }
    
    /**
     * 格式化为日期字符串：MM月dd日
     */
    fun formatDay(timestamp: Long): String {
        return dayFormat.format(Date(timestamp))
    }
    
    /**
     * 格式化为完整日期字符串：yyyy年MM月dd日
     */
    fun formatFullDay(timestamp: Long): String {
        return fullDayFormat.format(Date(timestamp))
    }
    
    /**
     * 获取星期几
     */
    fun getWeekDay(timestamp: Long): String {
        return weekDayFormat.format(Date(timestamp))
    }
    
    /**
     * 格式化为日期数字（仅天）
     */
    fun formatDayOnly(timestamp: Long): String {
        return dayOnlyFormat.format(Date(timestamp))
    }
    
    /**
     * 格式化为友好显示（今天、昨天、或具体日期）
     */
    fun formatFriendly(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        val today = calendar.timeInMillis
        
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        val yesterday = calendar.timeInMillis
        
        return when {
            isSameDay(timestamp, today) -> "今天"
            isSameDay(timestamp, yesterday) -> "昨天"
            isSameYear(timestamp, today) -> formatDay(timestamp)
            else -> formatFullDay(timestamp)
        }
    }
    
    // ==================== 日期计算方法 ====================
    
    /**
     * 获取今天的开始时间戳（00:00:00）
     */
    fun getTodayStart(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    /**
     * 获取今天的结束时间戳（23:59:59）
     */
    fun getTodayEnd(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }
    
    /**
     * 获取本周的开始时间戳
     */
    fun getWeekStart(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    /**
     * 获取本周的结束时间戳
     */
    fun getWeekEnd(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        calendar.add(Calendar.DAY_OF_WEEK, 6)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }
    
    /**
     * 获取本月的开始时间戳
     */
    fun getMonthStart(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    /**
     * 获取本月的结束时间戳
     */
    fun getMonthEnd(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }
    
    /**
     * 获取本年的开始时间戳
     */
    fun getYearStart(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.MONTH, Calendar.JANUARY)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    /**
     * 获取本年的结束时间戳
     */
    fun getYearEnd(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.MONTH, Calendar.DECEMBER)
        calendar.set(Calendar.DAY_OF_MONTH, 31)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }
    
    /**
     * 获取指定日期的开始时间戳
     */
    fun getDayStart(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    /**
     * 获取指定日期的结束时间戳
     */
    fun getDayEnd(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }
    
    // ==================== 判断方法 ====================
    
    /**
     * 判断两个时间戳是否为同一天
     */
    fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = timestamp1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
    
    /**
     * 判断两个时间戳是否为同一月
     */
    fun isSameMonth(timestamp1: Long, timestamp2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = timestamp1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
    }
    
    /**
     * 判断两个时间戳是否为同一年
     */
    fun isSameYear(timestamp1: Long, timestamp2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = timestamp1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
    }
    
    /**
     * 判断时间戳是否为今天
     */
    fun isToday(timestamp: Long): Boolean {
        return isSameDay(timestamp, System.currentTimeMillis())
    }
}

