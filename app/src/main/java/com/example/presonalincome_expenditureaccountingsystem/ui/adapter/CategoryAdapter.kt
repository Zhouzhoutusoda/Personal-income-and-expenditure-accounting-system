package com.example.presonalincome_expenditureaccountingsystem.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.presonalincome_expenditureaccountingsystem.R
import com.example.presonalincome_expenditureaccountingsystem.data.entity.Category
import com.example.presonalincome_expenditureaccountingsystem.databinding.ItemCategoryBinding

/**
 * 类别选择适配器
 */
class CategoryAdapter(
    private val onCategorySelected: (Category) -> Unit
) : ListAdapter<Category, CategoryAdapter.CategoryViewHolder>(CategoryDiffCallback()) {
    
    // 当前选中的类别ID
    private var selectedCategoryId: Long = -1
    
    // 当前类型（用于决定颜色）
    private var isExpense: Boolean = true
    
    /**
     * 设置选中的类别
     */
    fun setSelectedCategory(categoryId: Long) {
        if (selectedCategoryId == categoryId) return
        
        val oldSelectedId = selectedCategoryId
        selectedCategoryId = categoryId
        
        // 找到并更新旧选中项和新选中项
        val oldPosition = currentList.indexOfFirst { it.id == oldSelectedId }
        val newPosition = currentList.indexOfFirst { it.id == categoryId }
        
        if (oldPosition >= 0) {
            notifyItemChanged(oldPosition, PAYLOAD_SELECTION_CHANGED)
        }
        if (newPosition >= 0) {
            notifyItemChanged(newPosition, PAYLOAD_SELECTION_CHANGED)
        }
    }
    
    /**
     * 设置类型（支出/收入）
     */
    fun setExpenseType(isExpense: Boolean) {
        if (this.isExpense != isExpense) {
            this.isExpense = isExpense
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
        } else {
            // 只更新选中状态，不重新绑定整个视图
            holder.updateSelection(getItem(position))
        }
    }

    inner class CategoryViewHolder(
        private val binding: ItemCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentCategory: Category? = null

        init {
            binding.root.setOnClickListener {
                currentCategory?.let { category ->
                    // 先更新选中状态
                    val oldSelectedId = selectedCategoryId
                    selectedCategoryId = category.id
                    
                    // 更新旧选中项
                    val oldPosition = currentList.indexOfFirst { it.id == oldSelectedId }
                    if (oldPosition >= 0 && oldPosition != bindingAdapterPosition) {
                        notifyItemChanged(oldPosition, PAYLOAD_SELECTION_CHANGED)
                    }
                    
                    // 更新当前项
                    updateSelection(category)
                    
                    // 回调
                    onCategorySelected(category)
                }
            }
        }

        fun bind(category: Category) {
            currentCategory = category
            val context = binding.root.context
            
            // 设置类别名称
            binding.tvName.text = category.name
            
            // 设置图标
            val iconRes = getCategoryIcon(category.name)
            binding.ivIcon.setImageResource(iconRes)
            
            // 更新选中状态
            updateSelection(category)
        }
        
        fun updateSelection(category: Category) {
            val context = binding.root.context
            val isSelected = category.id == selectedCategoryId
            
            // 根据选中状态和类型设置颜色
            val primaryColor = if (isExpense) R.color.expense_red else R.color.income_green
            val lightColor = if (isExpense) R.color.expense_red_light else R.color.income_green_light
            
            if (isSelected) {
                // 选中状态
                binding.cardCategory.setCardBackgroundColor(
                    ContextCompat.getColor(context, primaryColor)
                )
                binding.cardCategory.strokeWidth = 0
                binding.ivIcon.imageTintList = ContextCompat.getColorStateList(context, android.R.color.white)
                binding.tvName.setTextColor(ContextCompat.getColor(context, primaryColor))
            } else {
                // 未选中状态
                binding.cardCategory.setCardBackgroundColor(
                    ContextCompat.getColor(context, lightColor)
                )
                binding.cardCategory.strokeWidth = 0
                binding.ivIcon.imageTintList = ContextCompat.getColorStateList(context, primaryColor)
                binding.tvName.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            }
        }
        
        /**
         * 根据类别名称获取图标
         */
        private fun getCategoryIcon(categoryName: String): Int {
            return when (categoryName) {
                "餐饮" -> R.drawable.ic_food
                "交通" -> R.drawable.ic_transport
                "购物" -> R.drawable.ic_shopping
                "娱乐" -> R.drawable.ic_entertainment
                "居住" -> R.drawable.ic_home_category
                "通讯" -> R.drawable.ic_phone
                "医疗" -> R.drawable.ic_medical
                "教育" -> R.drawable.ic_education
                "人情" -> R.drawable.ic_gift
                "工资" -> R.drawable.ic_salary
                "奖金" -> R.drawable.ic_bonus
                "投资" -> R.drawable.ic_investment
                "兼职" -> R.drawable.ic_parttime
                "理财" -> R.drawable.ic_finance
                "红包" -> R.drawable.ic_redpacket
                else -> R.drawable.ic_other
            }
        }
    }

    class CategoryDiffCallback : DiffUtil.ItemCallback<Category>() {
        override fun areItemsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem == newItem
        }
    }
    
    companion object {
        private const val PAYLOAD_SELECTION_CHANGED = "selection_changed"
    }
}
