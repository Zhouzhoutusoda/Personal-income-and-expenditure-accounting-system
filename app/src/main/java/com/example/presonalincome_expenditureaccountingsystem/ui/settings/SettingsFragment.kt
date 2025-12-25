package com.example.presonalincome_expenditureaccountingsystem.ui.settings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.presonalincome_expenditureaccountingsystem.AccountingApplication
import com.example.presonalincome_expenditureaccountingsystem.R
import com.example.presonalincome_expenditureaccountingsystem.data.entity.Account
import com.example.presonalincome_expenditureaccountingsystem.databinding.DialogAccountManagerBinding
import com.example.presonalincome_expenditureaccountingsystem.databinding.DialogAddAccountBinding
import com.example.presonalincome_expenditureaccountingsystem.databinding.FragmentSettingsBinding
import com.example.presonalincome_expenditureaccountingsystem.ui.adapter.AccountAdapter
import com.example.presonalincome_expenditureaccountingsystem.util.AccountManager
import com.example.presonalincome_expenditureaccountingsystem.util.BackupUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 设置页面 Fragment
 * 提供数据备份、恢复、清空等功能
 */
class SettingsFragment : Fragment() {

    companion object {
        private const val TAG = "SettingsFragment"
    }

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    // 文件选择器
    private lateinit var createFileLauncher: ActivityResultLauncher<Intent>
    private lateinit var openFileLauncher: ActivityResultLauncher<Intent>
    
    // 账本管理对话框
    private var accountManagerDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 注册文件创建回调（用于备份）
        createFileLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    performBackup(uri)
                }
            }
        }
        
        // 注册文件打开回调（用于恢复）
        openFileLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    showRestoreConfirmDialog(uri)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupClickListeners()
        loadRecordCount()
        observeCurrentAccount()
    }
    
    override fun onResume() {
        super.onResume()
        loadRecordCount()
        
        // 刷新当前账本信息
        viewLifecycleOwner.lifecycleScope.launch {
            AccountManager.refreshCurrentAccount()
        }
    }
    
    /**
     * 观察当前账本变化
     */
    private fun observeCurrentAccount() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                AccountManager.currentAccount.collect { account ->
                    if (_binding != null && account != null) {
                        updateCurrentAccountUI(account)
                    }
                }
            }
        }
    }
    
    /**
     * 更新当前账本 UI
     */
    private fun updateCurrentAccountUI(account: Account) {
        binding.tvAccountName.text = account.name
        binding.tvAccountIcon.text = getAccountIcon(account.icon)
    }
    
    /**
     * 获取账本图标
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

    /**
     * 加载当前账本的记录数量
     */
    private fun loadRecordCount() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val accountId = AccountManager.getCurrentAccountIdSync()
                val count = AccountingApplication.recordRepository.getRecordCountByAccount(accountId)
                binding.tvRecordSummary.text = "已记录 $count 笔"
            } catch (e: Exception) {
                binding.tvRecordSummary.text = "已记录 0 笔"
            }
        }
    }

    /**
     * 设置点击事件
     */
    private fun setupClickListeners() {
        // 账本切换
        binding.cardCurrentAccount.setOnClickListener {
            showAccountManagerDialog()
        }
        
        // 备份数据
        binding.layoutBackup.setOnClickListener {
            showBackupDialog()
        }

        // 恢复数据
        binding.layoutRestore.setOnClickListener {
            showRestoreDialog()
        }

        // 清空数据
        binding.layoutClear.setOnClickListener {
            showClearDialog()
        }

        // 关于
        binding.layoutAbout.setOnClickListener {
            showAboutDialog()
        }
    }
    
    /**
     * 显示账本管理对话框
     */
    private fun showAccountManagerDialog() {
        val dialogBinding = DialogAccountManagerBinding.inflate(layoutInflater)
        
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .create()
        
        accountManagerDialog = dialog
        
        // 设置 RecyclerView
        dialogBinding.rvAccounts.layoutManager = LinearLayoutManager(requireContext())
        
        // 加载账本列表
        loadAccountList(dialogBinding, dialog)
        
        // 添加账本按钮
        dialogBinding.btnAddAccount.setOnClickListener {
            dialog.dismiss()
            showAddAccountDialog()
        }
        
        dialog.show()
    }
    
    /**
     * 加载账本列表
     */
    private fun loadAccountList(dialogBinding: DialogAccountManagerBinding, dialog: AlertDialog) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val accounts = AccountingApplication.accountRepository.getAllAccountsList()
                val currentAccountId = AccountManager.getCurrentAccountIdSync()
                
                if (accounts.isEmpty()) {
                    dialogBinding.rvAccounts.visibility = View.GONE
                    dialogBinding.tvEmpty.visibility = View.VISIBLE
                } else {
                    dialogBinding.rvAccounts.visibility = View.VISIBLE
                    dialogBinding.tvEmpty.visibility = View.GONE
                    
                    val adapter = AccountAdapter(
                        currentAccountId = currentAccountId,
                        onItemClick = { account ->
                            // 切换账本
                            switchAccount(account, dialog)
                        },
                        onDeleteClick = { account ->
                            // 删除账本
                            showDeleteAccountDialog(account, dialogBinding, dialog)
                        }
                    )
                    
                    dialogBinding.rvAccounts.adapter = adapter
                    adapter.submitList(accounts)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "加载账本列表失败: ${e.message}", e)
            }
        }
    }
    
    /**
     * 切换账本
     */
    private fun switchAccount(account: Account, dialog: AlertDialog) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                AccountManager.switchAccount(account.id)
                dialog.dismiss()
                
                // 刷新记录数量
                loadRecordCount()
                
                Snackbar.make(
                    binding.root,
                    "已切换到「${account.name}」",
                    Snackbar.LENGTH_SHORT
                ).show()
                
                Log.d(TAG, "✅ 切换账本成功: ${account.name}")
                
            } catch (e: Exception) {
                Log.e(TAG, "切换账本失败: ${e.message}", e)
                showError("切换账本失败")
            }
        }
    }
    
    /**
     * 显示添加账本对话框
     */
    private fun showAddAccountDialog() {
        val dialogBinding = DialogAddAccountBinding.inflate(layoutInflater)
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("新建账本")
            .setView(dialogBinding.root)
            .setPositiveButton("创建") { _, _ ->
                val name = dialogBinding.etName.text.toString().trim()
                val description = dialogBinding.etDescription.text.toString().trim()
                val icon = getSelectedIcon(dialogBinding)
                
                if (name.isEmpty()) {
                    Toast.makeText(requireContext(), "请输入账本名称", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                createAccount(name, description, icon)
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    /**
     * 获取选中的图标
     */
    private fun getSelectedIcon(dialogBinding: DialogAddAccountBinding): String {
        return when (dialogBinding.chipGroupIcons.checkedChipId) {
            R.id.chip_wallet -> "ic_wallet"
            R.id.chip_travel -> "ic_travel"
            R.id.chip_home -> "ic_home"
            R.id.chip_car -> "ic_car"
            R.id.chip_shopping -> "ic_shopping"
            R.id.chip_education -> "ic_education"
            else -> "ic_wallet"
        }
    }
    
    /**
     * 创建账本
     */
    private fun createAccount(name: String, description: String, icon: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = AccountingApplication.accountRepository.createAccount(name, description, icon)
                
                if (result > 0) {
                    Snackbar.make(
                        binding.root,
                        "账本「$name」创建成功",
                        Snackbar.LENGTH_SHORT
                    ).show()
                    
                    Log.d(TAG, "✅ 创建账本成功: $name (ID: $result)")
                    
                    // 重新打开账本管理对话框
                    showAccountManagerDialog()
                } else {
                    Toast.makeText(requireContext(), "账本名称已存在", Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "创建账本失败: ${e.message}", e)
                showError("创建账本失败")
            }
        }
    }
    
    /**
     * 显示删除账本确认对话框
     */
    private fun showDeleteAccountDialog(
        account: Account,
        dialogBinding: DialogAccountManagerBinding,
        parentDialog: AlertDialog
    ) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("删除账本")
            .setMessage("确定要删除「${account.name}」吗？\n\n⚠️ 该账本下的所有记录也将被删除！")
            .setPositiveButton("删除") { _, _ ->
                deleteAccount(account, dialogBinding, parentDialog)
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    /**
     * 删除账本
     */
    private fun deleteAccount(
        account: Account,
        dialogBinding: DialogAccountManagerBinding,
        parentDialog: AlertDialog
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = AccountingApplication.accountRepository.deleteById(account.id)
                
                if (result > 0) {
                    Snackbar.make(
                        binding.root,
                        "账本「${account.name}」已删除",
                        Snackbar.LENGTH_SHORT
                    ).show()
                    
                    Log.d(TAG, "✅ 删除账本成功: ${account.name}")
                    
                    // 刷新列表
                    loadAccountList(dialogBinding, parentDialog)
                } else {
                    Toast.makeText(requireContext(), "无法删除该账本", Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "删除账本失败: ${e.message}", e)
                showError("删除账本失败")
            }
        }
    }
    
    /**
     * 显示备份对话框
     */
    private fun showBackupDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("备份数据")
            .setMessage("将所有记账数据导出为 JSON 文件，您可以稍后用它来恢复数据。")
            .setIcon(R.drawable.ic_backup)
            .setPositiveButton("开始备份") { _, _ ->
                startBackup()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    /**
     * 开始备份流程
     */
    private fun startBackup() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, BackupUtils.generateBackupFileName())
        }
        createFileLauncher.launch(intent)
    }
    
    /**
     * 执行备份操作
     */
    private fun performBackup(uri: android.net.Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                showLoading("正在备份数据...")
                
                val result = BackupUtils.exportData(requireContext(), uri)
                
                hideLoading()
                
                // 检查 binding 是否仍然有效
                if (_binding == null) return@launch
                
                if (result.isSuccess) {
                    Snackbar.make(
                        binding.root,
                        "✅ ${result.getOrNull()}",
                        Snackbar.LENGTH_LONG
                    ).show()
                } else {
                    showError("备份失败: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                hideLoading()
                if (_binding != null) {
                    showError("备份失败: ${e.message}")
                }
            }
        }
    }
    
    /**
     * 显示恢复对话框
     */
    private fun showRestoreDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("恢复数据")
            .setMessage("从 JSON 备份文件恢复数据。\n\n注意：如果恢复前选择清空数据，现有数据将被删除。")
            .setIcon(R.drawable.ic_restore)
            .setPositiveButton("选择文件") { _, _ ->
                startRestore()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    /**
     * 开始恢复流程
     */
    private fun startRestore() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }
        openFileLauncher.launch(intent)
    }
    
    /**
     * 显示恢复确认对话框
     */
    private fun showRestoreConfirmDialog(uri: android.net.Uri) {
        val options = arrayOf("保留现有数据（合并）", "清空现有数据（覆盖）")
        var selectedOption = 0
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("恢复选项")
            .setSingleChoiceItems(options, selectedOption) { _, which ->
                selectedOption = which
            }
            .setPositiveButton("开始恢复") { _, _ ->
                performRestore(uri, clearExisting = selectedOption == 1)
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    /**
     * 执行恢复操作
     */
    private fun performRestore(uri: android.net.Uri, clearExisting: Boolean) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                showLoading("正在恢复数据...")
                
                val result = BackupUtils.importData(requireContext(), uri, clearExisting)
                
                hideLoading()
                
                // 检查 binding 是否仍然有效
                if (_binding == null) return@launch
                
                if (result.isSuccess) {
                    loadRecordCount()
                    Snackbar.make(
                        binding.root,
                        "✅ ${result.getOrNull()}",
                        Snackbar.LENGTH_LONG
                    ).show()
                } else {
                    showError("恢复失败: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                hideLoading()
                if (_binding != null) {
                    showError("恢复失败: ${e.message}")
                }
            }
        }
    }
    
    /**
     * 显示清空数据对话框
     */
    private fun showClearDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("⚠️ 清空所有数据")
            .setMessage("此操作将删除所有记账记录！\n\n删除后无法恢复，建议先备份数据。\n\n确定要继续吗？")
            .setPositiveButton("清空数据") { _, _ ->
                showFinalClearConfirmation()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    /**
     * 显示最终清空确认
     */
    private fun showFinalClearConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("最后确认")
            .setMessage("请再次确认：删除所有数据后无法恢复！")
            .setPositiveButton("确认删除") { _, _ ->
                performClear()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    /**
     * 执行清空操作
     */
    private fun performClear() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                showLoading("正在清空数据...")
                
                val result = BackupUtils.clearAllData()
                
                hideLoading()
                
                // 检查 binding 是否仍然有效
                if (_binding == null) return@launch
                
                if (result.isSuccess) {
                    loadRecordCount()
                    Snackbar.make(
                        binding.root,
                        "✅ 数据已清空",
                        Snackbar.LENGTH_LONG
                    ).show()
                } else {
                    showError("清空失败: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                hideLoading()
                if (_binding != null) {
                    showError("清空失败: ${e.message}")
                }
            }
        }
    }
    
    /**
     * 显示关于对话框
     */
    private fun showAboutDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("关于应用")
            .setMessage("""
                个人收支记账系统
                
                版本：1.0.0
                
                功能特点：
                • 快速记账，简洁直观
                • 多种类别分类统计
                • 支持数据备份与恢复
                • 精美图表数据可视化
                
                技术栈：
                • Kotlin + XML
                • Room 数据库
                • MVVM 架构
                • Material Design 3
                • MPAndroidChart
            """.trimIndent())
            .setPositiveButton("确定", null)
            .show()
    }
    
    private var loadingDialog: AlertDialog? = null
    
    /**
     * 显示加载对话框
     */
    private fun showLoading(message: String) {
        loadingDialog = MaterialAlertDialogBuilder(requireContext())
            .setMessage(message)
            .setCancelable(false)
            .create()
        loadingDialog?.show()
    }
    
    /**
     * 隐藏加载对话框
     */
    private fun hideLoading() {
        loadingDialog?.dismiss()
        loadingDialog = null
    }
    
    /**
     * 显示错误消息
     */
    private fun showError(message: String) {
        // 安全检查 binding 是否有效
        if (_binding == null) return
        Snackbar.make(binding.root, "❌ $message", Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        loadingDialog?.dismiss()
        accountManagerDialog?.dismiss()
        _binding = null
    }
}
