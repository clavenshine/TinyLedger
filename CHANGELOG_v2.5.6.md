# TinyLedger v2.5.6 Release Notes

## 📅 Release Date
April 20, 2026

## ✨ New Features

### 1. Add Account Page Enhancement
- **Card Number Field**: Added "Last 4 digits of card number (optional)" input field for Cash Accounts and Credit Accounts
  - Only accepts numeric input
  - Limited to maximum 4 digits
  - Hidden for External Accounts
  
- **Credit Account Fields**: Added bill date and repayment date controls for Credit Accounts
  - Bill day input (default: 1, range: 1-31)
  - Repayment day input (default: 10, range: 1-31)
  - Both fields displayed in the same row
  - Only visible when Credit Account type is selected
  
- **Notes Field**: Added notes/purpose input field for all account types
  - Multi-line input support (up to 3 lines)
  - Available for Cash Accounts, Credit Accounts, and External Accounts
  - Mapped to Account's purpose field

### 2. UI/UX Improvements
- Dynamic field visibility based on account attribute selection
- Smooth transitions when switching between account types
- Proper input validation for all new fields

## 🔧 Technical Details
- Version: 2.5.6
- Version Code: 20506
- Min SDK: 26 (Android 8.0)
- Target SDK: 34 (Android 14)
- Compile SDK: 35 (Android 15)

## 📦 Installation
1. Download `TinyLedger-v2.5.6-release.apk`
2. Enable "Install from unknown sources" in your device settings
3. Install the APK
4. Open TinyLedger and enjoy!

## 🔄 Upgrade Notes
- This version is fully backward compatible
- No data migration required
- All existing accounts and transactions will be preserved

---

**Full Changelog**: https://github.com/clavedev/TinyLedger/compare/v2.5.5...v2.5.6
