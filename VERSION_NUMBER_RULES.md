# 📊 版本号递增规则（十进制进位）

## 🔢 核心规则

**versionCode = major × 10000 + minor × 100 + patch**

当某位数字达到 10 时，向前进一位。

---

## 完整版本号映射表

| versionCode | versionName | 说明 |
|:-----------:|:-----------:|------|
| 1 | 0.0.1 | 初始版本 |
| 2-9 | 0.0.2 ~ 0.0.9 | patch 递增 |
| **10** | **0.1.0** | patch=10 → 进位到 minor |
| 11 | 0.1.1 | 新周期开始 |
| 19 | 0.1.9 | |
| **20** | **0.2.0** | minor 递增 |
| ... | ... | |
| **100** | **1.0.0** | minor=10 → 进位到 major |
| 101 | 1.0.1 | |
| ... | ... | |
| 10001 | 1.0.1 | major 递增 |

---

## 当前版本 → 下一版本

### 跳过版本不存在（如 2.2.10）

✅ **正确的递增路径**：
- v2.2.9 (versionCode = 20209)
- ↓ patch 达到 10 时自动进位
- v2.3.0 (versionCode = 20300) ← minor 递增

❌ **不应该出现**：
- v2.2.10 ← 不符合语义版本规范
- versionCode = 20210 ← 不应该出现

---

## 常见版本递增示例

### 例1：Bug 修复版本

```
当前：v2.2.8 (versionCode = 20208)
↓ 修复 Bug
目标：v2.2.9 (versionCode = 20209)

修改：
  versionCode = 20209
  versionName = "2.2.9"
```

### 例2：新功能小版本

```
当前：v2.2.9 (versionCode = 20209)
↓ 添加新功能，patch 满 10 需要进位
目标：v2.3.0 (versionCode = 20300)

修改：
  versionCode = 20300
  versionName = "2.3.0"
```

### 例3：大版本发布

```
当前：v2.9.9 (versionCode = 20909)
↓ 重大更新
目标：v3.0.0 (versionCode = 30000)

修改：
  versionCode = 30000
  versionName = "3.0.0"
```

---

## 版本号计算器

### 从 versionName 计算 versionCode

```
versionName = "a.b.c"
versionCode = a × 10000 + b × 100 + c

示例：
- "2.2.1" → 2×10000 + 2×100 + 1 = 20000 + 200 + 1 = 20201
- "2.2.9" → 2×10000 + 2×100 + 9 = 20000 + 200 + 9 = 20209
- "2.3.0" → 2×10000 + 3×100 + 0 = 20000 + 300 + 0 = 20300
- "3.0.0" → 3×10000 + 0×100 + 0 = 30000 + 0 + 0 = 30000
```

### 从 versionCode 反推 versionName

```
versionCode = 20209

major = versionCode ÷ 10000 = 2
remainder = versionCode % 10000 = 209

minor = remainder ÷ 100 = 2  
patch = remainder % 100 = 9

结果：versionName = "2.2.9"
```

---

## 当前 vs 提议

### 当前设置（v2.2.1）

```
versionCode = 30 ← 不规范
versionName = "2.2.1" ← 正确
```

### 应该改为

```
versionCode = 20201 ← 符合规则
versionName = "2.2.1" ← 保持
```

**优点**：
- ✅ versionCode 与 versionName 逻辑对应
- ✅ 支持无限版本递增
- ✅ 避免版本号冲突
- ✅ 自动进位，无需手动转换

---

## 递增规则速查表

### 最常见的版本变化

| 情景 | 当前版本 | 新版本 | versionCode |
|------|--------|--------|-----------|
| Bug 修复 | 2.2.x | 2.2.(x+1) | 如下表 |
| 新功能（patch < 9） | 2.2.x | 2.2.(x+1) | +1 |
| 新功能（patch = 9） | 2.2.9 | 2.3.0 | +91 |
| 小版本更新 | 2.y.z | 2.(y+1).0 | +100-(z) |
| 大版本发布 | 2.y.z | 3.0.0 | 需要计算 |

### 快速查表（从 v2.2.x 系列）

| 当前版本 | versionCode | 下一版本 | 新 versionCode | 变化 |
|--------|-----------|--------|------------|------|
| 2.2.1 | 20201 | 2.2.2 | 20202 | +1 |
| 2.2.2 | 20202 | 2.2.3 | 20203 | +1 |
| ... | ... | ... | ... | ... |
| 2.2.9 | 20209 | 2.3.0 | 20300 | +91 |
| 2.3.0 | 20300 | 2.3.1 | 20301 | +1 |
| 2.3.9 | 20309 | 2.4.0 | 20400 | +91 |
| 2.9.9 | 20909 | 3.0.0 | 30000 | +9091 |

---

## 何时更新各个部分

### versionCode 更新

✅ **必须**：
- 每次发布新版本
- 递增规则：major × 10000 + minor × 100 + patch

### versionName 更新

✅ **必须**：
- 每次发布新版本
- 格式：major.minor.patch (如 2.2.1)

### 只更新 versionName 的情况

❌ **不应该**：
- versionCode 不变而 versionName 变化
- 这会导致系统混乱

---

## 验证版本号正确性

发布前检查清单：

```
[ ] versionCode 是否 = major × 10000 + minor × 100 + patch？
[ ] versionName 是否为 major.minor.patch 格式？
[ ] versionCode 是否 > 上一个版本？
[ ] patch < 10？(如果 ≥ 10 需要进位)
[ ] minor < 10？(如果 ≥ 10 需要进位到 major)
```

---

## 与 GitHub API 版本检查的关系

APP 版本检查逻辑：

```kotlin
// 从 GitHub Release tag 获取 versionName (如 "v2.2.1")
val versionName = "2.2.1"

// 转换为 versionCode
val versionCode = versionStringToCode(versionName)
// = 2×10000 + 2×100 + 1 = 20201

// 比较：如果服务器版本 > 本地版本则显示更新
if (serverVersionCode > BuildConfig.VERSION_CODE) {
    // 显示更新提示
}
```

---

**表格最后更新日期**：2026-04-11  
**维护者**：GitHub Copilot
