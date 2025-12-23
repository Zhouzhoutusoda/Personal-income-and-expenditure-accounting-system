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
        setupMonthNavigation()
        setupSearch()
        setupFilter()
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
                val position = viewHolder.adapterPosition
                val record = recordAdapter.currentList[position]
                
                // 先删除
                viewModel.deleteRecord(record)
                
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
            .setNegativeButton("删除") { _, _ ->
                showDeleteConfirmDialog(recordWithCategory)
            }
            .show()
    }
    
    /**
     * 显示记录操作选项
     */
    private fun showRecordOptions(recordWithCategory: RecordWithCategory) {
        val record = recordWithCategory.record
        val typeStr = if (record.isExpense) "支出" else "收入"
        
        val options = arrayOf("查看详情", "删除记录")
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("$typeStr ¥${String.format("%.2f", record.amount)}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showRecordDetail(recordWithCategory)
                    1 -> showDeleteConfirmDialog(recordWithCategory)
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
