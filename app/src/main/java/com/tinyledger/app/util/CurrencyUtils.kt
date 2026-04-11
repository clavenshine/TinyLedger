package com.tinyledger.app.util

import java.text.DecimalFormat

object CurrencyUtils {
    private val decimalFormat = DecimalFormat("#,##0.00")

    fun format(amount: Double, symbol: String = "¥"): String {
        // 始终显示两位小数
        return "$symbol${decimalFormat.format(amount)}"
    }

    fun formatWithSign(amount: Double, isIncome: Boolean, symbol: String = "¥"): String {
        val sign = if (isIncome) "+" else "-"
        return "$sign${format(amount, symbol)}"
    }

    // 纯数字格式（不带符号，始终两位小数）
    fun formatAmount(amount: Double): String {
        return decimalFormat.format(amount)
    }
}
