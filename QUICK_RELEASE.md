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

**规则：versionCode = major × 10000 + minor × 100 + patch**

| 版本 | versionCode | 说明 |
|------|-----------|------|
| 2.2.1 | 20201 | 2×10000 + 2×100 + 1 |
| 2.2.2 | 20202 | 递增 +1 |
| 2.2.9 | 20209 | patch 满 10 需进位 |
| 2.3.0 | 20300 | **进位后的新版本** |
| 3.0.0 | 30000 | 大版本发布 |

**当前**：v2.2.1 (versionCode = 20201)

**下一版**可选：
- Bug 修复 → v2.2.2 (versionCode = 20202)
- 新功能 → v2.3.0 (versionCode = 20300)
- 大版本 → v3.0.0 (versionCode = 30000)

✅ 脚本会自动计算 versionCode！

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
