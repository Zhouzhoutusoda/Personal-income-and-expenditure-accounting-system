# 个人收支记账系统

Personal Income & Expenditure Accounting System

## 📱 项目简介

这是一个基于 Android 的个人收支记账应用，用于帮助用户管理和记录日常收支情况。

## 🛠️ 技术栈

| 技术 | 说明 |
|------|------|
| **语言** | Kotlin |
| **构建工具** | Gradle (Kotlin DSL) |
| **最低版本** | Android 7.0 (API 24) |
| **目标版本** | Android 14 (API 35) |
| **UI 框架** | XML + ViewBinding |
| **导航组件** | Navigation Component |
| **数据库** | Room (SQLite) |
| **异步处理** | Kotlin Coroutines + Flow |
| **架构模式** | MVVM (ViewModel + Repository) |
| **设计规范** | Material Design 3 |

## 📂 项目结构

```
app/src/main/java/com/example/presonalincome_expenditureaccountingsystem/
│
├── AccountingApplication.kt          # 应用程序类（初始化数据库）
├── MainActivity.kt                    # 主 Activity
│
├── data/                              # 数据层
│   ├── entity/                        # 数据实体
│   │   ├── Record.kt                  # 收支记录实体
│   │   ├── Category.kt                # 类别实体
│   │   ├── Account.kt                 # 账本实体
│   │   ├── RecordWithCategory.kt      # 记录+类别关联查询
│   │   └── DailyStatistics.kt         # 统计数据类
│   ├── dao/                           # 数据访问对象
│   │   ├── RecordDao.kt               # 记录 DAO
│   │   ├── CategoryDao.kt             # 类别 DAO
│   │   └── AccountDao.kt              # 账本 DAO
│   ├── database/                      # 数据库
│   │   └── AppDatabase.kt             # Room 数据库
│   └── repository/                    # 仓库
│       ├── RecordRepository.kt        # 记录仓库
│       ├── CategoryRepository.kt      # 类别仓库
│       └── AccountRepository.kt       # 账本仓库
│
├── ui/                                # UI 层
│   ├── adapter/                       # 适配器
│   │   └── RecordAdapter.kt           # 记录列表适配器
│   ├── record/                        # 记账页面
│   │   ├── RecordFragment.kt
│   │   └── RecordViewModel.kt
│   ├── history/                       # 记录列表页面
│   │   ├── HistoryFragment.kt
│   │   └── HistoryViewModel.kt
│   ├── statistics/                    # 统计页面
│   │   ├── StatisticsFragment.kt
│   │   └── StatisticsViewModel.kt
│   └── settings/                      # 设置页面
│       └── SettingsFragment.kt
│
└── util/                              # 工具类
    ├── DateUtils.kt                   # 日期工具
    └── CurrencyUtils.kt               # 货币格式化工具
```

## ✅ 功能清单

### 基础功能
- [x] 底部导航栏 (BottomNavigationView)
- [x] 四个主页面 (记账/记录/统计/设置)
- [x] Fragment 导航管理
- [x] Material Design 3 主题
- [x] Room 数据库设计
- [x] 数据实体 (Record/Category/Account)
- [x] DAO 接口定义 (CRUD)
- [x] Repository 仓库模式
- [x] MVVM 架构 (ViewModel)
- [x] 预置默认类别数据（17个）
- [x] 新增收/支出记录
- [x] 历史记录列表显示
- [x] 删除记录
- [x] 月度收支统计
- [ ] 编辑记录
- [ ] 收支类型自定义

### 进阶功能
- [ ] 多账本管理
- [ ] 收支趋势图表 (MPAndroidChart)
- [ ] 本地备份/恢复功能

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

## 🚀 开发进度

- **阶段一**：✅ 项目初始化 + 基础 UI 架构
- **阶段二**：✅ 数据模型设计 + Room 数据库
- **阶段三**：✅ ViewModel + CRUD 功能实现
- **阶段四**：⬜ 图表统计 + 备份恢复
- **阶段五**：⬜ 完善与优化

## 📝 更新日志

### v0.3.0 (2024-12-23)
- 创建 AccountingApplication 初始化数据库
- 实现 MVVM 架构（RecordViewModel, HistoryViewModel, StatisticsViewModel）
- 完成记账页面功能（选择类别、输入金额、保存记录）
- 完成记录列表功能（显示列表、删除记录）
- 添加 RecyclerView Adapter
- 添加完整的 Logcat 日志输出

### v0.2.0 (2024-12-23)
- 添加 Room 数据库依赖
- 创建数据实体：Record, Category, Account
- 设计 DAO 接口：RecordDao, CategoryDao, AccountDao
- 实现 Repository 仓库模式
- 添加日期工具类 DateUtils
- 添加货币格式化工具 CurrencyUtils
- 预置 17 个默认类别

### v0.1.0 (2024-12-23)
- 初始化项目结构
- 创建四个主页面 Fragment
- 实现底部导航栏
- 配置 Material Design 3 主题
- 完成基础 UI 布局

## 📄 许可证

本项目仅供学习交流使用。
