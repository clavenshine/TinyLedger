package com.tinyledger.app.ui.screens.budget

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.tinyledger.app.domain.model.Account
import com.tinyledger.app.domain.model.AccountAttribute
import com.tinyledger.app.domain.model.AccountType
import com.tinyledger.app.domain.model.Category
import com.tinyledger.app.domain.model.Transaction
import com.tinyledger.app.domain.model.TransactionType
import com.tinyledger.app.ui.theme.IOSColors
import com.tinyledger.app.util.ScreenshotTransactionParser
import kotlinx.coroutines.launch
import com.tinyledger.app.ui.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.BitmapFactory
import com.tinyledger.app.util.ImagePreprocessor
import com.tinyledger.app.util.PaddleOcrEngine
import com.tinyledger.app.util.QianfanOcrEngine
import com.tinyledger.app.util.MinimaxOcrEngine
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * 单条识别记录（可编辑）- 包含分类、账户、日期信息
 */
data class ParsedRecord(
    val amount: String,
    val type: TransactionType,
    val note: String,
    val category: Category? = null,
    val accountId: Long? = null,
    val date: Long = System.currentTimeMillis(),
    val balance: String = "",
    val saved: Boolean = false,
    val balanceValid: BalanceValidation = BalanceValidation.NO_BALANCE
)

/** 余额链式校验状态 */
enum class BalanceValidation {
    /** 无余额数据，无法校验 */
    NO_BALANCE,
    /** 余额与上一笔金额一致 ✓ */
    VALID,
    /** 余额与预期不符 ⚠ */
    MISMATCH,
    /** 余额链式已校正金额 ↻ */
    CORRECTED
}


/** OCR引擎选项 */
enum class OcrEngineOption(val label: String, val priority: Int) {
    QIANFAN("\u5343\u5E06OCR", 1),     // "\u767E\u5EA6\u5343\u5E06OCR"
    MINIMAX("MiniMax", 2),
    PADDLE("Paddle", 3),
    MLKIT("ML Kit", 4);
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ScreenshotAccountingScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val homeState by viewModel.uiState.collectAsState()
    var recognizedText by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var parsedRecords by remember { mutableStateOf(listOf<ParsedRecord>()) }
    var showResult by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // PaddleOCR 初始化状态
    var paddleAvailable by remember { mutableStateOf(false) }
    var paddleInitMsg by remember { mutableStateOf("") }
    
    // 千帆OCR 可用状态（网络API，默认可用）
    var qianfanAvailable by remember { mutableStateOf(true) }

    // ── OCR引擎选择（默认千帆OCR）
    var selectedEngine by remember { mutableStateOf(OcrEngineOption.QIANFAN) }
    var minimaxAvailable by remember { mutableStateOf(true) }
    
    // 初始化 PaddleOCR 引擎
    LaunchedEffect(Unit) {
        val success = PaddleOcrEngine.init(context)
        paddleAvailable = success
        paddleInitMsg = if (success) "PaddleOCR \u5c31\u7eea" else "PaddleOCR \u672a\u5c31\u7eea: ${PaddleOcrEngine.getInitError() ?: "\u672a\u77e5"}"
        Log.d("ScreenshotOCR", "PaddleOCR \u521d\u59cb\u5316\u7ed3\u679c: $paddleInitMsg")
    }
    
    /**
     * \u6267\u884c OCR \u8bc6\u522b\uff08\u5343\u5e06OCR \u2192 PaddleOCR \u2192 ML Kit \u9010\u7ea7\u56de\u9000\uff09
     */
    /**
     * 执行 OCR 识别（根据用户选择 + 优先级降级：千帆→MiniMax→Paddle→ML Kit）
     */
    suspend fun performOcr(bitmap: android.graphics.Bitmap): List<ParsedRecord> {
        val engine = selectedEngine
        Log.d("ScreenshotOCR", "用户选择引擎: ${engine.label}, 优先级=${engine.priority}")

        // ── 按用户选择顺序 + 优先级降级尝试 ──
        val attemptOrder = buildList {
            add(engine)  // 用户选择优先
            // 补充剩余引擎，按优先级排序
            OcrEngineOption.entries
                .filter { it != engine }
                .sortedBy { it.priority }
                .forEach { add(it) }
        }

        val failLog = StringBuilder()
        for (attempt in attemptOrder) {
            val result = when (attempt) {
                OcrEngineOption.QIANFAN -> {
                    Log.d("ScreenshotOCR", "  → 尝试千帆OCR...")
                    val qianfanResult = QianfanOcrEngine.recognize(bitmap, homeState.accounts)
                    if (qianfanResult.isNotEmpty()) {
                        val prefix = if (selectedEngine != OcrEngineOption.QIANFAN) "已降级→千帆OCR" else "千帆OCR"
                        val extraInfo = if (failLog.isNotEmpty()) " | ${failLog}" else ""
                        recognizedText = "$prefix: ${qianfanResult.size}条$extraInfo"
                    } else {
                        failLog.append("千帆无结果; ")
                        Log.d("ScreenshotOCR", "  ⚠ 千帆OCR 无结果")
                    }
                    qianfanResult
                }
                OcrEngineOption.MINIMAX -> {
                    Log.d("ScreenshotOCR", "  → 尝试MiniMax (先OpenAI API, 后Anthropic API)...")
                    val minimaxResult = MinimaxOcrEngine.recognize(bitmap, homeState.accounts)
                    if (minimaxResult.isNotEmpty()) {
                        recognizedText = MinimaxOcrEngine.lastMethod.ifBlank { "MiniMax: ${minimaxResult.size}条" }
                    } else {
                        val errInfo = MinimaxOcrEngine.lastError.ifBlank { "未响应" }
                        failLog.append("MiniMax失败($errInfo); ")
                        recognizedText = "MiniMax失败($errInfo)"
                        Log.d("ScreenshotOCR", "  ⚠ MiniMax: $errInfo")
                    }
                    minimaxResult
                }
                OcrEngineOption.PADDLE -> {
                    if (!paddleAvailable) {
                        Log.d("ScreenshotOCR", "  ⚠ PaddleOCR 未就绪，跳过")
                        emptyList()
                    } else {
                        Log.d("ScreenshotOCR", "  → 尝试PaddleOCR...")
                        val processed = ImagePreprocessor.preprocess(bitmap)
                        val elements = PaddleOcrEngine.recognize(processed)
                        if (elements.isNotEmpty()) {
                            recognizedText = elements.joinToString(" ") { it.text }
                            ScreenshotTransactionParser.parseVisual(elements, homeState.accounts)
                        } else {
                            Log.d("ScreenshotOCR", "  ⚠ PaddleOCR 无结果")
                            emptyList()
                        }
                    }
                }
                OcrEngineOption.MLKIT -> {
                    Log.d("ScreenshotOCR", "  → 尝试ML Kit...")
                    val processed = ImagePreprocessor.preprocess(bitmap)
                    val image = com.google.mlkit.vision.common.InputImage.fromBitmap(processed, 0)
                    val recognizer = com.google.mlkit.vision.text.TextRecognition.getClient(
                        com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions.Builder().build()
                    )
                    val result = kotlinx.coroutines.suspendCancellableCoroutine<com.google.mlkit.vision.text.Text?> { cont ->
                        recognizer.process(image)
                            .addOnSuccessListener { cont.resume(it) }
                            .addOnFailureListener { cont.resume(null) }
                    }
                    if (result == null) {
                        recognizedText = "ML Kit 识别失败"
                        emptyList()
                    } else {
                        recognizedText = result.text
                        val ocrElements = extractOcrElements(result)
                        if (ocrElements.isNotEmpty()) {
                            ScreenshotTransactionParser.parseVisual(ocrElements, homeState.accounts)
                        } else {
                            ScreenshotTransactionParser.parse(result.text, homeState.accounts)
                        }
                    }
                }
            }
            if (result.isNotEmpty()) {
                Log.d("ScreenshotOCR", "\u2705 \u4F7F\u7528 ${attempt.label}: ${result.size} \u6761\u8BB0\u5F55")
                return result
            }
        }

        Log.w("ScreenshotOCR", "\u274C \u6240\u6709OCR\u5F15\u64CE\u5747\u5931\u8D25")
        recognizedText = "\u6240\u6709OCR\u5F15\u64CE\u5747\u65E0\u7ED3\u679C"
        return emptyList()
    }
        
    /** 显示识别结果提示 */
    fun showResultSnackbar(records: List<ParsedRecord>) {
        if (records.isNotEmpty()) {
            val expenseCount = records.count { it.type == TransactionType.EXPENSE }
            val incomeCount = records.count { it.type == TransactionType.INCOME }
            val mismatchCount = records.count { it.balanceValid == BalanceValidation.MISMATCH }
            val correctedCount = records.count { it.balanceValid == BalanceValidation.CORRECTED }
            val msg = buildString {
                if (expenseCount > 0) append("$expenseCount 笔支出")
                if (incomeCount > 0) {
                    if (isNotEmpty()) append("，")
                    append("$incomeCount 笔收入")
                }
                if (isNotEmpty()) append("，")
                append("已自动填充")
                if (correctedCount > 0) append("（${correctedCount}条已校正↻）")
                else if (mismatchCount > 0) append("（${mismatchCount}条余额异常⚠）")
            }
            coroutineScope.launch {
                snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            }
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isProcessing = true
            showResult = false
            parsedRecords = emptyList()
            coroutineScope.launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val sourceBitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                    if (sourceBitmap != null) {
                        parsedRecords = performOcr(sourceBitmap)
                        showResult = true
                        showResultSnackbar(parsedRecords)
                    } else {
                        recognizedText = "图片解码失败"
                    }
                } catch (e: Exception) {
                    Log.e("ScreenshotOCR", "Error", e)
                    recognizedText = "处理失败: ${e.message}"
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    val multiImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            isProcessing = true
            showResult = false
            parsedRecords = emptyList()
            coroutineScope.launch {
                var allRecords = mutableListOf<ParsedRecord>()
                try {
                    for (uri in uris) {
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val sourceBitmap = BitmapFactory.decodeStream(inputStream)
                        inputStream?.close()
                        if (sourceBitmap != null) {
                            allRecords.addAll(performOcr(sourceBitmap))
                        }
                    }
                    parsedRecords = allRecords
                    showResult = true
                    showResultSnackbar(allRecords)
                } catch (e: Exception) {
                    Log.e("ScreenshotOCR", "Multi image error", e)
                    parsedRecords = allRecords
                    showResult = true
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("截屏数据导入", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            modifier = Modifier.size(24.dp).padding(start = 10.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Instruction card - redesigned
            if (!showResult) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Screenshot,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "选择支付截图，自动识别金额",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "支持微信、支付宝、银行等支付截图，可识别多条记录",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Supported screenshot types tags
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(
                        Triple(Icons.Default.Chat, "微信支付", Color(0xFF07C160)),
                        Triple(Icons.Default.Payment, "支付宝", Color(0xFF1677FF)),
                        Triple(Icons.Default.AccountBalance, "银行卡", Color(0xFFFF6D00))
                    ).forEach { (icon, label, color) ->
                        Surface(
                            modifier = Modifier
                                .width(80.dp)
                                .height(30.dp)
                                .padding(horizontal = 2.dp),
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }


            }
        // ── OCR引擎选择 ──
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                    Text(
                        "OCR\u8BC6\u522B\u65B9\u6848",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OcrEngineOption.entries.forEach { option ->
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { selectedEngine = option }
                                    .padding(horizontal = 4.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedEngine == option,
                                    onClick = { selectedEngine = option },
                                    modifier = Modifier.size(16.dp),
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = option.label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = if (selectedEngine == option) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedEngine == option)
                                            MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

            // Select image button - enhanced
            item {
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val buttonColor = if (isPressed) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                } else {
                    MaterialTheme.colorScheme.primary
                }

                Button(
                    onClick = { imagePicker.launch("image/*") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .combinedClickable(
                            onClick = { imagePicker.launch("image/*") },
                            onLongClick = {
                                Toast.makeText(context, "可选择多张截图批量识别", Toast.LENGTH_SHORT).show()
                                multiImagePicker.launch("image/*")
                            },
                            interactionSource = interactionSource,
                            indication = null
                        ),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isProcessing,
                    colors = ButtonDefaults.buttonColors(containerColor = buttonColor)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("正在识别...", fontSize = 16.sp)
                    } else {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("选择截图", fontSize = 16.sp)
                    }
                }
            }

            // Example screenshots area (when no records yet)
            if (!showResult && !isProcessing) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("微信支付", "支付宝").forEach { label ->
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(100.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                if (label == "微信支付") Icons.Default.Chat else Icons.Default.Payment,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                                modifier = Modifier.size(32.dp)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                "${label}截图示例",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "如这样的支付截图可自动识别",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFBDBDBD)
                        )
                    }
                }
            }

            // ── 数据表格展示 ──
            if (showResult && parsedRecords.isNotEmpty()) {
                // 表头
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("#", modifier = Modifier.width(18.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center)
                        Text("账户", modifier = Modifier.width(40.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center)
                        Text("账号", modifier = Modifier.width(32.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center)
                        Text("日期", modifier = Modifier.weight(0.9f),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center)
                        Text("类型", modifier = Modifier.width(16.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center)
                        Text("金额", modifier = Modifier.weight(0.7f),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.End)
                        Text("余额", modifier = Modifier.weight(0.6f),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.End)
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("备注", modifier = Modifier.weight(1.0f),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.width(24.dp))
                    }
                }

                // 数据行
                itemsIndexed(parsedRecords) { index, record ->
                    val bgColor = if (record.saved) IOSColors.SystemGreen.copy(alpha = 0.04f)
                        else if (index % 2 == 0) MaterialTheme.colorScheme.surface
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    val lastRow = index == parsedRecords.lastIndex
                    val dateStr = java.util.Calendar.getInstance().also { it.timeInMillis = record.date }.let { cal -> "${cal.get(java.util.Calendar.YEAR)}${String.format("%02d", cal.get(java.util.Calendar.MONTH) + 1)}${String.format("%02d", cal.get(java.util.Calendar.DAY_OF_MONTH))}" }
                    val isExpense = record.type == TransactionType.EXPENSE
                    // ★ 查找账户信息
                    val account = homeState.accounts.find { it.id == record.accountId }
                    val bankName = account?.name?.take(3) ?: "未指定"
                    val last4 = account?.cardNumber?.takeIf { it.length >= 4 }?.takeLast(4) ?: ""

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = if (lastRow) RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp)
                            else RoundedCornerShape(0.dp),
                        colors = CardDefaults.cardColors(containerColor = bgColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 序号
                            Text(
                                "${index + 1}",
                                modifier = Modifier.width(18.dp),
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            // 账户（银行简称）
                            Text(
                                bankName,
                                modifier = Modifier.width(40.dp),
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            // 账号（卡号后4位）
                            Text(
                                last4.ifBlank { "-" },
                                modifier = Modifier.width(32.dp),
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                            // 日期
                            Text(
                                dateStr,
                                modifier = Modifier.weight(0.9f),
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                            // 类型
                            Text(
                                if (isExpense) "支" else "收",
                                modifier = Modifier.width(16.dp),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, fontSize = 10.sp),
                                color = if (isExpense) MaterialTheme.colorScheme.error
                                    else IOSColors.SystemGreen,
                                textAlign = TextAlign.Center
                            )
                            // 金额（可收缩字体）
                            Text(
                                "¥${record.amount}",
                                modifier = Modifier.weight(0.7f),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 10.sp, letterSpacing = (-0.5).sp),
                                color = if (isExpense) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.End,
                                maxLines = 1,
                                softWrap = false
                            )
                            // 余额
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(0.6f),
                                horizontalArrangement = Arrangement.End
                            ) {
                                when (record.balanceValid) {
                                    BalanceValidation.VALID -> Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "余额正确",
                                        tint = IOSColors.SystemGreen,
                                        modifier = Modifier.size(8.dp)
                                    )
                                    BalanceValidation.CORRECTED -> Icon(
                                        Icons.Default.Sync,
                                        contentDescription = "余额已校正",
                                        tint = Color(0xFF2196F3),
                                        modifier = Modifier.size(8.dp)
                                    )
                                    BalanceValidation.MISMATCH -> Icon(
                                        Icons.Default.Warning,
                                        contentDescription = "余额不匹配",
                                        tint = IOSColors.SystemOrange,
                                        modifier = Modifier.size(8.dp)
                                    )
                                    BalanceValidation.NO_BALANCE -> { /* 无余额不显示 */ }
                                }
                                Spacer(modifier = Modifier.width(1.dp))
                                Text(
                                    if (record.balance.isNotBlank()) "¥${record.balance}" else "-",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                                    color = when (record.balanceValid) {
                                        BalanceValidation.VALID -> IOSColors.SystemGreen
                                        BalanceValidation.CORRECTED -> Color(0xFF2196F3)
                                        BalanceValidation.MISMATCH -> IOSColors.SystemOrange
                                        BalanceValidation.NO_BALANCE -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    maxLines = 1,
                                    textAlign = TextAlign.End
                                )
                            }
                            Spacer(modifier = Modifier.width(2.dp))
                            // 备注
                            Text(
                                record.note,
                                modifier = Modifier.weight(1.0f),
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            // 删除按钮
                            if (!record.saved) {
                                IconButton(
                                    onClick = {
                                        parsedRecords = parsedRecords.toMutableList().also { it.removeAt(index) }
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "删除",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            } else {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "已保存",
                                    tint = IOSColors.SystemGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    // 行间分隔线
                    if (!lastRow) {
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )
                    }
                }

                // 一键全部保存
                val unsavedCount = parsedRecords.count { !it.saved && (it.amount.toDoubleOrNull() ?: 0.0) > 0 }
                if (unsavedCount > 0) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = {
                                // ★ 获取现金账户ID集合（账单页仅显示现金账户的交易）
                                val cashAccounts = homeState.accounts.filter { it.attribute == AccountAttribute.CASH }
                                val cashAccountIds = cashAccounts.map { it.id }.toSet()
                                val defaultCashAccountId = cashAccounts.firstOrNull()?.id
                                Log.d("ScreenshotSave", "CASH账户: ${cashAccounts.map { it.name }}, IDs=$cashAccountIds, 默认=$defaultCashAccountId")

                                parsedRecords = parsedRecords.mapIndexed { idx, record ->
                                    val rawAmount = record.amount.toDoubleOrNull()
                                    if (!record.saved && rawAmount != null && rawAmount > 0) {
                                        val signedAmount = if (record.type == TransactionType.EXPENSE) -rawAmount else rawAmount
                                        // ★ 智能分类匹配：优先使用解析时推断的分类，其次用备注匹配
                                        val category = record.category
                                            ?: ScreenshotTransactionParser.inferCategoryPublic(
                                                text = record.note,
                                                note = record.note,
                                                type = record.type
                                            )
                                            ?: if (record.type == TransactionType.INCOME)
                                                Category.fromId("redpacket", TransactionType.INCOME)
                                            else
                                                Category.fromId("other", TransactionType.EXPENSE)

                                        // ★ 确保 accountId 为现金账户（否则账单页不显示）
                                        val safeAccountId = when {
                                            record.accountId != null && record.accountId in cashAccountIds -> {
                                                Log.d("ScreenshotSave", "  [$idx] 账户匹配: id=${record.accountId} ✓")
                                                record.accountId
                                            }
                                            record.accountId != null -> {
                                                Log.w("ScreenshotSave", "  [$idx] 账户 ${record.accountId} 非CASH属性，改用默认 $defaultCashAccountId")
                                                defaultCashAccountId
                                            }
                                            else -> {
                                                Log.w("ScreenshotSave", "  [$idx] 无账户ID，使用默认 $defaultCashAccountId")
                                                defaultCashAccountId
                                            }
                                        }

                                        Log.d("ScreenshotSave", "  保存[$idx]: type=${record.type} amt=$signedAmount cat=${category.id} acct=$safeAccountId note='${record.note.take(30)}'")

                                        viewModel.insertTransaction(Transaction(
                                            type = record.type,
                                            category = category,
                                            amount = signedAmount,
                                            note = record.note.ifBlank { "截屏数据导入" },
                                            date = record.date,
                                            accountId = safeAccountId
                                        ))
                                        record.copy(saved = true)
                                    } else record
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = IOSColors.SystemGreen)
                        ) {
                            Icon(Icons.Default.SaveAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("全部保存 ($unsavedCount 条)")
                        }
                    }
                }

                // 识别统计
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "识别到 ${parsedRecords.size} 条记录",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            if (showResult && parsedRecords.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("未识别到有效金额记录", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "请确保截图中包含银行交易明细的有效金额信息",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Show recognized text for debugging
            if (showResult && recognizedText.isNotBlank()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "OCR识别文本", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = recognizedText.take(500),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordEditCard(
    index: Int,
    record: ParsedRecord,
    accounts: List<Account>,
    onUpdate: (ParsedRecord) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showAccountSelector by remember { mutableStateOf(false) }
    var showCategorySelector by remember { mutableStateOf(false) }
    val selectedAccount = accounts.find { it.id == record.accountId }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (record.saved) IOSColors.SystemGreen.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "记录 ${index + 1}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                if (!record.saved) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "删除", modifier = Modifier.size(18.dp))
                    }
                } else {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = IOSColors.SystemGreen, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Type selector
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = record.type == TransactionType.EXPENSE,
                    onClick = {
                        if (!record.saved) onUpdate(record.copy(
                            type = TransactionType.EXPENSE,
                            category = null
                        ))
                    },
                    label = { Text("支出") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                        selectedLabelColor = MaterialTheme.colorScheme.error
                    )
                )
                FilterChip(
                    selected = record.type == TransactionType.INCOME,
                    onClick = {
                        if (!record.saved) onUpdate(record.copy(
                            type = TransactionType.INCOME,
                            category = null
                        ))
                    },
                    label = { Text("收入") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = IOSColors.SystemGreen.copy(alpha = 0.2f),
                        selectedLabelColor = IOSColors.SystemGreen
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Amount field
            OutlinedTextField(
                value = record.amount,
                onValueChange = { input ->
                    if (!record.saved && (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$")))) {
                        onUpdate(record.copy(amount = input))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("金额") },
                prefix = { Text("¥") },
                singleLine = true,
                enabled = !record.saved
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Category selector
            Text(
                "分类",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            val categories = Category.getCategoriesByType(record.type)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(categories) { cat ->
                    FilterChip(
                        selected = record.category?.id == cat.id,
                        onClick = {
                            if (!record.saved) onUpdate(record.copy(category = cat))
                        },
                        label = { Text(cat.name, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.height(32.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Account selector
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !record.saved) { showAccountSelector = true },
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (selectedAccount != null)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = selectedAccount?.name ?: "选择账户",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (selectedAccount != null)
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Date selector
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !record.saved) { showDatePicker = true },
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                                .format(Date(record.date)),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Note field
            OutlinedTextField(
                value = record.note,
                onValueChange = { if (!record.saved) onUpdate(record.copy(note = it.take(80))) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("备注") },
                singleLine = true,
                enabled = !record.saved
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Save button
            if (!record.saved) {
                Button(
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = (record.amount.toDoubleOrNull() ?: 0.0) > 0
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("保存此条")
                }
            } else {
                Text(
                    "已保存",
                    style = MaterialTheme.typography.bodySmall,
                    color = IOSColors.SystemGreen,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = record.date
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            // Preserve time of day from original date, update only the date part
                            val origCal = Calendar.getInstance().apply { timeInMillis = record.date }
                            val newCal = Calendar.getInstance().apply { timeInMillis = millis }
                            newCal.set(Calendar.HOUR_OF_DAY, origCal.get(Calendar.HOUR_OF_DAY))
                            newCal.set(Calendar.MINUTE, origCal.get(Calendar.MINUTE))
                            newCal.set(Calendar.SECOND, origCal.get(Calendar.SECOND))
                            onUpdate(record.copy(date = newCal.timeInMillis))
                        }
                        showDatePicker = false
                    }
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Account Selector Dialog
    if (showAccountSelector) {
        val enabledAccounts = accounts.filter { !it.isDisabled }
        AlertDialog(
            onDismissRequest = { showAccountSelector = false },
            title = { Text("选择账户") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (enabledAccounts.isEmpty()) {
                        Text(
                            "暂无已启用的账户，请在\"账户\"管理中启用",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        enabledAccounts.forEach { account ->
                            ListItem(
                                headlineContent = { Text(account.name) },
                                supportingContent = {
                                    Text("余额: ¥${String.format("%.2f", account.currentBalance)}")
                                },
                                leadingContent = {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(parseAccountColor(account.color)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = getAccountTypeIcon(account.type),
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                },
                                trailingContent = {
                                    if (record.accountId == account.id) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                modifier = Modifier.clickable {
                                    onUpdate(record.copy(accountId = account.id))
                                    showAccountSelector = false
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAccountSelector = false }) { Text("关闭") }
            }
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────
// 辅助函数
// ──────────────────────────────────────────────────────────────────────────

private fun parseAccountColor(colorString: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorString))
    } catch (e: Exception) {
        Color(0xFF10B981)
    }
}

private fun getAccountTypeIcon(type: AccountType): androidx.compose.ui.graphics.vector.ImageVector {
    return when (type) {
        AccountType.BANK -> Icons.Default.AccountBalance
        AccountType.WECHAT -> Icons.Default.Chat
        AccountType.CASH -> Icons.Default.Wallet
        AccountType.YUEBAO -> Icons.Default.AccountBalanceWallet
        AccountType.OTHER -> Icons.Default.HelpOutline
        // 信用账户类型图标
        AccountType.CREDIT_CARD -> Icons.Default.CreditCard
        AccountType.CONSUMPTION_PLATFORM -> Icons.Default.ShoppingBag
        // 外部往来账户类型图标
        AccountType.PERSONAL_TRANSACTION -> Icons.Default.Person
        AccountType.LOAN_LIABILITY -> Icons.Default.AccountBalance
    }
}

/**
 * 从 ML Kit OCR 结果中提取所有带坐标的 TextElement
 * Phase 2: 视觉解析所需的核心数据提取
 */
private fun extractOcrElements(result: com.google.mlkit.vision.text.Text): List<ScreenshotTransactionParser.OcrElement> {
    val elements = mutableListOf<ScreenshotTransactionParser.OcrElement>()
    for (block in result.textBlocks) {
        for (line in block.lines) {
            for (element in line.elements) {
                element.boundingBox?.let { box ->
                    elements.add(ScreenshotTransactionParser.OcrElement(
                        text = element.text,
                        left = box.left, top = box.top,
                        right = box.right, bottom = box.bottom
                    ))
                }
            }
        }
    }
    return elements
}
