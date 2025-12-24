package com.example.presonalincome_expenditureaccountingsystem.ui.history

import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.presonalincome_expenditureaccountingsystem.R
import com.example.presonalincome_expenditureaccountingsystem.data.entity.Record
import com.example.presonalincome_expenditureaccountingsystem.data.entity.RecordWithCategory
import com.example.presonalincome_expenditureaccountingsystem.databinding.FragmentHistoryBinding
import com.example.presonalincome_expenditureaccountingsystem.ui.adapter.RecordAdapter
import com.example.presonalincome_expenditureaccountingsystem.util.CurrencyUtils
import com.example.presonalincome_expenditureaccountingsystem.util.DateUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * 记录列表页面 Fragment
 * 显示历史收支记录
 */
class HistoryFragment : Fragment() {

    companion object {
        private const val TAG = "HistoryFragment"
    }

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: HistoryViewModel by viewModels()
    
    private lateinit var recordAdapter: RecordAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        Log.d(TAG, "HistoryFragment 创建")

        setupRecyclerView()
        setupSwipeRefresh()
        setupTimeRangeSelector()
        setupMonthNavigation()
        setupSearch()
        setupFilter()
        setupSort()
        observeViewModel()
    }
    
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "HistoryFragment onResume - 刷新数据")
        viewModel.refresh()
    }

    /**
     * 设置 RecyclerView
     */
    private fun setupRecyclerView() {
        recordAdapter = RecordAdapter(
            onItemClick = { record ->
                showRecordDetail(record)
            },
            onItemLongClick = { record ->
                showRecordOptions(record)
                true
            }
        )
        
        binding.rvRecords.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recordAdapter
        }
        
        // 设置滑动删除
        setupSwipeToDelete()
    }
    
    /**
     * 设置滑动删除
     */
    private fun setupSwipeToDelete() {
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                @Suppress("DEPRECATION")
                val position = viewHolder.adapterPosition
                // 安全检查：确保位置有效
                if (position == RecyclerView.NO_POSITION || position >= recordAdapter.currentList.size) {
                    return
                }
                
                val record = recordAdapter.currentList[position]
                
                // 先删除
                viewModel.deleteRecord(record)
                
                // 检查 binding 是否有效
                if (_binding == null) return
                
                // 显示撤销 Snackbar
                Snackbar.make(
                    binding.root,
                    "已删除一条${if (record.record.isExpense) "支出" else "收入"}记录",
                    Snackbar.LENGTH_LONG
                ).setAction("撤销") {
                    // TODO: 实现撤销功能（重新插入记录）
                }.setActionTextColor(ContextCompat.getColor(requireContext(), R.color.income_green))
                .show()
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val background = ColorDrawable(ContextCompat.getColor(requireContext(), R.color.expense_red))
                val deleteIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_delete)!!
                
                // 绘制红色背景
                background.setBounds(
                    itemView.right + dX.toInt(),
                    itemView.top,
                    itemView.right,
                    itemView.bottom
                )
                background.draw(c)
                
                // 绘制删除图标
                val iconMargin = (itemView.height - deleteIcon.intrinsicHeight) / 2
                val iconTop = itemView.top + iconMargin
                val iconBottom = iconTop + deleteIcon.intrinsicHeight
                val iconRight = itemView.right - iconMargin
                val iconLeft = iconRight - deleteIcon.intrinsicWidth
                
                deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                deleteIcon.setTint(ContextCompat.getColor(requireContext(), android.R.color.white))
                deleteIcon.draw(c)
                
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }
        
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.rvRecords)
    }
    
    /**
     * 设置下拉刷新
     */
    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(
            R.color.primary,
            R.color.income_green,
            R.color.expense_red
        )
        
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refresh()
        }
    }
    
    /**
     * 设置时间范围选择器
     */
    private fun setupTimeRangeSelector() {
        binding.chipGroupTimeRange.setOnCheckedStateChangeListener { _, checkedIds ->
            val timeRange = when {
                checkedIds.contains(R.id.chip_month) -> HistoryViewModel.TIME_RANGE_MONTH
                checkedIds.contains(R.id.chip_year) -> HistoryViewModel.TIME_RANGE_YEAR
                checkedIds.contains(R.id.chip_all_time) -> HistoryViewModel.TIME_RANGE_ALL
                else -> HistoryViewModel.TIME_RANGE_MONTH
            }
            viewModel.setTimeRangeType(timeRange)
            
            // 全部模式下隐藏导航按钮
            binding.layoutTimeNavigation.visibility = if (timeRange == HistoryViewModel.TIME_RANGE_ALL) {
                View.GONE
            } else {
                View.VISIBLE
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
     * 设置搜索功能
     */
    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.setSearchKeyword(s?.toString() ?: "")
            }
        })
        
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                // 隐藏键盘
                binding.etSearch.clearFocus()
                true
            } else {
                false
            }
        }
    }
    
    /**
     * 设置筛选功能
     */
    private fun setupFilter() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            val filterType = when {
                checkedIds.contains(R.id.chip_expense) -> HistoryViewModel.FILTER_EXPENSE
                checkedIds.contains(R.id.chip_income) -> HistoryViewModel.FILTER_INCOME
                else -> HistoryViewModel.FILTER_ALL
            }
            viewModel.setFilterType(filterType)
        }
    }
    
    /**
     * 设置排序功能
     */
    private fun setupSort() {
        binding.btnSort.setOnClickListener { view ->
            showSortMenu(view)
        }
    }
    
    /**
     * 显示排序菜单
     */
    private fun showSortMenu(anchor: View) {
        val popupMenu = android.widget.PopupMenu(requireContext(), anchor)
        popupMenu.menu.apply {
            add(0, 0, 0, "⏰ 时间 新→旧")
            add(0, 1, 1, "⏰ 时间 旧→新")
            add(0, 2, 2, "💰 金额 高→低")
            add(0, 3, 3, "💰 金额 低→高")
        }
        
        popupMenu.setOnMenuItemClickListener { item ->
            val sortType = when (item.itemId) {
                0 -> HistoryViewModel.SORT_DATE_DESC
                1 -> HistoryViewModel.SORT_DATE_ASC
                2 -> HistoryViewModel.SORT_AMOUNT_DESC
                3 -> HistoryViewModel.SORT_AMOUNT_ASC
                else -> HistoryViewModel.SORT_DATE_DESC
            }
            viewModel.setSortType(sortType)
            updateSortButtonText(sortType)
            true
        }
        
        popupMenu.show()
    }
    
    /**
     * 更新排序按钮文字
     */
    private fun updateSortButtonText(sortType: Int) {
        val text = when (sortType) {
            HistoryViewModel.SORT_DATE_DESC -> "时间↓"
            HistoryViewModel.SORT_DATE_ASC -> "时间↑"
            HistoryViewModel.SORT_AMOUNT_DESC -> "金额↓"
            HistoryViewModel.SORT_AMOUNT_ASC -> "金额↑"
            else -> "排序"
        }
        binding.btnSort.text = text
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
                
                // 观察记录列表
                launch {
                    viewModel.records.collect { records ->
                        Log.d(TAG, "收到记录更新: ${records.size} 条")
                        
                        recordAdapter.submitList(records)
                        updateEmptyState(records.isEmpty())
                    }
                }
                
                // 观察搜索结果
                launch {
                    viewModel.hasSearchResult.collect { hasResult ->
                        val keyword = viewModel.searchKeyword.value
                        if (keyword.isNotEmpty() && !hasResult) {
                            binding.layoutNoResult.visibility = View.VISIBLE
                            binding.layoutEmpty.visibility = View.GONE
                            binding.rvRecords.visibility = View.GONE
                        } else {
                            binding.layoutNoResult.visibility = View.GONE
                        }
                    }
                }
                
                // 观察月度收入
                launch {
                    viewModel.monthlyIncome.collect { income ->
                        binding.tvIncome.text = CurrencyUtils.formatWithSymbol(income)
                    }
                }
                
                // 观察月度支出
                launch {
                    viewModel.monthlyExpense.collect { expense ->
                        binding.tvExpense.text = CurrencyUtils.formatWithSymbol(expense)
                    }
                }
                
                // 观察结余
                launch {
                    viewModel.monthlyIncome.collect { income ->
                        val expense = viewModel.monthlyExpense.value
                        val balance = income - expense
                        binding.tvBalance.text = CurrencyUtils.formatWithSymbol(balance)
                        
                        // 根据正负设置颜色
                        val color = when {
                            balance > 0 -> R.color.income_green
                            balance < 0 -> R.color.expense_red
                            else -> R.color.balance_blue
                        }
                        binding.tvBalance.setTextColor(ContextCompat.getColor(requireContext(), color))
                    }
                }
                
                launch {
                    viewModel.monthlyExpense.collect { expense ->
                        val income = viewModel.monthlyIncome.value
                        val balance = income - expense
                        binding.tvBalance.text = CurrencyUtils.formatWithSymbol(balance)
                        
                        val color = when {
                            balance > 0 -> R.color.income_green
                            balance < 0 -> R.color.expense_red
                            else -> R.color.balance_blue
                        }
                        binding.tvBalance.setTextColor(ContextCompat.getColor(requireContext(), color))
                    }
                }
                
                // 观察加载状态
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.swipeRefresh.isRefreshing = isLoading
                    }
                }
            }
        }
    }

    /**
     * 更新空状态显示
     */
    private fun updateEmptyState(isEmpty: Boolean) {
        val keyword = viewModel.searchKeyword.value
        
        if (isEmpty && keyword.isEmpty()) {
            binding.layoutEmpty.visibility = View.VISIBLE
            binding.rvRecords.visibility = View.GONE
        } else {
            binding.layoutEmpty.visibility = View.GONE
            binding.rvRecords.visibility = View.VISIBLE
        }
    }
    
    /**
     * 显示记录详情
     */
    private fun showRecordDetail(recordWithCategory: RecordWithCategory) {
        val record = recordWithCategory.record
        val category = recordWithCategory.category
        
        val typeStr = if (record.isExpense) "支出" else "收入"
        val message = """
            类型：$typeStr
            金额：¥${String.format("%.2f", record.amount)}
            类别：${category?.name ?: "未知"}
            日期：${DateUtils.formatFullDay(record.date)}
            备注：${record.note.ifEmpty { "无" }}
            创建时间：${DateUtils.formatDateTime(record.createdAt)}
        """.trimIndent()
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("记录详情")
            .setMessage(message)
            .setPositiveButton("确定", null)
            .setNeutralButton("编辑") { _, _ ->
                showEditDialog(recordWithCategory)
            }
            .setNegativeButton("删除") { _, _ ->
                showDeleteConfirmDialog(recordWithCategory)
            }
            .show()
    }
    
    /**
     * 显示编辑记录对话框
     */
    private fun showEditDialog(recordWithCategory: RecordWithCategory) {
        val record = recordWithCategory.record
        
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_record, null)
        val chipExpense = dialogView.findViewById<com.google.android.material.chip.Chip>(R.id.chip_expense)
        val chipIncome = dialogView.findViewById<com.google.android.material.chip.Chip>(R.id.chip_income)
        val etAmount = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_amount)
        val etNote = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_note)
        
        // 填充当前数据
        if (record.isExpense) {
            chipExpense.isChecked = true
        } else {
            chipIncome.isChecked = true
        }
        etAmount.setText(String.format("%.2f", record.amount))
        etNote.setText(record.note)
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("编辑记录")
            .setView(dialogView)
            .setPositiveButton("保存") { _, _ ->
                // 获取编辑后的数据
                val newAmount = etAmount.text.toString().toDoubleOrNull() ?: record.amount
                val newNote = etNote.text.toString()
                val newType = if (chipExpense.isChecked) Record.TYPE_EXPENSE else Record.TYPE_INCOME
                
                // 创建更新后的记录
                val updatedRecord = record.copy(
                    amount = newAmount,
                    note = newNote,
                    type = newType
                )
                
                // 更新记录
                viewModel.updateRecord(updatedRecord)
                
                Toast.makeText(requireContext(), "记录已更新", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    /**
     * 显示记录操作选项
     */
    private fun showRecordOptions(recordWithCategory: RecordWithCategory) {
        val record = recordWithCategory.record
        val typeStr = if (record.isExpense) "支出" else "收入"
        
        val options = arrayOf("查看详情", "编辑记录", "删除记录")
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("$typeStr ¥${String.format("%.2f", record.amount)}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showRecordDetail(recordWithCategory)
                    1 -> showEditDialog(recordWithCategory)
                    2 -> showDeleteConfirmDialog(recordWithCategory)
                }
            }
            .show()
    }
    
    /**
     * 显示删除确认对话框
     */
    private fun showDeleteConfirmDialog(recordWithCategory: RecordWithCategory) {
        val record = recordWithCategory.record
        val typeStr = if (record.isExpense) "支出" else "收入"
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("确认删除")
            .setMessage("确定要删除这条${typeStr}记录吗？\n金额：¥${String.format("%.2f", record.amount)}")
            .setPositiveButton("删除") { _, _ ->
                viewModel.deleteRecord(recordWithCategory)
                Log.d(TAG, "用户确认删除记录: ID=${record.id}")
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
