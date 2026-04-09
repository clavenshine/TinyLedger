package com.tinyledger.app.ui.screens.budget

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.tinyledger.app.domain.model.Category
import com.tinyledger.app.domain.model.Transaction
import com.tinyledger.app.domain.model.TransactionType
import com.tinyledger.app.ui.theme.IOSColors
import com.tinyledger.app.ui.viewmodel.HomeViewModel

/**
 * 单条识别记录（可编辑）
 */
data class ParsedRecord(
    val amount: String,
    val type: TransactionType,
    val note: String,
    val saved: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenshotAccountingScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var recognizedText by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var parsedRecords by remember { mutableStateOf(listOf<ParsedRecord>()) }
    var showResult by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isProcessing = true
            showResult = false
            parsedRecords = emptyList()
            try {
                val image = InputImage.fromFilePath(context, uri)
                val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
                recognizer.process(image)
                    .addOnSuccessListener { result ->
                        recognizedText = result.text
                        Log.d("ScreenshotOCR", "Recognized: ${result.text.take(200)}")
                        parsedRecords = parseScreenshotTextMulti(result.text)
                        showResult = true
                        isProcessing = false
                    }
                    .addOnFailureListener { e ->
                        Log.e("ScreenshotOCR", "Recognition failed", e)
                        recognizedText = "识别失败: ${e.message}"
                        isProcessing = false
                    }
            } catch (e: Exception) {
                Log.e("ScreenshotOCR", "Error", e)
                recognizedText = "处理失败: ${e.message}"
                isProcessing = false
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("截屏记账", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "返回", modifier = Modifier.size(28.dp))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFFF5F5F5)
                )
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Instruction card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Screenshot,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "选择支付截图，自动识别金额",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                        )
                        Text(
                            "支持微信、支付宝、银行等支付截图，可识别多条记录",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Select image button
            item {
                Button(
                    onClick = { imagePicker.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isProcessing
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("正在识别...")
                    } else {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("选择截图")
                    }
                }
            }

            // Result cards - one per parsed record
            if (showResult && parsedRecords.isNotEmpty()) {
                item {
                    Text(
                        "识别到 ${parsedRecords.size} 条记录",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                itemsIndexed(parsedRecords) { index, record ->
                    RecordEditCard(
                        index = index,
                        record = record,
                        onUpdate = { updated ->
                            parsedRecords = parsedRecords.toMutableList().also { it[index] = updated }
                        },
                        onSave = {
                            val amount = record.amount.toDoubleOrNull()
                            if (amount != null && amount > 0) {
                                val category = if (record.type == TransactionType.INCOME)
                                    Category.fromId("redpacket", TransactionType.INCOME)
                                else
                                    Category.fromId("other", TransactionType.EXPENSE)

                                val transaction = Transaction(
                                    type = record.type,
                                    category = category,
                                    amount = amount,
                                    note = record.note.ifBlank { "截屏记账" },
                                    date = System.currentTimeMillis()
                                )
                                viewModel.insertTransaction(transaction)
                                parsedRecords = parsedRecords.toMutableList().also {
                                    it[index] = record.copy(saved = true)
                                }
                            }
                        },
                        onDelete = {
                            parsedRecords = parsedRecords.toMutableList().also { it.removeAt(index) }
                        }
                    )
                }

                // 一键全部保存
                val unsavedCount = parsedRecords.count { !it.saved && (it.amount.toDoubleOrNull() ?: 0.0) > 0 }
                if (unsavedCount > 0) {
                    item {
                        Button(
                            onClick = {
                                parsedRecords = parsedRecords.mapIndexed { idx, record ->
                                    val amount = record.amount.toDoubleOrNull()
                                    if (!record.saved && amount != null && amount > 0) {
                                        val category = if (record.type == TransactionType.INCOME)
                                            Category.fromId("redpacket", TransactionType.INCOME)
                                        else
                                            Category.fromId("other", TransactionType.EXPENSE)

                                        viewModel.insertTransaction(Transaction(
                                            type = record.type,
                                            category = category,
                                            amount = amount,
                                            note = record.note.ifBlank { "截屏记账" },
                                            date = System.currentTimeMillis()
                                        ))
                                        record.copy(saved = true)
                                    } else record
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = IOSColors.SystemGreen)
                        ) {
                            Icon(Icons.Default.SaveAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("全部保存 ($unsavedCount 条)")
                        }
                    }
                }
            }

            if (showResult && parsedRecords.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("未识别到有效金额记录", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "请确保截图中包含「XX元」格式的金额信息",
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
                        colors = CardDefaults.cardColors(containerColor = Color.White)
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

@Composable
private fun RecordEditCard(
    index: Int,
    record: ParsedRecord,
    onUpdate: (ParsedRecord) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (record.saved) IOSColors.SystemGreen.copy(alpha = 0.05f) else Color.White
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
                    onClick = { if (!record.saved) onUpdate(record.copy(type = TransactionType.EXPENSE)) },
                    label = { Text("支出") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                        selectedLabelColor = MaterialTheme.colorScheme.error
                    )
                )
                FilterChip(
                    selected = record.type == TransactionType.INCOME,
                    onClick = { if (!record.saved) onUpdate(record.copy(type = TransactionType.INCOME)) },
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
}

/**
 * Parse screenshot OCR text to extract multiple records.
 * Each occurrence of "XX元" pattern becomes a separate record.
 */
private fun parseScreenshotTextMulti(text: String): List<ParsedRecord> {
    val records = mutableListOf<ParsedRecord>()

    // Determine default type from overall text
    val incomeKeywords = listOf("收款", "到账", "转入", "收入", "退款", "红包")
    val expenseKeywords = listOf("付款", "支付", "消费", "扣款", "转出", "转账", "充值", "缴费")

    val isIncome = incomeKeywords.any { text.contains(it) }
    val isExpense = expenseKeywords.any { text.contains(it) }
    val defaultType = if (isIncome && !isExpense) TransactionType.INCOME else TransactionType.EXPENSE

    // Find all "数字+元" patterns
    val yuanPattern = Regex("([0-9]+(?:\\.[0-9]{1,2})?)\\s*元")
    val matches = yuanPattern.findAll(text).toList()

    if (matches.isEmpty()) return records

    // Deduplicate amounts that appear at very close positions (within 20 chars)
    val deduped = mutableListOf<MatchResult>()
    for (match in matches) {
        val value = match.groupValues[1].toDoubleOrNull() ?: continue
        if (value <= 0 || value >= 10_000_000) continue
        // Skip if too close to the last added match with the same amount
        val isDup = deduped.any { prev ->
            kotlin.math.abs(prev.range.first - match.range.first) < 20 &&
                    prev.groupValues[1] == match.groupValues[1]
        }
        if (!isDup) deduped.add(match)
    }

    // Build a record for each match
    for (match in deduped) {
        val value = match.groupValues[1].toDoubleOrNull() ?: continue
        val amountStr = String.format("%.2f", value)

        // Look at surrounding context (100 chars before) to determine type and note
        val contextStart = (match.range.first - 100).coerceAtLeast(0)
        val contextEnd = (match.range.last + 50).coerceAtMost(text.length)
        val context = text.substring(contextStart, contextEnd)

        val localIncome = incomeKeywords.any { context.contains(it) }
        val localExpense = expenseKeywords.any { context.contains(it) }
        val type = when {
            localIncome && !localExpense -> TransactionType.INCOME
            localExpense && !localIncome -> TransactionType.EXPENSE
            else -> defaultType
        }

        // Try to extract note from context
        var note = ""
        val merchantPatterns = listOf(
            Regex("(?:商户|商家|收款方|付款方|店铺)[：:]*\\s*(.+?)(?:\\n|$)"),
            Regex("(?:备注|说明|用途)[：:]*\\s*(.+?)(?:\\n|$)")
        )
        for (pattern in merchantPatterns) {
            val noteMatch = pattern.find(context)
            if (noteMatch != null) {
                note = noteMatch.groupValues[1].trim().take(30)
                break
            }
        }
        if (note.isBlank()) {
            when {
                context.contains("微信") -> note = "微信${if (type == TransactionType.INCOME) "收款" else "付款"}"
                context.contains("支付宝") -> note = "支付宝${if (type == TransactionType.INCOME) "收款" else "消费"}"
                context.contains("京东") -> note = "京东购物"
                context.contains("淘宝") || context.contains("天猫") -> note = "淘宝购物"
                context.contains("美团") -> note = "美团消费"
                context.contains("抖音") -> note = "抖音消费"
                else -> note = "截屏记账"
            }
        }

        records.add(ParsedRecord(amount = amountStr, type = type, note = note))
    }

    return records
}
