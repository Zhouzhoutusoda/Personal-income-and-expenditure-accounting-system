# 个人收支记账系统

Personal Income & Expenditure Accounting System

## 📱 项目简介

这是一个功能完善的 Android 个人收支记账应用，采用现代化架构设计，帮助用户轻松管理和记录日常收支情况。支持智能快速记账、多维度统计分析、数据备份恢复等功能。

## ✨ 功能特点

- 🎯 **快速记账** - 支持常规记账和智能语义识别快速记账
- 📊 **数据统计** - 饼图、折线图多维度可视化分析
- 🔍 **记录管理** - 支持搜索、筛选、滑动删除
- 💾 **数据备份** - JSON 格式导出/导入，数据永不丢失
- 🎨 **精美界面** - Material Design 3 设计规范

## 🛠️ 技术栈

| 技术 | 说明 |
|------|------|
| **语言** | Kotlin |
| **构建工具** | Gradle (Kotlin DSL) |
| **最低版本** | Android 7.0 (API 24) |
| **目标版本** | Android 15 (API 35) |
| **UI 框架** | XML + ViewBinding |
| **导航组件** | Navigation Component |
| **数据库** | Room (SQLite) |
| **异步处理** | Kotlin Coroutines + Flow |
| **架构模式** | MVVM (ViewModel + Repository) |
| **图表库** | MPAndroidChart |
| **设计规范** | Material Design 3 |

## 📂 项目结构

```
app/src/main/java/com/example/presonalincome_expenditureaccountingsystem/
│
├── AccountingApplication.kt          # 应用程序类（初始化数据库）
├── MainActivity.kt                   # 主 Activity
│
├── data/                             # 数据层
│   ├── entity/                       # 数据实体
│   │   ├── Record.kt                 # 收支记录实体
│   │   ├── Category.kt               # 类别实体
│   │   ├── Account.kt                # 账本实体
│   │   ├── RecordWithCategory.kt     # 记录+类别关联查询
│   │   └── DailyStatistics.kt        # 统计数据类
│   ├── dao/                          # 数据访问对象
│   │   ├── RecordDao.kt              # 记录 DAO
│   │   ├── CategoryDao.kt            # 类别 DAO
│   │   └── AccountDao.kt             # 账本 DAO
│   ├── database/                     # 数据库
│   │   └── AppDatabase.kt            # Room 数据库
│   └── repository/                   # 仓库
│       ├── RecordRepository.kt       # 记录仓库
│       ├── CategoryRepository.kt     # 类别仓库
│       └── AccountRepository.kt      # 账本仓库
│
├── ui/                               # UI 层
│   ├── adapter/                      # 适配器
│   │   ├── RecordAdapter.kt          # 记录列表适配器
│   │   ├── CategoryAdapter.kt        # 类别选择适配器
│   │   └── CategoryStatAdapter.kt    # 统计类别适配器
│   ├── view/                         # 自定义视图
│   │   ├── PieChartView.kt           # 饼图视图
│   │   └── TrendChartView.kt         # 趋势图视图
│   ├── record/                       # 记账页面
│   │   ├── RecordFragment.kt
│   │   └── RecordViewModel.kt
│   ├── history/                      # 记录列表页面
│   │   ├── HistoryFragment.kt
│   │   └── HistoryViewModel.kt
│   ├── statistics/                   # 统计页面
│   │   ├── StatisticsFragment.kt
│   │   └── StatisticsViewModel.kt
│   └── settings/                     # 设置页面
│       └── SettingsFragment.kt
│
└── util/                             # 工具类
    ├── DateUtils.kt                  # 日期工具
    ├── CurrencyUtils.kt              # 货币格式化工具
    ├── BackupUtils.kt                # 备份恢复工具
    └── SmartRecordParser.kt          # 智能记账解析器
```

## ✅ 功能清单

### 📝 记账功能
- [x] 收入/支出快速切换
- [x] 17 个预置类别（餐饮、交通、购物、工资等）
- [x] 快捷金额选择（¥10、¥50、¥100、¥500）
- [x] 智能快速记账（自然语言识别，如"午饭20元"）
- [x] 日期选择器
- [x] 备注功能
- [x] 保存成功动画反馈

### 📋 记录管理
- [x] 历史记录列表显示
- [x] 按月/年/全部时间筛选
- [x] 收入/支出类型筛选
- [x] 关键词搜索
- [x] 滑动删除记录
- [x] 长按查看详情
- [x] 下拉刷新

### 📊 统计分析
- [x] 月度/年度/全部收支统计
- [x] 收支结余计算
- [x] 日均/月均/年均统计
- [x] 支出分类饼图
- [x] 收入分类饼图
- [x] 收支趋势折线图
- [x] 分类排行列表

### ⚙️ 设置功能
- [x] 数据备份（导出 JSON）
- [x] 数据恢复（导入 JSON）
- [x] 清空所有数据
- [x] 关于应用

### 🏗️ 架构特性
- [x] MVVM 架构模式
- [x] Repository 仓库模式
- [x] Room 数据库持久化
- [x] Kotlin Coroutines 异步处理
- [x] Flow 响应式数据流
- [x] ViewBinding 视图绑定
- [x] Navigation Component 导航
- [x] Material Design 3 主题

## 🔍 Logcat 日志标签

在 Logcat 中过滤以下标签可以查看数据操作日志：

| 标签 | 说明 |
|------|------|
| `AccountingApp` | 应用初始化日志 |
| `RecordViewModel` | 记账操作日志 |
| `HistoryViewModel` | 记录列表日志 |
| `StatisticsViewModel` | 统计数据日志 |
| `RecordFragment` | 记账页面日志 |
| `HistoryFragment` | 记录页面日志 |
| `BackupUtils` | 备份恢复日志 |

## 📸 应用截图

| 记账页面 | 记录列表 | 统计分析 | 设置页面 |
|:--------:|:--------:|:--------:|:--------:|
| 快速记账 | 历史记录 | 图表统计 | 数据管理 |

## 🚀 快速开始

### 环境要求
- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 11 或更高版本
- Android SDK 35

### 构建步骤
1. 克隆项目
```bash
git clone https://github.com/your-username/personal-accounting.git
```

2. 使用 Android Studio 打开项目

3. 同步 Gradle 依赖

4. 运行应用
```bash
./gradlew assembleDebug
```

## 📝 更新日志

### v1.0.0 (2024-12-24)
- 🎉 项目完成，发布正式版本
- ✨ 完成所有核心功能
- 🐛 修复潜在的闪退问题
- ⚡ 优化协程异常处理
- 🔒 增强应用稳定性

### v0.4.0 (2024-12-24)
- 添加智能快速记账功能
- 添加数据备份/恢复功能
- 优化统计页面图表显示
- 添加多时间范围筛选

### v0.3.0 (2024-12-23)
- 创建 AccountingApplication 初始化数据库
- 实现 MVVM 架构
- 完成记账页面功能
- 完成记录列表功能
- 添加 RecyclerView Adapter

### v0.2.0 (2024-12-23)
- 添加 Room 数据库依赖
- 创建数据实体：Record, Category, Account
- 设计 DAO 接口
- 实现 Repository 仓库模式
- 添加工具类

### v0.1.0 (2024-12-23)
- 初始化项目结构
- 创建四个主页面 Fragment
- 实现底部导航栏
- 配置 Material Design 3 主题
- 完成基础 UI 布局

## 📄 许可证

本项目仅供学习交流使用。

## 👨‍💻 作者

个人开发项目

---

⭐ 如果这个项目对你有帮助，欢迎 Star！
