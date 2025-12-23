package com.example.presonalincome_expenditureaccountingsystem.util

import java.util.Calendar
import java.util.regex.Pattern

/**
 * 智能记账解析器
 * 从自然语言中提取记账信息
 */
object SmartRecordParser {
    
    /**
     * 解析结果
     */
    data class ParseResult(
        val amount: Double?,
        val categoryName: String?,
        val isExpense: Boolean,
        val date: Long,
        val note: String,
        val confidence: Int  // 置信度 0-100
    ) {
        fun isValid(): Boolean = amount != null && amount > 0
        
        fun getSummary(): String {
            val parts = mutableListOf<String>()
            if (amount != null) parts.add("金额: ¥${String.format("%.2f", amount)}")
            if (categoryName != null) parts.add("类别: $categoryName")
            parts.add("类型: ${if (isExpense) "支出" else "收入"}")
            if (note.isNotEmpty()) parts.add("备注: $note")
            return parts.joinToString("\n")
        }
    }
    
    // 类别关键词映射
    private val expenseCategoryKeywords = mapOf(
        "餐饮" to listOf("吃", "早饭", "午饭", "晚饭", "早餐", "午餐", "晚餐", "饭", "餐", "食", "外卖", "美团", "饿了么", "奶茶", "咖啡", "饮料", "零食", "水果", "蔬菜", "肉", "菜", "面", "粉", "粥", "火锅", "烧烤", "小吃", "甜点", "蛋糕"),
        "交通" to listOf("打车", "滴滴", "出租", "地铁", "公交", "车费", "油费", "加油", "高铁", "火车", "飞机", "机票", "停车", "过路费", "骑车", "单车", "共享"),
        "购物" to listOf("买", "购", "淘宝", "京东", "拼多多", "商场", "超市", "日用", "衣服", "鞋", "包", "化妆品", "护肤", "家电", "数码", "手机", "电脑"),
        "娱乐" to listOf("电影", "游戏", "KTV", "唱歌", "旅游", "玩", "门票", "景点", "演唱会", "运动", "健身", "游泳", "球"),
        "居住" to listOf("房租", "水费", "电费", "燃气", "物业", "维修", "装修", "家具"),
        "通讯" to listOf("话费", "流量", "充值", "网费", "宽带"),
        "医疗" to listOf("医院", "药", "看病", "挂号", "体检", "牙", "眼镜"),
        "教育" to listOf("学费", "培训", "课程", "书", "教材", "考试", "学习"),
        "人情" to listOf("红包", "礼物", "送礼", "份子钱", "请客", "聚餐", "随礼")
    )
    
    private val incomeCategoryKeywords = mapOf(
        "工资" to listOf("工资", "薪资", "薪水", "月薪", "底薪", "发工资"),
        "奖金" to listOf("奖金", "年终奖", "绩效", "提成", "分红"),
        "投资" to listOf("股票", "基金", "理财", "利息", "收益", "投资"),
        "兼职" to listOf("兼职", "副业", "外快", "私活"),
        "红包" to listOf("红包", "收红包", "压岁钱", "转账")
    )
    
    // 收入关键词
    private val incomeIndicators = listOf("收入", "收到", "进账", "入账", "发", "工资", "奖金", "红包", "赚")
    
    // 金额正则表达式
    private val amountPatterns = listOf(
        Pattern.compile("(\\d+\\.?\\d*)\\s*(块|元|¥|￥|圆|RMB|rmb)"),
        Pattern.compile("(¥|￥)\\s*(\\d+\\.?\\d*)"),
        Pattern.compile("(\\d+\\.?\\d*)\\s*(块钱|元钱)"),
        Pattern.compile("(\\d+)"),  // 兜底：纯数字
        // 中文数字
        Pattern.compile("([零一二三四五六七八九十百千万]+)\\s*(块|元|¥|￥|圆)")
    )
    
    // 日期关键词
    private val dateKeywords = mapOf(
        "今天" to 0,
        "今日" to 0,
        "昨天" to -1,
        "昨日" to -1,
        "前天" to -2,
        "前日" to -2,
        "大前天" to -3
    )
    
    /**
     * 解析输入文本
     */
    fun parse(input: String): ParseResult {
        val text = input.trim()
        
        if (text.isEmpty()) {
            return ParseResult(null, null, true, System.currentTimeMillis(), "", 0)
        }
        
        // 1. 判断是收入还是支出
        val isExpense = !incomeIndicators.any { text.contains(it) }
        
        // 2. 提取金额
        val amount = extractAmount(text)
        
        // 3. 识别类别
        val categoryName = extractCategory(text, isExpense)
        
        // 4. 提取日期
        val date = extractDate(text)
        
        // 5. 生成备注（原文去掉金额数字）
        val note = generateNote(text)
        
        // 6. 计算置信度
        val confidence = calculateConfidence(amount, categoryName)
        
        return ParseResult(
            amount = amount,
            categoryName = categoryName,
            isExpense = isExpense,
            date = date,
            note = note,
            confidence = confidence
        )
    }
    
    /**
     * 提取金额
     */
    private fun extractAmount(text: String): Double? {
        // 先尝试阿拉伯数字
        for (pattern in amountPatterns.dropLast(1)) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val amountStr = if (matcher.groupCount() >= 2 && matcher.group(1)?.matches(Regex("[¥￥]")) == true) {
                    matcher.group(2)
                } else {
                    matcher.group(1)
                }
                amountStr?.toDoubleOrNull()?.let { return it }
            }
        }
        
        // 尝试中文数字
        val chinesePattern = Pattern.compile("([零一二三四五六七八九十百千万]+)")
        val matcher = chinesePattern.matcher(text)
        if (matcher.find()) {
            val chineseNum = matcher.group(1)
            chineseToNumber(chineseNum)?.let { return it }
        }
        
        // 兜底：找任何数字
        val numberPattern = Pattern.compile("(\\d+\\.?\\d*)")
        val numMatcher = numberPattern.matcher(text)
        if (numMatcher.find()) {
            numMatcher.group(1)?.toDoubleOrNull()?.let { return it }
        }
        
        return null
    }
    
    /**
     * 中文数字转阿拉伯数字
     */
    private fun chineseToNumber(chinese: String): Double? {
        val digitMap = mapOf(
            '零' to 0, '一' to 1, '二' to 2, '三' to 3, '四' to 4,
            '五' to 5, '六' to 6, '七' to 7, '八' to 8, '九' to 9,
            '两' to 2
        )
        val unitMap = mapOf(
            '十' to 10, '百' to 100, '千' to 1000, '万' to 10000
        )
        
        var result = 0.0
        var temp = 0.0
        var lastUnit = 1
        
        for (char in chinese) {
            when {
                digitMap.containsKey(char) -> {
                    temp = digitMap[char]!!.toDouble()
                }
                unitMap.containsKey(char) -> {
                    val unit = unitMap[char]!!
                    if (temp == 0.0 && char == '十') temp = 1.0
                    if (unit == 10000) {
                        result = (result + temp * unit)
                        temp = 0.0
                    } else {
                        result += temp * unit
                        temp = 0.0
                    }
                    lastUnit = unit
                }
            }
        }
        result += temp
        
        return if (result > 0) result else null
    }
    
    /**
     * 识别类别
     */
    private fun extractCategory(text: String, isExpense: Boolean): String? {
        val keywords = if (isExpense) expenseCategoryKeywords else incomeCategoryKeywords
        
        for ((category, keywordList) in keywords) {
            for (keyword in keywordList) {
                if (text.contains(keyword)) {
                    return category
                }
            }
        }
        
        return null
    }
    
    /**
     * 提取日期
     */
    private fun extractDate(text: String): Long {
        val calendar = Calendar.getInstance()
        
        for ((keyword, offset) in dateKeywords) {
            if (text.contains(keyword)) {
                calendar.add(Calendar.DAY_OF_YEAR, offset)
                break
            }
        }
        
        return calendar.timeInMillis
    }
    
    /**
     * 生成备注
     */
    private fun generateNote(text: String): String {
        // 移除金额相关的部分，保留有意义的描述
        var note = text
        // 移除数字+单位的金额
        note = note.replace(Regex("\\d+\\.?\\d*\\s*(块钱?|元钱?|¥|￥|圆|RMB|rmb)?"), "")
        // 移除日期关键词
        dateKeywords.keys.forEach { note = note.replace(it, "") }
        // 移除多余空格
        note = note.replace(Regex("\\s+"), " ").trim()
        
        return if (note.length > 50) note.substring(0, 50) else note
    }
    
    /**
     * 计算置信度
     */
    private fun calculateConfidence(amount: Double?, categoryName: String?): Int {
        var confidence = 0
        if (amount != null && amount > 0) confidence += 50
        if (categoryName != null) confidence += 50
        return confidence
    }
    
    /**
     * 获取示例文本
     */
    fun getExamples(): List<String> = listOf(
        "今天早上吃了20块的早餐",
        "打车花了35元",
        "淘宝买衣服花了299",
        "收到工资8000元",
        "昨天请客吃饭花了三百块"
    )
}

