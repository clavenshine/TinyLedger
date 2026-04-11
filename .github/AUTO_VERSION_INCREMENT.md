# 🤖 GitHub Actions 自动版本号递增

## 📋 规则说明

### 核心规则
**versionCode = major × 10000 + minor × 100 + patch**

每次发布新版本时：
- versionCode 自动根据 versionName 计算
- 递增规则：每次 +1，满 10 向前进位
- 无需手动计算，GitHub Actions 自动处理

### 版本号示例

| versionName | versionCode | 计算方式 |
|-------------|-------------|----------|
| 2.3.0 | 20300 | 2×10000 + 3×100 + 0 |
| 2.3.1 | 20301 | 2×10000 + 3×100 + 1 |
| 2.3.9 | 20309 | 2×10000 + 3×100 + 9 |
| 2.4.0 | 20400 | 2×10000 + 4×100 + 0 (进位) |
| 3.0.0 | 30000 | 3×10000 + 0×100 + 0 (进位) |

## 🚀 使用方式

### 发布新版本的步骤

1. **创建 Git Tag**
   ```bash
   git tag v2.3.1
   git push origin v2.3.1
   ```

2. **GitHub Actions 自动执行**
   - ✅ 自动提取 versionName (从 tag: v2.3.1 → 2.3.1)
   - ✅ 自动计算 versionCode (2×10000 + 3×100 + 1 = 20301)
   - ✅ 自动更新 `app/build.gradle.kts`
   - ✅ 自动编译 APK
   - ✅ 自动创建 GitHub Release
   - ✅ 自动上传 APK 到 Release

3. **完成！**
   - 不需要手动修改任何版本号
   - 只需要创建正确的 tag 即可

## 📝 工作流程详情

### 自动执行的步骤

```yaml
1. Extract version from tag
   ↓
2. Auto-increment versionCode (自动计算并更新)
   ↓
3. Build Release APK (编译 APK)
   ↓
4. Commit version update (提交版本更新)
   ↓
5. Verify APK exists (验证 APK)
   ↓
6. Create Release (创建 Release)
   ↓
7. Upload APK to Release (上传 APK)
   ↓
8. Mark as latest release (标记为最新)
```

### 关键代码

```bash
# 解析 versionName
VERSION_NAME="2.3.1"
IFS='.' read -r MAJOR MINOR PATCH <<< "$VERSION_NAME"
# MAJOR=2, MINOR=3, PATCH=1

# 计算 versionCode
NEW_VERSION_CODE=$((MAJOR * 10000 + MINOR * 100 + PATCH))
# NEW_VERSION_CODE = 20301

# 更新 build.gradle.kts
sed -i "s/versionCode = .*/versionCode = $NEW_VERSION_CODE/" app/build.gradle.kts
sed -i "s/versionName = .*/versionName = \"$VERSION_NAME\"/" app/build.gradle.kts
```

## ⚠️ 注意事项

### Tag 命名规范

✅ **正确格式**：
```bash
git tag v2.3.0
git tag v2.3.1
git tag v3.0.0
```

❌ **错误格式**：
```bash
git tag 2.3.0      # 缺少 'v' 前缀
git tag v2.3.10    # patch 不应 ≥ 10
git tag v2.10.0    # minor 不应 ≥ 10
```

### 进位规则

- **patch (0-9)**: 每次 bug 修复或小功能 +1
- **minor (0-9)**: patch 达到 10 时，minor +1，patch 归 0
- **major (0-9)**: minor 达到 10 时，major +1，minor 和 patch 归 0

### 实际操作示例

```bash
# 场景1: Bug 修复
当前版本: v2.3.0
git tag v2.3.1
# 自动: versionCode = 20301

# 场景2: 新功能（patch 进位）
当前版本: v2.3.9
git tag v2.4.0  # 注意：不要创建 v2.3.10
# 自动: versionCode = 20400

# 场景3: 大版本更新
当前版本: v2.9.9
git tag v3.0.0  # 注意：不要创建 v2.10.0
# 自动: versionCode = 30000
```

## 🔍 验证

### 查看 Actions 日志

每次发布后，可以在 GitHub Actions 中查看版本计算日志：

```
📊 版本信息:
  versionName: 2.3.1
  versionCode: 20301
  计算公式: 2 × 10000 + 3 × 100 + 1 = 20301

✅ 已更新 build.gradle.kts:
    versionCode = 20301
    versionName = "2.3.1"
```

### 手动验证

```bash
# 查看当前版本
grep -E "versionCode|versionName" app/build.gradle.kts

# 预期输出
versionCode = 20301
versionName = "2.3.1"
```

## 📚 相关文档

- [VERSION_NUMBER_RULES.md](./VERSION_NUMBER_RULES.md) - 详细版本号规则
- [.github/workflows/release.yml](./.github/workflows/release.yml) - CI/CD 工作流配置

---

**最后更新**: 2026-04-11  
**维护者**: GitHub Actions Automation
