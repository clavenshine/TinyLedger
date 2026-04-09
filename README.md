# 小小记账本 (TinyLedger)

一个简洁易用的Android记账应用，使用现代Android开发技术栈构建。

## 📱 功能特性

### 核心功能
- ✅ **记账功能** - 快速记录收入/支出
- ✅ **分类管理** - 预设常用分类（餐饮、交通、购物等）
- ✅ **账单管理** - 按月/类型/分类查看账单
- ✅ **统计报表** - 月度收支概览、支出分类饼图
- ✅ **数据持久化** - 使用Room数据库本地存储
- ✅ **主题切换** - 支持浅色/深色/跟随系统

### 技术亮点
- 🎨 **Material Design 3** - 遵循最新设计规范
- 📱 **Jetpack Compose** - 现代声明式UI
- 🏗️ **MVVM架构** - 清晰的分层设计
- 💉 **Hilt依赖注入** - 简化依赖管理
- 🗄️ **Room数据库** - 高效本地数据存储
- 🎯 **Kotlin协程** - 优雅的异步处理

## 🛠️ 技术栈

| 技术 | 版本 |
|------|------|
| Kotlin | 1.9.24 |
| Jetpack Compose | BOM 2024.02.00 |
| Android Gradle Plugin | 8.5.0 |
| Gradle | 8.14 |
| Hilt | 2.51.1 |
| Room | 2.6.1 |
| Target SDK | 34 |
| Min SDK | 26 |

## 📁 项目结构

```
app/src/main/java/com/tinyledger/app/
├── MainActivity.kt           # 应用入口
├── TinyLedgerApp.kt          # Application类
├── di/                       # 依赖注入模块
│   ├── AppModule.kt
│   └── DatabaseModule.kt
├── data/                     # 数据层
│   ├── local/
│   │   ├── AppDatabase.kt
│   │   ├── dao/
│   │   │   └── TransactionDao.kt
│   │   └── entity/
│   │       └── TransactionEntity.kt
│   └── repository/
│       ├── TransactionRepositoryImpl.kt
│       └── PreferencesRepositoryImpl.kt
├── domain/                   # 领域层
│   ├── model/
│   │   ├── Transaction.kt
│   │   ├── Statistics.kt
│   │   └── AppSettings.kt
│   └── repository/
│       ├── TransactionRepository.kt
│       └── PreferencesRepository.kt
├── ui/                       # UI层
│   ├── theme/
│   │   ├── Color.kt
│   │   ├── Shape.kt
│   │   ├── Theme.kt
│   │   └── Type.kt
│   ├── navigation/
│   │   ├── Screen.kt
│   │   ├── BottomNavItem.kt
│   │   └── NavHost.kt
│   ├── components/
│   │   ├── TransactionCard.kt
│   │   ├── CategorySelector.kt
│   │   └── AmountDisplay.kt
│   ├── screens/
│   │   ├── home/
│   │   │   ├── HomeScreen.kt
│   │   │   └── AddTransactionScreen.kt
│   │   ├── bills/
│   │   │   └── BillsScreen.kt
│   │   ├── statistics/
│   │   │   └── StatisticsScreen.kt
│   │   └── settings/
│   │       └── SettingsScreen.kt
│   └── viewmodel/
│       ├── HomeViewModel.kt
│       ├── AddTransactionViewModel.kt
│       ├── BillsViewModel.kt
│       ├── StatisticsViewModel.kt
│       └── SettingsViewModel.kt
└── util/
    ├── DateUtils.kt
    └── CurrencyUtils.kt
```

## 🚀 快速开始

### 环境要求
- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17+
- Android SDK 35
- Gradle 8.14

### 构建步骤

1. **克隆项目**
```bash
git clone <repository-url>
cd TinyLedger
```

2. **配置环境变量** (Windows)
```powershell
$env:ANDROID_HOME = "C:\Users\<YourUser>\AppData\Local\Android\Sdk"
$env:ANDROID_SDK_ROOT = "C:\Users\<YourUser>\AppData\Local\Android\Sdk"
```

3. **构建Debug APK**
```bash
./gradlew assembleDebug
```

4. **APK位置**
```
app/build/outputs/apk/debug/app-debug.apk
```

### 构建Release版本
```bash
./gradlew assembleRelease
```

## 📖 使用说明

### 添加记账记录
1. 点击底部导航的"记账"或FloatingActionButton
2. 选择收入/支出类型
3. 输入金额
4. 选择分类
5. 可选：添加备注、选择日期
6. 点击"保存"

### 查看账单
- 使用顶部筛选器（全部/收入/支出）
- 使用搜索框搜索备注
- 点击记录可编辑

### 查看统计
- 左右滑动切换月份
- 查看月度收支概览
- 查看支出分类饼图

### 设置
- **主题**：切换浅色/深色/跟随系统
- **货币符号**：自定义显示的货币符号

## 🎨 预置分类

### 支出分类
- 🍜 餐饮
- 🚗 交通
- 🛒 购物
- 🎮 娱乐
- 🏠 居住
- 💊 医疗
- 📚 教育
- 📱 通讯
- 📦 其他

### 收入分类
- 💰 工资
- 🎁 奖金
- 📈 投资
- 💼 兼职
- 🧧 红包
- 💵 其他收入

## 📝 开发指南

### 添加新分类
编辑 `domain/model/Transaction.kt` 中的 `Category` 对象：

```kotlin
val defaultExpenseCategories = listOf(
    // ... 现有分类
    Category("new_category", "新分类", "ic_new", TransactionType.EXPENSE)
)
```

### 添加新屏幕
1. 在 `ui/screens/` 创建新包
2. 创建ViewModel处理业务逻辑
3. 在 `navigation/NavHost.kt` 注册路由
4. 在底部导航添加入口

### 数据库迁移
编辑 `data/local/AppDatabase.kt`：

```kotlin
@Database(
    entities = [TransactionEntity::class],
    version = 2,  // 版本号+1
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    // 添加migration或使用fallbackToDestructiveMigration
}
```

## 🐛 常见问题

**Q: 构建失败提示 "Unsupported class file major version 69"**
A: 需要JDK 17+。当前环境JDK 25与Gradle 8.14不兼容，请安装JDK 17或18。

**Q: Gradle下载超时**
A: 修改 `gradle/wrapper/gradle-wrapper.properties` 使用国内镜像：
```properties
distributionUrl=https\://mirrors.cloud.tencent.com/gradle/gradle-8.14-bin.zip
```

**Q: SDK找不到**
A: 设置ANDROID_HOME环境变量指向SDK目录。

## 📄 许可证

本项目仅供学习交流使用。

## 👤 作者

TinyLedger Team

---

**版本**: 1.0.0  
**更新日期**: 2026-04-02
