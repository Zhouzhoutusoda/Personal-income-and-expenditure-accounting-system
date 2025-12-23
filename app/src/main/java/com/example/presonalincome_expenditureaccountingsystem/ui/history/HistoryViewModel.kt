package com.example.presonalincome_expenditureaccountingsystem.ui.history

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.presonalincome_expenditureaccountingsystem.AccountingApplication
import com.example.presonalincome_expenditureaccountingsystem.data.entity.Record
import com.example.presonalincome_expenditureaccountingsystem.data.entity.RecordWithCategory
import com.example.presonalincome_expenditureaccountingsystem.util.DateUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * 记录列表页面 ViewModel
 */
class HistoryViewModel : ViewModel() {
    
    companion object {
        private const val TAG = "HistoryViewModel"
        
        // 筛选类型
        const val FILTER_ALL = 0
        const val FILTER_EXPENSE = 1
        const val FILTER_INCOME = 2
        
        // 时间范围类型
        const val TIME_RANGE_MONTH = 0   // 按月
        const val TIME_RANGE_YEAR = 1    // 按年
        const val TIME_RANGE_ALL = 2     // 全部
    }
    
    private val recordRepository = AccountingApplication.recordRepository
    
    // 当前选中的年月
    private var currentYear = Calendar.getInstance().get(Calendar.YEAR)
    private var currentMonth = Calendar.getInstance().get(Calendar.MONTH)
    
    // 时间范围类型
    private val _timeRangeType = MutableStateFlow(TIME_RANGE_MONTH)
    val timeRangeType: StateFlow<Int> = _timeRangeType.asStateFlow()
    
    // 时间显示
    private val _timeDisplay = MutableStateFlow("")
    val timeDisplay: StateFlow<String> = _timeDisplay.asStateFlow()
    
    // 当前筛选类型
    private val _filterType = MutableStateFlow(FILTER_ALL)
    val filterType: StateFlow<Int> = _filterType.asStateFlow()
    
    // 搜索关键词
    private val _searchKeyword = MutableStateFlow("")
    val searchKeyword: StateFlow<String> = _searchKeyword.asStateFlow()
    
    // 原始记录列表
    private var allRecords: List<RecordWithCategory> = emptyList()
    
    // 筛选后的记录列表
    private val _records = MutableStateFlow<List<RecordWithCategory>>(emptyList())
    val records: StateFlow<List<RecordWithCategory>> = _records.asStateFlow()
    
    // 统计数据
    private val _totalIncome = MutableStateFlow(0.0)
    val totalIncome: StateFlow<Double> = _totalIncome.asStateFlow()
    
    private val _totalExpense = MutableStateFlow(0.0)
    val totalExpense: StateFlow<Double> = _totalExpense.asStateFlow()
    
    // 兼容旧的命名
    val monthlyIncome: StateFlow<Double> = _totalIncome
    val monthlyExpense: StateFlow<Double> = _totalExpense
    
    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // 是否有搜索结果
    private val _hasSearchResult = MutableStateFlow(true)
    val hasSearchResult: StateFlow<Boolean> = _hasSearchResult.asStateFlow()
    
    // 是否可以切换到下一个时间段
    private val _canGoNext = MutableStateFlow(false)
    val canGoNext: StateFlow<Boolean> = _canGoNext.asStateFlow()
    
    // 兼容旧命名
    val monthDisplay: StateFlow<String> = _timeDisplay
    
    private var loadJob: Job? = null
    
    init {
        updateTimeDisplay()
        loadRecords()
    }
    
    /**
     * 设置时间范围类型
     */
    fun setTimeRangeType(type: Int) {
        _timeRangeType.value = type
        updateTimeDisplay()
        loadRecords()
    }
    
    /**
     * 更新时间显示
     */
    private fun updateTimeDisplay() {
        when (_timeRangeType.value) {
            TIME_RANGE_MONTH -> {
                val calendar = Calendar.getInstance()
                calendar.set(currentYear, currentMonth, 1)
                _timeDisplay.value = DateUtils.formatMonth(calendar.timeInMillis)
                
                // 判断是否可以切换到下个月
                val now = Calendar.getInstance()
                _canGoNext.value = currentYear < now.get(Calendar.YEAR) ||
                        (currentYear == now.get(Calendar.YEAR) && currentMonth < now.get(Calendar.MONTH))
            }
            TIME_RANGE_YEAR -> {
                _timeDisplay.value = "${currentYear}年"
                
                // 判断是否可以切换到下一年
                val now = Calendar.getInstance()
                _canGoNext.value = currentYear < now.get(Calendar.YEAR)
            }
            TIME_RANGE_ALL -> {
                _timeDisplay.value = "全部记录"
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
                // 全部模式不需要切换
                return
            }
        }
        updateTimeDisplay()
        loadRecords()
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
                // 全部模式不需要切换
                return
            }
        }
        updateTimeDisplay()
        loadRecords()
    }
    
    /**
     * 设置筛选类型
     */
    fun setFilterType(type: Int) {
        _filterType.value = type
        applyFilters()
    }
    
    /**
     * 设置搜索关键词
     */
    fun setSearchKeyword(keyword: String) {
        _searchKeyword.value = keyword
        applyFilters()
    }
    
    /**
     * 加载记录
     */
    fun loadRecords() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // 计算日期范围
                val (startDate, endDate) = calculateDateRange()
                
                Log.d(TAG, "----------------------------------------")
                Log.d(TAG, "加载记录: ${_timeDisplay.value}")
                Log.d(TAG, "  开始日期: ${if (startDate == 0L) "无限制" else DateUtils.formatDate(startDate)}")
                Log.d(TAG, "  结束日期: ${if (endDate == Long.MAX_VALUE) "无限制" else DateUtils.formatDate(endDate)}")
                
                // 获取统计数据
                val income: Double
                val expense: Double
                
                if (_timeRangeType.value == TIME_RANGE_ALL) {
                    income = recordRepository.getTotalIncomeAll()
                    expense = recordRepository.getTotalExpenseAll()
                } else {
                    income = recordRepository.getTotalIncome(startDate, endDate)
                    expense = recordRepository.getTotalExpense(startDate, endDate)
                }
                
                _totalIncome.value = income
                _totalExpense.value = expense
                
                Log.d(TAG, "统计数据:")
                Log.d(TAG, "  - 收入: ¥$income")
                Log.d(TAG, "  - 支出: ¥$expense")
                Log.d(TAG, "  - 结余: ¥${income - expense}")
                
                // 收集 Flow 数据
                val recordFlow = if (_timeRangeType.value == TIME_RANGE_ALL) {
                    recordRepository.getAllRecordsWithCategory()
                } else {
                    recordRepository.getRecordsByDateRange(startDate, endDate)
                }
                
                recordFlow.collect { recordList ->
                    allRecords = recordList
                    Log.d(TAG, "加载到 ${recordList.size} 条记录")
                    
                    applyFilters()
                    _isLoading.value = false
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "加载记录失败: ${e.message}", e)
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 计算日期范围
     */
    private fun calculateDateRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        
        return when (_timeRangeType.value) {
            TIME_RANGE_MONTH -> {
                calendar.set(currentYear, currentMonth, 1, 0, 0, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startDate = calendar.timeInMillis
                
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val endDate = calendar.timeInMillis
                
                Pair(startDate, endDate)
            }
            TIME_RANGE_YEAR -> {
                calendar.set(currentYear, Calendar.JANUARY, 1, 0, 0, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startDate = calendar.timeInMillis
                
                calendar.set(currentYear, Calendar.DECEMBER, 31, 23, 59, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val endDate = calendar.timeInMillis
                
                Pair(startDate, endDate)
            }
            else -> {
                // 全部记录
                Pair(0L, Long.MAX_VALUE)
            }
        }
    }
    
    /**
     * 应用筛选和搜索
     */
    private fun applyFilters() {
        var filtered = allRecords
        
        // 应用类型筛选
        when (_filterType.value) {
            FILTER_EXPENSE -> {
                filtered = filtered.filter { it.record.type == Record.TYPE_EXPENSE }
            }
            FILTER_INCOME -> {
                filtered = filtered.filter { it.record.type == Record.TYPE_INCOME }
            }
        }
        
        // 应用搜索筛选
        val keyword = _searchKeyword.value.trim()
        if (keyword.isNotEmpty()) {
            filtered = filtered.filter { record ->
                record.record.note.contains(keyword, ignoreCase = true) ||
                record.category?.name?.contains(keyword, ignoreCase = true) == true
            }
        }
        
        // 按日期倒序排序
        filtered = filtered.sortedByDescending { it.record.date }
        
        _records.value = filtered
        _hasSearchResult.value = keyword.isEmpty() || filtered.isNotEmpty()
        
        Log.d(TAG, "筛选结果: ${filtered.size} 条记录 (关键词: '$keyword', 类型: ${_filterType.value})")
    }
    
    /**
     * 删除记录
     */
    fun deleteRecord(record: RecordWithCategory) {
        viewModelScope.launch {
            try {
                recordRepository.delete(record.record)
                
                Log.d(TAG, "✅ 删除记录成功: ID=${record.record.id}")
                
                // 重新加载数据
                loadRecords()
                
            } catch (e: Exception) {
                Log.e(TAG, "删除记录失败: ${e.message}", e)
            }
        }
    }
    
    /**
     * 刷新数据
     */
    fun refresh() {
        loadRecords()
    }
    
    override fun onCleared() {
        super.onCleared()
        loadJob?.cancel()
    }
}
