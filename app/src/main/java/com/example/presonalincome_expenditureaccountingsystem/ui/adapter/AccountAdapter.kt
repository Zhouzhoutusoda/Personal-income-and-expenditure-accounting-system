package com.example.presonalincome_expenditureaccountingsystem.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.presonalincome_expenditureaccountingsystem.R
import com.example.presonalincome_expenditureaccountingsystem.data.entity.Account
import com.example.presonalincome_expenditureaccountingsystem.databinding.ItemAccountBinding

/**
 * 账本列表适配器
 */
class AccountAdapter(
    private val currentAccountId: Long,
    private val onItemClick: (Account) -> Unit,
    private val onDeleteClick: (Account) -> Unit
) : ListAdapter<Account, AccountAdapter.AccountViewHolder>(AccountDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        val binding = ItemAccountBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AccountViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AccountViewHolder(
        private val binding: ItemAccountBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(account: Account) {
            binding.apply {
                // 账本名称
                tvName.text = account.name
                
                // 账本描述
                tvDescription.text = account.description.ifEmpty { "暂无描述" }
                
                // 账本图标
                tvIcon.text = getAccountIcon(account.icon)
                
                // 是否为当前账本
                val isCurrent = account.id == currentAccountId
                tvCurrentTag.visibility = if (isCurrent) View.VISIBLE else View.GONE
                ivSelected.visibility = if (isCurrent) View.VISIBLE else View.GONE
                
                // 是否为默认账本
                tvDefaultTag.visibility = if (account.isDefault && !isCurrent) View.VISIBLE else View.GONE
                
                // 删除按钮（当前账本和默认账本不能删除）
                val canDelete = !isCurrent && !account.isDefault
                ivDelete.visibility = if (canDelete) View.VISIBLE else View.GONE
                
                // 高亮当前账本
                if (isCurrent) {
                    cardAccount.strokeColor = ContextCompat.getColor(root.context, R.color.primary)
                    cardAccount.strokeWidth = root.context.resources.getDimensionPixelSize(R.dimen.stroke_width_selected)
                    viewIconBg.backgroundTintList = ContextCompat.getColorStateList(root.context, R.color.primary_light)
                } else {
                    cardAccount.strokeColor = ContextCompat.getColor(root.context, R.color.divider)
                    cardAccount.strokeWidth = root.context.resources.getDimensionPixelSize(R.dimen.stroke_width_normal)
                    viewIconBg.backgroundTintList = ContextCompat.getColorStateList(root.context, R.color.background)
                }
                
                // 点击事件
                cardAccount.setOnClickListener {
                    onItemClick(account)
                }
                
                // 删除事件
                ivDelete.setOnClickListener {
                    onDeleteClick(account)
                }
            }
        }
        
        /**
         * 根据图标名称获取 Emoji
         */
        private fun getAccountIcon(iconName: String): String {
            return when (iconName) {
                "ic_wallet" -> "💰"
                "ic_travel" -> "✈️"
                "ic_home" -> "🏠"
                "ic_car" -> "🚗"
                "ic_gift" -> "🎁"
                "ic_shopping" -> "🛒"
                "ic_food" -> "🍔"
                "ic_health" -> "💊"
                "ic_education" -> "📚"
                "ic_entertainment" -> "🎮"
                else -> "📒"
            }
        }
    }

    class AccountDiffCallback : DiffUtil.ItemCallback<Account>() {
        override fun areItemsTheSame(oldItem: Account, newItem: Account): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Account, newItem: Account): Boolean {
            return oldItem == newItem
        }
    }
}

