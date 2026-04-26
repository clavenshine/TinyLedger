package com.tinyledger.app.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.tinyledger.app.domain.model.Account
import com.tinyledger.app.domain.model.Category
import com.tinyledger.app.domain.model.TransactionType
import com.tinyledger.app.ui.screens.budget.BalanceValidation
import com.tinyledger.app.ui.screens.budget.ParsedRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * 百度千帆 OCR 大模型引擎 — 截屏识别最优先选项
 *
 * 通过千帆 v2/chat/completions API 调用 qianfan-ocr 多模态大模型，
 * 直接识别银行交易截图并返回结构化 JSON 交易记录。
 *
 * 设计原则：
 *   - 所有异常静默处理，返回空列表 → 自动回退 PaddleOCR
 *   - 使用原始彩色截图（大模型对自然图像效果更好）
 *   - 支持 JSON 与 Markdown 代码块两种响应格式
 */
object QianfanOcrEngine {

    private const val TAG = "QianfanOcrEngine"
    private const val API_URL = "https://qianfan.baidubce.com/v2/chat/completions"
    private const val API_KEY = "bce-v3/ALTAK-B8vrkU5qjIScuPGVqMwLv/52762d7ad81ddda2ba76325bbe1d5c6719e05612"
    private const val MODEL = "qianfan-ocr"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // ─── JSON 媒体类型 ───
    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

    /**
     * 调用千帆 OCR 识别银行交易截图
     *
     * @param bitmap 原始彩色截图（不要送预处理后的灰度图！）
     * @param accounts 当前账户列表（用于账户匹配）
     * @return 识别到的交易记录列表；失败时返回空列表
     */
    suspend fun recognize(bitmap: Bitmap, accounts: List<Account>): List<ParsedRecord> {
        return try {
            withContext(Dispatchers.IO) {
                Log.d(TAG, "开始千帆 OCR 识别...")

                // 1. 图片转 base64 JPEG
                val base64 = bitmapToBase64(bitmap)

                // 2. 构建请求体
                val requestBody = buildRequestBody(base64)

                // 3. 发送 HTTP 请求
                val responseText = callApi(requestBody)
                    ?: return@withContext emptyList<ParsedRecord>()

                Log.d(TAG, "API 响应长度: ${responseText.length} 字符")
                Log.d(TAG, "API 响应前 300 字符: ${responseText.take(300)}")

                // 4. 解析响应 → 交易记录
                parseResponse(responseText, accounts)
            }
        } catch (e: Exception) {
            Log.e(TAG, "千帆 OCR 异常: ${e.message}", e)
            emptyList()
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // 图片编码
    // ════════════════════════════════════════════════════════════════════

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        // 压缩为 JPEG，质量 85%（平衡文件大小与清晰度）
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        val bytes = stream.toByteArray()
        Log.d(TAG, "图片编码: ${bitmap.width}x${bitmap.height}, JPEG ${bytes.size / 1024}KB")
        return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
    }

    // ════════════════════════════════════════════════════════════════════
    // 请求构建
    // ════════════════════════════════════════════════════════════════════

    private fun buildRequestBody(base64Image: String): String {
        // 系统提示词
        val systemPrompt = """你是一个专业的银行交易截图识别助手。你的任务是从银行APP截图中提取所有交易记录。

请严格遵守以下规则：
1. 逐笔列出截图中的所有交易记录（不要遗漏任何一笔）
2. 金额格式：支出为负数（如 -68.97），收入为正数（如 500.00）
3. 日期格式统一为 YYYY-MM-DD
4. 如果截图中有余额信息，也请一并提取
5. 如果无法确定某个字段，使用空字符串 ""

请严格按照以下 JSON 格式返回，不要添加任何额外文字或注释：

```json
{
  "transactions": [
    {
      "date": "2025-12-31",
      "time": "14:30:05",
      "amount": -9.00,
      "description": "财付通-微信支付-某某商户",
      "balance": 2706.07
    }
  ]
}
```"""

        val userPrompt = "请识别这张银行交易截图中的所有交易记录，按上述 JSON 格式返回。"

        // 构建 OpenAI 兼容的 multimodal 请求
        val json = JSONObject().apply {
            put("model", MODEL)

            put("messages", JSONArray().apply {
                // System message
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                // User message with image
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", JSONArray().apply {
                        put(JSONObject().apply {
                            put("type", "text")
                            put("text", userPrompt)
                        })
                        put(JSONObject().apply {
                            put("type", "image_url")
                            put("image_url", JSONObject().apply {
                                put("url", "data:image/jpeg;base64,$base64Image")
                            })
                        })
                    })
                })
            })

            put("temperature", 0.1)    // 低温，稳定输出
            put("max_tokens", 4096)
        }

        return json.toString()
    }

    // ════════════════════════════════════════════════════════════════════
    // API 调用
    // ════════════════════════════════════════════════════════════════════

    private suspend fun callApi(requestBody: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val body = requestBody.toRequestBody(JSON_MEDIA)
                val request = Request.Builder()
                    .url(API_URL)
                    .addHeader("Authorization", "Bearer $API_KEY")
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build()

                Log.d(TAG, "发送 API 请求...")
                val response = client.newCall(request).execute()

                val responseBody = response.body?.string() ?: ""
                Log.d(TAG, "HTTP ${response.code}: ${responseBody.length} 字符")

                if (!response.isSuccessful) {
                    Log.e(TAG, "API 请求失败: HTTP ${response.code}, body=${responseBody.take(200)}")
                    return@withContext null
                }

                responseBody
            } catch (e: Exception) {
                Log.e(TAG, "API 网络异常: ${e.message}", e)
                null
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // 响应解析
    // ════════════════════════════════════════════════════════════════════

    private fun parseResponse(responseText: String, accounts: List<Account>): List<ParsedRecord> {
        return try {
            // Step 1: 从 API 响应中提取 assistant 的 content
            val apiJson = JSONObject(responseText)
            val choices = apiJson.optJSONArray("choices")
                ?: return tryParseRawJson(responseText, accounts)

            val firstChoice = choices.optJSONObject(0)
                ?: return emptyList()

            val message = firstChoice.optJSONObject("message")
                ?: return emptyList()

            val content = message.optString("content", "")
            if (content.isBlank()) {
                Log.w(TAG, "API 返回空 content")
                return emptyList()
            }

            // Step 2: 从 content 中提取 JSON
            val jsonText = extractJson(content)
            if (jsonText.isBlank()) {
                Log.w(TAG, "无法从 content 中提取 JSON，尝试作为原始文本解析")
                // 回退：将整个 content 作为 OCR 文本送给文本解析器
                return ScreenshotTransactionParser.parse(content, accounts)
            }

            // Step 3: 解析交易 JSON
            parseTransactionsJson(jsonText, accounts)

        } catch (e: Exception) {
            Log.e(TAG, "响应解析异常: ${e.message}", e)
            // 尝试直接解析为 JSON（可能是纯 JSON 响应）
            tryParseRawJson(responseText, accounts)
        }
    }

    /**
     * 从 LLM 输出的文本中提取 JSON 块
     * 支持格式：
     *   - ```json { ... } ```
     *   - ``` { ... } ```
     *   - 纯 JSON 文本 { ... }
     */
    private fun extractJson(text: String): String {
        // 尝试匹配 markdown 代码块中的 JSON
        val mdPattern = Regex("""```(?:json)?\s*\n?([\s\S]*?)```""")
        val mdMatch = mdPattern.find(text)
        if (mdMatch != null) {
            return mdMatch.groupValues[1].trim()
        }

        // 尝试找到第一个 { 到最后一个 }
        val firstBrace = text.indexOf('{')
        val lastBrace = text.lastIndexOf('}')
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return text.substring(firstBrace, lastBrace + 1).trim()
        }

        return ""
    }

    /**
     * 尝试将原始文本直接解析为 JSON（用于纯 JSON 响应或兜底）
     */
    private fun tryParseRawJson(rawText: String, accounts: List<Account>): List<ParsedRecord> {
        return try {
            val jsonText = extractJson(rawText)
            if (jsonText.isNotBlank()) {
                parseTransactionsJson(jsonText, accounts)
            } else {
                Log.w(TAG, "无法从原始文本中提取 JSON")
                emptyList()
            }
        } catch (e: Exception) {
            Log.w(TAG, "原始文本 JSON 解析失败: ${e.message}")
            emptyList()
        }
    }

    /**
     * 解析交易 JSON → List<ParsedRecord>
     */
    private fun parseTransactionsJson(jsonText: String, accounts: List<Account>): List<ParsedRecord> {
        val root = JSONObject(jsonText)
        val txArray = root.optJSONArray("transactions")
            ?: root.optJSONArray("records")
            ?: root.optJSONArray("data")
            ?: return emptyList()

        val records = mutableListOf<ParsedRecord>()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        for (i in 0 until txArray.length()) {
            try {
                val tx = txArray.optJSONObject(i) ?: continue

                val date = tx.optString("date", "").trim()
                val time = tx.optString("time", "").trim()
                val amount = tx.optDouble("amount", Double.NaN)
                val description = tx.optString("description", "").trim()
                    .ifBlank { tx.optString("desc", "").trim() }
                    .ifBlank { tx.optString("note", "").trim() }
                    .ifBlank { tx.optString("merchant", "").trim() }
                    .ifBlank { "银行交易" }
                val balance = tx.optDouble("balance", Double.NaN)

                // 跳过无金额的记录
                if (amount.isNaN()) {
                    Log.d(TAG, "  跳过无金额记录: date=$date desc=$description")
                    continue
                }

                // 构建日期时间字符串
                val dateTimeStr = if (time.isNotEmpty()) "$date $time" else "$date 00:00:00"
                val dateLong = try {
                    dateFormat.parse(dateTimeStr)?.time ?: System.currentTimeMillis()
                } catch (e: Exception) {
                    // 尝试只用日期解析
                    try {
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            .parse(date)?.time ?: System.currentTimeMillis()
                    } catch (e2: Exception) {
                        System.currentTimeMillis()
                    }
                }

                val txType = if (amount < 0) TransactionType.EXPENSE else TransactionType.INCOME
                val amountStr = String.format("%.2f", kotlin.math.abs(amount))
                val balanceStr = if (balance.isNaN()) "" else String.format("%.2f", balance)

                // 分类推断（复用现有逻辑）
                val category = ScreenshotTransactionParser.inferCategoryPublic(
                    description, description, txType
                )
                // 账户匹配
                val accountId = ScreenshotTransactionParser.inferAccountPublic(
                    description, description, accounts
                )

                records.add(ParsedRecord(
                    amount = amountStr,
                    type = txType,
                    note = description.take(80),
                    category = category,
                    accountId = accountId,
                    date = dateLong,
                    balance = balanceStr
                ))

                Log.d(TAG, "  → 记录: date=$date time=$time amt=$amount" +
                        " desc=\"$description\" bal=$balanceStr")

            } catch (e: Exception) {
                Log.w(TAG, "  解析单条记录异常: ${e.message}")
                // 继续处理下一条
            }
        }

        Log.d(TAG, "解析完成: ${records.size} 条记录")

        // 余额验证（复用现有逻辑）
        return ScreenshotTransactionParser.validatePublic(records)
            .also { validated ->
                Log.d(TAG, "验证后: ${validated.size} 条有效记录")
            }
    }

    // ════════════════════════════════════════════════════════════════════
    // 发票识别
    // ════════════════════════════════════════════════════════════════════

    /**
     * 发票识别结果
     */
    data class InvoiceResult(
        val date: String,         // YYYY-MM-DD
        val amount: String,       // 发票金额（格式化字符串）
        val amountDouble: Double, // 发票金额（数值）
        val description: String   // 交易事项
    )

    /**
     * 对多张发票图片进行识别
     * @param imagePaths 发票图片文件路径列表
     * @return 按顺序识别的发票信息列表
     */
    suspend fun recognizeInvoices(imagePaths: List<String>): List<InvoiceResult> {
        return try {
            coroutineScope {
                imagePaths.mapIndexed { index, path ->
                    async(Dispatchers.IO) {
                        try {
                            recognizeSingleInvoice(path, index)
                        } catch (e: Exception) {
                            Log.w(TAG, "发票[$index]识别失败: ${e.message}")
                            emptyList()
                        }
                    }
                }.awaitAll().flatten()
            }
        } catch (e: Exception) {
            Log.e(TAG, "批量发票识别异常: ${e.message}", e)
            emptyList()
        }
    }

    private suspend fun recognizeSingleInvoice(imagePath: String, index: Int): List<InvoiceResult> {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(imagePath)
                if (!file.exists()) {
                    Log.w(TAG, "发票图片不存在: $imagePath")
                    return@withContext emptyList()
                }

                val bitmap = BitmapFactory.decodeFile(imagePath)
                    ?: return@withContext emptyList()

                val base64 = bitmapToBase64(bitmap)
                bitmap.recycle()

                val requestBody = buildInvoiceRequestBody(base64)
                val responseText = callApi(requestBody) ?: return@withContext emptyList()

                Log.d(TAG, "发票[$index] API 响应: ${responseText.length} 字符")

                parseInvoiceResponse(responseText)
            } catch (e: Exception) {
                Log.w(TAG, "发票[$index]解析异常: ${e.message}")
                emptyList()
            }
        }
    }

    private fun buildInvoiceRequestBody(base64Image: String): String {
        val systemPrompt = """你是一个专业的中国发票识别助手。请从发票图片中提取关键信息。

请严格遵守以下规则：
1. 识别发票中的所有项目信息
2. 日期格式统一为 YYYY-MM-DD
3. 金额保留两位小数，使用正数（如 150.00）
4. 交易事项填写品名或服务名称
5. 如果无法确定某个字段，使用空字符串 ""

请严格按照以下 JSON 格式返回，不要添加任何额外文字或注释：

```json
{
  "invoices": [
    {
      "date": "2026-01-15",
      "amount": 150.00,
      "description": "办公用品-打印纸"
    }
  ]
}
```"""

        val userPrompt = "请识别这张发票图片中的所有项目信息，按上述 JSON 格式返回。"

        val json = JSONObject().apply {
            put("model", MODEL)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", JSONArray().apply {
                        put(JSONObject().apply {
                            put("type", "text")
                            put("text", userPrompt)
                        })
                        put(JSONObject().apply {
                            put("type", "image_url")
                            put("image_url", JSONObject().apply {
                                put("url", "data:image/jpeg;base64,$base64Image")
                            })
                        })
                    })
                })
            })
            put("temperature", 0.1)
            put("max_tokens", 4096)
        }

        return json.toString()
    }

    private fun parseInvoiceResponse(responseText: String): List<InvoiceResult> {
        return try {
            val apiJson = JSONObject(responseText)
            val choices = apiJson.optJSONArray("choices")
            val content = choices?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content", "") ?: ""

            val jsonText = if (content.isNotBlank()) extractJson(content) else extractJson(responseText)
            if (jsonText.isBlank()) return emptyList()

            parseInvoiceJson(jsonText)
        } catch (e: Exception) {
            Log.w(TAG, "发票响应解析异常: ${e.message}")
            emptyList()
        }
    }

    private fun parseInvoiceJson(jsonText: String): List<InvoiceResult> {
        val root = JSONObject(jsonText)
        val invArray = root.optJSONArray("invoices")
            ?: root.optJSONArray("items")
            ?: return emptyList()

        val results = mutableListOf<InvoiceResult>()
        for (i in 0 until invArray.length()) {
            try {
                val inv = invArray.optJSONObject(i) ?: continue
                val date = inv.optString("date", "").trim()
                val amount = inv.optDouble("amount", Double.NaN)
                val desc = inv.optString("description", "").trim()
                    .ifBlank { inv.optString("desc", "").trim() }
                    .ifBlank { inv.optString("item", "").trim() }

                if (amount.isNaN() || amount <= 0) continue

                results.add(InvoiceResult(
                    date = date.ifBlank { "未知" },
                    amount = String.format("%.2f", amount),
                    amountDouble = amount,
                    description = desc.ifBlank { "未识别" }
                ))
            } catch (e: Exception) {
                Log.w(TAG, "单条发票解析异常: ${e.message}")
            }
        }

        Log.d(TAG, "发票解析完成: ${results.size} 条")
        return results
    }
}
