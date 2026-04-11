# TinyLedger v2.2.0 更新日志

## 🎉 新功能

### 自动检查更新
- ✨ **APP启动时自动检查GitHub latest版本**
- 📲 本地版本低于GitHub版本时，版本号处显示红点提示
- 💬 点击版本号查看更新内容详情
- 🔄 支持一键下载并自动安装最新版本
- ✅ 版本一致时无提示，点击版本号无反应

## 🏗️ 技术改进

### 新增模块
- `UpdateCheckRepository` - GitHub版本检查接口
- `UpdateCheckRepositoryImpl` - 调用GitHub API获取latest release信息
- `UpdateCheckViewModel` - 管理版本检查和下载流程
- `UpdateCheckDialog` - 版本更新对话框UI

### 功能特性
- 从GitHub API实时获取latest release版本信息
- 智能版本号比较（支持多位版本号）
- APK自动下载和安装（使用系统下载管理器）
- 下载过程中显示进度反馈
- 网络超时和错误处理

### 权限和配置
- 已配置`REQUEST_INSTALL_PACKAGES`权限进行应用内安装
- FileProvider正确配置用于APK安装
- 网络访问权限（已有）

## 📦 版本信息
- 版本名称：2.2.0
- 版本代码：29
- GitHub Release: https://github.com/clavenshine/TinyLedger/releases/tag/v2.2.0

## 🔗 以前版本
- v2.1.8：账号管理优化
- v2.1.0：账单分类完善
- v2.0.0：Jetpack Compose重构

---

**发布日期**: 2026年4月11日  
**开发者**: shineclaven
