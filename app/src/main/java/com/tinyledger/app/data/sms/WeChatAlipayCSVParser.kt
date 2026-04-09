package com.tinyledger.app.data.sms

import com.tinyledger.app.domain.model.TransactionType
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * 微信/支付宝账单解析器（同时支持 CSV 和 xlsx 格式）
 *
 * 微信账单格式（标准导出）：
 *   CSV / xlsx 第1行起有说明行，找到含"交易时间"+"交易类型"的列头行后解析
 *   列：交易时间, 交易类型, 交易对方, 商品, 收/支, 金额(元), 支付方式, 当前状态, 交易单号, 商户单号, 备注
 *
 * 支付宝账单格式（标准导出）：
 *   CSV / xlsx 找到含"交易号"/"交易创建时间"+"金额"的列头行后解析
 *   列：交易号, 商家订单号, 交易创建时间, ..., 金额（元）, 收/支, 备注, 资金状态
 */
object WeChatAlipayCSVParser {

    enum class Source { WECHAT, ALIPAY }

    /**
     * 解析结果数据类（与 SmsTransaction 兼容，复用已有导入逻辑）
     */
    data class CsvTransaction(
        val id: String,
        val source: String,           // 来源平台
        val counterpart: String,      // 交易对方
        val description: String,      // 商品/服务描述
        val type: TransactionType,
        val amount: Double,
        val paymentMethod: String,    // 支付方式（微信钱包/银行卡 等）
        val date: Long,               // 时间戳
        val status: String,           // 交易状态
        val remark: String = ""
    )

    // ──────────────────────────────────────────────────────────────────────────
    // 公共入口：CSV 文本解析
    // ──────────────────────────────────────────────────────────────────────────

    fun parse(csvText: String): List<CsvTransaction> {
        return when (detectSource(csvText)) {
            Source.WECHAT -> parseWeChat(csvText)
            Source.ALIPAY -> parseAlipay(csvText)
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 公共入口：xlsx InputStream 解析（调用方负责关闭流）
    // ──────────────────────────────────────────────────────────────────────────

    fun parseXlsx(inputStream: InputStream): List<CsvTransaction> {
        val table = XlsxParser.parseToTable(inputStream)
        if (table.isEmpty()) return emptyList()

        // 将 xlsx 表格转成 CSV 格式文本（用制表符拼接），复用 CSV 解析逻辑
        val csvText = tableToFakeCsv(table)
        return when (detectSource(csvText)) {
            Source.WECHAT -> parseWeChatTable(table)
            Source.ALIPAY -> parseAlipayTable(table)
        }
    }

    /**
     * 将二维表格前几行转成文字，用于 detectSource 检测来源
     */
    private fun tableToFakeCsv(table: List<List<String>>): String {
        return table.take(20).joinToString("\n") { row -> row.joinToString(",") }
    }

    fun detectSource(csvText: String): Source {
        val header = csvText.take(500)
        return when {
            header.contains("微信") || header.contains("Wechat") || header.contains("weixin") -> Source.WECHAT
            header.contains("支付宝") || header.contains("Alipay") -> Source.ALIPAY
            // 根据列名进一步判断
            header.contains("交易类型") && header.contains("支付方式") -> Source.WECHAT
            header.contains("交易来源地") || header.contains("资金状态") -> Source.ALIPAY
            else -> Source.WECHAT // 默认微信格式
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 微信账单解析
    // ──────────────────────────────────────────────────────────────────────────

    private fun parseWeChat(csvText: String): List<CsvTransaction> {
        val lines = csvText.lines()
        val result = mutableListOf<CsvTransaction>()

        // 找到数据起始行（包含"交易时间"的列头行）
        var headerLineIndex = -1
        for (i in lines.indices) {
            val line = lines[i].trim()
            if (line.contains("交易时间") && line.contains("交易类型")) {
                headerLineIndex = i
                break
            }
        }
        if (headerLineIndex < 0) return emptyList()

        val headerLine = lines[headerLineIndex]
        val headers = parseCsvLine(headerLine)

        // 列索引映射
        val idxTime = headers.indexOfFirst { it.contains("交易时间") }
        val idxType = headers.indexOfFirst { it.contains("交易类型") }
        val idxCounterpart = headers.indexOfFirst { it.contains("交易对方") }
        val idxGoods = headers.indexOfFirst { it.contains("商品") }
        val idxInOut = headers.indexOfFirst { it.contains("收/支") || it.contains("收支") }
        val idxAmount = headers.indexOfFirst { it.contains("金额") }
        val idxPayMethod = headers.indexOfFirst { it.contains("支付方式") }
        val idxStatus = headers.indexOfFirst { it.contains("当前状态") || it.contains("状态") }
        val idxOrderNo = headers.indexOfFirst { it.contains("交易单号") }
        val idxRemark = headers.indexOfFirst { it.contains("备注") }

        // 解析数据行
        for (i in (headerLineIndex + 1) until lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty()) continue

            val cols = parseCsvLine(line)
            if (cols.size < 5) continue

            try {
                val inOutStr = if (idxInOut >= 0 && idxInOut < cols.size) cols[idxInOut].trim() else ""
                val amountStr = if (idxAmount >= 0 && idxAmount < cols.size)
                    cols[idxAmount].trim().removePrefix("¥").replace(",", "")
                else continue

                val amount = amountStr.toDoubleOrNull() ?: continue
                if (amount <= 0) continue

                // 过滤不记录的类型（如：不计收支）
                if (inOutStr.isEmpty() || inOutStr == "/" || inOutStr == "不计收支") continue

                val txType = when {
                    inOutStr.contains("收入") -> TransactionType.INCOME
                    inOutStr.contains("支出") -> TransactionType.EXPENSE
                    else -> continue
                }

                val timeStr = if (idxTime >= 0 && idxTime < cols.size) cols[idxTime].trim() else ""
                val date = parseDateTime(timeStr) ?: System.currentTimeMillis()

                val counterpart = if (idxCounterpart >= 0 && idxCounterpart < cols.size) cols[idxCounterpart].trim() else ""
                val goods = if (idxGoods >= 0 && idxGoods < cols.size) cols[idxGoods].trim() else ""
                val payMethod = if (idxPayMethod >= 0 && idxPayMethod < cols.size) cols[idxPayMethod].trim() else ""
                val status = if (idxStatus >= 0 && idxStatus < cols.size) cols[idxStatus].trim() else ""
                val orderNo = if (idxOrderNo >= 0 && idxOrderNo < cols.size) cols[idxOrderNo].trim() else UUID.randomUUID().toString()
                val remark = if (idxRemark >= 0 && idxRemark < cols.size) cols[idxRemark].trim() else ""

                // 过滤退款/充值中/已退款等无意义状态
                if (status.contains("退款中") || status.contains("已全额退款")) continue

                result.add(
                    CsvTransaction(
                        id = orderNo.ifEmpty { UUID.randomUUID().toString() },
                        source = "微信支付",
                        counterpart = counterpart,
                        description = goods.ifEmpty { counterpart },
                        type = txType,
                        amount = amount,
                        paymentMethod = payMethod,
                        date = date,
                        status = status,
                        remark = remark
                    )
                )
            } catch (e: Exception) {
                // 单行解析失败跳过
            }
        }

        return result
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 支付宝账单解析
    // ──────────────────────────────────────────────────────────────────────────

    private fun parseAlipay(csvText: String): List<CsvTransaction> {
        val lines = csvText.lines()
        val result = mutableListOf<CsvTransaction>()

        // 找到列头行
        var headerLineIndex = -1
        for (i in lines.indices) {
            val line = lines[i].trim()
            if ((line.contains("交易号") || line.contains("交易创建时间")) && line.contains("金额")) {
                headerLineIndex = i
                break
            }
        }
        if (headerLineIndex < 0) return emptyList()

        val headers = parseCsvLine(lines[headerLineIndex])

        // 列索引映射（支付宝）
        val idxOrderNo = headers.indexOfFirst { it.contains("交易号") || it.contains("交易单号") }
        val idxTime = headers.indexOfFirst { it.contains("交易创建时间") || it.contains("时间") }
        val idxCounterpart = headers.indexOfFirst { it.contains("交易对方") }
        val idxGoods = headers.indexOfFirst { it.contains("商品名称") || it.contains("商品") }
        val idxAmount = headers.indexOfFirst { it.contains("金额") }
        val idxInOut = headers.indexOfFirst { it.contains("收/支") || it.contains("收支") }
        val idxRemark = headers.indexOfFirst { it.contains("备注") }
        val idxFundStatus = headers.indexOfFirst { it.contains("资金状态") }

        for (i in (headerLineIndex + 1) until lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty()) continue

            // 停止解析：遇到页脚说明行
            if (line.startsWith("---") || line.startsWith("注：")) break

            val cols = parseCsvLine(line)
            if (cols.size < 5) continue

            try {
                val inOutStr = if (idxInOut >= 0 && idxInOut < cols.size) cols[idxInOut].trim() else ""
                val amountStr = if (idxAmount >= 0 && idxAmount < cols.size)
                    cols[idxAmount].trim().removePrefix("¥").replace(",", "")
                else continue

                val amount = amountStr.toDoubleOrNull() ?: continue
                if (amount <= 0) continue

                if (inOutStr.isEmpty() || inOutStr == "/" || inOutStr == "不计收支") continue

                val txType = when {
                    inOutStr.contains("收入") -> TransactionType.INCOME
                    inOutStr.contains("支出") || inOutStr.contains("支付") -> TransactionType.EXPENSE
                    else -> continue
                }

                // 过滤资金状态（如：退款、冻结中）
                val fundStatus = if (idxFundStatus >= 0 && idxFundStatus < cols.size) cols[idxFundStatus].trim() else ""
                if (fundStatus.contains("退款") || fundStatus.contains("冻结")) continue

                val timeStr = if (idxTime >= 0 && idxTime < cols.size) cols[idxTime].trim() else ""
                val date = parseDateTime(timeStr) ?: System.currentTimeMillis()

                val counterpart = if (idxCounterpart >= 0 && idxCounterpart < cols.size) cols[idxCounterpart].trim() else ""
                val goods = if (idxGoods >= 0 && idxGoods < cols.size) cols[idxGoods].trim() else ""
                val orderNo = if (idxOrderNo >= 0 && idxOrderNo < cols.size) cols[idxOrderNo].trim() else UUID.randomUUID().toString()
                val remark = if (idxRemark >= 0 && idxRemark < cols.size) cols[idxRemark].trim() else ""

                result.add(
                    CsvTransaction(
                        id = orderNo.ifEmpty { UUID.randomUUID().toString() },
                        source = "支付宝",
                        counterpart = counterpart,
                        description = goods.ifEmpty { counterpart },
                        type = txType,
                        amount = amount,
                        paymentMethod = "支付宝",
                        date = date,
                        status = fundStatus,
                        remark = remark
                    )
                )
            } catch (e: Exception) {
                // 单行解析失败跳过
            }
        }

        return result
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 工具方法
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 解析 CSV 行（处理引号内逗号）
     */
    fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false

        for (ch in line) {
            when {
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    result.add(current.toString().trim())
                    current.clear()
                }
                else -> current.append(ch)
            }
        }
        result.add(current.toString().trim())
        return result
    }

    /**
     * 解析日期时间字符串为时间戳
     * 支持格式：
     *   "2024-01-15 14:30:22"
     *   "2024/01/15 14:30:22"
     *   "2024-01-15"
     *   Excel 日期序列号（如 45291.0）
     */
    internal fun parseDateTime(dateStr: String): Long? {
        if (dateStr.isBlank()) return null
        // Excel 日期序列号
        if (XlsxParser.isExcelDateSerial(dateStr)) {
            return XlsxParser.excelSerialToMs(dateStr.toDouble())
        }
        val cleanDate = dateStr.replace("/", "-").trim()
        val formats = listOf(
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd"
        )
        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.getDefault())
                sdf.isLenient = false
                return sdf.parse(cleanDate)?.time
            } catch (_: Exception) {}
        }
        return null
    }

    // ──────────────────────────────────────────────────────────────────────────
    // xlsx 表格解析：微信
    // ──────────────────────────────────────────────────────────────────────────

    private fun parseWeChatTable(table: List<List<String>>): List<CsvTransaction> {
        val result = mutableListOf<CsvTransaction>()

        // 找到列头行
        var headerRowIndex = -1
        for (i in table.indices) {
            val row = table[i]
            val joined = row.joinToString(",")
            if (joined.contains("交易时间") && joined.contains("交易类型")) {
                headerRowIndex = i
                break
            }
        }
        if (headerRowIndex < 0) return emptyList()

        val headers = table[headerRowIndex]

        val idxTime       = headers.indexOfFirst { it.contains("交易时间") }
        val idxCounterpart= headers.indexOfFirst { it.contains("交易对方") }
        val idxGoods      = headers.indexOfFirst { it.contains("商品") }
        val idxInOut      = headers.indexOfFirst { it.contains("收/支") || it.contains("收支") }
        val idxAmount     = headers.indexOfFirst { it.contains("金额") }
        val idxPayMethod  = headers.indexOfFirst { it.contains("支付方式") }
        val idxStatus     = headers.indexOfFirst { it.contains("当前状态") || it.contains("状态") }
        val idxOrderNo    = headers.indexOfFirst { it.contains("交易单号") }
        val idxRemark     = headers.indexOfFirst { it.contains("备注") }

        for (i in (headerRowIndex + 1) until table.size) {
            val cols = table[i]
            if (cols.all { it.isBlank() }) continue
            if (cols.size < 5) continue

            try {
                val inOutStr = cols.getOrElse(idxInOut) { "" }.trim()
                val amountStr = cols.getOrElse(idxAmount) { "" }
                    .trim().removePrefix("¥").removePrefix("￥").replace(",", "")
                val amount = amountStr.toDoubleOrNull() ?: continue
                if (amount <= 0) continue

                if (inOutStr.isEmpty() || inOutStr == "/" || inOutStr == "不计收支") continue

                val txType = when {
                    inOutStr.contains("收入") -> TransactionType.INCOME
                    inOutStr.contains("支出") -> TransactionType.EXPENSE
                    else -> continue
                }

                val timeStr = cols.getOrElse(idxTime) { "" }.trim()
                val date = parseDateTime(timeStr) ?: System.currentTimeMillis()

                val counterpart = cols.getOrElse(idxCounterpart) { "" }.trim()
                val goods       = cols.getOrElse(idxGoods) { "" }.trim()
                val payMethod   = cols.getOrElse(idxPayMethod) { "" }.trim()
                val status      = cols.getOrElse(idxStatus) { "" }.trim()
                val orderNo     = cols.getOrElse(idxOrderNo) { "" }.trim()
                val remark      = cols.getOrElse(idxRemark) { "" }.trim()

                if (status.contains("退款中") || status.contains("已全额退款")) continue

                result.add(
                    CsvTransaction(
                        id = orderNo.ifEmpty { UUID.randomUUID().toString() },
                        source = "微信支付",
                        counterpart = counterpart,
                        description = goods.ifEmpty { counterpart },
                        type = txType,
                        amount = amount,
                        paymentMethod = payMethod,
                        date = date,
                        status = status,
                        remark = remark
                    )
                )
            } catch (_: Exception) {}
        }

        return result
    }

    // ──────────────────────────────────────────────────────────────────────────
    // xlsx 表格解析：支付宝
    // ──────────────────────────────────────────────────────────────────────────

    private fun parseAlipayTable(table: List<List<String>>): List<CsvTransaction> {
        val result = mutableListOf<CsvTransaction>()

        var headerRowIndex = -1
        for (i in table.indices) {
            val row = table[i]
            val joined = row.joinToString(",")
            if ((joined.contains("交易号") || joined.contains("交易创建时间")) && joined.contains("金额")) {
                headerRowIndex = i
                break
            }
        }
        if (headerRowIndex < 0) return emptyList()

        val headers = table[headerRowIndex]

        val idxOrderNo    = headers.indexOfFirst { it.contains("交易号") || it.contains("交易单号") }
        val idxTime       = headers.indexOfFirst { it.contains("交易创建时间") || it.contains("时间") }
        val idxCounterpart= headers.indexOfFirst { it.contains("交易对方") }
        val idxGoods      = headers.indexOfFirst { it.contains("商品名称") || it.contains("商品") }
        val idxAmount     = headers.indexOfFirst { it.contains("金额") }
        val idxInOut      = headers.indexOfFirst { it.contains("收/支") || it.contains("收支") }
        val idxRemark     = headers.indexOfFirst { it.contains("备注") }
        val idxFundStatus = headers.indexOfFirst { it.contains("资金状态") }

        for (i in (headerRowIndex + 1) until table.size) {
            val cols = table[i]
            if (cols.all { it.isBlank() }) continue
            if (cols.size < 5) continue

            val firstCell = cols.firstOrNull { it.isNotBlank() } ?: ""
            if (firstCell.startsWith("---") || firstCell.startsWith("注：")) break

            try {
                val inOutStr = cols.getOrElse(idxInOut) { "" }.trim()
                val amountStr = cols.getOrElse(idxAmount) { "" }
                    .trim().removePrefix("¥").removePrefix("￥").replace(",", "")
                val amount = amountStr.toDoubleOrNull() ?: continue
                if (amount <= 0) continue

                if (inOutStr.isEmpty() || inOutStr == "/" || inOutStr == "不计收支") continue

                val txType = when {
                    inOutStr.contains("收入") -> TransactionType.INCOME
                    inOutStr.contains("支出") || inOutStr.contains("支付") -> TransactionType.EXPENSE
                    else -> continue
                }

                val fundStatus = cols.getOrElse(idxFundStatus) { "" }.trim()
                if (fundStatus.contains("退款") || fundStatus.contains("冻结")) continue

                val timeStr = cols.getOrElse(idxTime) { "" }.trim()
                val date = parseDateTime(timeStr) ?: System.currentTimeMillis()

                val counterpart = cols.getOrElse(idxCounterpart) { "" }.trim()
                val goods       = cols.getOrElse(idxGoods) { "" }.trim()
                val orderNo     = cols.getOrElse(idxOrderNo) { "" }.trim()
                val remark      = cols.getOrElse(idxRemark) { "" }.trim()

                result.add(
                    CsvTransaction(
                        id = orderNo.ifEmpty { UUID.randomUUID().toString() },
                        source = "支付宝",
                        counterpart = counterpart,
                        description = goods.ifEmpty { counterpart },
                        type = txType,
                        amount = amount,
                        paymentMethod = "支付宝",
                        date = date,
                        status = fundStatus,
                        remark = remark
                    )
                )
            } catch (_: Exception) {}
        }

        return result
    }
}
