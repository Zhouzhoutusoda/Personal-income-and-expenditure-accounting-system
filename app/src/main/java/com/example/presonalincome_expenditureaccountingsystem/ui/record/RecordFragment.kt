package com.example.presonalincome_expenditureaccountingsystem.ui.record

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.EditorInfo
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.example.presonalincome_expenditureaccountingsystem.R
import com.example.presonalincome_expenditureaccountingsystem.data.entity.Record
import com.example.presonalincome_expenditureaccountingsystem.databinding.DialogQuickRecordBinding
import com.example.presonalincome_expenditureaccountingsystem.databinding.FragmentRecordBinding
import com.example.presonalincome_expenditureaccountingsystem.ui.adapter.CategoryAdapter
import com.example.presonalincome_expenditureaccountingsystem.util.DateUtils
import com.example.presonalincome_expenditureaccountingsystem.util.SmartRecordParser
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * 记账页面 Fragment
 * 用于添加新的收入/支出记录
 */
class RecordFragment : Fragment() {

    companion object {
        private const val TAG = "RecordFragment"
    }

    private var _binding: FragmentRecordBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: RecordViewModel by viewModels()
    
    // 类别适配器
    private lateinit var categoryAdapter: CategoryAdapter
    
    // 当前选中的日期
    private val selectedDate = Calendar.getInstance()
    
    // 当前类型是否为支出
    private var isExpenseType = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        Log.d(TAG, "RecordFragment 创建")
        
        setupTypeToggle()
        setupAmountInput()
        setupQuickAmountChips()
        setupQuickRecordButton()
        setupCategoryGrid()
        setupDatePicker()
        setupSaveButton()
        observeViewModel()
        updateDateDisplay()
    }

    /**
     * 设置收入/支出切换
     */
    private fun setupTypeToggle() {
        binding.toggleType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btn_expense -> {
                        isExpenseType = true
                        viewModel.setType(Record.TYPE_EXPENSE)
                        updateTypeColors()
                        Log.d(TAG, "切换到支出模式")
                    }
                    R.id.btn_income -> {
                        isExpenseType = false
                        viewModel.setType(Record.TYPE_INCOME)
                        updateTypeColors()
                        Log.d(TAG, "切换到收入模式")
                    }
                }
            }
        }
    }
    
    /**
     * 更新类型相关颜色
     */
    private fun updateTypeColors() {
        val color = if (isExpenseType) R.color.expense_red else R.color.income_green
        binding.tvCurrency.setTextColor(ContextCompat.getColor(requireContext(), color))
        categoryAdapter.setExpenseType(isExpenseType)
    }
    
    /**
     * 设置金额输入
     */
    private fun setupAmountInput() {
        binding.etAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // 隐藏错误提示
                hideAmountError()
                
                // 格式化输入（限制小数位数）
                val text = s?.toString() ?: return
                if (text.contains(".")) {
                    val parts = text.split(".")
                    if (parts.size > 1 && parts[1].length > 2) {
                        val formatted = "${parts[0]}.${parts[1].substring(0, 2)}"
                        binding.etAmount.setText(formatted)
                        binding.etAmount.setSelection(formatted.length)
                    }
                }
            }
        })
        
        // 键盘完成按钮
        binding.etAmount.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.etNote.requestFocus()
                true
            } else {
                false
            }
        }
    }
    
    /**
     * 设置快捷金额按钮
     */
    private fun setupQuickAmountChips() {
        // 使用 ChipGroup 的单选监听器
        binding.chipGroupAmount.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val amount = when (checkedIds[0]) {
                    R.id.chip_10 -> 10.0
                    R.id.chip_50 -> 50.0
                    R.id.chip_100 -> 100.0
                    R.id.chip_500 -> 500.0
                    else -> return@setOnCheckedStateChangeListener
                }
                setQuickAmount(amount)
            }
        }
        
        // 当用户手动输入金额时，清除快捷金额的选中状态
        binding.etAmount.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.chipGroupAmount.clearCheck()
            }
        }
    }
    
    /**
     * 设置快捷金额
     */
    private fun setQuickAmount(amount: Double) {
        binding.etAmount.setText(String.format("%.2f", amount))
        binding.etAmount.setSelection(binding.etAmount.text?.length ?: 0)
        hideAmountError()
    }
    
    /**
     * 设置快速记账按钮
     */
    private fun setupQuickRecordButton() {
        binding.btnQuickRecord.setOnClickListener {
            showQuickRecordDialog()
        }
    }
    
    /**
     * 显示快速记账对话框
     */
    private fun showQuickRecordDialog() {
        val dialogBinding = DialogQuickRecordBinding.inflate(layoutInflater)
        
        var currentParseResult: SmartRecordParser.ParseResult? = null
        
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .create()
        
        // 设置示例点击
        dialogBinding.chipExample1.setOnClickListener {
            dialogBinding.etInput.setText("午饭20元")
        }
        dialogBinding.chipExample2.setOnClickListener {
            dialogBinding.etInput.setText("打车35块")
        }
        dialogBinding.chipExample3.setOnClickListener {
            dialogBinding.etInput.setText("收到工资8000")
        }
        
        // 实时解析输入
        dialogBinding.etInput.addTextChangedListener { text ->
            if (text.isNullOrBlank()) {
                dialogBinding.cardPreview.visibility = View.GONE
                dialogBinding.cardError.visibility = View.GONE
                dialogBinding.btnConfirm.isEnabled = false
                currentParseResult = null
            } else {
                // 实时解析
                val result = SmartRecordParser.parse(text.toString())
                currentParseResult = result
                
                if (result.isValid()) {
                    // 显示解析结果
                    dialogBinding.cardPreview.visibility = View.VISIBLE
                    dialogBinding.cardError.visibility = View.GONE
                    dialogBinding.btnConfirm.isEnabled = true
                    
                    val previewText = buildString {
                        append("金额: ¥${String.format("%.2f", result.amount)}")
                        append("  |  类型: ${if (result.isExpense) "支出" else "收入"}")
                        if (result.categoryName != null) {
                            append("\n类别: ${result.categoryName}")
                        }
                        if (result.note.isNotEmpty()) {
                            append("\n备注: ${result.note}")
                        }
                    }
                    dialogBinding.tvPreview.text = previewText
                    
                    // 根据收入/支出设置颜色
                    if (result.isExpense) {
                        dialogBinding.cardPreview.setCardBackgroundColor(
                            ContextCompat.getColor(requireContext(), R.color.expense_red_light)
                        )
                        dialogBinding.cardPreview.strokeColor = 
                            ContextCompat.getColor(requireContext(), R.color.expense_red)
                    } else {
                        dialogBinding.cardPreview.setCardBackgroundColor(
                            ContextCompat.getColor(requireContext(), R.color.income_green_light)
                        )
                        dialogBinding.cardPreview.strokeColor = 
                            ContextCompat.getColor(requireContext(), R.color.income_green)
                    }
                } else {
                    // 无法识别
                    dialogBinding.cardPreview.visibility = View.GONE
                    dialogBinding.cardError.visibility = View.VISIBLE
                    dialogBinding.btnConfirm.isEnabled = false
                }
            }
        }
        
        // 取消按钮
        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        // 确认按钮
        dialogBinding.btnConfirm.setOnClickListener {
            currentParseResult?.let { result ->
                if (result.isValid()) {
                    // 应用解析结果到表单
                    applyQuickRecordResult(result)
                    dialog.dismiss()
                }
            }
        }
        
        dialog.show()
    }
    
    /**
     * 应用快速记账结果到表单
     */
    private fun applyQuickRecordResult(result: SmartRecordParser.ParseResult) {
        Log.d(TAG, "应用快速记账结果: ${result.getSummary()}")
        
        // 1. 设置收入/支出类型
        if (result.isExpense) {
            binding.toggleType.check(R.id.btn_expense)
        } else {
            binding.toggleType.check(R.id.btn_income)
        }
        
        // 2. 设置金额
        result.amount?.let { amount ->
            binding.etAmount.setText(String.format("%.2f", amount))
        }
        
        // 3. 设置日期
        selectedDate.timeInMillis = result.date
        updateDateDisplay()
        
        // 4. 设置备注
        if (result.note.isNotEmpty()) {
            binding.etNote.setText(result.note)
        }
        
        // 5. 尝试匹配类别
        result.categoryName?.let { categoryName ->
            val categories = if (result.isExpense) {
                viewModel.expenseCategories.value
            } else {
                viewModel.incomeCategories.value
            }
            
            val matchedCategory = categories.find { it.name == categoryName }
            if (matchedCategory != null) {
                viewModel.selectCategory(matchedCategory)
                categoryAdapter.setSelectedCategory(matchedCategory.id)
            }
        }
        
        // 6. 清除快捷金额选中状态
        binding.chipGroupAmount.clearCheck()
        
        hideAmountError()
        hideCategoryError()
    }
    
    /**
     * 设置类别网格
     */
    private fun setupCategoryGrid() {
        categoryAdapter = CategoryAdapter { category ->
            viewModel.selectCategory(category)
            hideCategoryError()
        }
        
        binding.rvCategories.apply {
            layoutManager = GridLayoutManager(requireContext(), 5)
            adapter = categoryAdapter
            isNestedScrollingEnabled = false
        }
    }

    /**
     * 设置日期选择器
     */
    private fun setupDatePicker() {
        binding.layoutDate.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    selectedDate.set(year, month, dayOfMonth)
                    updateDateDisplay()
                    Log.d(TAG, "选择日期: ${DateUtils.formatDate(selectedDate.timeInMillis)}")
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
            ).apply {
                // 限制最大日期为今天
                datePicker.maxDate = System.currentTimeMillis()
            }.show()
        }
    }

    /**
     * 更新日期显示
     */
    private fun updateDateDisplay() {
        val displayText = DateUtils.formatFriendly(selectedDate.timeInMillis)
        val weekDay = DateUtils.getWeekDay(selectedDate.timeInMillis)
        binding.tvDate.text = "$displayText $weekDay"
    }

    /**
     * 设置保存按钮
     */
    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            if (validateInput()) {
                saveRecord()
            }
        }
    }
    
    /**
     * 输入校验
     */
    private fun validateInput(): Boolean {
        var isValid = true
        
        // 校验金额
        val amountText = binding.etAmount.text.toString().trim()
        if (amountText.isEmpty()) {
            showAmountError("请输入金额")
            isValid = false
        } else {
            val amount = amountText.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                showAmountError("请输入有效金额")
                isValid = false
            } else if (amount > 99999999.99) {
                showAmountError("金额超出限制")
                isValid = false
            }
        }
        
        // 校验类别
        if (viewModel.selectedCategory.value == null) {
            showCategoryError("请选择类别")
            isValid = false
        }
        
        return isValid
    }
    
    /**
     * 显示金额错误
     */
    private fun showAmountError(message: String) {
        binding.tvAmountError.text = message
        binding.tvAmountError.visibility = View.VISIBLE
        // 震动效果
        binding.etAmount.animate()
            .translationX(-10f)
            .setDuration(50)
            .withEndAction {
                binding.etAmount.animate()
                    .translationX(10f)
                    .setDuration(50)
                    .withEndAction {
                        binding.etAmount.animate()
                            .translationX(0f)
                            .setDuration(50)
                            .start()
                    }
                    .start()
            }
            .start()
    }
    
    /**
     * 隐藏金额错误
     */
    private fun hideAmountError() {
        binding.tvAmountError.visibility = View.GONE
    }
    
    /**
     * 显示类别错误
     */
    private fun showCategoryError(message: String) {
        binding.tvCategoryError.text = message
        binding.tvCategoryError.visibility = View.VISIBLE
    }
    
    /**
     * 隐藏类别错误
     */
    private fun hideCategoryError() {
        binding.tvCategoryError.visibility = View.GONE
    }
    
    /**
     * 保存记录
     */
    private fun saveRecord() {
        val amountText = binding.etAmount.text.toString().trim()
        val amount = amountText.toDoubleOrNull() ?: return
        val note = binding.etNote.text?.toString()?.trim() ?: ""
        val date = DateUtils.getDayStart(selectedDate.timeInMillis)
        
        Log.d(TAG, "准备保存记录: 金额=$amount, 日期=${DateUtils.formatDate(date)}")
        
        viewModel.saveRecord(amount, date, note)
    }
    
    /**
     * 观察 ViewModel 状态变化
     */
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 观察当前类型变化，更新类别列表
                launch {
                    viewModel.currentType.collect { type ->
                        isExpenseType = (type == Record.TYPE_EXPENSE)
                        updateTypeColors()
                        updateCategoryList()
                    }
                }
                
                // 观察支出类别列表
                launch {
                    viewModel.expenseCategories.collect { categories ->
                        if (isExpenseType && categories.isNotEmpty()) {
                            updateCategoryList()
                        }
                    }
                }
                
                // 观察收入类别列表
                launch {
                    viewModel.incomeCategories.collect { categories ->
                        if (!isExpenseType && categories.isNotEmpty()) {
                            updateCategoryList()
                        }
                    }
                }
                
                // 观察选中的类别
                launch {
                    viewModel.selectedCategory.collect { category ->
                        category?.let {
                            categoryAdapter.setSelectedCategory(it.id)
                        }
                    }
                }
                
                // 观察保存状态
                launch {
                    viewModel.saveState.collect { state ->
                        when (state) {
                            is RecordViewModel.SaveState.Loading -> {
                                binding.btnSave.isEnabled = false
                                binding.btnSave.text = "保存中..."
                            }
                            is RecordViewModel.SaveState.Success -> {
                                showSuccessAnimation(state.type, state.amount)
                            }
                            is RecordViewModel.SaveState.Error -> {
                                binding.btnSave.isEnabled = true
                                binding.btnSave.text = "保存记录"
                                showAmountError(state.message)
                                viewModel.resetSaveState()
                            }
                            is RecordViewModel.SaveState.Idle -> {
                                binding.btnSave.isEnabled = true
                                binding.btnSave.text = "保存记录"
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 更新类别列表
     */
    private fun updateCategoryList() {
        val categories = if (isExpenseType) {
            viewModel.expenseCategories.value
        } else {
            viewModel.incomeCategories.value
        }
        
        if (categories.isNotEmpty()) {
            categoryAdapter.submitList(categories) {
                // 列表提交完成后，检查是否需要默认选中第一个
                if (viewModel.selectedCategory.value == null || 
                    viewModel.selectedCategory.value?.type != (if (isExpenseType) Record.TYPE_EXPENSE else Record.TYPE_INCOME)) {
                    viewModel.selectCategory(categories[0])
                }
            }
        }
    }
    
    /**
     * 显示保存成功动画
     */
    private fun showSuccessAnimation(type: String, amount: Double) {
        // 设置成功消息
        binding.tvSuccessMessage.text = "$type ¥${String.format("%.2f", amount)}\n保存成功！"
        
        // 设置背景色
        val bgColor = if (type == "支出") R.color.expense_red else R.color.income_green
        binding.cardSuccess.setCardBackgroundColor(ContextCompat.getColor(requireContext(), bgColor))
        
        // 显示动画
        binding.cardSuccess.apply {
            visibility = View.VISIBLE
            scaleX = 0f
            scaleY = 0f
            alpha = 0f
            
            animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(300)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        // 延迟后隐藏
                        postDelayed({
                            animate()
                                .scaleX(0f)
                                .scaleY(0f)
                                .alpha(0f)
                                .setDuration(200)
                                .setListener(object : AnimatorListenerAdapter() {
                                    override fun onAnimationEnd(animation: Animator) {
                                        visibility = View.GONE
                                        
                                        // 重置表单
                                        clearInputs()
                                        binding.btnSave.isEnabled = true
                                        binding.btnSave.text = "保存记录"
                                        viewModel.resetSaveState()
                                    }
                                })
                                .start()
                        }, 1500)
                    }
                })
                .start()
        }
    }
    
    /**
     * 清空输入
     */
    private fun clearInputs() {
        binding.etAmount.text?.clear()
        binding.etNote.text?.clear()
        binding.chipGroupAmount.clearCheck()  // 清除快捷金额选中状态
        selectedDate.timeInMillis = System.currentTimeMillis()
        updateDateDisplay()
        hideAmountError()
        hideCategoryError()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
