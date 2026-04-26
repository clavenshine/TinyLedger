package com.tinyledger.app.util

import android.util.Log
import com.tinyledger.app.domain.model.Account
import com.tinyledger.app.domain.model.AccountType
import com.tinyledger.app.domain.model.Category
import com.tinyledger.app.domain.model.TransactionType
import com.tinyledger.app.ui.screens.budget.ParsedRecord
import java.util.*
import com.tinyledger.app.ui.screens.budget.BalanceValidation

/**
 * 截屏交易解析器 v13 — 余额位置检测修复版
 *   - 余额链式反推真实金额（|余额差| = 交易额，数学确定）
 *   - 余额检测：标签"余额" + 回退位置搜索（金额右侧的¥数字）
 *   - 日期后向填充 + 时间独立识别
 *   - 无小数点恢复（OCR丢失小数点时除以100）
 * 保留 v8 纯文本解析作为 fallback
 */
object ScreenshotTransactionParser {
    private val TAG = "ScreenshotTxnParser"

    private data class DateCtx(val year: String, val month: String, val day: String)

    /** 带坐标的OCR文本元素 — Phase 2 视觉解析核心数据结构 */
    data class OcrElement(
        val text: String,
        val left: Int, val top: Int, val right: Int, val bottom: Int
    ) {
        val centerY: Float get() = (top + bottom) / 2f
        val centerX: Float get() = (left + right) / 2f
        val height: Int get() = bottom - top
    }

    // ─── 噪声关键词 ───
    private val NOISE_WORDS = setOf(
        "交易明细", "收支分析", "暂无交易", "已登机", "账户明细",
        "筛选", "加载更多", "查看更多", "全部交易", "显示全部",
        "查看月账单", "更多", "全选", "删除", "导出", "分享",
        "长城电子", "借记卡", "信用卡", "I类账户", "II类账户",
        "III类账户", "登机", "航班", "西10D", "截图", "选择截图",
        "自动识别", "金额备注", "类型", "日期", "返回",
        "建设银行", "工商银行", "农业银行", "中国银行", "招商银行",
        "账户明细", "账单", "总支出", "总收入", "筛选了"
    )

    // ─── 交易类型关键词 ───
    private val TX_TYPE_PATTERNS = listOf(
        "跨行转账", "转账存入", "网上快捷支付", "网上支付", "快捷支付",
        "网上快捷提现", "网上提现", "结息", "存入", "汇入",
        "消费", "缴费", "充值", "还款", "退款",
        "生活费", "工资", "代发", "代扣",
        "转账", "收款", "付款", "支付", "收入", "支出",
        "提现", "取款", "转入", "转出", "利息", "赎回",
        "还信用卡", "赎回薪金煲", "支付宝", "财付通快捷支付", "美团支付"
    )

    // ─── 正则 ───
    private val ANCHOR_RMB = Regex("""人民[币市]元\s*([+-]?[\d,]+\.?\d*)""")
    // ¥后跟符号（建行/CCB格式： ¥-9.00）
    private val YEN_SIGN_AFTER = Regex("""^\s*[¥￥]\s*([+-]?)\s*([\d,]+\.?\d*)\s*$""")
    // 符号在¥前（工行/ICBC格式：-¥0.79）
    private val YEN_SIGN_BEFORE = Regex("""^\s*([+-])\s*[¥￥]\s*([\d,]+\.?\d*)\s*$""")
    // 行内¥金额
    private val YEN_INLINE = Regex("""[¥￥]\s*([+-]?)\s*([\d,]+\.?\d*)""")
    // 余额行：匹配所有简繁体 [余餘][额額]
    private val IS_BALANCE = Regex("""[余餘]\s*[额額]|余[:：]""")
    // 余额值提取
    private val BALANCE_VAL = Regex("""[余餘]\s*[额額][:：\s]*[¥￥]?\s*([\d,]+\.?\d*)""")
    // 建行类型行
    private val CCB_TYPE = Regex("""^\s*(消费|缴费|充值|还款|转账|退款|付款|支付|收入|收款|提现|取款)\s*$""")
    // 日期
    private val DATETIME_RE = Regex("""\d{4}[-./]\d{1,2}[-./]\d{1,2}\s+\d{1,2}:\d{2}""")
    private val MONTH_RE = Regex("""(\d{1,2})月[/\s]*(\d{4})""")
    // 中文完整日期 2026年4月15日
    private val CN_FULLDATE = Regex("""(\d{4})\s*年\s*(\d{1,2})\s*月\s*(\d{1,2})\s*日""")
    // 简化中文日期 4月15日
    private val CN_MD = Regex("""(\d{1,2})\s*月\s*(\d{1,2})\s*日""")
    // ★ 短日期 MM-DD / MM/DD（银行截图最常见格式：04-15、04/15、4.15）
    private val SHORT_DATE = Regex("""(\d{1,2})[-./](\d{1,2})(?:\s|$)""")
    // ★ 完整日期+星期 2025-12-31 周三 / 2026-1-1 周四
    private val DATE_WITH_WK = Regex("""(\d{4})[-./](\d{1,2})[-./](\d{1,2})\s+周[一二三四五六日天]""")
    // ★ 年月头 2025年12月 / 2026年04月（仅月头，后面跟具体日号）
    private val YM_HEADER = Regex("""(\d{4})\s*年\s*(\d{1,2})\s*月""")
    // ★ 独立月份行 "4月"（无年份，后续独立数字即为日号）
    private val MONTH_ONLY = Regex("""^(\d{2}|\d)\s*月\s*$""")
    private val DAYWK_RE = Regex("""(\d{1,2})\s*(?:周[一二三四五六日天]|星期[一二三四五六日天])""")
    private val FULLDATE_RE = Regex("""(\d{4})[-./](\d{1,2})[-./](\d{1,2})""")
    private val SUMMARY_RE = Regex("""^(支出|收入|借方|贷方|总支出|总收入)\s+[\d,]+\.?\d*$""")
    private val CARD_RE = Regex("""\d{4}\s*\*+\s*\d{3,4}""")
    private val SIGNED_NUM = Regex("""^([+-])\s*([\d,]+\.?\d*)$""")
    // ★ 纯逗号分隔正数兜底（如OCR拆分后的独立 "49,000.00"）
    private val COMMA_NUM = Regex("""^[\d,]+(\.\d+)?$""")
    // ★ OCR噪声：箭头/时间/无意义短词
    private val ARROW_NOISE = Regex("""→|->|-->>""")
    // ★ 时间元素 HH:MM 或 HH:MM:SS
    private val TIME_ELEMENT = Regex("""^\d{1,2}:\d{2}(:\d{2})?$""")

    // ─── 关键词 ───
    private val INCOME_KW = listOf(
        "收款", "到账", "转入", "收入", "退款", "红包", "工资",
        "薪资", "奖金", "报销", "返现", "汇入", "存入", "结息",
        "跨行转入", "转账存入", "利息", "转入", "赎回"
    )
    private val EXPENSE_KW = listOf(
        "付款", "支付", "消费", "扣款", "转出", "缴费", "取款",
        "提现", "支出", "充值", "还款", "购物"
    )
    // ★ 扩展：备注中的噪声关键词（直接排除）
    private val NOTE_EXCLUDE = setOf(
        "消费", "缴费", "充值", "还款", "转账", "退款", "付款",
        "支付", "收入", "收款", "提现", "取款", "余额", "餘額",
        "余額", "余额:", "时间", "款", "支", "支出", "元"
    )
    // ★ 备注中需要排除的模式（包含匹配）
    private val NOTE_EXCLUDE_CONTAINS = listOf(
        "时间 →", "时间→", "- 时间", "-时间", " →", "→",
        "---", "---→", "缴费 -"
    )

    // ════════════════════════════════════════════════════════════════════
    // 日期预扫描 — 前向传播 + 后向填充 + 多格式支持
    // ════════════════════════════════════════════════════════════════════
    private fun buildDateCtx(lines: List<String>): Map<Int, DateCtx> {
        val map = mutableMapOf<Int, DateCtx>()
        var year = Calendar.getInstance().get(Calendar.YEAR).toString()
        var month = ""
        var day = ""

        // === 第一遍：前向扫描 ===
        for ((idx, line) in lines.withIndex()) {
            val cl = cleanLine(line)
            var handled = false

            // ★ 完整日期+星期 2025-12-31 周三（优先于纯日期，因为更具体）
            if (!handled) {
                DATE_WITH_WK.find(cl)?.let { m ->
                    year = m.groupValues[1]
                    month = m.groupValues[2].padStart(2, '0')
                    day = m.groupValues[3].padStart(2, '0')
                    map[idx] = DateCtx(year, month, day); handled = true
                }
            }
            // 完整日期时间 2026-04-15 14:30
            if (!handled && DATETIME_RE.containsMatchIn(cl)) {
                FULLDATE_RE.find(cl)?.let { df ->
                    year = df.groupValues[1]; month = df.groupValues[2].padStart(2, '0')
                    day = df.groupValues[3].padStart(2, '0')
                }
                map[idx] = DateCtx(year, month, day); handled = true
            }
            // 中文完整日期 2026年4月15日
            if (!handled) {
                CN_FULLDATE.find(cl)?.let { m ->
                    year = m.groupValues[1]; month = m.groupValues[2].padStart(2, '0')
                    day = m.groupValues[3].padStart(2, '0')
                    map[idx] = DateCtx(year, month, day); handled = true
                }
            }
            // ★ 年月头 2025年12月（设置年月上下文，清空day等待日号）
            if (!handled) {
                YM_HEADER.find(cl)?.let { m ->
                    year = m.groupValues[1]; month = m.groupValues[2].padStart(2, '0')
                    day = ""
                    map[idx] = DateCtx(year, month, day); handled = true
                }
            }
            // 纯日期 2026-04-15
            if (!handled) {
                val fd = FULLDATE_RE.find(cl)
                if (fd != null) {
                    year = fd.groupValues[1]; month = fd.groupValues[2].padStart(2, '0')
                    day = fd.groupValues[3].padStart(2, '0')
                    map[idx] = DateCtx(year, month, day); handled = true
                }
            }
            // 月份头 4月/2026
            if (!handled) {
                val mh = MONTH_RE.find(cl)
                if (mh != null) {
                    month = mh.groupValues[1].padStart(2, '0'); year = mh.groupValues[2]
                    day = ""
                    map[idx] = DateCtx(year, month, day); handled = true
                }
            }
            // 独立月份 "4月"（无年份，用当前年份；后续独立数字即为日号）
            if (!handled) {
                val mo = MONTH_ONLY.find(cl)
                if (mo != null) {
                    month = mo.groupValues[1].padStart(2, '0')
                    day = ""
                    map[idx] = DateCtx(year, month, day); handled = true
                }
            }
            // 短日期 04-15 / 04/15 / 4.15（直接设置月+日）
            if (!handled) {
                val sd = SHORT_DATE.find(cl)
                if (sd != null) {
                    val mVal = sd.groupValues[1].toIntOrNull() ?: 0
                    val dVal = sd.groupValues[2].toIntOrNull() ?: 0
                    if (mVal in 1..12 && dVal in 1..31) {
                        month = sd.groupValues[1].padStart(2, '0')
                        day = sd.groupValues[2].padStart(2, '0')
                        map[idx] = DateCtx(year, month, day); handled = true
                    }
                }
            }
            // 中文月日 4月15日（无年份，用当前年份）
            if (!handled) {
                CN_MD.find(cl)?.let { m ->
                    month = m.groupValues[1].padStart(2, '0')
                    day = m.groupValues[2].padStart(2, '0')
                    map[idx] = DateCtx(year, month, day); handled = true
                }
            }
            // 日期+星期 15 周三
            if (!handled) {
                val dw = DAYWK_RE.find(cl)
                if (dw != null) {
                    day = dw.groupValues[1].padStart(2, '0')
                    map[idx] = DateCtx(year, month, day); handled = true
                }
            }
            // 独立日号 1-31（仅当已有月份上下文）
            if (!handled && cl.matches(Regex("""^\d{1,2}$"""))) {
                val n = cl.toIntOrNull() ?: 0
                if (n in 1..31 && month.isNotEmpty()) {
                    day = cl.padStart(2, '0')
                    map[idx] = DateCtx(year, month, day); handled = true
                }
            }

            if (!handled || !map.containsKey(idx)) {
                map[idx] = DateCtx(year, month, day)
            }
        }

        // === 第二遍：后向填充缺失的日期 ===
        var nextComplete: DateCtx? = null
        for (idx in lines.indices.reversed()) {
            val cur = map[idx] ?: continue
            if (cur.day.isNotBlank() && cur.month.isNotBlank()) {
                nextComplete = cur
            } else if (nextComplete != null && (cur.day.isBlank() || cur.month.isBlank())) {
                map[idx] = if (cur.month.isNotBlank()) {
                    cur.copy(day = nextComplete.day)
                } else {
                    nextComplete
                }
            }
        }

        return map
    }

    // ════════════════════════════════════════════════════════════════════
    // 主入口 — v8 纯文本解析（fallback）
    // ════════════════════════════════════════════════════════════════════
    fun parse(ocrText: String, accounts: List<Account>): List<ParsedRecord> {
        val lines = ocrText.lines().map { it.trim() }.filter { it.isNotBlank() }
        Log.d(TAG, "=== v8 文本解析 fallback，${lines.size} 行 ===")
        if (lines.size < 3) return emptyList()

        lines.take(50).forEachIndexed { i, l -> Log.d(TAG, "  OCR[$i]: $l") }

        val dateMap = buildDateCtx(lines)
        dateMap.entries.take(10).forEach { (idx, ctx) ->
            Log.d(TAG, "  DateCtx[$idx]: ${ctx.year}-${ctx.month}-${ctx.day}")
        }

        val all = mutableListOf<ParsedRecord>()

        val r1 = parseAnchors(lines, accounts, dateMap)
        if (r1.isNotEmpty()) { all.addAll(r1); Log.d(TAG, "锚点 ${r1.size} 条") }

        if (all.size < 2) {
            val r2 = parseSingleLine(lines, accounts)
            if (r2.isNotEmpty()) { all.addAll(r2); Log.d(TAG, "单行 ${r2.size} 条") }
        }

        val result = all.distinctBy { "${it.amount}_${it.note}" }
        Log.d(TAG, "=== 去重后 ${result.size} 条 ===")
        return validateAndCorrectFromBalance(result)
    }

    // ════════════════════════════════════════════════════════════════════
    // Phase 2 主入口 — 带坐标视觉解析（Y分组 + X列识别）v13
    // ════════════════════════════════════════════════════════════════════
    fun parseVisual(elements: List<OcrElement>, accounts: List<Account>): List<ParsedRecord> {
        if (elements.size < 5) {
            Log.d(TAG, "=== parseVisual: 元素太少(${elements.size})，跳过 ===")
            return emptyList()
        }
        Log.d(TAG, "=== Phase 2 v13 视觉解析，${elements.size} 个元素 ===")

        // 1. 按Y坐标分组为视觉行
        val rows = groupByY(elements)
        Log.d(TAG, "  Y分组: ${rows.size} 行 (阈值=$yGroupThreshold)")

        // 2. 行内按X排序
        rows.forEach { it.sortBy { e -> e.centerX } }

        // 3. 调试：打印所有行的元素
        rows.forEachIndexed { i, row ->
            val texts = row.map { "\"${it.text}\"@x=${it.centerX.toInt()}" }
            Log.d(TAG, "  Row[$i] y~${row.first().centerY.toInt()}: ${texts.joinToString(" | ")}")
        }

        // ═══ 4. 第一遍：构建日期上下文 + 时间上下文（前向+后向） ═══
        val dateCtxMap = buildVisualDateMap(rows)
        val timeMap = buildVisualTimeMap(rows)

        // ═══ 5. 第二遍：逐行解析交易记录 ═══
        var accumType = ""
        val accumDesc = mutableListOf<String>()
        val records = mutableListOf<ParsedRecord>()

        var rowsWithBalance = 0
        var rowsWithAmount = 0

        for ((rowIdx, row) in rows.withIndex()) {
            val rowDateEl = row.firstOrNull { isDateElement(it.text) }
            val rowTimeEl = row.firstOrNull { isTimeElement(it.text) }
            val rowAmtEl = row.firstOrNull { isAmountElement(it.text) }
            // ★ v13: 余额检测 — 先找"余额"标签，找不到则向金额右侧搜索¥数字
            val rowBalByLabel = row.firstOrNull { isBalanceElement(it.text) }
            val rowBalEl = rowBalByLabel
                ?: (rowAmtEl?.let { amt -> findBalanceInRow(row, amt) })

            // ★ 诊断：每行¥元素统计
            val yenElements = row.filter { YEN_INLINE.containsMatchIn(it.text.trim()) }
            if (rowAmtEl != null) rowsWithAmount++
            if (rowBalEl != null) rowsWithBalance++
            if (yenElements.isNotEmpty() && rowBalEl == null && rowAmtEl != null) {
                Log.d(TAG, "  ⚠ Row[$rowIdx] Y~${row.first().top}: amt=\"${rowAmtEl.text}\"@${rowAmtEl.centerX} | yenCount=${yenElements.size} | balByLabel=${rowBalByLabel?.text ?: "无"} | balByPos=无 | 所有¥元素: [${yenElements.joinToString { "\"${it.text}\"@${it.centerX}" }}]")
            }

            // 非日期/时间/金额/余额的"其他元素" → 作为描述/类型
            val rowOther = row.filter { el ->
                val t = el.text.trim()
                !isDateElement(t) && !isTimeElement(t) && !isAmountElement(t) && !isBalanceElement(t) && t.length >= 2
            }

            // ★ 累积类型/描述
            if (rowOther.isNotEmpty()) {
                val txt = rowOther.joinToString(" ") { it.text.trim() }
                if (isTypeKeywordOnly(txt)) {
                    accumType = txt
                } else if (txt.isNotBlank() && !isNoiseText(txt)) {
                    accumDesc.add(txt)
                }
            }

            // ★ 发现金额锚点 → 输出一条记录
            if (rowAmtEl != null) {
                val signedAmt = parseSignedAmountWithRecovery(rowAmtEl.text)
                val balVal = rowBalEl?.let { extractBalanceNumber(it.text) } ?: ""
                val ctx = dateCtxMap[rowIdx] ?: DateCtx("", "", "")
                val timePart = timeMap[rowIdx] ?: ""

                // 组装描述：类型 + 备注（不含时间！）
                val effectiveType = accumType.ifBlank {
                    accumDesc.firstOrNull { isTypeKeywordOnly(it) } ?: ""
                }
                val descParts = if (effectiveType.isNotEmpty()) {
                    accumDesc.filter { it != effectiveType }
                } else {
                    accumDesc.toList()
                }
                // ★ 再次过滤：确保描述中不含时间
                val cleanDescParts = descParts.filter { !isTimeElement(it) && !isDateElement(it) }
                val desc = buildString {
                    if (effectiveType.isNotEmpty()) append(effectiveType)
                    if (cleanDescParts.isNotEmpty()) {
                        if (isNotEmpty()) append(" ")
                        append(cleanDescParts.joinToString(" "))
                    }
                }.trim()

                // ★ 日期+时间合并
                val dateStr = buildDate(ctx.year, ctx.month, ctx.day)
                val dateTimeStr = if (timePart.isNotEmpty()) {
                    dateStr.replace("00:00:00", "$timePart")
                } else {
                    dateStr
                }

                val txType = if (signedAmt < 0) TransactionType.EXPENSE else TransactionType.INCOME
                val note = desc.ifBlank { "银行交易" }.take(80)

                records.add(ParsedRecord(
                    amount = String.format("%.2f", kotlin.math.abs(signedAmt)),
                    type = txType,
                    note = note,
                    category = inferCategory(note, note, txType),
                    accountId = inferAccount(note, note, accounts),
                    date = parseDateTime(dateTimeStr),
                    balance = balVal
                ))
                Log.d(TAG, "  → Record: amt=$signedAmt desc=\"$note\" bal=$balVal date=${ctx.year}-${ctx.month}-${ctx.day} time=$timePart")

                // 重置累积器
                accumType = ""
                accumDesc.clear()
            }
        }

        // ── v13 后处理：余额向前传播 ──
        // BOC等格式中，余额行和交易行分离；将余额行的余额赋给前一交易记录
        Log.d(TAG, "=== 主循环: ${rowsWithAmount}行有金额, ${rowsWithBalance}行有余额, 共${rows.size}视觉行, ${records.size}条记录 ===")
        var propagatedCount = 0
        for ((rowIdx, row) in rows.withIndex()) {
            // 找余额元素：先找"余额"标签，再找最右的¥数字
            val rowBalEl = row.firstOrNull { isBalanceElement(it.text) }
                ?: row.filter { el ->
                    val t = el.text.trim()
                    YEN_INLINE.containsMatchIn(t) && !isDateElement(t) && !isTimeElement(t)
                }.maxByOrNull { it.centerX }
                ?: continue
            val balVal = extractBalanceNumber(rowBalEl.text)
            if (balVal.isBlank()) continue

            // 跳过已存在于某条记录中的余额值
            if (records.any { it.balance == balVal }) continue

            // 向前搜索最近的无余额交易记录
            for (j in records.indices.reversed()) {
                if (records[j].balance.isBlank()) {
                    records[j] = records[j].copy(balance = balVal)
                    propagatedCount++
                    Log.d(TAG, "  v13 余额传播: Row[$rowIdx] bal=$balVal → Record[$j] \"${records[j].note}\"")
                    break
                }
            }
        }
        if (propagatedCount > 0) {
            Log.d(TAG, "=== 余额传播: $propagatedCount 条余额已分配到交易记录 ===")
        }

        Log.d(TAG, "=== 视觉解析: ${records.size} 条记录 ===")
        return validateAndCorrectFromBalance(records)
    }

    // ════════════════════════════════════════════════════════════════════
    // v10 新增：视觉行日期上下文构建（前向+后向）
    // ════════════════════════════════════════════════════════════════════
    private fun buildVisualDateMap(rows: List<List<OcrElement>>): Map<Int, DateCtx> {
        val map = mutableMapOf<Int, DateCtx>()
        var year = Calendar.getInstance().get(Calendar.YEAR).toString()
        var month = ""
        var day = ""

        // === 前向扫描 ===
        for ((rowIdx, row) in rows.withIndex()) {
            val dateEl = row.firstOrNull { isDateElement(it.text) }
            if (dateEl != null) {
                val d = extractFullDateFromText(dateEl.text, year)
                if (d != null) { year = d.year; month = d.month; day = d.day }
            }
            // 年月头处理
            val ymHead = row.firstOrNull { YM_HEADER.containsMatchIn(it.text.trim()) }
            if (ymHead != null) {
                YM_HEADER.find(ymHead.text.trim())?.let { m ->
                    year = m.groupValues[1]; month = m.groupValues[2].padStart(2, '0'); day = ""
                }
            }
            // 独立月份
            val monOnly = row.firstOrNull { MONTH_ONLY.matches(it.text.trim()) }
            if (monOnly != null) {
                MONTH_ONLY.find(monOnly.text.trim())?.let { m ->
                    month = m.groupValues[1].padStart(2, '0'); day = ""
                }
            }
            map[rowIdx] = DateCtx(year, month, day)
        }

        // === 后向填充 ===
        var nextComplete: DateCtx? = null
        for (rowIdx in rows.indices.reversed()) {
            val cur = map[rowIdx] ?: continue
            if (cur.day.isNotBlank() && cur.month.isNotBlank()) {
                nextComplete = cur
            } else if (nextComplete != null) {
                map[rowIdx] = if (cur.month.isNotBlank()) {
                    cur.copy(day = nextComplete.day)
                } else {
                    nextComplete
                }
            }
        }

        // 调试
        map.entries.take(15).forEach { (idx, ctx) ->
            Log.d(TAG, "  DateMap[$idx]: ${ctx.year}-${ctx.month}-${ctx.day}")
        }
        return map
    }

    // ════════════════════════════════════════════════════════════════════
    // v10 新增：视觉行时间上下文构建（前向+后向）
    // ════════════════════════════════════════════════════════════════════
    private fun buildVisualTimeMap(rows: List<List<OcrElement>>): Map<Int, String> {
        val map = mutableMapOf<Int, String>()
        var currentTime = ""

        // 前向传播
        for ((rowIdx, row) in rows.withIndex()) {
            val timeEl = row.firstOrNull { isTimeElement(it.text) }
            if (timeEl != null) { currentTime = timeEl.text.trim() }
            map[rowIdx] = currentTime
        }

        // 后向传播（时间可能在交易行下方）
        var nextTime: String? = null
        for (rowIdx in rows.indices.reversed()) {
            val t = map[rowIdx] ?: ""
            if (t.isNotEmpty()) { nextTime = t }
            else if (nextTime != null) { map[rowIdx] = nextTime!! }
        }

        map.entries.filter { it.value.isNotEmpty() }.take(10).forEach { (idx, t) ->
            Log.d(TAG, "  TimeMap[$idx]: $t")
        }
        return map
    }

    // ════════════════════════════════════════════════════════════════════
    // Phase 2 辅助：Y坐标分组
    // ════════════════════════════════════════════════════════════════════
    private var yGroupThreshold: Float = 10f

    private fun groupByY(elements: List<OcrElement>): MutableList<MutableList<OcrElement>> {
        // 自适应阈值：中位高度 × 0.75，下限提升至 10px（减少碎片化）
        val heights = elements.map { it.height }.sorted()
        val medianH = if (heights.isNotEmpty()) heights[heights.size / 2] else 20
        yGroupThreshold = maxOf(medianH * 0.75f, 10f)
        Log.d(TAG, "  Y阈值: medianH=$medianH → threshold=$yGroupThreshold")

        val sortedByY = elements.sortedBy { it.centerY }
        val rows = mutableListOf<MutableList<OcrElement>>()

        for (e in sortedByY) {
            val lastRow = rows.lastOrNull()
            if (lastRow != null) {
                val rowMedY = lastRow.map { it.centerY }.sorted().let {
                    it[it.size / 2]
                }
                if (e.centerY - rowMedY <= yGroupThreshold) {
                    lastRow.add(e)
                    continue
                }
            }
            rows.add(mutableListOf(e))
        }
        return rows
    }

    // ════════════════════════════════════════════════════════════════════
    // Phase 2 辅助：元素分类
    // ════════════════════════════════════════════════════════════════════

    private fun isDateElement(text: String): Boolean {
        val t = text.trim()
        return FULLDATE_RE.matches(t) ||
               SHORT_DATE.find(t)?.let { m ->
                    val mon = m.groupValues[1].toIntOrNull() ?: 0
                    val day = m.groupValues[2].toIntOrNull() ?: 0
                    mon in 1..12 && day in 1..31
               } == true ||
               CN_MD.matches(t) ||
               CN_FULLDATE.matches(t) ||
               DATE_WITH_WK.containsMatchIn(t) ||
               DAYWK_RE.matches(t) ||
               (t.matches(Regex("""^\d{1,2}$""")) && t.toIntOrNull() in 1..31)
    }

    private fun isTimeElement(text: String): Boolean {
        val t = text.trim()
        return TIME_ELEMENT.matches(t)
    }

    private fun isAmountElement(text: String): Boolean {
        val t = text.trim()
        // ★ 排除纯时间格式
        if (TIME_ELEMENT.matches(t)) return false
        return ANCHOR_RMB.containsMatchIn(t) ||
               YEN_SIGN_AFTER.matches(t) ||
               YEN_SIGN_BEFORE.matches(t) ||
               (YEN_INLINE.containsMatchIn(t) && !IS_BALANCE.containsMatchIn(t)) ||
               SIGNED_NUM.matches(t) ||
               (COMMA_NUM.matches(t) && t.contains(",") && !IS_BALANCE.containsMatchIn(t))
    }

    private fun isBalanceElement(text: String): Boolean {
        return IS_BALANCE.containsMatchIn(text.trim())
    }

    /**
     * v13: 在同行内搜索余额候选值（位于金额元素右侧的¥或纯数字）
     * 用于银行截屏中没有"余额"标签但余额列存在¥前缀数字的情况
     */
    private fun findBalanceInRow(row: List<OcrElement>, amtEl: OcrElement): OcrElement? {
        return row.filter { el ->
            el !== amtEl && el.centerX > amtEl.centerX
        }.filter { el ->
            val t = el.text.trim()
            // ¥前缀数字 或 纯长数字（至少4位，排除日期/时间）
            YEN_INLINE.containsMatchIn(t) ||
            (t.length >= 4 && t.replace(Regex("[^0-9.,]"), "").replace(",", "").toDoubleOrNull() != null &&
             t.replace(Regex("[^0-9.]"), "").length >= 4 &&
             !isDateElement(t) && !isTimeElement(t))
        }.maxByOrNull { it.centerX }  // 取最右的
    }

    private fun isTypeKeywordOnly(text: String): Boolean {
        val t = text.trim()
        return TX_TYPE_PATTERNS.any { t == it }
    }

    private fun isNoiseText(text: String): Boolean {
        val t = text.trim()
        return t.length <= 1 ||
               NOISE_WORDS.any { t == it || t.startsWith(it) } ||
               SUMMARY_RE.containsMatchIn(t) ||
               CARD_RE.containsMatchIn(t) ||
               ARROW_NOISE.containsMatchIn(t) ||
               MONTH_ONLY.matches(t) ||
               YM_HEADER.matches(t) ||
               TIME_ELEMENT.matches(t)  // ★ 时间不算描述文本
    }

    // ════════════════════════════════════════════════════════════════════
    // Phase 2 辅助：数值提取
    // ════════════════════════════════════════════════════════════════════

    private fun extractFullDateFromText(text: String, fallbackYear: String): DateCtx? {
        val t = text.trim()

        FULLDATE_RE.find(t)?.let { m ->
            return DateCtx(m.groupValues[1], m.groupValues[2].padStart(2, '0'), m.groupValues[3].padStart(2, '0'))
        }
        DATE_WITH_WK.find(t)?.let { m ->
            return DateCtx(m.groupValues[1], m.groupValues[2].padStart(2, '0'), m.groupValues[3].padStart(2, '0'))
        }
        CN_FULLDATE.find(t)?.let { m ->
            return DateCtx(m.groupValues[1], m.groupValues[2].padStart(2, '0'), m.groupValues[3].padStart(2, '0'))
        }
        CN_MD.find(t)?.let { m ->
            return DateCtx(fallbackYear, m.groupValues[1].padStart(2, '0'), m.groupValues[2].padStart(2, '0'))
        }
        SHORT_DATE.find(t)?.let { m ->
            val mon = m.groupValues[1].toIntOrNull() ?: 0
            val dayVal = m.groupValues[2].toIntOrNull() ?: 0
            if (mon in 1..12 && dayVal in 1..31) {
                return DateCtx(fallbackYear, m.groupValues[1].padStart(2, '0'), m.groupValues[2].padStart(2, '0'))
            }
        }
        DAYWK_RE.find(t)?.let { m ->
            val d = m.groupValues[1].padStart(2, '0')
            return DateCtx(fallbackYear, "", d)
        }
        if (t.matches(Regex("""^\d{1,2}$"""))) {
            val n = t.toIntOrNull() ?: 0
            if (n in 1..31) {
                return DateCtx(fallbackYear, "", t.padStart(2, '0'))
            }
        }
        return null
    }

    private fun parseSignedAmountFromElement(text: String): Double {
        val t = text.trim()

        ANCHOR_RMB.find(t)?.let { m ->
            val raw = parseAmount(m.groupValues[1])
            return if (raw != 0.0) raw else 0.0
        }
        YEN_SIGN_AFTER.find(t)?.let { m ->
            val raw = parseAmount(m.groupValues[2])
            return if (m.groupValues[1] == "-") -raw else raw
        }
        YEN_SIGN_BEFORE.find(t)?.let { m ->
            val raw = parseAmount(m.groupValues[2])
            return if (m.groupValues[1] == "-") -raw else raw
        }
        YEN_INLINE.find(t)?.let { m ->
            val raw = parseAmount(m.groupValues[2])
            val signAfter = m.groupValues[1]
            val yPos = t.indexOf('¥').takeIf { it >= 0 } ?: t.indexOf('￥')
            val signBefore = if (yPos != null && yPos > 0) {
                val ch = t[yPos - 1]; if (ch == '+' || ch == '-') ch.toString() else ""
            } else ""
            val effSign = signBefore.ifBlank { signAfter }
            return if (effSign == "-") -raw else raw
        }
        SIGNED_NUM.find(t)?.let { m ->
            val raw = parseAmount(m.groupValues[2])
            return if (m.groupValues[1] == "-") -raw else raw
        }
        val pure = parseAmount(t)
        if (pure != 0.0) return pure
        return 0.0
    }

    // ★ v12：仅做无小数点恢复，有小数点的金额由余额链式校正
    private fun parseSignedAmountWithRecovery(text: String): Double {
        val std = parseSignedAmountFromElement(text)
        val absAmt = kotlin.math.abs(std)
        val t = text.trim()

        // 有小数点 → 信任OCR原始值，后续由余额链校正
        if (t.contains('.') || t.contains('。')) return std

        // 无小数点 → 尝试恢复
        if (absAmt == 0.0) return std

        // 银行截屏金额始终有2位小数，无小数点说明OCR丢失了
        // 条件：金额>5000 且 纯数字末尾2位为00 → 除以100恢复
        val numericOnly = t.replace(Regex("[^0-9]"), "")
        if (absAmt > 5000 && numericOnly.endsWith("00") && numericOnly.length >= 4) {
            val recovered = std / 100.0
            if (kotlin.math.abs(recovered) < 500_000) {
                Log.d(TAG, "  ★ 小数点恢复: \"$t\" ($std) → $recovered")
                return recovered
            }
        }

        if (absAmt > 1_000_000) {
            val recovered = std / 100.0
            if (kotlin.math.abs(recovered) < 500_000) {
                Log.d(TAG, "  ★ 激进恢复: \"$t\" ($std) → $recovered")
                return recovered
            }
        }

        return std
    }

    private fun extractBalanceNumber(text: String): String {
        BALANCE_VAL.find(text)?.let { m -> return m.groupValues[1].replace(",", "") }
        val numRe = Regex("""([\d,]+\\.?\d*)""")
        val matches = numRe.findAll(text).toList()
        return if (matches.isNotEmpty()) matches.last().groupValues[1].replace(",", "") else ""
    }

    /**
     * v13.1: 从备注文本中提取余额值
     * BOC等格式中，余额栏被OCR合并到备注/金额文本中（非独立元素）
     * 常见模式："生活费 余额69.52"、"¥693.28 余额353.20"、"余关15.000"（OCR噪声）
     */
    private fun extractBalanceFromNote(note: String, amount: String): String {
        val cleanAmt = amount.replace(",", "")

        // Pattern 1: "余额" 后跟数字
        val balLabel = Regex("""余额\s*([\d,]+\\.?\d*)""")
        balLabel.find(note)?.let { m ->
            val v = m.groupValues[1].replace(",", "")
            if (v.toDoubleOrNull() != null) return v
        }

        // Pattern 2: "系额"（"余额"的OCR噪声）后跟数字
        val ocrNoise1 = Regex("""系额\s*([\d,]+\\.?\d*)""")
        ocrNoise1.find(note)?.let { m ->
            val v = m.groupValues[1].replace(",", "")
            if (v.toDoubleOrNull() != null) return v
        }

        // Pattern 3: "余关"、"余酒"等OCR噪声（"余" + 非"额"字符）
        val ocrNoise2 = Regex("""余([^额\s])\s*([\d,]+\\.?\d+)""")
        ocrNoise2.find(note)?.let { m ->
            val v = m.groupValues[2].replace(",", "")
            if (v.toDoubleOrNull() != null && v != cleanAmt) return v
        }

        // Pattern 4: ¥数字（取与金额不同的，最右的那个）
        val yenNums = Regex("""¥\s*([\d,]+\\.?\d*)""").findAll(note)
            .map { it.groupValues[1].replace(",", "") }
            .filter { it != cleanAmt && it.toDoubleOrNull() != null }
            .toList()
        if (yenNums.size >= 1 && yenNums.last() != cleanAmt) {
            return yenNums.last()
        }

        // Pattern 5: 备注中有多个数字，取最后一个（最可能是余额）
        val allNums = Regex("""([\d,]+\\.?\d+)""").findAll(note)
            .map { it.groupValues[1].replace(",", "") }
            .filter { it != cleanAmt && it.toDoubleOrNull() != null && it.toDoubleOrNull()!! > 0.01 }
            .toList()
        if (allNums.size >= 2) {
            return allNums.last()
        }

        return ""
    }

    // ════════════════════════════════════════════════════════════════════
    // 余额链式校验 + 金额校正 (v13)
    // 核心原理：余额是权威数据，|当前余额 - 上一笔余额| = 真实交易额
    // 如果OCR金额与余额推算偏差 > 3%，用余额推算值校正
    // 这是数学确定的，适用于任何银行/任何格式，无需启发式猜测
    // ════════════════════════════════════════════════════════════════════
    private fun validateAndCorrectFromBalance(records: List<ParsedRecord>): List<ParsedRecord> {
        // v13.1: 从备注中二次提取余额（BOC格式余额与说明合并在同一OCR元素）
        var enrichedCount = 0
        val enriched = records.map { r ->
            if (r.balance.isNotBlank() && r.balance.toDoubleOrNull() != null) {
                r
            } else {
                val extracted = extractBalanceFromNote(r.note, r.amount)
                if (extracted.isNotBlank() && extracted.toDoubleOrNull() != null) {
                    enrichedCount++
                    Log.d(TAG, "  v13.1 备注提取余额: note=${r.note.take(40)} -> bal=$extracted")
                    r.copy(balance = extracted)
                } else r
            }
        }
        if (enrichedCount > 0) {
            Log.d(TAG, "=== 备注余额提取: $enrichedCount 条从备注中提取到余额值 ===")
        }

        val corrected = enriched.toMutableList()
        var correctedCount = 0
        var validatedCount = 0

        val withBal = corrected.count { it.balance.isNotBlank() && it.balance.toDoubleOrNull() != null }
        val withAmt = corrected.count { it.amount.toDoubleOrNull()?.let { a -> a > 0 } == true }
        Log.d(TAG, "=== 余额校验入口: ${corrected.size}条, ${withBal}条有余额, ${withAmt}条有金额 ===")
        if (withBal < 2) {
            Log.d(TAG, "=== 余额不足2条，跳过校正 ===")
            return records.map { r ->
                if (r.balance.isBlank() || r.balance.toDoubleOrNull() == null)
                    r.copy(balanceValid = BalanceValidation.NO_BALANCE)
                else
                    r.copy(balanceValid = BalanceValidation.NO_BALANCE)
            }
        }

        // 前向扫描：逐对比较余额链
        for (i in 1 until records.size) {
            val prev = records[i - 1]
            val curr = records[i]
            val prevBal = prev.balance.toDoubleOrNull() ?: continue
            val currBal = curr.balance.toDoubleOrNull() ?: continue

            val balanceDelta = kotlin.math.abs(currBal - prevBal)
            val ocrAmt = curr.amount.toDoubleOrNull() ?: continue

            // 余额变化极小 → 跳过（零金额或重复行）
            if (balanceDelta < 0.01) {
                if (ocrAmt < 0.01) {
                    corrected[i] = curr.copy(balanceValid = BalanceValidation.VALID)
                    validatedCount++
                }
                continue
            }

            // 从余额推算方向
            val isExpense = currBal < prevBal
            val correctedType = if (isExpense) TransactionType.EXPENSE else TransactionType.INCOME

            // 计算相对偏差
            val relDiff = if (ocrAmt > 0.01) {
                kotlin.math.abs(balanceDelta - ocrAmt) / ocrAmt
            } else {
                1.0 // OCR金额≈0，肯定需校正
            }

            if (relDiff > 0.03 && balanceDelta < 500_000) {
                // OCR金额不可信 → 用余额推算值替换
                corrected[i] = curr.copy(
                    amount = String.format("%.2f", balanceDelta),
                    type = correctedType,
                    balanceValid = BalanceValidation.CORRECTED
                )
                correctedCount++
                Log.d(TAG, "  ★ 余额校正[#$i]: OCR=¥$ocrAmt → 余额推算=¥$balanceDelta (偏差${(relDiff * 100).toInt()}%)")
            } else if (relDiff <= 0.03) {
                // OCR金额可信
                corrected[i] = curr.copy(balanceValid = BalanceValidation.VALID)
                validatedCount++
            } else {
                // 余额推算金额超限，不校正
                corrected[i] = curr.copy(balanceValid = BalanceValidation.MISMATCH)
            }
        }

        // 标记无余额的 + 第一条有余额但无上一笔的
        return corrected.mapIndexed { idx, r ->
            if (r.balanceValid != BalanceValidation.NO_BALANCE) {
                r // 已在上面处理
            } else if (r.balance.isBlank() || r.balance.toDoubleOrNull() == null) {
                r.copy(balanceValid = BalanceValidation.NO_BALANCE)
            } else {
                // 有余额但未被处理（第一条记录或孤立的余额行）
                r.copy(balanceValid = BalanceValidation.NO_BALANCE)
            }
        }.also {
            Log.d(TAG, "=== 余额校验: $validatedCount 条通过, $correctedCount 条已校正, ${it.count { r -> r.balanceValid == BalanceValidation.MISMATCH }} 条异常 ===")
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // 锚点扫描（v8 纯文本，保留）
    // ════════════════════════════════════════════════════════════════════
    private fun parseAnchors(
        lines: List<String>, accounts: List<Account>, dateMap: Map<Int, DateCtx>
    ): List<ParsedRecord> {
        val records = mutableListOf<ParsedRecord>()
        var pendingType: String? = null
        var pendingTypeIdx: Int = -1

        for ((idx, line) in lines.withIndex()) {
            if (isNoise(line)) continue

            CCB_TYPE.find(line)?.let { m ->
                if (ANCHOR_RMB.find(line) == null && !line.contains("¥")) {
                    pendingType = m.groupValues[1]; pendingTypeIdx = idx
                }
            }
            if (pendingTypeIdx == idx && pendingType != null && !line.contains("¥")) continue

            val rmb = ANCHOR_RMB.find(line)
            if (rmb != null) {
                val amt = parseAmount(rmb.groupValues[1])
                if (amt != 0.0 && kotlin.math.abs(amt) < 10_000_000) {
                    makeRecord(records, lines, accounts, dateMap, idx,
                        kotlin.math.abs(amt), amt, pendingType, pendingTypeIdx, "人民币元")
                    pendingType = null; continue
                }
            }

            if (IS_BALANCE.containsMatchIn(line)) continue

            val ya = YEN_SIGN_AFTER.find(line)
            val yb = YEN_SIGN_BEFORE.find(line)
            if (ya != null || yb != null) {
                val sign: String; val rawAmt: Double
                if (ya != null) { sign = ya.groupValues[1]; rawAmt = parseAmount(ya.groupValues[2]) }
                else { sign = yb!!.groupValues[1]; rawAmt = parseAmount(yb.groupValues[2]) }
                if (rawAmt > 0 && rawAmt < 10_000_000) {
                    val signed = if (sign == "-") -rawAmt else rawAmt
                    makeRecord(records, lines, accounts, dateMap, idx,
                        kotlin.math.abs(signed), signed, pendingType, pendingTypeIdx, "¥行")
                    pendingType = null; continue
                }
            }

            if (!SUMMARY_RE.containsMatchIn(line)) {
                val yi = YEN_INLINE.find(line)
                if (yi != null) {
                    val yPos = line.indexOf('¥').takeIf { it >= 0 } ?: line.indexOf('￥')
                    val signAfter = yi.groupValues[1]
                    val signBefore = if (yPos >= 0 && yPos > 0) {
                        val ch = line[yPos - 1]; if (ch == '+' || ch == '-') ch.toString() else ""
                    } else ""
                    val effSign = signBefore.ifBlank { signAfter }
                    val rawAmt = parseAmount(yi.groupValues[2])
                    if (rawAmt > 0 && rawAmt < 10_000_000) {
                        val signed = if (effSign == "-") -rawAmt else rawAmt
                        makeRecord(records, lines, accounts, dateMap, idx,
                            kotlin.math.abs(signed), signed, pendingType, pendingTypeIdx, "¥内联")
                        pendingType = null; continue
                    }
                }
            }

            val sn = SIGNED_NUM.find(line)
            if (sn != null) {
                val rawAmt = parseAmount(sn.groupValues[2])
                if (rawAmt > 0 && rawAmt < 10_000_000) {
                    val signed = if (sn.groupValues[1] == "-") -rawAmt else rawAmt
                    makeRecord(records, lines, accounts, dateMap, idx,
                        kotlin.math.abs(signed), signed, pendingType, pendingTypeIdx, "±金额")
                    pendingType = null; continue
                }
            }
        }
        return records
    }

    private fun makeRecord(
        records: MutableList<ParsedRecord>,
        lines: List<String>, accounts: List<Account>,
        dateMap: Map<Int, DateCtx>,
        idx: Int, absAmt: Double, signedAmt: Double,
        pendingType: String?, pendingTypeIdx: Int,
        tag: String
    ) {
        val desc = findDesc(lines, idx)
        val remark = findRemark(lines, idx)
        val balance = findBalance(lines, idx)
        val ctx = dateMap[idx] ?: DateCtx("", "", "")
        val dateStr = buildDate(ctx.year, ctx.month, ctx.day)
        val txType = inferType(lines[idx], desc, signedAmt)

        val effDesc = if (pendingType != null && idx - pendingTypeIdx in 1..3) pendingType else desc

        val cleanRemark = when {
            remark.isBlank() -> ""
            NOTE_EXCLUDE.contains(remark.trim()) -> ""
            NOTE_EXCLUDE_CONTAINS.any { remark.contains(it) } -> ""
            remark.length <= 1 -> ""
            remark.trim().matches(Regex("""^[→→→→\-\s]+$""")) -> ""
            TIME_ELEMENT.matches(remark.trim()) -> ""  // ★ 过滤时间
            else -> remark
        }

        val note = when {
            cleanRemark.isNotBlank() && cleanRemark != effDesc -> "$effDesc - $cleanRemark"
            else -> effDesc
        }

        records.add(ParsedRecord(
            amount = String.format("%.2f", absAmt),
            type = txType,
            note = note.take(80),
            category = inferCategory(lines[idx], note, txType),
            accountId = inferAccount(lines[idx], note, accounts),
            date = parseDateTime(dateStr),
            balance = balance
        ))
        Log.d(TAG, "  [$tag] signed=$signedAmt desc=$effDesc remark=$cleanRemark balance=$balance date=$dateStr")
    }

    // ════════════════════════════════════════════════════════════════════
    // 单行完整格式
    // ════════════════════════════════════════════════════════════════════
    private fun parseSingleLine(lines: List<String>, accounts: List<Account>): List<ParsedRecord> {
        val records = mutableListOf<ParsedRecord>()
        val fmtICBC = Regex(
            """^(.+?)\s+[¥￥]?\s*([+-])\s*[¥￥]?\s*([\d,]+\.?\d*)\s+(\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2}(?::\d{2})?)\s+余额\s+[\s¥￥]*([\d,]+\.?\d*)$"""
        )
        for (line in lines) {
            fmtICBC.find(line)?.let { m ->
                val raw = parseAmount(m.groupValues[3])
                if (raw == 0.0) return@let
                val signed = if (m.groupValues[2] == "-") -raw else raw
                records.add(ParsedRecord(
                    amount = String.format("%.2f", kotlin.math.abs(signed)),
                    type = inferType(line, m.groupValues[1].trim(), signed),
                    note = m.groupValues[1].trim().take(80),
                    category = inferCategory(line, m.groupValues[1].trim(), inferType(line, m.groupValues[1].trim(), signed)),
                    accountId = inferAccount(line, m.groupValues[1].trim(), accounts),
                    date = parseDateTime(m.groupValues[4]),
                    balance = m.groupValues[5]
                ))
            }
        }
        return records
    }

    // ════════════════════════════════════════════════════════════════════
    // 上下文辅助
    // ════════════════════════════════════════════════════════════════════

    private fun findDesc(lines: List<String>, anchorIdx: Int): String {
        val start = (anchorIdx - 25).coerceAtLeast(0)
        for (i in (anchorIdx - 1) downTo start) {
            val line = cleanLine(lines[i])
            if (line.isBlank() || isNoise(line) || isDateOrNum(line)) continue
            if (IS_BALANCE.containsMatchIn(line)) continue
            if (CARD_RE.containsMatchIn(line)) continue
            TX_TYPE_PATTERNS.firstOrNull { line == it || line.startsWith(it) }?.let { return line.take(60) }
        }
        for (i in (anchorIdx - 1) downTo start) {
            val line = cleanLine(lines[i])
            if (line.isBlank() || isNoise(line) || isDateOrNum(line)) continue
            if (IS_BALANCE.containsMatchIn(line)) continue
            if (CARD_RE.containsMatchIn(line)) continue
            if (ANCHOR_RMB.containsMatchIn(line) || YEN_INLINE.containsMatchIn(line)) continue
            if (YEN_SIGN_AFTER.containsMatchIn(line) || YEN_SIGN_BEFORE.containsMatchIn(line)) continue
            if (SIGNED_NUM.containsMatchIn(line)) continue
            if (TIME_ELEMENT.matches(line)) continue  // ★ 跳过时间
            if (line.length < 2) continue
            return line.take(60)
        }
        return "银行交易"
    }

    private fun findRemark(lines: List<String>, anchorIdx: Int): String {
        for (i in (anchorIdx + 1)..(anchorIdx + 6).coerceAtMost(lines.lastIndex)) {
            val line = cleanLine(lines[i])
            if (line.isBlank()) continue
            if (isNoise(line)) continue
            if (IS_BALANCE.containsMatchIn(line)) continue
            if (isDateOrNum(line)) continue
            if (CCB_TYPE.containsMatchIn(line)) continue
            if (NOTE_EXCLUDE.contains(line.trim())) continue
            if (NOTE_EXCLUDE_CONTAINS.any { line.contains(it) }) continue
            if (ARROW_NOISE.containsMatchIn(line)) continue
            if (ANCHOR_RMB.containsMatchIn(line)) continue
            if (YEN_INLINE.containsMatchIn(line) && !IS_BALANCE.containsMatchIn(line)) continue
            if (TIME_ELEMENT.matches(line)) continue  // ★ 跳过时间
            if (line.contains("人民币") || line.contains("人民市")) continue
            if (line.length in 2..80) return line.take(60)
        }
        for (i in (anchorIdx - 1) downTo (anchorIdx - 10).coerceAtLeast(0)) {
            val line = cleanLine(lines[i])
            if (line.isBlank()) continue
            if (isNoise(line)) continue
            if (IS_BALANCE.containsMatchIn(line)) continue
            if (isDateOrNum(line)) continue
            if (CCB_TYPE.containsMatchIn(line)) continue
            if (TX_TYPE_PATTERNS.any { line == it || line.startsWith(it) }) continue
            if (NOTE_EXCLUDE.contains(line.trim())) continue
            if (NOTE_EXCLUDE_CONTAINS.any { line.contains(it) }) continue
            if (ARROW_NOISE.containsMatchIn(line)) continue
            if (TIME_ELEMENT.matches(line)) continue  // ★ 跳过时间
            if (line.contains("¥") || line.contains("人民币") || line.contains("人民市")) continue
            if (line.length in 2..60) return line.take(60)
        }
        return ""
    }

    private fun findBalance(lines: List<String>, anchorIdx: Int): String {
        for (i in (anchorIdx + 1)..(anchorIdx + 3).coerceAtMost(lines.lastIndex)) {
            BALANCE_VAL.find(lines[i])?.let { return it.groupValues[1] }
        }
        for (i in (anchorIdx - 1) downTo (anchorIdx - 5).coerceAtLeast(0)) {
            BALANCE_VAL.find(lines[i])?.let { return it.groupValues[1] }
        }
        for (i in (anchorIdx - 6) downTo (anchorIdx - 15).coerceAtLeast(0)) {
            BALANCE_VAL.find(lines[i])?.let { m ->
                var conflict = false
                for (j in (i + 1) until anchorIdx) {
                    if (ANCHOR_RMB.containsMatchIn(lines[j]) ||
                        (YEN_INLINE.containsMatchIn(lines[j]) && !IS_BALANCE.containsMatchIn(lines[j]))) {
                        conflict = true; break
                    }
                }
                if (!conflict) return m.groupValues[1]
            }
        }
        return ""
    }

    // ════════════════════════════════════════════════════════════════════
    // 工具
    // ════════════════════════════════════════════════════════════════════

    private fun cleanLine(line: String): String =
        line.replace(Regex("^[|/\\\\]+\\s*"), "").replace(Regex("\\s*[|/\\\\]+$"), "")
            .replace(Regex("\\s+/\\s*$"), "").trim()

    private fun isNoise(line: String): Boolean {
        val t = line.trim()
        if (t.length <= 1) return true
        if (NOISE_WORDS.any { t == it || t.startsWith(it) }) return true
        if (SUMMARY_RE.containsMatchIn(t)) return true
        if (MONTH_RE.containsMatchIn(t) && t.length <= 12) return true
        if (MONTH_ONLY.containsMatchIn(t)) return true
        if (YM_HEADER.containsMatchIn(t) && t.length <= 14) return true
        if (TIME_ELEMENT.matches(t)) return true  // ★ 时间不算有效交易行
        if (t.matches(Regex("""^[|\\\-_=+*/\\\\]+$"""))) return true
        if (t.matches(Regex("""^\d{4}[./]\d{1,2}$"""))) return true
        if (ARROW_NOISE.containsMatchIn(t) && t.length <= 8) return true
        return false
    }

    private fun isDateOrNum(line: String): Boolean {
        val t = line.trim()
        if (t.matches(Regex("""^[\d,.:\s/\-]+$"""))) return true
        if (DAYWK_RE.containsMatchIn(t) && t.length <= 8) return true
        if (DATETIME_RE.containsMatchIn(t) && t.length <= 20) return true
        if (FULLDATE_RE.containsMatchIn(t) && t.length <= 12) return true
        if (DATE_WITH_WK.containsMatchIn(t) && t.length <= 18) return true
        if (SHORT_DATE.containsMatchIn(t) && t.length <= 6) return true
        if (TIME_ELEMENT.matches(t)) return true  // ★ 时间也算日期类
        return false
    }

    private fun buildDate(year: String, month: String, day: String): String {
        val y = year.ifBlank { Calendar.getInstance().get(Calendar.YEAR).toString() }
        val m = month.ifBlank { "01" }
        val d = day.ifBlank { "01" }
        return "$y-$m-$d 00:00:00"
    }

    private fun parseAmount(str: String): Double {
        val cleaned = str.replace(Regex("[^0-9.+-]"), "")
        return runCatching { cleaned.toDouble() }.getOrDefault(0.0)
    }

    private fun parseDateTime(str: String): Long {
        val formats = listOf(
            "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd",
            "yyyy/MM/dd HH:mm:ss", "yyyy/MM/dd",
            "yyyy.MM.dd HH:mm:ss", "yyyy.MM.dd", "yyyy-MM"
        )
        for (fmt in formats) {
            try {
                val sdf = java.text.SimpleDateFormat(fmt, Locale.getDefault())
                sdf.isLenient = false
                return sdf.parse(str)?.time ?: continue
            } catch (_: Exception) { }
        }
        return System.currentTimeMillis()
    }

    private fun inferType(fullText: String, note: String, amount: Double): TransactionType {
        val text = "$fullText $note"
        if (amount < 0) return TransactionType.EXPENSE
        if (amount > 0 && !EXPENSE_KW.any { text.contains(it) }) return TransactionType.INCOME
        val inc = INCOME_KW.count { text.contains(it) }
        val exp = EXPENSE_KW.count { text.contains(it) }
        return if (inc > exp) TransactionType.INCOME else TransactionType.EXPENSE
    }

    private fun inferCategory(text: String, note: String, type: TransactionType): Category? {
        val ctx = "$text $note"
        if (type == TransactionType.INCOME) {
            return when {
                ctx.containsAny("工资", "薪资", "代发") -> Category.fromId("salary", TransactionType.INCOME)
                ctx.containsAny("奖金", "绩效") -> Category.fromId("bonus", TransactionType.INCOME)
                ctx.containsAny("分红", "股息", "结息", "利息") -> Category.fromId("investment", TransactionType.INCOME)
                ctx.containsAny("退款", "退还") -> Category.fromId("refund", TransactionType.INCOME)
                ctx.containsAny("报销") -> Category.fromId("reimbursement", TransactionType.INCOME)
                ctx.containsAny("红包") -> Category.fromId("redpacket", TransactionType.INCOME)
                ctx.containsAny("转入", "汇入", "存入", "转账存入") -> Category.fromId("transfer", TransactionType.INCOME)
                else -> null
            }
        }
        return when {
            ctx.containsAny("餐", "饭", "食", "外卖", "美团", "饿了么",
                "肯德基", "麦当劳", "海底捞", "奶茶", "咖啡", "瑞幸",
                "星巴克", "烧烤", "火锅", "小吃", "早餐", "午餐", "晚餐",
                "食堂", "饮料", "甜品", "蛋糕", "米线", "面馆",
                "吴记", "花生", "喜茶") ->
                Category.fromId("food", TransactionType.EXPENSE)
            ctx.containsAny("打车", "滴滴", "地铁", "公交", "高铁", "机票",
                "加油", "停车", "高速", "出租", "通行宝", "石油", "昆仑",
                "铁路", "12306", "航空", "ETC", "充电桩") ->
                Category.fromId("transport", TransactionType.EXPENSE)
            ctx.containsAny("购物", "京东", "淘宝", "天猫", "超市", "商场",
                "拼多多", "唯品会", "苏宁", "沃尔玛", "永辉",
                "全家", "711", "罗森", "水果", "盒马", "零食", "百货") ->
                Category.fromId("shopping", TransactionType.EXPENSE)
            ctx.containsAny("水费", "电费", "燃气", "天然气", "煤气", "暖气",
                "宽带", "网费", "物业费", "供暖", "电力", "国网") ->
                Category.fromId("utilities", TransactionType.EXPENSE)
            ctx.containsAny("娱乐", "电影", "游戏", "视频", "音乐", "KTV",
                "网吧", "直播", "会员") ->
                Category.fromId("entertainment", TransactionType.EXPENSE)
            ctx.containsAny("医院", "药店", "医疗", "诊所", "挂号",
                "体检", "药房", "药品") ->
                Category.fromId("medical", TransactionType.EXPENSE)
            ctx.containsAny("酒店", "宾馆", "民宿", "旅馆", "住宿",
                "如家", "汉庭", "全季", "亚朵") ->
                Category.fromId("accommodation", TransactionType.EXPENSE)
            ctx.containsAny("房租", "物业", "租房") ->
                Category.fromId("housing", TransactionType.EXPENSE)
            ctx.containsAny("话费", "流量", "通讯", "中国移动", "中国联通",
                "中国电信", "手机费") ->
                Category.fromId("communication", TransactionType.EXPENSE)
            ctx.containsAny("学费", "培训", "教育", "课程", "网课") ->
                Category.fromId("education", TransactionType.EXPENSE)
            ctx.containsAny("保险", "保费", "社保", "医保", "车险", "人寿") ->
                Category.fromId("insurance", TransactionType.EXPENSE)
            ctx.containsAny("旅游", "景区", "门票", "携程", "去哪儿") ->
                Category.fromId("travel", TransactionType.EXPENSE)
            ctx.containsAny("还款", "白条", "花呗", "借呗", "京东金融", "信用卡") ->
                Category.fromId("debt", TransactionType.EXPENSE)
            else -> null
        }
    }

    private fun inferAccount(text: String, note: String?, accounts: List<Account>): Long? {
        if (accounts.isEmpty()) return null
        val ctx = if (note != null) "$text $note" else text

        Regex("尾号(\\d{4})").find(ctx)?.let { m ->
            accounts.find { it.cardNumber?.endsWith(m.groupValues[1]) == true || it.cardNumber == m.groupValues[1] }
                ?.let { return it.id }
        }
        Regex("\\d{4}\\s*\\*+\\s*\\d{3,4}").find(ctx)?.let { m ->
            accounts.find { it.cardNumber?.endsWith(m.value.takeLast(4)) == true }
                ?.let { return it.id }
        }
        return when {
            ctx.containsAny("微信支付", "微信", "零钱", "财付通") ->
                accounts.find { it.type == AccountType.WECHAT }?.id
            ctx.containsAny("支付宝", "余额宝", "花呗", "蚂蚁") ->
                accounts.find { it.type == AccountType.CONSUMPTION_PLATFORM }?.id
                    ?: accounts.find { it.type == AccountType.YUEBAO }?.id
            ctx.containsAny("银行", "储蓄卡", "借记卡", "信用卡", "工商",
                "建设", "农业", "中国银行", "招商", "交通", "邮储",
                "民生", "光大", "兴业", "浦发", "中信", "长城") -> {
                val banks = accounts.filter { it.type == AccountType.BANK }
                (banks.find { ctx.contains(it.name.take(4)) } ?: banks.firstOrNull())?.id
            }
            else -> accounts.firstOrNull()?.id
        }
    }

    private fun String.containsAny(vararg kw: String): Boolean = kw.any { this.contains(it) }

    // ════════════════════════════════════════════════════════════════════
    // 公开方法 — 供 QianfanOcrEngine 等外部引擎复用
    // ════════════════════════════════════════════════════════════════════

    fun inferCategoryPublic(text: String, note: String, type: TransactionType): Category? {
        return inferCategory(text, note, type)
    }

    fun inferAccountPublic(text: String, note: String, accounts: List<Account>): Long? {
        return inferAccount(text, note, accounts)
    }

    fun validatePublic(records: List<ParsedRecord>): List<ParsedRecord> {
        return validateAndCorrectFromBalance(records)
    }
}
