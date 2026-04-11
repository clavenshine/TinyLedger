# 🚀 快速发布 - 一句话总结

## 最简单的发布流程 (5 分钟完成)

### 推荐方式：GitHub Actions 自动化

```bash
# 1️⃣ 更新版本号（编辑一个文件）
编辑 app/build.gradle.kts:
  versionCode = [递增数字]     # 从 30 改成 31
  versionName = "[版本号]"      # 从 "2.2.1" 改成 "2.2.2"

# 2️⃣ 提交变更
git add app/build.gradle.kts
git commit -m "chore: bump version to 2.2.2"
git push origin main

# 3️⃣ 创建标签触发自动发布
git tag v2.2.2
git push origin v2.2.2

# ✅ 完成！自动处理剩余工作：
#    - 编译 APK
#    - 创建 Release
#    - 上传 APK
#    - 标记为 Latest
```

**只需 3 分钟！**

---

## 版本号怎么填？

| 类型 | 版本变化 | versionCode |
|------|--------|-----------|
| Bug 修复 | 2.2.1 → 2.2.2 | 30 → 31 |
| 小功能 | 2.2.0 → 2.3.0 | 29 → 32 |
| 大版本 | 2.0.0 → 3.0.0 | 20 → 100 |

**规则**：versionCode 只要 > 当前版本就行

**当前状态**：
- 版本：v2.2.1
- versionCode：30

**下一步**：
- v2.2.2：versionCode = 31 ✅

---

## 本地一键发布（Windows）

```powershell
.\scripts\release.ps1 -version "2.2.2" -notes "Bug 修复和性能优化"
```

自动完成所有 8 个步骤！

---

## 验证发布成功

```
✅ 代码已提交
✅ Tag 已创建  
✅ GitHub Actions 运行中
✅ Release 已发布
✅ APK 已上传
✅ 标记为 Latest
```

查看状态：https://github.com/clavenshine/TinyLedger/actions

---

**就这么简单！** 🎉
