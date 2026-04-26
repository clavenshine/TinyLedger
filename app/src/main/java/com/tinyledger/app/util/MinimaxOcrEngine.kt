package com.tinyledger.app.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.tinyledger.app.domain.model.Account
import com.tinyledger.app.domain.model.TransactionType
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
 * MiniMax 大模型 OCR 引擎 — 截屏识别与发票识别的备选方案
 *
 * 通过 MiniMax OpenAI 兼容 API 调用多模态大模型，
 * 识别银行交易截图/发票并返回结构化 JSON 记录。
 *
 * 设计原则：
 *   - 所有异常静默处理，返回空列表 → 自动回退下一优先级
 *   - 使用原始彩色截图（大模型对自然图像效果更好）
 *   - 支持 JSON 与 Markdown 代码块两种响应格式
 */
object MinimaxOcrEngine {

    private const val TAG = "MinimaxOcrEngine"
    private const val API_URL_OPENAI = "https://api.minimaxi.com/v1/chat/completions"
    private const val API_URL_VLM = "https://api.minimaxi.com/v1/coding_plan/vlm"
    private const val API_URL_ANTHROPIC = "https://api.minimaxi.com/anthropic/v1/messages"
    /** 按量计费 Key — 用于 OpenAI 兼容 API（支持图片理解） */
    private const val API_KEY_PAY_GO = "********"
    /** Token Plan Key — 用于 Anthropic 兼容 API */
    private const val API_KEY_TOKEN_PLAN = "******"
    private const val MODEL = "MiniMax-M2.7"

    /** 最后一次API调用的错误信息，供UI展示 */
    @Volatile
    var lastError: String = ""
        private set

    /** 最后成功使用的API方式 */
    @Volatile
    var lastMethod: String = ""
        private set

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

    // ════════════════════════════════════════════════════════════════════
    // 公开方法：截屏交易识别
    // ════════════════════════════════════════════════════════════════════

    /**
     * 调用 MiniMax 大模型识别银行交易截图
     *
     * @param bitmap 原始彩色截图
     * @param accounts 当前账户列表（用于账户匹配）
     * @return 识别到的交易记录列表；失败时返回空列表
     */
    suspend fun recognize(bitmap: Bitmap, accounts: List<Account>): List<ParsedRecord> {
        lastError = ""
        lastMethod = ""
        return try {
            withContext(Dispatchers.IO) {
                Log.d(TAG, "开始 MiniMax OCR 识别...")

                val base64 = bitmapToBase64(bitmap)
                Log.d(TAG, "Base64长度: ${base64.length}字符")
                val errLog = StringBuilder()

                // ── 方式1: OpenAI 兼容 API（带409/429重试）──
                Log.d(TAG, "[方式1] 尝试 OpenAI API (按量计费Key, $API_URL_OPENAI)")
                val openAiBody = buildOpenAiRequestBody(base64)
                var openAiResponse: String? = null
                for (retry in 0..2) {
                    if (retry > 0) {
                        val delayMs = retry * 3000L
                        Log.d(TAG, "  ⏳ 429重试 $retry/2, 等待${delayMs}ms...")
                        kotlinx.coroutines.delay(delayMs)
                    }
                    openAiResponse = callOpenAiApi(openAiBody)
                    if (openAiResponse != null) break
                    if (!lastError.contains("429")) break // 非429不重试
                }
                if (openAiResponse != null) {
                    Log.d(TAG, "[方式1] ✅ OpenAI API 有响应，长度=${openAiResponse.length}")
                    lastMethod = "MiniMax (OpenAI API)"
                    val records = parseResponse(openAiResponse, accounts)
                    if (records.isNotEmpty()) {
                        lastMethod = "MiniMax (OpenAI): ${records.size}条"
                        return@withContext records
                    }
                    Log.d(TAG, "[方式1] ⚠ 解析结果为空，尝试 Anthropic API")
                    errLog.append("OpenAI解析空; ")
                } else {
                    errLog.append("OpenAI: ${lastError}; ")
                    Log.d(TAG, "[方式1] ❌ OpenAI API 失败: ${lastError}, 尝试 Anthropic API")
                }

                // ── 方式2: VLM 视觉API (Token Plan Key) ──
                Log.d(TAG, "[方式2] 尝试 VLM 视觉API (Token Plan Key, $API_URL_VLM)")
                val vlmResponse = callVlmApi(base64)
                if (vlmResponse != null) {
                    Log.d(TAG, "[方式2] ✅ VLM API 有响应，长度=${vlmResponse.length}")
                    lastMethod = "MiniMax (VLM)"
                    val records = parseResponse(vlmResponse, accounts)
                    if (records.isNotEmpty()) {
                        lastMethod = "MiniMax (VLM): ${records.size}条"
                        return@withContext records
                    }
                    Log.d(TAG, "[方式2] ⚠ VLM解析结果为空")
                    errLog.append("VLM解析空; ")
                } else {
                    errLog.append("VLM: ${lastError}; ")
                    Log.d(TAG, "[方式2] ❌ VLM API 失败: ${lastError}")
                }

                // ── 方式3: Anthropic API + 按量计费Key ──
                Log.d(TAG, "[方式3] 尝试 Anthropic API (按量计费Key)")
                val anthroBody2 = buildAnthropicRequestBody(base64)
                Log.d(TAG, "Anthropic3请求体(前200): ${anthroBody2.take(200)}")
                val anthroResponse2 = callAnthropicApiWithKey(anthroBody2, API_KEY_PAY_GO)
                if (anthroResponse2 != null) {
                    Log.d(TAG, "[方式3] ✅ Anthropic(按量) 有响应")
                    lastMethod = "MiniMax (Anthropic按量)"
                    val records = parseAnthropicResponse(anthroResponse2, accounts)
                    if (records.isNotEmpty()) {
                        lastMethod = "MiniMax (Anthropic按量): ${records.size}条"
                        return@withContext records
                    }
                    errLog.append("Anthropic按量解析空; ")
                } else {
                    errLog.append("Anthropic按量失败; ")
                }

                lastError = errLog.toString().trimEnd(' ', ';')
                Log.w(TAG, "所有 MiniMax API 方式均失败: $lastError")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "MiniMax OCR 异常: ${e.message}", e)
            lastError = "异常: ${e.message}"
            emptyList()
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // 公开方法：发票识别
    // ════════════════════════════════════════════════════════════════════

    /**
     * 发票识别结果
     */
    /**
     * 对多张发票图片进行识别
     */
    suspend fun recognizeInvoices(imagePaths: List<String>): List<QianfanOcrEngine.InvoiceResult> {
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

    private suspend fun recognizeSingleInvoice(imagePath: String, index: Int): List<QianfanOcrEngine.InvoiceResult> {
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
                Log.d(TAG, "Base64长度: ${base64.length}字符")
                bitmap.recycle()

                val requestBody = buildInvoiceRequestBody(base64)
                val responseText = callOpenAiApi(requestBody) ?: return@withContext emptyList()

                Log.d(TAG, "发票[$index] API 响应: ${responseText.length} 字符")
                parseInvoiceResponse(responseText)
            } catch (e: Exception) {
                Log.w(TAG, "发票[$index]解析异常: ${e.message}")
                emptyList()
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // 图片编码
    // ════════════════════════════════════════════════════════════════════

    private fun bitmapToBase64(bitmap: Bitmap): String {
        // 压缩超长图片：限制最大高度为 2048px，保持宽高比
        var processedBitmap = bitmap
        if (bitmap.height > 2048) {
            val scale = 2048.0 / bitmap.height
            val newWidth = (bitmap.width * scale).toInt()
            processedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, 2048, true)
            Log.d(TAG, "图片压缩: ${bitmap.width}x${bitmap.height} -> ${newWidth}x2048")
        }

        val stream = ByteArrayOutputStream()
        processedBitmap.compress(Bitmap.CompressFormat.JPEG, 75, stream)
        val bytes = stream.toByteArray()
        Log.d(TAG, "图片编码: ${processedBitmap.width}x${processedBitmap.height}, JPEG ${bytes.size / 1024}KB")
        
        // 释放压缩后的bitmap（如果是新创建的）
        if (processedBitmap !== bitmap) {
            processedBitmap.recycle()
        }
        
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    // ════════════════════════════════════════════════════════════════════
    // 截屏交易请求构建
    // ════════════════════════════════════════════════════════════════════

    private fun buildOpenAiRequestBody(base64Image: String): String {
        val systemPrompt = """你是一个专业的银行交易截图识别助手。你的任务是从银行APP截图中提取所有交易记录。

请严格遵守以下规则：
1. 逐笔列出截图中的所有交易记录（不要遗漏任何一笔）
2. 金额格式：支出为负数（如 -68.97），收入为正数（如 500.00）
3. 日期格式统一为 YYYY-MM-DD
4. 如果截图中有余额信息，也请一并提取
5. 如果无法确定某个字段，使用空字符串 ""

严格只输出JSON，不要输出任何解释、markdown标记或额外文字。直接返回JSON对象，不要加反引号或注释。

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

        val userPrompt = "这是一张银行APP截图，请识别图中所有交易记录并输出JSON。不要输出任何其他文字或格式标记，只输出纯JSON。"

        val json = JSONObject().apply {
            put("model", MODEL)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", JSONArray().apply {
                        put(JSONObject().apply {
                            put("type", "image")
                            put("source", JSONObject().apply {
                                put("type", "base64")
                                put("media_type", "image/jpeg")
                                put("data", base64Image)
                            })
                        })
                        put(JSONObject().apply {
                            put("type", "text")
                            put("text", userPrompt)
                        })
                    })
                })
            })
            put("temperature", 0.1)
            put("max_completion_tokens", 4096)
        }

        return json.toString()
    }

    // ════════════════════════════════════════════════════════════════════
    // 发票请求构建
    // ════════════════════════════════════════════════════════════════════

    private fun buildInvoiceRequestBody(base64Image: String): String {
        val systemPrompt = """你是一个专业的中国发票识别助手。请从发票图片中提取关键信息。

请严格遵守以下规则：
1. 识别发票中的所有项目信息
2. 日期格式统一为 YYYY-MM-DD
3. 金额保留两位小数，使用正数（如 150.00）
4. 交易事项填写品名或服务名称
5. 如果无法确定某个字段，使用空字符串 ""

严格只输出JSON，不要输出任何解释、markdown标记或额外文字。直接返回JSON对象，不要加反引号或注释。

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
                    put("role", "user")
                    put("content", JSONArray().apply {
                        put(JSONObject().apply {
                            put("type", "image")
                            put("source", JSONObject().apply {
                                put("type", "base64")
                                put("media_type", "image/jpeg")
                                put("data", base64Image)
                            })
                        })
                        put(JSONObject().apply {
                            put("type", "text")
                            put("text", userPrompt)
                        })
                    })
                })
            })
            put("temperature", 0.1)
            put("max_completion_tokens", 4096)
        }

        return json.toString()
    }

    // ════════════════════════════════════════════════════════════════════
    // API 调用
    // ════════════════════════════════════════════════════════════════════

    private suspend fun callOpenAiApi(requestBody: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val body = requestBody.toRequestBody(JSON_MEDIA)
                val request = Request.Builder()
                    .url(API_URL_OPENAI)
                    .addHeader("Authorization", "Bearer $API_KEY_PAY_GO")
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build()

                Log.d(TAG, "发送 API 请求...")
                val response = client.newCall(request).execute()

                val responseBody = response.body?.string() ?: ""
                Log.d(TAG, "HTTP ${response.code}: ${responseBody.length} 字符")

                if (!response.isSuccessful) {
                    val errMsg = "HTTP ${response.code}: ${responseBody.take(300)}"
                    Log.e(TAG, "API 请求失败: $errMsg")
                    lastError = "OpenAI HTTP ${response.code}"
                    return@withContext null
                }

                responseBody
            } catch (e: Exception) {
                Log.e(TAG, "OpenAI API 网络异常: ${e.message}", e)
                lastError = "OpenAI网络: ${e.message?.take(30)}"
                null
            }
        }
    }


    // ════════════════════════════════════════════════════════════════════
    // Anthropic 兼容 API 请求构建
    // ════════════════════════════════════════════════════════════════════

    private fun buildAnthropicRequestBody(base64Image: String): String {
        val systemPrompt = """你是一个专业的银行交易截图识别助手。你的任务是从银行APP截图中提取所有交易记录。

请严格遵守以下规则：
1. 逐笔列出截图中的所有交易记录（不要遗漏任何一笔）
2. 金额格式：支出为负数（如 -68.97），收入为正数（如 500.00）
3. 日期格式统一为 YYYY-MM-DD
4. 如果截图中有余额信息，也请一并提取
5. 如果无法确定某个字段，使用空字符串 ""

严格只输出JSON，不要输出任何解释、markdown标记或额外文字。直接返回JSON对象，不要加反引号或注释。

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

        val userPrompt = "这是一张银行APP截图，请识别图中所有交易记录并输出JSON。不要输出任何其他文字或格式标记，只输出纯JSON。"

        val json = JSONObject().apply {
            put("model", MODEL)
            put("max_completion_tokens", 4096)
            put("thinking", JSONObject().apply { put("type", "skip") })
            put("system", systemPrompt)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", JSONArray().apply {
                        put(JSONObject().apply {
                            put("type", "image")
                            put("source", JSONObject().apply {
                                put("type", "base64")
                                put("media_type", "image/jpeg")
                                put("data", base64Image)
                            })
                        })
                        put(JSONObject().apply {
                            put("type", "text")
                            put("text", userPrompt)
                        })
                    })
                })
            })
            put("temperature", 0.1)
        }

        return json.toString()
    }

    // ════════════════════════════════════════════════════════════════════
    // Anthropic 兼容 API 调用
    // ════════════════════════════════════════════════════════════════════

    private suspend fun callAnthropicApi(requestBody: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val body = requestBody.toRequestBody(JSON_MEDIA)
                val request = Request.Builder()
                    .url(API_URL_ANTHROPIC)
                    .addHeader("x-api-key", API_KEY_TOKEN_PLAN)
                    .addHeader("anthropic-version", "2023-06-01")
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build()

                Log.d(TAG, "发送 Anthropic API 请求...")
                val response = client.newCall(request).execute()

                val responseBody = response.body?.string() ?: ""
                Log.d(TAG, "Anthropic HTTP ${response.code}: ${responseBody.length} 字符")

                if (!response.isSuccessful) {
                    val errMsg = "Anthropic HTTP ${response.code}: ${responseBody.take(300)}"
                    Log.e(TAG, "Anthropic API 请求失败: $errMsg")
                    lastError = "Anthropic HTTP ${response.code}"
                    return@withContext null
                }

                responseBody
            } catch (e: Exception) {
                Log.e(TAG, "Anthropic API 网络异常: ${e.message}", e)
                lastError = "Anthropic网络: ${e.message?.take(30)}"
                null
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // VLM 视觉 API 调用 (Token Plan Key)
    // ════════════════════════════════════════════════════════════════════

    private suspend fun callVlmApi(base64Image: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = """你是一个专业的银行交易截图识别助手。请从银行APP截图中提取所有交易记录。
金额格式：支出为负数，收入为正数。日期格式：YYYYMMDD。
严格只输出JSON：{"transactions":[{"date":"YYYYMMDD","description":"描述","amount":-99.99,"balance":1000.00,"merchant":"商户","note":"备注"}]}
不要输出任何其他文字或格式标记。""".trimIndent()

                val json = JSONObject().apply {
                    put("prompt", prompt)
                    put("image_url", "data:image/jpeg;base64,$base64Image")
                }
                val requestBody = json.toString()
                Log.d(TAG, "VLM请求体长度: ${requestBody.length}")

                val body = requestBody.toRequestBody(JSON_MEDIA)
                val request = Request.Builder()
                    .url(API_URL_VLM)
                    .addHeader("Authorization", "Bearer $API_KEY_TOKEN_PLAN")
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build()

                Log.d(TAG, "发送 VLM API 请求...")
                val response = client.newCall(request).execute()

                val responseBody = response.body?.string() ?: ""
                Log.d(TAG, "VLM HTTP ${response.code}: ${responseBody.length} 字符")
                Log.d(TAG, "VLM响应(前300): ${responseBody.take(300)}")

                if (!response.isSuccessful) {
                    val errMsg = "VLM HTTP ${response.code}: ${responseBody.take(200)}"
                    Log.e(TAG, "VLM API 请求失败: $errMsg")
                    lastError = "VLM HTTP ${response.code}"
                    return@withContext null
                }

                // Parse VLM response: {"content": "...", "base_resp": {...}}
                val respJson = JSONObject(responseBody)
                val content = respJson.optString("content", "")
                val statusCode = respJson.optJSONObject("base_resp")?.optInt("status_code", -1) ?: -1
                Log.d(TAG, "VLM status_code: $statusCode, content长度: ${content.length}")

                if (statusCode != 0 || content.isBlank()) {
                    val statusMsg = respJson.optJSONObject("base_resp")?.optString("status_msg", "") ?: ""
                    Log.w(TAG, "VLM返回异常: status=$statusCode msg=$statusMsg")
                    lastError = "VLM: $statusMsg"
                    return@withContext null
                }

                content
            } catch (e: Exception) {
                Log.e(TAG, "VLM API 网络异常: ${e.message}", e)
                lastError = "VLM网络: ${e.message?.take(30)}"
                null
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // Anthropic 兼容 API 调用（可指定Key）
    // ════════════════════════════════════════════════════════════════════

    private suspend fun callAnthropicApiWithKey(requestBody: String, apiKey: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val body = requestBody.toRequestBody(JSON_MEDIA)
                val request = Request.Builder()
                    .url(API_URL_ANTHROPIC)
                    .addHeader("x-api-key", apiKey)
                    .addHeader("anthropic-version", "2023-06-01")
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build()

                Log.d(TAG, "发送 Anthropic(通用) API 请求...")
                val response = client.newCall(request).execute()

                val responseBody = response.body?.string() ?: ""
                Log.d(TAG, "Anthropic(通用) HTTP ${response.code}: ${responseBody.length} 字符")

                if (!response.isSuccessful) {
                    val errMsg = "Anthropic(通用) HTTP ${response.code}: ${responseBody.take(200)}"
                    Log.e(TAG, "Anthropic(通用) API 请求失败: $errMsg")
                    lastError = "Anth通用 HTTP ${response.code}"
                    return@withContext null
                }

                responseBody
            } catch (e: Exception) {
                Log.e(TAG, "Anthropic(通用) 网络异常: ${e.message}", e)
                lastError = "Anth通用网络: ${e.message?.take(30)}"
                null
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // Anthropic 响应解析
    // ════════════════════════════════════════════════════════════════════

    private fun parseAnthropicResponse(responseText: String, accounts: List<Account>): List<ParsedRecord> {
        return try {
            val apiJson = JSONObject(responseText)
            val contentArray = apiJson.optJSONArray("content")
                ?: return tryParseRawJson(responseText, accounts)

            // Anthropic 返回 content 数组，收集所有文本内容（跳过signature，只取thinking和text的text字段）
            val textContent = StringBuilder()
            for (i in 0 until contentArray.length()) {
                val block = contentArray.optJSONObject(i) ?: continue
                val blockType = block.optString("type", "")
                if (blockType == "thinking") {
                    // thinking 块内有 text 字段存放思维过程，也收集
                    val thinkText = block.optString("text", "")
                    if (thinkText.isNotBlank()) textContent.append(thinkText).append(" ")
                } else if (blockType == "text") {
                    textContent.append(block.optString("text", ""))
                }
            }
            val content = textContent.toString()
            Log.d(TAG, "Anthropic textContent(前500): ${content.take(500)}")
            if (content.isBlank()) {
                Log.w(TAG, "Anthropic 返回空 content, raw(前300): ${responseText.take(300)}")
                return emptyList()
            }

            val jsonText = extractJson(content)
            if (jsonText.isNotBlank()) {
                parseTransactionsJson(jsonText, accounts)
            } else {
                Log.w(TAG, "Anthropic: 无法提取JSON, content(前300): ${content.take(300)}")
                ScreenshotTransactionParser.parse(content, accounts)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Anthropic 响应解析异常: ${e.message}", e)
            tryParseRawJson(responseText, accounts)
        }
    }
    // ════════════════════════════════════════════════════════════════════
    // 响应解析（截屏交易）
    // ════════════════════════════════════════════════════════════════════

    private fun parseResponse(responseText: String, accounts: List<Account>): List<ParsedRecord> {
        return try {
            val apiJson = JSONObject(responseText)
            val choices = apiJson.optJSONArray("choices")
                ?: return tryParseRawJson(responseText, accounts)

            val firstChoice = choices.optJSONObject(0) ?: return emptyList()
            val message = firstChoice.optJSONObject("message") ?: return emptyList()
            val content = message.optString("content", "")
            if (content.isBlank()) {
                Log.w(TAG, "API 返回空 content")
                return emptyList()
            }

            val jsonText = extractJson(content)
            if (jsonText.isBlank()) {
                Log.w(TAG, "无法从 content 中提取 JSON，尝试文本解析")
                return ScreenshotTransactionParser.parse(content, accounts)
            }

            parseTransactionsJson(jsonText, accounts)
        } catch (e: Exception) {
            Log.e(TAG, "响应解析异常: ${e.message}", e)
            tryParseRawJson(responseText, accounts)
        }
    }

    private fun extractJson(text: String): String {
        val mdPattern = Regex("""```(?:json)?\s*\n?([\s\S]*?)```""")
        val mdMatch = mdPattern.find(text)
        if (mdMatch != null) {
            return mdMatch.groupValues[1].trim()
        }

        val firstBrace = text.indexOf('{')
        val lastBrace = text.lastIndexOf('}')
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return text.substring(firstBrace, lastBrace + 1).trim()
        }

        return ""
    }

    private fun tryParseRawJson(rawText: String, accounts: List<Account>): List<ParsedRecord> {
        return try {
            val jsonText = extractJson(rawText)
            if (jsonText.isNotBlank()) {
                parseTransactionsJson(jsonText, accounts)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

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

                if (amount.isNaN()) {
                    Log.d(TAG, "  跳过无金额记录: date=$date desc=$description")
                    continue
                }

                val dateTimeStr = if (time.isNotEmpty()) "$date $time" else "$date 00:00:00"
                val dateLong = try {
                    dateFormat.parse(dateTimeStr)?.time ?: System.currentTimeMillis()
                } catch (e: Exception) {
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

                val category = ScreenshotTransactionParser.inferCategoryPublic(
                    description, description, txType
                )
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
            }
        }

        Log.d(TAG, "解析完成: ${records.size} 条记录")
        return ScreenshotTransactionParser.validatePublic(records)
            .also { validated ->
                Log.d(TAG, "验证后: ${validated.size} 条有效记录")
            }
    }

    // ════════════════════════════════════════════════════════════════════
    // 发票响应解析
    // ════════════════════════════════════════════════════════════════════

    private fun parseInvoiceResponse(responseText: String): List<QianfanOcrEngine.InvoiceResult> {
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

    private fun parseInvoiceJson(jsonText: String): List<QianfanOcrEngine.InvoiceResult> {
        val root = JSONObject(jsonText)
        val invArray = root.optJSONArray("invoices")
            ?: root.optJSONArray("items")
            ?: return emptyList()

        val results = mutableListOf<QianfanOcrEngine.InvoiceResult>()
        for (i in 0 until invArray.length()) {
            try {
                val inv = invArray.optJSONObject(i) ?: continue
                val date = inv.optString("date", "").trim()
                val amount = inv.optDouble("amount", Double.NaN)
                val desc = inv.optString("description", "").trim()
                    .ifBlank { inv.optString("desc", "").trim() }
                    .ifBlank { inv.optString("item", "").trim() }

                if (amount.isNaN() || amount <= 0) continue

                results.add(QianfanOcrEngine.InvoiceResult(
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
