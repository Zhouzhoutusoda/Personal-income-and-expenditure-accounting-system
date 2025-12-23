package com.example.presonalincome_expenditureaccountingsystem.ui.statistics

import android.graphics.Color
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.presonalincome_expenditureaccountingsystem.AccountingApplication
import com.example.presonalincome_expenditureaccountingsystem.data.entity.CategoryStatistics
import com.example.presonalincome_expenditureaccountingsystem.data.entity.Record
import com.example.presonalincome_expenditureaccountingsystem.util.DateUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * 统计页面 ViewModel
 */
class StatisticsViewModel : ViewModel() {

    companion object {
        private const val TAG = "StatisticsViewModel"
        
        // 时间范围类型
        const val TIME_RANGE_MONTH = 0   // 按月
        const val TIME_RANGE_YEAR = 1    // 按年
        const val TIME_RANGE_ALL = 2     // 全部
        
        // 饼图颜色列表 - 支出（暖色系）
        private val EXPENSE_COLORS = listOf(
            Color.parseColor("#FF6B6B"), // 珊瑚红
            Color.parseColor("#FFE66D"), // 柠檬黄
            Color.parseColor("#FF8E53"), // 橙色
            Color.parseColor("#FFA07A"), // 浅鲑鱼色
            Color.parseColor("#FF7F7F"), // 浅红
            Color.parseColor("#FFB347"), // 杏色
            Color.parseColor("#FF6961"), // 浅红玫瑰
            Color.parseColor("#F4A460"), // 沙棕色
            Color.parseColor("#E9967A"), // 深鲑鱼色
            Color.parseColor("#FA8072")  // 鲑鱼色
        )
        
        // 饼图颜色列表 - 收入（冷色系）
        private val INCOME_COLORS = listOf(
            Color.parseColor("#4ECDC4"), // 薄荷绿
            Color.parseColor("#45B7D1"), // 天蓝
            Color.parseColor("#96CEB4"), // 薄荷色
            Color.parseColor("#88D8B0"), // 浅绿
            Color.parseColor("#6BCB77"), // 翠绿
            Color.parseColor("#5DADE2"), // 淡蓝
            Color.parseColor("#48C9B0"), // 绿松石
            Color.parseColor("#52BE80"), // 中绿
            Color.parseColor("#73C6B6"), // 浅青绿
            Color.parseColor("#82E0AA")  // 浅绿
        )
    }

    private val recordRepository = AccountingApplication.recordRepository

    // 当前选中的年月
    private var currentYear = Calendar.getInstance().get(Calendar.YEAR)
    private var currentMonth = Calendar.getInstance().get(Calendar.MONTH)
    
    // 时间范围类型
    private val _timeRangeType = MutableStateFlow(TIME_RANGE_MONTH)
    val timeRangeType: StateFlow<Int> = _timeRangeType.asStateFlow()
    
    // 当前选中的账本ID（null 表示全部）
    private val _currentAccountId = MutableStateFlow<Long?>(null)
    val currentAccountId: StateFlow<Long?> = _currentAccountId.asStateFlow()

    // 时间显示
    private val _monthDisplay = MutableStateFlow("")
    val monthDisplay: StateFlow<String> = _monthDisplay.asStateFlow()

    // 统计数据
    private val _monthlyIncome = MutableStateFlow(0.0)
    val monthlyIncome: StateFlow<Double> = _monthlyIncome.asStateFlow()

    private val _monthlyExpense = MutableStateFlow(0.0)
    val monthlyExpense: StateFlow<Double> = _monthlyExpense.asStateFlow()

    private val _recordCount = MutableStateFlow(0)
    val recordCount: StateFlow<Int> = _recordCount.asStateFlow()

    // 日均统计
    private val _dailyExpense = MutableStateFlow(0.0)
    val dailyExpense: StateFlow<Double> = _dailyExpense.asStateFlow()

    private val _dailyIncome = MutableStateFlow(0.0)
    val dailyIncome: StateFlow<Double> = _dailyIncome.asStateFlow()

    private val _expenseDays = MutableStateFlow(0)
    val expenseDays: StateFlow<Int> = _expenseDays.asStateFlow()

    private val _incomeDays = MutableStateFlow(0)
    val incomeDays: StateFlow<Int> = _incomeDays.asStateFlow()

    // 分类统计
    private val _expenseCategories = MutableStateFlow<List<CategoryStatistics>>(emptyList())
    val expenseCategories: StateFlow<List<CategoryStatistics>> = _expenseCategories.asStateFlow()

    private val _incomeCategories = MutableStateFlow<List<CategoryStatistics>>(emptyList())
    val incomeCategories: StateFlow<List<CategoryStatistics>> = _incomeCategories.asStateFlow()

    // 趋势数据：Triple<标签, 支出, 收入>
    private val _trendData = MutableStateFlow<List<Triple<String, Double, Double>>>(emptyList())
    val trendData: StateFlow<List<Triple<String, Double, Double>>> = _trendData.asStateFlow()

    // 是否可以切换到下一个时间段
    private val _canGoNext = MutableStateFlow(false)
    val canGoNext: StateFlow<Boolean> = _canGoNext.asStateFlow()

    private var loadJob: Job? = null

    init {
        updateTimeDisplay()
        loadData()
    }
    
    /**
     * 设置时间范围类型
     */
    fun setTimeRangeType(type: Int) {
        _timeRangeType.value = type
        updateTimeDisplay()
        loadData()
    }
    
    /**
     * 设置账本
     */
    fun setAccount(accountId: Long?) {
        _currentAccountId.value = accountId
        loadData()
    }

    /**
     * 更新时间显示
     */
    private fun updateTimeDisplay() {
        when (_timeRangeType.value) {
            TIME_RANGE_MONTH -> {
                val calendar = Calendar.getInstance()
                calendar.set(currentYear, currentMonth, 1)
                _monthDisplay.value = DateUtils.formatMonth(calendar.timeInMillis)
                
                // 判断是否可以切换到下个月
                val now = Calendar.getInstance()
                _canGoNext.value = currentYear < now.get(Calendar.YEAR) ||
                        (currentYear == now.get(Calendar.YEAR) && currentMonth < now.get(Calendar.MONTH))
            }
            TIME_RANGE_YEAR -> {
                _monthDisplay.value = "${currentYear}年"
                
                // 判断是否可以切换到下一年
                val now = Calendar.getInstance()
                _canGoNext.value = currentYear < now.get(Calendar.YEAR)
            }
            TIME_RANGE_ALL -> {
                _monthDisplay.value = "全部统计"
                _canGoNext.value = false
            }
        }
    }

    /**
     * 切换到上一个时间段
     */
    fun previousMonth() {
        when (_timeRangeType.value) {
            TIME_RANGE_MONTH -> {
                currentMonth--
                if (currentMonth < 0) {
                    currentMonth = 11
                    currentYear--
                }
            }
            TIME_RANGE_YEAR -> {
                currentYear--
            }
            TIME_RANGE_ALL -> {
                return
            }
        }
        updateTimeDisplay()
        loadData()
    }

    /**
     * 切换到下一个时间段
     */
    fun nextMonth() {
        val now = Calendar.getInstance()
        
        when (_timeRangeType.value) {
            TIME_RANGE_MONTH -> {
                if (currentYear < now.get(Calendar.YEAR) ||
                    (currentYear == now.get(Calendar.YEAR) && currentMonth < now.get(Calendar.MONTH))) {
                    currentMonth++
                    if (currentMonth > 11) {
                        currentMonth = 0
                        currentYear++
                    }
                }
            }
            TIME_RANGE_YEAR -> {
                if (currentYear < now.get(Calendar.YEAR)) {
                    currentYear++
                }
            }
            TIME_RANGE_ALL -> {
                return
            }
        }
        updateTimeDisplay()
        loadData()
    }

    /**
     * 加载统计数据
     */
    fun loadData() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                Log.d(TAG, "========================================")
                Log.d(TAG, "加载统计数据: ${_monthDisplay.value}")

                // 计算日期范围
                val (startDate, endDate, periodDays) = calculateDateRange()

                // 获取总收入和总支出
                val income: Double
                val expense: Double
                
                if (_timeRangeType.value == TIME_RANGE_ALL) {
                    income = recordRepository.getTotalIncomeAll()
                    expense = recordRepository.getTotalExpenseAll()
                } else {
                    income = recordRepository.getTotalIncome(startDate, endDate)
                    expense = recordRepository.getTotalExpense(startDate, endDate)
                }

                _monthlyIncome.value = income
                _monthlyExpense.value = expense

                Log.d(TAG, "统计数据:")
                Log.d(TAG, "  - 收入: ¥$income")
                Log.d(TAG, "  - 支出: ¥$expense")
                Log.d(TAG, "  - 结余: ¥${income - expense}")

                // 获取分类统计
                loadCategoryStatistics(startDate, endDate)

                // 获取趋势数据
                loadTrendData(startDate, endDate, periodDays)

            } catch (e: Exception) {
                Log.e(TAG, "加载统计数据失败: ${e.message}", e)
            }
        }
    }
    
    /**
     * 计算日期范围
     */
    private fun calculateDateRange(): Triple<Long, Long, Int> {
        val calendar = Calendar.getInstance()
        
        return when (_timeRangeType.value) {
            TIME_RANGE_MONTH -> {
                calendar.set(currentYear, currentMonth, 1, 0, 0, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startDate = calendar.timeInMillis
                
                val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                
                calendar.set(Calendar.DAY_OF_MONTH, daysInMonth)
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val endDate = calendar.timeInMillis
                
                Triple(startDate, endDate, daysInMonth)
            }
            TIME_RANGE_YEAR -> {
                calendar.set(currentYear, Calendar.JANUARY, 1, 0, 0, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startDate = calendar.timeInMillis
                
                calendar.set(currentYear, Calendar.DECEMBER, 31, 23, 59, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val endDate = calendar.timeInMillis
                
                Triple(startDate, endDate, 12) // 12个月
            }
            else -> {
                // 全部记录 - 使用很长的时间范围
                Triple(0L, Long.MAX_VALUE, 0)
            }
        }
    }

    /**
     * 加载分类统计
     */
    private suspend fun loadCategoryStatistics(startDate: Long, endDate: Long) {
        try {
            // 获取支出分类统计
            val expenseStats = if (_timeRangeType.value == TIME_RANGE_ALL) {
                recordRepository.getExpenseCategoryStatistics(0L, Long.MAX_VALUE)
            } else {
                recordRepository.getExpenseCategoryStatistics(startDate, endDate)
            }
            val expenseTotal = expenseStats.sumOf { it.totalAmount }
            
            val expenseWithColors = expenseStats.mapIndexed { index, stat ->
                stat.copy(
                    percentage = if (expenseTotal > 0) (stat.totalAmount / expenseTotal * 100).toFloat() else 0f
                ).apply {
                    color = EXPENSE_COLORS[index % EXPENSE_COLORS.size]
                }
            }
            _expenseCategories.value = expenseWithColors
            
            Log.d(TAG, "支出分类统计: ${expenseStats.size} 个类别")

            // 获取收入分类统计
            val incomeStats = if (_timeRangeType.value == TIME_RANGE_ALL) {
                recordRepository.getIncomeCategoryStatistics(0L, Long.MAX_VALUE)
            } else {
                recordRepository.getIncomeCategoryStatistics(startDate, endDate)
            }
            val incomeTotal = incomeStats.sumOf { it.totalAmount }
            
            val incomeWithColors = incomeStats.mapIndexed { index, stat ->
                stat.copy(
                    percentage = if (incomeTotal > 0) (stat.totalAmount / incomeTotal * 100).toFloat() else 0f
                ).apply {
                    color = INCOME_COLORS[index % INCOME_COLORS.size]
                }
            }
            _incomeCategories.value = incomeWithColors
            
            Log.d(TAG, "收入分类统计: ${incomeStats.size} 个类别")

        } catch (e: Exception) {
            Log.e(TAG, "加载分类统计失败: ${e.message}", e)
        }
    }

    /**
     * 加载趋势数据
     */
    private suspend fun loadTrendData(startDate: Long, endDate: Long, periodDays: Int) {
        try {
            // 收集记录
            val recordFlow = if (_timeRangeType.value == TIME_RANGE_ALL) {
                recordRepository.getAllRecordsWithCategory()
            } else {
                recordRepository.getRecordsByDateRange(startDate, endDate)
            }
            
            recordFlow.collect { records ->
                _recordCount.value = records.size

                when (_timeRangeType.value) {
                    TIME_RANGE_MONTH -> {
                        // 按周分组
                        val dailyExpenseMap = mutableMapOf<Int, Double>()
                        val dailyIncomeMap = mutableMapOf<Int, Double>()
                        val expenseDaysSet = mutableSetOf<Int>()
                        val incomeDaysSet = mutableSetOf<Int>()

                        records.forEach { recordWithCategory ->
                            val record = recordWithCategory.record
                            val calendar = Calendar.getInstance()
                            calendar.timeInMillis = record.date
                            val day = calendar.get(Calendar.DAY_OF_MONTH)
                            
                            if (record.type == Record.TYPE_EXPENSE) {
                                dailyExpenseMap[day] = (dailyExpenseMap[day] ?: 0.0) + record.amount
                                expenseDaysSet.add(day)
                            } else {
                                dailyIncomeMap[day] = (dailyIncomeMap[day] ?: 0.0) + record.amount
                                incomeDaysSet.add(day)
                            }
                        }

                        _expenseDays.value = expenseDaysSet.size
                        _incomeDays.value = incomeDaysSet.size
                        
                        val totalExpense = _monthlyExpense.value
                        val totalIncome = _monthlyIncome.value
                        
                        _dailyExpense.value = if (expenseDaysSet.isNotEmpty()) totalExpense / expenseDaysSet.size else 0.0
                        _dailyIncome.value = if (incomeDaysSet.isNotEmpty()) totalIncome / incomeDaysSet.size else 0.0

                        // 生成周趋势数据
                        val weeklyData = mutableListOf<Triple<String, Double, Double>>()
                        for (week in 1..((periodDays + 6) / 7)) {
                            val startDay = (week - 1) * 7 + 1
                            val endDay = minOf(week * 7, periodDays)
                            
                            var weekExpense = 0.0
                            var weekIncome = 0.0
                            
                            for (day in startDay..endDay) {
                                weekExpense += dailyExpenseMap[day] ?: 0.0
                                weekIncome += dailyIncomeMap[day] ?: 0.0
                            }
                            
                            weeklyData.add(Triple("第${week}周", weekExpense, weekIncome))
                        }
                        _trendData.value = weeklyData
                    }
                    TIME_RANGE_YEAR -> {
                        // 按月分组
                        val monthlyExpenseMap = mutableMapOf<Int, Double>()
                        val monthlyIncomeMap = mutableMapOf<Int, Double>()
                        val expenseMonthsSet = mutableSetOf<Int>()
                        val incomeMonthsSet = mutableSetOf<Int>()

                        records.forEach { recordWithCategory ->
                            val record = recordWithCategory.record
                            val calendar = Calendar.getInstance()
                            calendar.timeInMillis = record.date
                            val month = calendar.get(Calendar.MONTH)
                            
                            if (record.type == Record.TYPE_EXPENSE) {
                                monthlyExpenseMap[month] = (monthlyExpenseMap[month] ?: 0.0) + record.amount
                                expenseMonthsSet.add(month)
                            } else {
                                monthlyIncomeMap[month] = (monthlyIncomeMap[month] ?: 0.0) + record.amount
                                incomeMonthsSet.add(month)
                            }
                        }

                        _expenseDays.value = expenseMonthsSet.size
                        _incomeDays.value = incomeMonthsSet.size
                        
                        val totalExpense = _monthlyExpense.value
                        val totalIncome = _monthlyIncome.value
                        
                        _dailyExpense.value = if (expenseMonthsSet.isNotEmpty()) totalExpense / expenseMonthsSet.size else 0.0
                        _dailyIncome.value = if (incomeMonthsSet.isNotEmpty()) totalIncome / incomeMonthsSet.size else 0.0

                        // 生成月趋势数据
                        val monthNames = arrayOf("1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月", "12月")
                        val monthlyData = mutableListOf<Triple<String, Double, Double>>()
                        for (month in 0..11) {
                            monthlyData.add(Triple(
                                monthNames[month],
                                monthlyExpenseMap[month] ?: 0.0,
                                monthlyIncomeMap[month] ?: 0.0
                            ))
                        }
                        _trendData.value = monthlyData
                    }
                    TIME_RANGE_ALL -> {
                        // 按年分组
                        val yearlyExpenseMap = mutableMapOf<Int, Double>()
                        val yearlyIncomeMap = mutableMapOf<Int, Double>()
                        val expenseYearsSet = mutableSetOf<Int>()
                        val incomeYearsSet = mutableSetOf<Int>()

                        records.forEach { recordWithCategory ->
                            val record = recordWithCategory.record
                            val calendar = Calendar.getInstance()
                            calendar.timeInMillis = record.date
                            val year = calendar.get(Calendar.YEAR)
                            
                            if (record.type == Record.TYPE_EXPENSE) {
                                yearlyExpenseMap[year] = (yearlyExpenseMap[year] ?: 0.0) + record.amount
                                expenseYearsSet.add(year)
                            } else {
                                yearlyIncomeMap[year] = (yearlyIncomeMap[year] ?: 0.0) + record.amount
                                incomeYearsSet.add(year)
                            }
                        }

                        _expenseDays.value = expenseYearsSet.size
                        _incomeDays.value = incomeYearsSet.size
                        
                        val totalExpense = _monthlyExpense.value
                        val totalIncome = _monthlyIncome.value
                        
                        _dailyExpense.value = if (expenseYearsSet.isNotEmpty()) totalExpense / expenseYearsSet.size else 0.0
                        _dailyIncome.value = if (incomeYearsSet.isNotEmpty()) totalIncome / incomeYearsSet.size else 0.0

                        // 生成年趋势数据
                        val years = (yearlyExpenseMap.keys + yearlyIncomeMap.keys).distinct().sorted()
                        val yearlyData = years.map { year ->
                            Triple(
                                "${year}年",
                                yearlyExpenseMap[year] ?: 0.0,
                                yearlyIncomeMap[year] ?: 0.0
                            )
                        }
                        _trendData.value = yearlyData
                    }
                }

                Log.d(TAG, "趋势数据: ${_trendData.value.size} 个数据点")
                Log.d(TAG, "========================================")
            }
        } catch (e: Exception) {
            Log.e(TAG, "加载趋势数据失败: ${e.message}", e)
        }
    }

    /**
     * 刷新数据
     */
    fun refresh() {
        loadData()
    }

    override fun onCleared() {
        super.onCleared()
        loadJob?.cancel()
    }
}
