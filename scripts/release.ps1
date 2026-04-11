# TinyLedger 版本发布脚本
# 用法: .\scripts\release.ps1 -version "2.2.2" -notes "更新日志内容"

param(
    [Parameter(Mandatory=$true)]
    [string]$version,
    
    [Parameter(Mandatory=$true)]
    [string]$notes,
    
    [string]$gitHubToken = $env:GITHUB_TOKEN,
    [string]$owner = "clavenshine",
    [string]$repo = "TinyLedger"
)

# 颜色输出
function Write-Success { Write-Host "✅ $args" -ForegroundColor Green }
function Write-Info { Write-Host "ℹ️  $args" -ForegroundColor Cyan }
function Write-Error { Write-Host "❌ $args" -ForegroundColor Red }
function Write-Warning { Write-Host "⚠️  $args" -ForegroundColor Yellow }

# 验证版本格式
if ($version -notmatch '^\d+\.\d+\.\d+$') {
    Write-Error "版本格式无效: $version (应为: major.minor.patch 如 2.2.2)"
    exit 1
}

$projectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
Set-Location $projectRoot

Write-Info "=========================================="
Write-Info "TinyLedger 版本发布工具"
Write-Info "=========================================="
Write-Info "版本: v$version"
Write-Info "项目路径: $projectRoot"

# 步骤1: 更新版本号
Write-Info "步骤 1/5: 更新版本号"
$buildGradleFile = "app/build.gradle.kts"
$gradleContent = Get-Content $buildGradleFile -Raw

# 计算版本代码：versionCode = major × 10000 + minor × 100 + patch
function Get-VersionCode {
    param([string]$versionName)
    if ($versionName -match '^(\d+)\.(\d+)\.(\d+)$') {
        $major = [int]$matches[1]
        $minor = [int]$matches[2]
        $patch = [int]$matches[3]
        return $major * 10000 + $minor * 100 + $patch
    } else {
        Write-Error "版本格式无效: $versionName (应为: major.minor.patch)"
        exit 1
    }
}

# 计算新的版本代码
$newCode = Get-VersionCode -versionName $version
Write-Info "版本号 $version 对应的代码: $newCode"

# 提取当前版本
if ($gradleContent -match 'versionCode = (\d+)') {
    $currentCode = [int]$matches[1]
    Write-Info "当前版本代码: $currentCode → 新版本代码: $newCode"
    if ($newCode -le $currentCode) {
        Write-Error "新版本代码 ($newCode) 必须大于当前版本代码 ($currentCode)"
        exit 1
    }
} else {
    Write-Error "无法解析 versionCode"
    exit 1
}

if ($gradleContent -match 'versionName = "([^"]+)"') {
    $currentVersion = $matches[1]
    Write-Info "当前版本: $currentVersion → 新版本: $version"
} else {
    Write-Error "无法解析 versionName"
    exit 1
}

# 更新 build.gradle.kts
$newGradleContent = $gradleContent -replace 'versionCode = \d+', "versionCode = $newCode" `
                                  -replace 'versionName = "[^"]+"', "versionName = `"$version`""

Set-Content $buildGradleFile -Value $newGradleContent -Encoding UTF8
Write-Success "版本号已更新"

# 步骤2: 编译 Release APK
Write-Info "步骤 2/5: 编译 Release APK"
Write-Warning "正在编译，请等待..."

$buildOutput = & .\gradlew.bat assembleRelease 2>&1
$buildSuccess = $buildOutput | Select-String "BUILD SUCCESSFUL"

if ($buildSuccess) {
    Write-Success "APK 编译成功"
    $apkFile = "app/build/outputs/apk/release/TinyLedger-v$version-release.apk"
    if (Test-Path $apkFile) {
        $apkSize = (Get-Item $apkFile).Length / 1MB
        Write-Info "APK 文件: $apkFile ($([math]::Round($apkSize, 2)) MB)"
    }
} else {
    Write-Error "APK 编译失败"
    exit 1
}

# 步骤3: Git 提交和标签
Write-Info "步骤 3/5: Git 提交和标签"

# 检查 git 状态
$gitStatus = git status --porcelain
if ($gitStatus) {
    Write-Info "提交文件变更..."
    git add app/build.gradle.kts
    git commit -m "chore: bump version to $version"
    Write-Success "已提交"
} else {
    Write-Info "无文件变更"
}

# 创建 tag
$tagName = "v$version"
Write-Info "创建标签: $tagName"
git tag $tagName
Write-Success "标签已创建"

# 步骤4: 推送到 GitHub
Write-Info "步骤 4/5: 推送到 GitHub"
git push origin main
Write-Success "main 分支已推送"
git push origin $tagName
Write-Success "标签已推送"

# 步骤5: 创建 GitHub Release
Write-Info "步骤 5/5: 创建 GitHub Release"

if (-not $gitHubToken) {
    Write-Error "未设置 GITHUB_TOKEN 环境变量"
    Write-Warning "使用本地 git credential 尝试上传..."
    Write-Info "请手动在 GitHub 创建 Release: https://github.com/$owner/$repo/releases/new"
    exit 0
}

$headers = @{
    "Authorization" = "token $gitHubToken"
    "Accept" = "application/vnd.github.v3+json"
}

# 创建 release
$releaseData = @{
    tag_name = $tagName
    name = $tagName
    body = $notes
    draft = $false
    prerelease = $false
} | ConvertTo-Json

Write-Info "创建 Release..."
$releaseResponse = Invoke-RestMethod `
    -Uri "https://api.github.com/repos/$owner/$repo/releases" `
    -Method Post `
    -Headers $headers `
    -Body $releaseData

$releaseId = $releaseResponse.id
Write-Success "Release 已创建 (ID: $releaseId)"

# 上传 APK
Write-Info "上传 APK 文件..."
$apkPath = "app/build/outputs/apk/release/TinyLedger-v$version-release.apk"
if (Test-Path $apkPath) {
    $apkBytes = [System.IO.File]::ReadAllBytes($apkPath)
    $uploadHeaders = @{
        "Authorization" = "token $gitHubToken"
        "Content-Type" = "application/octet-stream"
    }
    
    $assetResponse = Invoke-RestMethod `
        -Uri "https://uploads.github.com/repos/$owner/$repo/releases/$releaseId/assets?name=TinyLedger-v$version-release.apk" `
        -Method Post `
        -Headers $uploadHeaders `
        -Body $apkBytes
    
    Write-Success "APK 已上传"
    Write-Info "下载地址: $($assetResponse.browser_download_url)"
} else {
    Write-Error "APK 文件不存在: $apkPath"
    exit 1
}

# 标记为最新版本
Write-Info "标记为最新版本..."
$updateData = @{
    make_latest = "true"
} | ConvertTo-Json

$patchResponse = Invoke-RestMethod `
    -Uri "https://api.github.com/repos/$owner/$repo/releases/$releaseId" `
    -Method Patch `
    -Headers $headers `
    -Body $updateData

Write-Success "已标记为最新版本"

# 完成
Write-Info "=========================================="
Write-Success "版本发布完成！"
Write-Info "版本: v$version"
Write-Info "Release 页面: https://github.com/$owner/$repo/releases/tag/$tagName"
Write-Info "=========================================="
