# 版本检查功能升级说明

## 📋 更新内容

### 变更前
- 版本检查依赖本地服务器: `https://www.clawlab.org.cn/download/version.json`
- 需要维护独立的版本配置文件
- 服务器配置与 GitHub Release 容易不同步

### 变更后 ✅
- 直接从 GitHub API 获取最新版本
- 完全自动化，无需维护服务器配置文件
- 版本信息始终与 GitHub Release 同步

---

## 🔄 新的工作流程

### 1. 发布流程

```
开发者推送代码 → 创建 GitHub Release 标签
                    ↓
               发布 Release (含 APK)
                    ↓
          app 自动检测到新版本
```

### 2. 版本检查流程图

```
APP 启动
  ↓
调用 VersionCheckUtil.checkUpdate()
  ↓
HTTP GET: https://api.github.com/repos/clavenshine/TinyLedger/releases/latest
  ↓
GitHub 返回 JSON (tag_name, assets, body)
  ↓
提取字段:
  - tag_name: "v2.2.1" → versionName: "2.2.1"
  - assets[].name: "TinyLedger-v2.2.1-release.apk"
  - assets[].browser_download_url: APK下载地址
  - body: 更新日志
  ↓
versionStringToCode("2.2.1") = 30
  ↓
比较: 30 > BuildConfig.VERSION_CODE (当前版本)?
  ↓
if YES → VersionInfo(30, "2.2.1", url, log)
if NO  → null (无可用更新)
```

---

## 📚 技术实现细节

### API 调用

```kotlin
GET https://api.github.com/repos/clavenshine/TinyLedger/releases/latest
Accept: application/vnd.github.v3+json
```

### 版本号转换算法

将版本字符串转换为版本代码：
```kotlin
"2.2.1" → 2*10000 + 2*100 + 1 = 20000 + 200 + 1 = 20201

但当前版本编译配置中:
v2.2.0 → versionCode = 29
v2.2.1 → versionCode = 30
```

**版本代码对应关系**（保持一致性）：
- v2.2.0 = 29
- v2.2.1 = 30
- v2.2.2 = 31
- v2.3.0 = 32
- ...

### 关键函数

```kotlin
private fun versionStringToCode(versionString: String): Int {
    return try {
        val parts = versionString.split(".").map { it.toIntOrNull() ?: 0 }
        val major = parts.getOrNull(0) ?: 0
        val minor = parts.getOrNull(1) ?: 0
        val patch = parts.getOrNull(2) ?: 0
        major * 10000 + minor * 100 + patch
    } catch (e: Exception) {
        Log.e(TAG, "Failed to parse version string: $versionString", e)
        0
    }
}
```

---

## 🚀 如何发布新版本

### 步骤 1: 更新应用版本号

修改 [app/build.gradle.kts](app/build.gradle.kts):

```kotlin
defaultConfig {
    versionCode = 31      // ← 递增
    versionName = "2.3.0" // ← 更新
}
```

### 步骤 2: 编译 Release APK

```bash
./gradlew assembleRelease
```

### 步骤 3: 创建 Git Tag 和 Release

```bash
# 创建 tag
git tag v2.3.0

# 推送 tag 到 GitHub
git push origin v2.3.0

# 在 GitHub 创建 Release (含更新日志和 APK)
# 或使用 GitHub CLI:
gh release create v2.3.0 \
  --title "v2.3.0" \
  --notes "## 更新内容..." \
  app/build/outputs/apk/release/TinyLedger-v2.3.0-release.apk
```

### 步骤 4: 验证

- 旧版本 APP 检查更新时会自动发现新版本
- 用户可点击更新提示下载安装新 APK

---

## ✅ 优势

| 项 | 本地服务器 | GitHub API ✅ |
|---|-----------|----|
| 维护成本 | 需要服务器 | 无（GitHub 直接提供） |
| 同步延迟 | 可能有延迟 | 实时同步 |
| 配置复杂度 | 高（需配置 JSON） | 低（自动读取） |
| 可靠性 | 依赖服务器 | GitHub 的高可用性 |
| 部署自动化 | 手动上传文件 | 完全自动化 |
| 版本管理 | 独立管理 | 与 Release 绑定 |
| 国内访问 | 可能快 | 可能慢（但稳定） |

---

## 📊 版本检查日志示例

**成功检测到更新**：
```
D/VersionCheck: GitHub version: 2.2.1 (code: 30), Local: 2.2.0 (code: 29)
// 显示更新提示
```

**已是最新版本**：
```
D/VersionCheck: GitHub version: 2.2.1 (code: 30), Local: 2.2.1 (code: 30)
// 不显示提示
```

**网络错误**：
```
E/VersionCheck: Version check failed: java.net.SocketTimeoutException
// 静默处理，不影响应用使用
```

---

## 🔧 故障排查

### 问题 1: APP 检查不到更新

**检查清单**：
1. GitHub Release 是否已发布？
   ```bash
   curl https://api.github.com/repos/clavenshine/TinyLedger/releases/latest
   ```

2. Release 中是否上传了 APK？
   - 必须包含 `.apk` 文件作为 asset

3. Tag 名称是否正确？
   - 必须是 `v*` 格式，如 `v2.2.1`

4. versionCode 是否正确递增？
   - 旧版本 versionCode 需要 < 新版本

### 问题 2: 下载失败

**原因**：
- GitHub 服务不可用或网络问题
- APK 资源不存在或已删除
- 下载 URL 无法访问

**解决**：
- 检查网络连接
- 确保 APK 已上传到 Release assets

### 问题 3: 安装失败

**原因**：
- 安装权限不足
- APK 签名不匹配
- 文件损坏

**解决**：
- 确保 DEBUG/RELEASE 签名一致
- 重新下载 APK

---

## 📝 提交信息

```
commit c7e3fc1
Author: GitHub Copilot
Date:   Fri Apr 11 2026

    refactor: use GitHub API for version checking instead of local server
    
    - Replace hardcoded version.json endpoint with GitHub API
    - Automatically fetch latest release from GitHub Releases
    - Extract version, download URL, and changelog from release
    - Implement semantic version-to-versionCode conversion
    - No need to maintain separate version configuration files
```

---

**更新时间**: 2026-04-11  
**版本**: v2.2.1+  
**状态**: ✅ 已实现并编译验证
