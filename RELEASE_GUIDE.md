# 🚀 版本发布快速指南

## 📋 发布流程

### 方式 1: 本地 PowerShell 脚本（Windows）

**最快的方式 - 一条命令完成所有操作！**

```powershell
.\scripts\release.ps1 -version "2.2.2" -notes "# v2.2.2 更新内容

## 新增功能
- 功能 1
- 功能 2

## 修复 Bug
- Bug 修复 1"
```

**脚本会自动执行**：
1. ✅ 更新 `build.gradle.kts` 版本号
2. ✅ 编译 Release APK
3. ✅ 提交代码变更
4. ✅ 创建 Git Tag
5. ✅ 推送到 GitHub
6. ✅ 创建 GitHub Release
7. ✅ 上传 APK 文件
8. ✅ 标记为最新版本

### 方式 2: GitHub Actions（自动化）- 推荐！

**优势**：无需本地编译，全部在云端自动完成

```bash
# 前提：已在 build.gradle.kts 中更新版本号

# 1. 本地提交
git add app/build.gradle.kts
git commit -m "chore: bump version to 2.2.2"

# 2. 创建 Tag（自动触发发布工作流）
git tag v2.2.2
git push origin v2.2.2

# 3. 完成！GitHub Actions 自动处理剩余工作
```

### 方式 3: 手动命令

```bash
# 1. 更新版本号
# 编辑 app/build.gradle.kts
# - versionCode = 31
# - versionName = "2.2.2"

# 2. 编译
./gradlew assembleRelease

# 3. 提交
git add app/build.gradle.kts
git commit -m "chore: bump version to 2.2.2"

# 4. Tag
git tag v2.2.2

# 5. 推送
git push origin main v2.2.2

# 6. 在 GitHub 手动创建 Release 并上传 APK
```

---

## 📊 版本号递增规则

**版本格式**：`major.minor.patch` 例如：`2.2.1`

| 情景 | 版本变化 | versionCode |
|------|--------|------------|
| 重大功能更新 | 2.2.x → 3.0.0 | +100 |
| 小功能更新 | 2.2.x → 2.3.0 | +1 |
| Bug 修复 | 2.2.1 → 2.2.2 | +1 |

**当前版本**：v2.2.1 (versionCode = 30)

**下个版本示例**：
- 修复 Bug → v2.2.2 (versionCode = 31)
- 新功能 → v2.3.0 (versionCode = 32)
- 大版本 → v3.0.0 (versionCode = 100)

---

## 🔧 初始化说明

### 第一次使用（已完成 ✅）

```bash
# 工作流文件已创建
.github/workflows/release.yml

# 本地脚本已创建
scripts/release.ps1

# 确保 GitHub Token 可用（可选，自动化使用）
# Windows PowerShell:
$env:GITHUB_TOKEN = "your_github_token"

# Linux/Mac:
export GITHUB_TOKEN="your_github_token"
```

### 获取 GitHub Token

1. 访问 https://github.com/settings/tokens
2. 点击 "Generate new token"
3. 选择权限：`repo` (完整访问)
4. 复制 Token
5. 设置环境变量：`GITHUB_TOKEN=...`

---

## 📝 发布清单

### 发布前检查 ✓

- [ ] 所有功能已完成并测试
- [ ] 代码已 review 和 merge 到 main
- [ ] 决定版本号（major.minor.patch）
- [ ] 准备好更新日志

### 选择发布方式

**推荐**：使用 GitHub Actions（最自动化）

```bash
# 1. 本地更新版本
code app/build.gradle.kts
# 修改:
# versionCode = 31
# versionName = "2.2.2"

# 2. 提交
git add app/build.gradle.kts
git commit -m "chore: bump version to 2.2.2"
git push origin main

# 3. 创建 Tag（自动触发工作流）
git tag v2.2.2 -m "Release v2.2.2: Bug fixes and improvements"
git push origin v2.2.2

# 4. 检查进度
# → https://github.com/clavenshine/TinyLedger/actions
```

### 或使用本地脚本

```powershell
.\scripts\release.ps1 -version "2.2.2" `
  -notes @"
# v2.2.2 更新内容

## Bug 修复
- 修复日历显示问题
- 修复金额计算错误

## 性能优化
- 提升应用启动速度
"@
```

---

## ✅ 发布后验证

1. **检查 Release 页面**
   - 访问：https://github.com/clavenshine/TinyLedger/releases
   - 确认 v2.2.2 为 "Latest"

2. **验证 APK 下载**
   - 点击 APK 文件进行下载测试
   - 确认文件大小约 46 MB

3. **测试自动更新**
   - 安装旧版本 APP (v2.2.1)
   - 打开"我的"页面
   - 验证显示"有更新 v2.2.2"提示

---

## 🔍 常见问题

### Q1: 如何跳过某个版本？

**不推荐**，但可以这样做：

```bash
# 编辑 build.gradle.kts
versionCode = 35  # 跳过 32-34
versionName = "2.3.0"

# 发布
git tag v2.3.0
git push origin v2.3.0
```

### Q2: 如何回滚发布？

```bash
# 删除本地 tag
git tag -d v2.2.2

# 删除远程 tag
git push origin --delete v2.2.2

# 在 GitHub 手动删除 Release
# https://github.com/clavenshine/TinyLedger/releases
```

### Q3: 编译失败怎么办？

```bash
# 检查 Gradle 错误
./gradlew assembleRelease --stacktrace

# 清理重试
./gradlew clean
./gradlew assembleRelease
```

### Q4: APK 上传失败？

- 检查网络连接
- 确认 GitHub Token 有效
- 或手动在 GitHub 上传 APK

### Q5: 版本号如何保持一致？

- ✅ versionCode 必须递增
- ✅ versionName 必须为 X.Y.Z 格式
- ✅ Git Tag 必须为 vX.Y.Z 格式
- ✅ GitHub Release tag 自动与 Git 同步

---

## 📚 文件位置

| 文件 | 位置 | 说明 |
|-----|------|------|
| 本地脚本 | `scripts/release.ps1` | Windows PowerShell 发布脚本 |
| GitHub Actions | `.github/workflows/release.yml` | 自动化编译和发布 |
| 版本配置 | `app/build.gradle.kts` | 版本号定义 |
| 版本检查 | `app/src/main/java/com/tinyledger/app/util/VersionCheckUtil.kt` | GitHub API 集成 |

---

## 🎯 完整发布示例

### 场景：发布 v2.3.0（新功能版本）

**步骤 1：准备**
```bash
cd TinyLedger
git checkout main
git pull origin main
```

**步骤 2：更新版本**
```
编辑 app/build.gradle.kts:
  versionCode = 32  (递增)
  versionName = "2.3.0"
```

**步骤 3：提交**
```bash
git add app/build.gradle.kts
git commit -m "chore: bump version to 2.3.0"
git push origin main
```

**步骤 4：发布（选一种方式）**

*方式 A - GitHub Actions（推荐）*：
```bash
git tag v2.3.0 -m "Release v2.3.0: New features and improvements"
git push origin v2.3.0
# 自动触发 GitHub Actions 工作流
```

*方式 B - 本地脚本*：
```powershell
.\scripts\release.ps1 -version "2.3.0" -notes "# v2.3.0

## 新增功能
- 新的数据导入方式
- 增强的统计报表"
```

**步骤 5：验证**
- 访问 Release 页面确认 v2.3.0 发布成功
- 检查 APK 已上传
- 测试自动更新功能

---

**最后更新**：2026-04-11  
**维护者**：GitHub Copilot
