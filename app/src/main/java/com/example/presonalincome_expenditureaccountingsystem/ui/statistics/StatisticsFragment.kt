package com.example.presonalincome_expenditureaccountingsystem.ui.statistics

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.presonalincome_expenditureaccountingsystem.AccountingApplication
import com.example.presonalincome_expenditureaccountingsystem.R
import com.example.presonalincome_expenditureaccountingsystem.data.entity.Account
import com.example.presonalincome_expenditureaccountingsystem.data.entity.CategoryStatistics
import com.example.presonalincome_expenditureaccountingsystem.databinding.FragmentStatisticsBinding
import com.example.presonalincome_expenditureaccountingsystem.ui.adapter.CategoryStatAdapter
import com.example.presonalincome_expenditureaccountingsystem.util.CurrencyUtils
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 统计页面 Fragment
 */
class StatisticsFragment : Fragment() {

    companion object {
        private const val TAG = "StatisticsFragment"
    }

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StatisticsViewModel by viewModels()

    private lateinit var expenseAdapter: CategoryStatAdapter
    private lateinit var incomeAdapter: CategoryStatAdapter
    
    // 账本列表
    private var accounts: List<Account> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d(TAG, "StatisticsFragment 创建")

        setupTimeRangeSelector()
        setupMonthNavigation()
        setupAccountSelector()
        setupRecyclerViews()
        setupLineChart()
        setupPieCharts()
        observeViewModel()
        loadAccounts()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "StatisticsFragment onResume - 刷新数据")
        viewModel.refresh()
    }
    
    /**
     * 加载账本列表
     */
    private fun loadAccounts() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // 安全检查：确保 Application 已初始化
                if (!AccountingApplication.isInitialized) {
                    Log.w(TAG, "Application 尚未初始化完成")
                    return@launch
                }
                
                AccountingApplication.accountRepository.getAllAccounts().collect { accountList ->
                    // 检查 binding 是否仍然有效
                    if (_binding == null) return@collect
                    
                    accounts = accountList
                    // 更新当前选中账本显示
                    val currentAccount = accounts.find { it.id == viewModel.currentAccountId.value }
                    binding.btnAccount.text = currentAccount?.name ?: "全部账本"
                }
            } catch (e: Exception) {
                Log.e(TAG, "加载账本列表失败: ${e.message}", e)
            }
        }
    }

    /**
     * 设置账本选择器
     */
    private fun setupAccountSelector() {
        binding.btnAccount.setOnClickListener {
            showAccountPickerDialog()
        }
    }
    
    /**
     * 显示账本选择对话框
     */
    private fun showAccountPickerDialog() {
        val accountNames = mutableListOf("全部账本")
        accountNames.addAll(accounts.map { it.name })
        
        val currentIndex = if (viewModel.currentAccountId.value == null) {
            0
        } else {
            accounts.indexOfFirst { it.id == viewModel.currentAccountId.value } + 1
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("选择账本")
            .setSingleChoiceItems(accountNames.toTypedArray(), currentIndex) { dialog, which ->
                val selectedAccountId = if (which == 0) null else accounts[which - 1].id
                viewModel.setAccount(selectedAccountId)
                binding.btnAccount.text = accountNames[which]
                dialog.dismiss()
            }
            .show()
    }

    /**
     * 设置时间范围选择器
     */
    private fun setupTimeRangeSelector() {
        binding.chipGroupTimeRange.setOnCheckedStateChangeListener { _, checkedIds ->
            val timeRange = when {
                checkedIds.contains(R.id.chip_month) -> StatisticsViewModel.TIME_RANGE_MONTH
                checkedIds.contains(R.id.chip_year) -> StatisticsViewModel.TIME_RANGE_YEAR
                checkedIds.contains(R.id.chip_all_time) -> StatisticsViewModel.TIME_RANGE_ALL
                else -> StatisticsViewModel.TIME_RANGE_MONTH
            }
            viewModel.setTimeRangeType(timeRange)
            
            // 全部模式下隐藏导航按钮
            binding.layoutTimeNavigation.visibility = if (timeRange == StatisticsViewModel.TIME_RANGE_ALL) {
                View.GONE
            } else {
                View.VISIBLE
            }
            
            // 更新标签文字
            updateLabels(timeRange)
        }
    }
    
    /**
     * 更新标签文字
     */
    private fun updateLabels(timeRange: Int) {
        when (timeRange) {
            StatisticsViewModel.TIME_RANGE_MONTH -> {
                binding.tvBalanceLabel.text = "本月结余"
                binding.tvDailyExpenseLabel.text = "日均支出"
                binding.tvDailyIncomeLabel.text = "日均收入"
                binding.tvExpenseEmptyHint.text = "本月暂无支出"
                binding.tvIncomeEmptyHint.text = "本月暂无收入"
            }
            StatisticsViewModel.TIME_RANGE_YEAR -> {
                binding.tvBalanceLabel.text = "本年结余"
                binding.tvDailyExpenseLabel.text = "月均支出"
                binding.tvDailyIncomeLabel.text = "月均收入"
                binding.tvExpenseEmptyHint.text = "本年暂无支出"
                binding.tvIncomeEmptyHint.text = "本年暂无收入"
            }
            StatisticsViewModel.TIME_RANGE_ALL -> {
                binding.tvBalanceLabel.text = "总结余"
                binding.tvDailyExpenseLabel.text = "年均支出"
                binding.tvDailyIncomeLabel.text = "年均收入"
                binding.tvExpenseEmptyHint.text = "暂无支出记录"
                binding.tvIncomeEmptyHint.text = "暂无收入记录"
            }
        }
    }
    
    /**
     * 设置月份导航
     */
    private fun setupMonthNavigation() {
        binding.btnPrevMonth.setOnClickListener {
            viewModel.previousMonth()
        }

        binding.btnNextMonth.setOnClickListener {
            viewModel.nextMonth()
        }
    }

    /**
     * 设置 RecyclerView
     */
    private fun setupRecyclerViews() {
        expenseAdapter = CategoryStatAdapter()
        incomeAdapter = CategoryStatAdapter()

        binding.rvExpenseCategories.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = expenseAdapter
            isNestedScrollingEnabled = false
        }

        binding.rvIncomeCategories.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = incomeAdapter
            isNestedScrollingEnabled = false
        }
    }

    /**
     * 设置折线图
     */
    private fun setupLineChart() {
        binding.lineChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(false)
            setPinchZoom(false)
            setDrawGridBackground(false)
            
            // X 轴设置
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
                textSize = 10f
            }
            
            // Y 轴设置
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = ContextCompat.getColor(requireContext(), R.color.divider)
                textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
                textSize = 10f
                axisMinimum = 0f
            }
            axisRight.isEnabled = false
            
            // 图例设置
            legend.apply {
                isEnabled = true
                verticalAlignment = Legend.LegendVerticalAlignment.TOP
                horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
                orientation = Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
                textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
                textSize = 11f
            }
            
            // 无数据时显示
            setNoDataText("暂无数据")
            setNoDataTextColor(ContextCompat.getColor(requireContext(), R.color.text_hint))
            
            // 动画
            animateX(1000, Easing.EaseInOutQuad)
        }
    }

    /**
     * 设置饼图
     */
    private fun setupPieCharts() {
        setupPieChart(binding.pieChartExpense)
        setupPieChart(binding.pieChartIncome)
    }

    private fun setupPieChart(pieChart: PieChart) {
        pieChart.apply {
            description.isEnabled = false
            setUsePercentValues(true)
            setExtraOffsets(5f, 10f, 5f, 5f)
            
            dragDecelerationFrictionCoef = 0.95f
            
            isDrawHoleEnabled = true
            setHoleColor(Color.WHITE)
            setTransparentCircleColor(Color.WHITE)
            setTransparentCircleAlpha(110)
            
            holeRadius = 55f
            transparentCircleRadius = 60f
            
            setDrawCenterText(true)
            centerText = ""
            setCenterTextSize(16f)
            setCenterTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            
            rotationAngle = 0f
            isRotationEnabled = true
            isHighlightPerTapEnabled = true
            
            // 图例设置
            legend.apply {
                isEnabled = true
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                orientation = Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
                xEntrySpace = 12f
                yEntrySpace = 0f
                yOffset = 8f
                textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
                textSize = 11f
                isWordWrapEnabled = true
            }
            
            // 无数据时显示
            setNoDataText("暂无数据")
            setNoDataTextColor(ContextCompat.getColor(requireContext(), R.color.text_hint))
            
            // 动画
            animateY(1000, Easing.EaseInOutQuad)
        }
    }

    /**
     * 观察 ViewModel 状态变化
     */
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // 观察月份显示
                launch {
                    viewModel.monthDisplay.collect { month ->
                        binding.tvMonth.text = month
                    }
                }

                // 观察是否可以切换到下个月
                launch {
                    viewModel.canGoNext.collect { canGoNext ->
                        binding.btnNextMonth.isEnabled = canGoNext
                        binding.btnNextMonth.alpha = if (canGoNext) 1f else 0.3f
                    }
                }

                // 观察月度收入
                launch {
                    viewModel.monthlyIncome.collect { income ->
                        binding.tvIncome.text = CurrencyUtils.formatWithSymbol(income)
                        binding.tvIncomeTotal.text = "共 ${CurrencyUtils.formatWithSymbol(income)}"
                        updateBalance()
                    }
                }

                // 观察月度支出
                launch {
                    viewModel.monthlyExpense.collect { expense ->
                        binding.tvExpense.text = CurrencyUtils.formatWithSymbol(expense)
                        binding.tvExpenseTotal.text = "共 ${CurrencyUtils.formatWithSymbol(expense)}"
                        updateBalance()
                    }
                }

                // 观察记录数量
                launch {
                    viewModel.recordCount.collect { count ->
                        binding.tvRecordCount.text = "${count}笔"
                    }
                }

                // 观察日均支出
                launch {
                    viewModel.dailyExpense.collect { daily ->
                        binding.tvDailyExpense.text = CurrencyUtils.formatWithSymbol(daily)
                    }
                }

                // 观察日均收入
                launch {
                    viewModel.dailyIncome.collect { daily ->
                        binding.tvDailyIncome.text = CurrencyUtils.formatWithSymbol(daily)
                    }
                }

                // 观察支出天数/月数/年数
                launch {
                    viewModel.expenseDays.collect { count ->
                        val unit = when (viewModel.timeRangeType.value) {
                            StatisticsViewModel.TIME_RANGE_MONTH -> "天"
                            StatisticsViewModel.TIME_RANGE_YEAR -> "月"
                            else -> "年"
                        }
                        binding.tvExpenseDays.text = "共${count}${unit}有支出"
                    }
                }

                // 观察收入天数/月数/年数
                launch {
                    viewModel.incomeDays.collect { count ->
                        val unit = when (viewModel.timeRangeType.value) {
                            StatisticsViewModel.TIME_RANGE_MONTH -> "天"
                            StatisticsViewModel.TIME_RANGE_YEAR -> "月"
                            else -> "年"
                        }
                        binding.tvIncomeDays.text = "共${count}${unit}有收入"
                    }
                }
                
                // 观察时间范围类型变化
                launch {
                    viewModel.timeRangeType.collect { timeRange ->
                        updateLabels(timeRange)
                    }
                }

                // 观察支出分类
                launch {
                    viewModel.expenseCategories.collect { categories ->
                        Log.d(TAG, "收到支出分类更新: ${categories.size} 个")

                        if (categories.isEmpty()) {
                            binding.pieChartExpense.visibility = View.GONE
                            binding.rvExpenseCategories.visibility = View.GONE
                            binding.layoutExpenseEmpty.visibility = View.VISIBLE
                        } else {
                            binding.pieChartExpense.visibility = View.VISIBLE
                            binding.rvExpenseCategories.visibility = View.VISIBLE
                            binding.layoutExpenseEmpty.visibility = View.GONE

                            // 更新饼图
                            updateExpensePieChart(categories)

                            // 更新列表
                            expenseAdapter.submitList(categories)
                        }
                    }
                }

                // 观察收入分类
                launch {
                    viewModel.incomeCategories.collect { categories ->
                        Log.d(TAG, "收到收入分类更新: ${categories.size} 个")

                        if (categories.isEmpty()) {
                            binding.pieChartIncome.visibility = View.GONE
                            binding.rvIncomeCategories.visibility = View.GONE
                            binding.layoutIncomeEmpty.visibility = View.VISIBLE
                        } else {
                            binding.pieChartIncome.visibility = View.VISIBLE
                            binding.rvIncomeCategories.visibility = View.VISIBLE
                            binding.layoutIncomeEmpty.visibility = View.GONE

                            // 更新饼图
                            updateIncomePieChart(categories)

                            // 更新列表
                            incomeAdapter.submitList(categories)
                        }
                    }
                }

                // 观察趋势数据
                launch {
                    viewModel.trendData.collect { trendData ->
                        Log.d(TAG, "收到趋势数据更新: ${trendData.size} 个数据点")

                        if (trendData.isEmpty() || trendData.all { it.second == 0.0 && it.third == 0.0 }) {
                            binding.lineChart.visibility = View.GONE
                            binding.layoutTrendEmpty.visibility = View.VISIBLE
                        } else {
                            binding.lineChart.visibility = View.VISIBLE
                            binding.layoutTrendEmpty.visibility = View.GONE
                            updateLineChart(trendData)
                        }
                    }
                }
            }
        }
    }

    /**
     * 更新结余显示
     */
    private fun updateBalance() {
        val income = viewModel.monthlyIncome.value
        val expense = viewModel.monthlyExpense.value
        val balance = income - expense

        binding.tvBalance.text = CurrencyUtils.formatWithSymbol(balance)
    }

    /**
     * 更新折线图
     */
    private fun updateLineChart(data: List<Triple<String, Double, Double>>) {
        val expenseEntries = mutableListOf<Entry>()
        val incomeEntries = mutableListOf<Entry>()
        val labels = mutableListOf<String>()

        data.forEachIndexed { index, (label, expense, income) ->
            expenseEntries.add(Entry(index.toFloat(), expense.toFloat()))
            incomeEntries.add(Entry(index.toFloat(), income.toFloat()))
            labels.add(label)
        }

        // 支出数据集
        val expenseDataSet = LineDataSet(expenseEntries, "支出").apply {
            color = ContextCompat.getColor(requireContext(), R.color.expense_red)
            setCircleColor(ContextCompat.getColor(requireContext(), R.color.expense_red))
            lineWidth = 2.5f
            circleRadius = 4f
            setDrawCircleHole(true)
            circleHoleRadius = 2f
            valueTextSize = 9f
            valueTextColor = ContextCompat.getColor(requireContext(), R.color.expense_red)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = ContextCompat.getColor(requireContext(), R.color.expense_red_light)
            fillAlpha = 100
        }

        // 收入数据集
        val incomeDataSet = LineDataSet(incomeEntries, "收入").apply {
            color = ContextCompat.getColor(requireContext(), R.color.income_green)
            setCircleColor(ContextCompat.getColor(requireContext(), R.color.income_green))
            lineWidth = 2.5f
            circleRadius = 4f
            setDrawCircleHole(true)
            circleHoleRadius = 2f
            valueTextSize = 9f
            valueTextColor = ContextCompat.getColor(requireContext(), R.color.income_green)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = ContextCompat.getColor(requireContext(), R.color.income_green_light)
            fillAlpha = 100
        }

        binding.lineChart.apply {
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            xAxis.labelCount = labels.size
            this.data = LineData(expenseDataSet, incomeDataSet)
            invalidate()
            animateX(800, Easing.EaseInOutQuad)
        }
    }

    /**
     * 更新支出饼图
     */
    private fun updateExpensePieChart(categories: List<CategoryStatistics>) {
        val entries = categories.map { stat ->
            PieEntry(stat.totalAmount.toFloat(), stat.categoryName)
        }

        val colors = categories.map { it.color }

        val dataSet = PieDataSet(entries, "").apply {
            this.colors = colors
            sliceSpace = 2f
            selectionShift = 5f
            valueLinePart1Length = 0.4f
            valueLinePart2Length = 0.4f
            valueLineColor = ContextCompat.getColor(requireContext(), R.color.text_hint)
            yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        }

        val data = PieData(dataSet).apply {
            setValueFormatter(PercentFormatter(binding.pieChartExpense))
            setValueTextSize(11f)
            setValueTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
        }

        binding.pieChartExpense.apply {
            this.data = data
            centerText = "支出\n${CurrencyUtils.formatLargeAmountWithSymbol(categories.sumOf { it.totalAmount })}"
            invalidate()
            animateY(800, Easing.EaseInOutQuad)
        }
    }

    /**
     * 更新收入饼图
     */
    private fun updateIncomePieChart(categories: List<CategoryStatistics>) {
        val entries = categories.map { stat ->
            PieEntry(stat.totalAmount.toFloat(), stat.categoryName)
        }

        val colors = categories.map { it.color }

        val dataSet = PieDataSet(entries, "").apply {
            this.colors = colors
            sliceSpace = 2f
            selectionShift = 5f
            valueLinePart1Length = 0.4f
            valueLinePart2Length = 0.4f
            valueLineColor = ContextCompat.getColor(requireContext(), R.color.text_hint)
            yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        }

        val data = PieData(dataSet).apply {
            setValueFormatter(PercentFormatter(binding.pieChartIncome))
            setValueTextSize(11f)
            setValueTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
        }

        binding.pieChartIncome.apply {
            this.data = data
            centerText = "收入\n${CurrencyUtils.formatLargeAmountWithSymbol(categories.sumOf { it.totalAmount })}"
            invalidate()
            animateY(800, Easing.EaseInOutQuad)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
