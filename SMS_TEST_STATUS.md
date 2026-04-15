# 银行短信解析测试状态报告

**测试时间：** 2026-04-15 20:35  
**应用版本：** v2.5.3  
**设备：** 小米设备 (5bfe43c0)

---

## 📊 当前状态

### ✅ 已验证功能

1. **通知监听服务运行正常**
   ```
   TxNotifService: [通知捕获] pkg=com.android.mms
   TxNotifService: [通知捕获] 检测到短信应用: com.android.mms
   ```

2. **短信应用包名识别正确**
   - 小米短信应用：`com.android.mms` ✅

3. **短信接收事件捕获**
   - 20:32:31 - 收到短信
   - 20:34:40 - 收到短信
   - 20:35:30 - 收到短信

---

## ⚠️ 发现的问题

### 问题1：通知内容被系统隐藏

**现象：**
```
TxNotifService: [通知捕获] pkg=com.android.mms 
  title='短信' 
  text='已隐藏敏感通知内容' 
  bigText='已隐藏敏感通知内容'
```

**原因：**
小米 HyperOS/MIUI 系统默认隐藏短信通知的详细内容，这是系统级安全特性。

**影响：**
- 通知监听服务无法获取短信完整内容
- 无法通过通知栏捕获银行短信详情
- 只能捕获到"有短信"的通知，但看不到内容

---

### 问题2：SmsReceiver 权限被拒绝

**现象：**
```
BroadcastQueue: Delivery state of BroadcastRecord{...SMS_RECEIVED/u0} 
to ResolveInfo{...com.tinyledger.app/.data.sms.SmsReceiver ...} 
changed from PENDING to SKIPPED because skipped by policy at enqueue: 
Permission Denial: receiving Intent { act=android.provider.Telephony.SMS_RECEIVED ... } 
to com.tinyledger.app/.data.sms.SmsReceiver 
requires android.permission.RECEIVE_SMS due to sender com.android.phone (uid 1001)
```

**原因：**
- Android 13+ (API 33+) 对 `RECEIVE_SMS` 权限有更严格的限制
- 小米 MIUI 可能额外限制了第三方应用接收短信广播

**影响：**
- 短信广播接收器无法接收到短信
- 只能通过其他方式获取短信内容

---

## 🔍 解决方案

### 方案1：使用短信内容提供器（推荐）

TinyLedger 已经实现了这个功能，通过查询 `content://sms` 直接读取短信数据库。

**优势：**
- 不依赖通知内容
- 不依赖短信广播
- 可以读取所有短信（包括历史短信）

**需要的权限：**
- `READ_SMS` 权限（需要在设置中手动授予）

**测试步骤：**
1. 在手机上授予 TinyLedger 短信权限
   ```
   设置 → 应用管理 → TinyLedger → 权限管理 → 短信 → 允许
   ```

2. 打开 TinyLedger
3. 进入"短信导入"功能
4. 点击"扫描短信"
5. 应用会直接查询短信数据库

---

### 方案2：解决通知内容隐藏问题

**尝试以下设置：**

1. **关闭短信通知隐藏**
   ```
   设置 → 通知与控制中心 → 锁屏通知 → 显示通知内容
   ```

2. **允许 TinyLedger 读取通知详情**
   ```
   设置 → 通知与控制中心 → 通知使用权 → 小小记账本通知监听
   → 确保已开启所有权限
   ```

3. **关闭小米安全中心的短信保护**
   ```
   手机管家 → 骚扰拦截 → 设置 → 通知设置
   → 关闭"隐藏通知内容"
   ```

---

### 方案3：结合使用两种方式

**最佳实践：**
1. 使用**通知监听**实时捕获（如果内容可见）
2. 使用**短信数据库查询**作为补充和备份
3. 使用**去重机制**避免重复记账

---

## 📝 立即测试步骤

### 步骤1：授予短信权限

```
设置 → 应用管理 → TinyLedger → 权限管理 → 短信 → 允许
```

### 步骤2：使用短信导入功能测试

1. 确保手机上有银行短信（可以请朋友转账或自己进行一笔交易）
2. 打开 TinyLedger
3. 进入"设置" → "短信导入" 或 "自动记账"
4. 点击"扫描短信" 或 "导入短信"
5. 观察是否能读取并解析银行短信

### 步骤3：查看日志确认

```bash
# 实时查看日志
adb logcat | Select-String -Pattern "SmsTransactionParser|SmsReader|短信"

# 或查看最近日志
adb logcat -d | Select-String -Pattern "SmsTransactionParser" | Select-Object -Last 30
```

**预期日志：**
```
SmsTransactionParser: 开始扫描短信...
SmsTransactionParser: 读取到短信: 106980095533 - 【建设银行】...
SmsTransactionParser: 解析成功: EXPENSE ¥100.00
SmsTransactionParser: 银行来源: 建设银行
```

---

## 🎯 测试检查清单

### 权限检查
- [ ] 通知使用权已开启
- [ ] 短信权限已授予（READ_SMS）
- [ ] 通知权限已授予
- [ ] 后台运行权限已授予

### 功能检查
- [ ] 通知监听服务运行正常
- [ ] 短信数据库查询可用
- [ ] 银行短信解析逻辑正常
- [ ] 金额提取正确
- [ ] 交易类型判断正确

### 测试场景
- [ ] 建设银行短信解析
- [ ] 中信银行短信解析
- [ ] 微信/支付宝通知解析
- [ ] 无重复记账
- [ ] 记账确认通知弹出

---

## 💡 下一步行动

### 立即可做：

1. **授予短信权限**
   ```
   设置 → 应用管理 → TinyLedger → 权限管理 → 短信 → 允许
   ```

2. **使用短信导入功能**
   - 打开 TinyLedger
   - 找到"短信导入"功能
   - 扫描现有短信

3. **监控日志**
   ```bash
   # 双击运行
   quick_debug.bat
   # 选择 2
   ```

### 如果需要真实银行短信：

1. **进行一笔真实交易**
   - 使用银行卡网上购物
   - 或请朋友转账

2. **观察短信是否被正确解析**

3. **记录测试结果**

---

## 📞 技术支持

如果遇到问题：

1. **导出日志**
   ```bash
   adb logcat -d > sms_test_log_%date:~0,4%%date:~5,2%%date:~8,2%.txt
   ```

2. **检查数据库**
   ```bash
   adb shell "run-as com.tinyledger.app ls /data/data/com.tinyledger.app/databases/"
   ```

3. **查看权限状态**
   ```bash
   adb shell dumpsys package com.tinyledger.app | findstr "permission"
   ```

---

**报告生成时间：** 2026-04-15 20:36  
**日志监控状态：** 运行中 (Terminal 3)  
**下次测试建议：** 授予短信权限后使用短信导入功能测试
