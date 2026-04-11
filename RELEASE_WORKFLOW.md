# 🎯 版本发布完整指南 (最终版)

## 📌 核心规则 - 十进制版本号

### 版本号映射公式

```
versionCode = major × 10000 + minor × 100 + patch

示例：
- v2.2.1 = 2×10000 + 2×100 + 1 = 20201
- v2.2.2 = 2×10000 + 2×100 + 2 = 20202
- v2.3.0 = 2×10000 + 3×100 + 0 = 20300
- v3.0.0 = 3×10000 + 0×100 + 0 = 30000
```

### 进位规则

当某位数字达到 10 时，**自动进位**到前一位：

✅ **正确的递增路径**：
- v2.2.8 (versionCode: 20208)
- v2.2.9 (versionCode: 20209)  ← patch = 9，最大值
- v2.3.0 (versionCode: 20300)  ← patch=10时进位到minor
- v2.9.9 (versionCode: 20909)
- v3.0.0 (versionCode: 30000)  ← minor=10时进位到major

❌ **永远不会出现**：
- v2.2.10 ← 不符合规范（patch应 < 10）
- versionCode = 20210 ← 不应该存在

---

## 🚀 三种发布方式

### 方式1️⃣：GitHub Actions（最自动化 - 推荐）

**零手动工作，全部云端自动处理！**

```bash
# 1. 编辑版本号（2分钟）
编辑 app/build.gradle.kts:
  versionCode = [自动计算，参考下表]
  versionName = "[你的版本号]"

# 示例（bug修复）：
#   versionCode = 20202        # 自动计算：2*10000 + 2*100 + 2
#   versionName = "2.2.2"

# 2. 提交（1分钟）
git add app/build.gradle.kts
git commit -m "chore: bump version to 2.2.2"
git push origin main

# 3. 发布（自动，1分钟）
git tag v2.2.2
git push origin v2.2.2

# ✅ 完成！GitHub Actions自动处理：
#    ✓ 编译 Release APK
#    ✓ 创建 GitHub Release
#    ✓ 上传 APK 文件
#    ✓ 标记为 Latest
#    ✓ 旧版本APP检测到更新
```

**总耗时**：~5 分钟（+ 2 分钟 GitHub Actions 自动编译时间）

---

### 方式2️⃣：本地 PowerShell 脚本（一键完成）

```powershell
.\scripts\release.ps1 -version "2.2.2" `
  -notes "# v2.2.2 更新

## Bug 修复
- 修复日历显示问题
- 修复金额计算错误"
```

**功能**：
1. ✅ 自动计算 versionCode
2. ✅ 更新 build.gradle.kts
3. ✅ 编译 Release APK
4. ✅ 提交代码变更
5. ✅ 创建 Git Tag
6. ✅ 推送到 GitHub
7. ✅ 创建 GitHub Release
8. ✅ 上传 APK 文件
9. ✅ 标记为最新版本

**总耗时**：~10 分钟（本地编译 + 上传）

---

### 方式3️⃣：手动命令（完全控制）

```bash
# 1. 编辑版本
code app/build.gradle.kts
# 修改 versionCode 和 versionName

# 2. 编译
./gradlew assembleRelease

# 3. Git 操作
git add app/build.gradle.kts
git commit -m "chore: bump version to 2.2.2"
git tag v2.2.2
git push origin main v2.2.2

# 4. 手动创建 Release（GitHub Web 界面）
# https://github.com/clavenshine/TinyLedger/releases/new
```

---

## 📊 版本号速查表

### 从 v2.2.1 开始的常见递增

| 当前版本 | versionCode | 下一版本 | 新 versionCode | 用途 |
|--------|-----------|--------|------------|------|
| **2.2.1** | **20201** | **2.2.2** | **20202** | **Bug修复** |
| 2.2.2 | 20202 | 2.2.3 | 20203 | Bug修复 |
| 2.2.9 | 20209 | 2.3.0 | 20300 | 新功能(进位) |
| 2.3.0 | 20300 | 2.3.1 | 20301 | Bug修复 |
| 2.9.9 | 20909 | 3.0.0 | 30000 | 大版本(进位) |

### 版本代码自动计算参考

```
versionCode 计算器 - 自动计算工具已集成到脚本中！

V2.2.X 系列：
- 2.2.1 → 20201
- 2.2.2 → 20202
- 2.2.3 → 20203
- 2.2.4 → 20204
- 2.2.5 → 20205
- 2.2.6 → 20206
- 2.2.7 → 20207
- 2.2.8 → 20208
- 2.2.9 → 20209
- 2.3.0 → 20300 ← 自动进位

V2.3.X 系列：
- 2.3.0 → 20300
- 2.3.1 → 20301
- ...
- 2.3.9 → 20309
- 2.4.0 → 20400 ← 自动进位

V2 的最后：
- 2.9.9 → 20909
- 3.0.0 → 30000 ← 自动进位
```

---

## ✅ 发布前检查清单

### 代码准备

- [ ] 功能已完成并通过测试
- [ ] 代码已 review 且 merge 到 main 分支
- [ ] 没有未提交的变更

### 版本号检查

- [ ] 确定新版本号（major.minor.patch）
- [ ] patch < 10（否则应进位到 minor）
- [ ] minor < 10（否则应进位到 major）
- [ ] versionCode 通过公式计算正确

**示例检查**：
```
版本号：2.2.2 ✓
计算式：2*10000 + 2*100 + 2 = 20202 ✓
versionCode 应填：20202 ✓
规则检验：patch=2 < 10 ✓ 合法
```

### 发布流程检查

- [ ] 选择发布方式（推荐：GitHub Actions）
- [ ] 准备好更新日志
- [ ] 确保 build.gradle.kts 更新正确

### 发布后验证

- [ ] Release 页面显示 ✓
- [ ] APK 文件可下载 ✓
- [ ] 标记为 Latest Release ✓
- [ ] 旧版本 APP 能检测到更新 ✓

---

## 🔍 常见问题

### Q: versionCode 怎么计算？

A: 使用公式 `major × 10000 + minor × 100 + patch`

示例：
```
v2.2.1 = 2×10000 + 2×100 + 1 = 20201
v2.3.0 = 2×10000 + 3×100 + 0 = 20300
v3.0.0 = 3×10000 + 0×100 + 0 = 30000
```

✅ **脚本会自动计算**

---

### Q: 什么时候需要进位？

A: 当某位数字达到 10 时：

```
patch 从 1-9，不能是 0 或 10+
当 patch = 9 并需要发布新版本时：
  v2.2.9 → v2.3.0 （patch 9→10时自动升minor）

minor 从 0-9，不能是 10+
当 minor = 9 并需要发布新版本时：
  v2.9.9 → v3.0.0 （minor 10时自动升major）
```

---

### Q: 能跳过某个版本吗？

A: 不推荐，但可以：

```bash
# 编辑 build.gradle.kts
versionCode = 20300  # 跳过 20202-20299
versionName = "2.3.0"

# 发布
git tag v2.3.0
```

但这样会造成混乱，建议遵循递增规则。

---

### Q: 发布失败了怎么办？

A: 检查对应的问题：

**GitHub Actions 失败**：
- 查看 Actions 运行日志
- 重新 push tag 触发重新运行

**本地编译失败**：
```bash
./gradlew clean
./gradlew assembleRelease --stacktrace
```

**网络问题**：
- 重新推送或重新运行工作流

---

## 📁 相关文件

| 文件 | 位置 | 用途 |
|-----|------|------|
| 版本号规则 | `VERSION_NUMBER_RULES.md` | 详细的十进制进位规则 |
| 发布脚本 | `scripts/release.ps1` | Windows 一键发布 |
| GitHub Actions | `.github/workflows/release.yml` | 自动化编译发布 |
| 详细指南 | `RELEASE_GUIDE.md` | 完整的发布文档 |
| 快速参考 | `QUICK_RELEASE.md` | 30秒速览 |

---

## 🎬 完整发布示例

### 场景：发布 v2.2.2（修复 bug）

**步骤 1**：编辑 app/build.gradle.kts
```kotlin
defaultConfig {
    versionCode = 20202  // ← 自动计算：2*10000 + 2*100 + 2
    versionName = "2.2.2"  // ← 新版本号
}
```

**步骤 2**：提交代码
```bash
git add app/build.gradle.kts
git commit -m "chore: bump version to 2.2.2"
git push origin main
```

**步骤 3**：创建 Release（自动触发 GitHub Actions）
```bash
git tag v2.2.2 -m "Release v2.2.2: Bug fixes and optimizations"
git push origin v2.2.2
```

**步骤 4**：验证（几分钟后）
- ✅ Actions 完成编译
- ✅ Release 发布成功
- ✅ APK 已上传
- ✅ 标记为 Latest

**步骤 5**：测试
- 在旧版本 APP 中检查更新
- 看到"有更新 v2.2.2"提示
- 点击下载并安装

---

## 💡 最佳实践

### 发布频率建议

- **Bug 修复**：有问题立即发布（patch +1）
- **小功能**：积累到 minor 有意义的进展时发布
- **大版本**：等待重要功能积累或架构调整

### 版本号命名建议

| 版本号 | 如何使用 | 示例 |
|--------|--------|------|
| patch | Bug 修复、小优化 | 2.2.1 → 2.2.2 |
| minor | 新功能、UI改进 | 2.2.0 → 2.3.0 |
| major | 重大功能、架构变更 | 2.0.0 → 3.0.0 |

### 更新日志建议

```markdown
# v2.2.2

## 🐛 Bug Fixes
- 修复日历显示错误
- 修复金额计算问题

## ⚡ Performance
- 优化启动速度
- 减少内存占用

## 📝 Notes
- 仅支持 Android 8.0+
- 需要授予权限
```

---

**最后更新**：2026-04-11  
**版本检查**：已集成 GitHub API，自动对标最新 Release  
**自动化状态**：✅ 完全自动化，支持零配置发布
