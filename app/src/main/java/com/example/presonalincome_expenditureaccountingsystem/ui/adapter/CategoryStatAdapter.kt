package com.example.presonalincome_expenditureaccountingsystem.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.presonalincome_expenditureaccountingsystem.data.entity.CategoryStatistics
import com.example.presonalincome_expenditureaccountingsystem.databinding.ItemCategoryStatBinding
import com.example.presonalincome_expenditureaccountingsystem.util.CurrencyUtils

/**
 * 分类统计列表适配器
 */
class CategoryStatAdapter : ListAdapter<CategoryStatistics, CategoryStatAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCategoryStatBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemCategoryStatBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CategoryStatistics) {
            binding.tvCategoryName.text = item.categoryName
            binding.tvAmount.text = CurrencyUtils.formatWithSymbol(item.totalAmount)
            binding.tvPercentage.text = String.format("%.1f%%", item.percentage)
            
            // 设置颜色指示器
            binding.viewColor.backgroundTintList = android.content.res.ColorStateList.valueOf(item.color)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<CategoryStatistics>() {
        override fun areItemsTheSame(oldItem: CategoryStatistics, newItem: CategoryStatistics): Boolean {
            return oldItem.categoryName == newItem.categoryName
        }

        override fun areContentsTheSame(oldItem: CategoryStatistics, newItem: CategoryStatistics): Boolean {
            return oldItem == newItem
        }
    }
}

