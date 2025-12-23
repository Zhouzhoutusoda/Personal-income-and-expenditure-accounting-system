package com.example.presonalincome_expenditureaccountingsystem

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.presonalincome_expenditureaccountingsystem.databinding.ActivityMainBinding

/**
 * 主 Activity
 * 包含底部导航栏和 Fragment 容器
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 启用 Edge-to-Edge 显示
        WindowCompat.setDecorFitsSystemWindows(window, true)
        
        // 初始化 ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 设置导航
        setupNavigation()
    }

    /**
     * 设置底部导航栏与 Navigation 组件的关联
     */
    private fun setupNavigation() {
        // 获取 NavHostFragment
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        
        // 获取 NavController
        val navController = navHostFragment.navController

        // 将底部导航栏与 NavController 关联
        binding.bottomNavigation.setupWithNavController(navController)
    }
}
