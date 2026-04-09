package com.tinyledger.app.data.sms

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * 轻量级 xlsx 解析器（无需 Apache POI）
 *
 * xlsx 本质是 ZIP 压缩包，结构如下：
 *   xl/sharedStrings.xml  — 字符串共享表（单元格字符串值存这里）
 *   xl/worksheets/sheet1.xml — 第一张工作表数据
 *
 * 微信账单 xlsx 格式（标准导出）：
 *   第1行：说明行  第17行起为数据（与CSV相同列顺序）
 *   列：交易时间, 交易类型, 交易对方, 商品, 收/支, 金额(元), 支付方式, 当前状态, 交易单号, 商户单号, 备注
 *
 * 支付宝账单 xlsx 格式（标准导出）：
 *   前几行为说明，找到含"交易号"/"交易创建时间"的行作为列头
 *   列：交易号, 商家订单号, 交易创建时间, ..., 金额（元）, 收/支, ...
 */
object XlsxParser {

    /**
     * 解析 xlsx InputStream，返回二维字符串表格（行 × 列）
     * 输入流不会被关闭，由调用方负责关闭
     */
    fun parseToTable(inputStream: InputStream): List<List<String>> {
        // ── 1. 读取 ZIP 内容 ────────────────────────────────────────────────
        val zipBytes = inputStream.readBytes()

        var sharedStrings: List<String> = emptyList()
        var sheetXmlBytes: ByteArray? = null

        ZipInputStream(zipBytes.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                when {
                    entry.name == "xl/sharedStrings.xml" -> {
                        sharedStrings = parseSharedStrings(zip.readBytes())
                    }
                    entry.name == "xl/worksheets/sheet1.xml" -> {
                        sheetXmlBytes = zip.readBytes()
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        val sheetBytes = sheetXmlBytes ?: return emptyList()

        // ── 2. 解析工作表 XML ──────────────────────────────────────────────
        return parseSheetXml(sheetBytes, sharedStrings)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 解析 xl/sharedStrings.xml
    // ──────────────────────────────────────────────────────────────────────────

    private fun parseSharedStrings(bytes: ByteArray): List<String> {
        val result = mutableListOf<String>()
        val parser = Xml.newPullParser()
        parser.setInput(bytes.inputStream(), "UTF-8")

        var inT = false
        val current = StringBuilder()

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "si" -> current.clear()
                        "t" -> { inT = true }
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inT) current.append(parser.text)
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "t" -> inT = false
                        "si" -> result.add(current.toString())
                    }
                }
            }
            event = parser.next()
        }
        return result
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 解析 xl/worksheets/sheet1.xml
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 列地址转数字索引：A→0, B→1, ..., Z→25, AA→26, ...
     */
    private fun colLetterToIndex(col: String): Int {
        var result = 0
        for (ch in col.uppercase()) {
            result = result * 26 + (ch - 'A' + 1)
        }
        return result - 1
    }

    /**
     * 从单元格地址（如 A1, BC23）中提取列字母部分
     */
    private fun extractColLetters(ref: String): String {
        return ref.takeWhile { it.isLetter() }
    }

    /**
     * 从单元格地址（如 A1, BC23）中提取行号（1-based）
     */
    private fun extractRowNumber(ref: String): Int? {
        return ref.dropWhile { it.isLetter() }.toIntOrNull()
    }

    private fun parseSheetXml(bytes: ByteArray, sharedStrings: List<String>): List<List<String>> {
        // 使用 Map<Int, Map<Int, String>> 存储稀疏矩阵：rowIndex -> (colIndex -> value)
        val sparseData = mutableMapOf<Int, MutableMap<Int, String>>()

        val parser = Xml.newPullParser()
        parser.setInput(bytes.inputStream(), "UTF-8")

        var currentRef = ""       // 当前单元格地址，如 "A1"
        var currentType = ""      // 当前单元格类型：s=sharedString, 其他=数字/日期/公式
        var inV = false           // 是否在 <v> 标签内
        var inIs = false          // 是否在 <is><t> 内联字符串中
        var cellValue = StringBuilder()

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "c" -> {
                            currentRef = parser.getAttributeValue(null, "r") ?: ""
                            currentType = parser.getAttributeValue(null, "t") ?: ""
                            cellValue.clear()
                            inV = false
                            inIs = false
                        }
                        "v" -> { inV = true; cellValue.clear() }
                        "t" -> if (inIs) { /* 继续 */ }
                        "is" -> inIs = true
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inV || inIs) cellValue.append(parser.text)
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "v" -> {
                            inV = false
                            storeCellValue(
                                ref = currentRef,
                                type = currentType,
                                rawValue = cellValue.toString(),
                                sharedStrings = sharedStrings,
                                sparseData = sparseData
                            )
                        }
                        "is" -> {
                            inIs = false
                            storeCellValue(
                                ref = currentRef,
                                type = "inlineStr",
                                rawValue = cellValue.toString(),
                                sharedStrings = sharedStrings,
                                sparseData = sparseData
                            )
                        }
                    }
                }
            }
            event = parser.next()
        }

        // 稀疏矩阵转二维表
        if (sparseData.isEmpty()) return emptyList()
        val maxRow = sparseData.keys.max()
        val maxCol = sparseData.values.flatMap { it.keys }.maxOrNull() ?: 0

        val result = mutableListOf<List<String>>()
        for (r in 0..maxRow) {
            val rowMap = sparseData[r] ?: emptyMap()
            val row = (0..maxCol).map { c -> rowMap[c] ?: "" }
            result.add(row)
        }
        return result
    }

    private fun storeCellValue(
        ref: String,
        type: String,
        rawValue: String,
        sharedStrings: List<String>,
        sparseData: MutableMap<Int, MutableMap<Int, String>>
    ) {
        if (ref.isBlank()) return
        val colLetters = extractColLetters(ref)
        val rowNum = extractRowNumber(ref) ?: return
        val colIdx = colLetterToIndex(colLetters)
        val rowIdx = rowNum - 1  // 转为0-based

        val cellStr = when (type) {
            "s" -> {  // sharedString
                val idx = rawValue.trim().toIntOrNull() ?: return
                sharedStrings.getOrNull(idx) ?: ""
            }
            "b" -> if (rawValue == "1") "TRUE" else "FALSE"
            "inlineStr" -> rawValue
            else -> {
                // 数字类型（包括日期序列号）：直接返回原始字符串
                // 日期由上层根据列名判断后自行转换
                rawValue
            }
        }

        sparseData.getOrPut(rowIdx) { mutableMapOf() }[colIdx] = cellStr
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Excel 日期序列号 → 时间戳（毫秒）
    // Excel 的日期从 1900-01-01 开始，序列号1 = 1900-01-01
    // 注意：Excel 有一个 1900-02-29 的 Bug，需要跳过序列号60
    // ──────────────────────────────────────────────────────────────────────────

    private val EXCEL_EPOCH_MS: Long = -2209161600000L  // 1899-12-31 00:00:00 UTC in ms
    private const val MS_PER_DAY = 86400000L

    fun excelSerialToMs(serial: Double): Long {
        // Excel Bug: 序列号60对应虚构的1900-02-29，真实日期从61开始对应1900-03-01
        val adjusted = if (serial > 60) serial - 1 else serial
        return EXCEL_EPOCH_MS + (adjusted * MS_PER_DAY).toLong()
    }

    /**
     * 判断一个字符串是否是 Excel 日期序列号（整数或带小数的数字，范围合理）
     * Excel 2024年的日期约为 45xxx
     */
    fun isExcelDateSerial(value: String): Boolean {
        val d = value.toDoubleOrNull() ?: return false
        return d in 10000.0..99999.0  // 1927年 ~ 2173年
    }
}
