package com.tinyledger.app.util

import java.text.DecimalFormat

object CurrencyUtils {
    private val decimalFormat = DecimalFormat("#,##0.00")
    private val integerFormat = DecimalFormat("#,##0")

    fun format(amount: Double, symbol: String = "¥"): String {
        return if (amount == amount.toLong().toDouble()) {
            "$symbol${integerFormat.format(amount)}"
        } else {
            "$symbol${decimalFormat.format(amount)}"
        }
    }

    fun formatWithSign(amount: Double, isIncome: Boolean, symbol: String = "¥"): String {
        val sign = if (isIncome) "+" else "-"
        return "$sign${format(amount, symbol)}"
    }

    // 纯数字格式（不带符号）
    fun formatAmount(amount: Double): String {
        return if (amount == amount.toLong().toDouble()) {
            integerFormat.format(amount)
        } else {
            decimalFormat.format(amount)
        }
    }
}
