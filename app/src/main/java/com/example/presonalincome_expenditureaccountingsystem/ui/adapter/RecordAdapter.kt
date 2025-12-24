package com.example.presonalincome_expenditureaccountingsystem.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.presonalincome_expenditureaccountingsystem.R
import com.example.presonalincome_expenditureaccountingsystem.data.entity.RecordWithCategory
import com.example.presonalincome_expenditureaccountingsystem.databinding.ItemRecordBinding
import com.example.presonalincome_expenditureaccountingsystem.util.CurrencyUtils
import com.example.presonalincome_expenditureaccountingsystem.util.DateUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 记录列表适配器
 */
class RecordAdapter(
    private val onItemClick: ((RecordWithCategory) -> Unit)? = null,
    private val onItemLongClick: ((RecordWithCategory) -> Boolean)? = null
) : ListAdapter<RecordWithCategory, RecordAdapter.RecordViewHolder>(RecordDiffCallback()) {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.CHINA)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val binding = ItemRecordBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RecordViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RecordViewHolder(
        private val binding: ItemRecordBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                @Suppress("DEPRECATION")
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick?.invoke(getItem(position))
                }
            }
            
            binding.root.setOnLongClickListener {
                @Suppress("DEPRECATION")
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemLongClick?.invoke(getItem(position)) ?: false
                } else {
                    false
                }
            }
        }

        fun bind(item: RecordWithCategory) {
            val record = item.record
            val category = item.category
            val context = binding.root.context
            
            // 设置类别名称
            binding.tvCategory.text = category?.name ?: "未知类别"
            
            // 设置备注
            binding.tvNote.text = if (record.note.isNotEmpty()) {
                record.note
            } else {
                DateUtils.formatFriendly(record.date)
            }
            
            // 设置时间
            binding.tvTime.text = timeFormat.format(Date(record.date))
            
            // 设置金额
            val amountText = CurrencyUtils.formatWithSign(record.amount, record.isIncome)
            binding.tvAmount.text = amountText
            
            // 根据类型设置颜色
            val amountColor = if (record.isExpense) {
                R.color.expense_red
            } else {
                R.color.income_green
            }
            binding.tvAmount.setTextColor(ContextCompat.getColor(context, amountColor))
            
            // 设置图标背景色
            val bgColor = if (record.isExpense) {
                R.color.expense_red_light
            } else {
                R.color.income_green_light
            }
            binding.viewIconBg.backgroundTintList = ContextCompat.getColorStateList(context, bgColor)
            
            // 设置 Emoji 图标
            val emoji = getCategoryEmoji(category?.name ?: "")
            binding.tvCategoryIcon.text = emoji
        }
        
        /**
         * 根据类别名称获取 Emoji 图标
         */
        private fun getCategoryEmoji(categoryName: String): String {
            return when (categoryName) {
                "餐饮" -> "🍔"
                "交通" -> "🚗"
                "购物" -> "🛒"
                "娱乐" -> "🎮"
                "居住" -> "🏠"
                "通讯" -> "📱"
                "医疗" -> "💊"
                "教育" -> "📕"
                "人情" -> "🎁"
                "工资" -> "💵"
                "奖金" -> "🎉"
                "投资" -> "📈"
                "兼职" -> "💼"
                "理财" -> "🏦"
                "红包" -> "🧧"
                "其他" -> "📝"
                else -> "💰"
            }
        }
    }

    /**
     * DiffUtil 回调
     */
    class RecordDiffCallback : DiffUtil.ItemCallback<RecordWithCategory>() {
        override fun areItemsTheSame(oldItem: RecordWithCategory, newItem: RecordWithCategory): Boolean {
            return oldItem.record.id == newItem.record.id
        }

        override fun areContentsTheSame(oldItem: RecordWithCategory, newItem: RecordWithCategory): Boolean {
            return oldItem == newItem
        }
    }
}
