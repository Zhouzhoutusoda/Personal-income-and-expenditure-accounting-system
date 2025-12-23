package com.example.presonalincome_expenditureaccountingsystem.util

import java.text.DecimalFormat

/**
 * 货币工具类
 * 
 * 提供金额格式化相关的方法
 */
object CurrencyUtils {
    
    private val decimalFormat = DecimalFormat("#,##0.00")
    private val integerFormat = DecimalFormat("#,##0")
    
    /**
     * 格式化金额（保留两位小数）
     * 例如：1234.56 -> "1,234.56"
     */
    fun format(amount: Double): String {
        return decimalFormat.format(amount)
    }
    
    /**
     * 格式化金额（带货币符号）
     * 例如：1234.56 -> "¥1,234.56"
     */
    fun formatWithSymbol(amount: Double): String {
        return "¥${format(amount)}"
    }
    
    /**
     * 格式化金额（带正负号）
     * 例如：支出 1234.56 -> "-¥1,234.56"
     *      收入 1234.56 -> "+¥1,234.56"
     */
    fun formatWithSign(amount: Double, isIncome: Boolean): String {
        val sign = if (isIncome) "+" else "-"
        return "$sign¥${format(amount)}"
    }
    
    /**
     * 格式化大金额（超过万显示为 x.xx万）
     * 例如：12345.67 -> "1.23万"
     */
    fun formatLargeAmount(amount: Double): String {
        return when {
            amount >= 100000000 -> "${decimalFormat.format(amount / 100000000)}亿"
            amount >= 10000 -> "${decimalFormat.format(amount / 10000)}万"
            else -> format(amount)
        }
    }
    
    /**
     * 格式化大金额（带货币符号）
     */
    fun formatLargeAmountWithSymbol(amount: Double): String {
        return "¥${formatLargeAmount(amount)}"
    }
    
    /**
     * 格式化整数金额（不带小数）
     * 例如：1234 -> "1,234"
     */
    fun formatInteger(amount: Double): String {
        return integerFormat.format(amount)
    }
    
    /**
     * 解析金额字符串为 Double
     * 移除千分位分隔符和货币符号
     */
    fun parse(amountStr: String): Double? {
        return try {
            amountStr
                .replace(",", "")
                .replace("¥", "")
                .replace("+", "")
                .replace("-", "")
                .replace("万", "")
                .replace("亿", "")
                .toDoubleOrNull()
        } catch (e: Exception) {
            null
        }
    }
}

