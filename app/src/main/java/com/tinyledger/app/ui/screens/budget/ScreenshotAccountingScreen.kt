package com.tinyledger.app.ui.screens.budget

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenshotAccountingScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var recognizedText by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var parsedAmount by remember { mutableStateOf("") }
    var parsedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var parsedNote by remember { mutableStateOf("") }
    var showResult by remember { mutableStateOf(false) }
    var saveSuccess by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isProcessing = true
            showResult = false
            saveSuccess = false
            try {
                val image = InputImage.fromFilePath(context, uri)
                val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
                recognizer.process(image)
                    .addOnSuccessListener { result ->
                        recognizedText = result.text
                        Log.d("ScreenshotOCR", "Recognized: ${result.text.take(200)}")
                        // Parse the recognized text
                        val parsed = parseScreenshotText(result.text)
                        parsedAmount = parsed.first
                        parsedType = parsed.second
                        parsedNote = parsed.third
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Instruction card
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
                        "支持微信、支付宝、银行等支付截图",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Select image button
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

            // Result card
            if (showResult) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("识别结果", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(16.dp))

                        // Type selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = parsedType == TransactionType.EXPENSE,
                                onClick = { parsedType = TransactionType.EXPENSE },
                                label = { Text("支出") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                                    selectedLabelColor = MaterialTheme.colorScheme.error
                                )
                            )
                            FilterChip(
                                selected = parsedType == TransactionType.INCOME,
                                onClick = { parsedType = TransactionType.INCOME },
                                label = { Text("收入") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = IOSColors.SystemGreen.copy(alpha = 0.2f),
                                    selectedLabelColor = IOSColors.SystemGreen
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Amount field
                        OutlinedTextField(
                            value = parsedAmount,
                            onValueChange = { input ->
                                if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                    parsedAmount = input
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("金额") },
                            prefix = { Text("¥") },
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Note field
                        OutlinedTextField(
                            value = parsedNote,
                            onValueChange = { parsedNote = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("备注") },
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Save button
                        Button(
                            onClick = {
                                val amount = parsedAmount.toDoubleOrNull()
                                if (amount != null && amount > 0) {
                                    val category = if (parsedType == TransactionType.INCOME)
                                        Category.fromId("redpacket", TransactionType.INCOME)
                                    else
                                        Category.fromId("other", TransactionType.EXPENSE)

                                    val transaction = Transaction(
                                        type = parsedType,
                                        category = category,
                                        amount = amount,
                                        note = parsedNote.ifBlank { "截屏记账" },
                                        date = System.currentTimeMillis()
                                    )
                                    viewModel.insertTransaction(transaction)
                                    saveSuccess = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            enabled = parsedAmount.toDoubleOrNull() != null && parsedAmount.toDoubleOrNull()!! > 0 && !saveSuccess
                        ) {
                            if (saveSuccess) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("已保存")
                            } else {
                                Icon(Icons.Default.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("保存记账")
                            }
                        }

                        if (saveSuccess) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "记账成功！",
                                style = MaterialTheme.typography.bodySmall,
                                color = IOSColors.SystemGreen
                            )
                        }
                    }
                }

                // Show recognized text for debugging
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("OCR识别文本", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
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

/**
 * Parse screenshot OCR text to extract amount, type, and note.
 * Returns Triple(amountString, transactionType, note)
 */
private fun parseScreenshotText(text: String): Triple<String, TransactionType, String> {
    var amount = ""
    var type = TransactionType.EXPENSE
    var note = ""

    // Determine type
    val incomeKeywords = listOf("收款", "到账", "转入", "收入", "退款", "红包")
    val expenseKeywords = listOf("付款", "支付", "消费", "扣款", "转出", "转账", "充值", "缴费")

    val isIncome = incomeKeywords.any { text.contains(it) }
    val isExpense = expenseKeywords.any { text.contains(it) }

    type = if (isIncome && !isExpense) TransactionType.INCOME else TransactionType.EXPENSE

    // 必须匹配 "数字+元" 模式，否则不认为是交易
    val yuanPattern = Regex("([0-9]+(?:\\.[0-9]{1,2})?)\\s*元")
    val yuanMatch = yuanPattern.find(text)
    if (yuanMatch != null) {
        val value = yuanMatch.groupValues[1].toDoubleOrNull()
        if (value != null && value > 0 && value < 10_000_000) {
            amount = String.format("%.2f", value)
        }
    }

    // Extract note / merchant
    val merchantPatterns = listOf(
        Regex("(?:商户|商家|收款方|付款方|店铺)[：:]*\\s*(.+?)(?:\\n|$)"),
        Regex("(?:备注|说明|用途)[：:]*\\s*(.+?)(?:\\n|$)")
    )
    for (pattern in merchantPatterns) {
        val match = pattern.find(text)
        if (match != null) {
            note = match.groupValues[1].trim().take(30)
            break
        }
    }

    if (note.isBlank()) {
        // Try to infer note from context
        when {
            text.contains("微信") -> note = "微信${if (type == TransactionType.INCOME) "收款" else "付款"}"
            text.contains("支付宝") -> note = "支付宝${if (type == TransactionType.INCOME) "收款" else "消费"}"
            text.contains("京东") -> note = "京东购物"
            text.contains("淘宝") || text.contains("天猫") -> note = "淘宝购物"
            text.contains("美团") -> note = "美团消费"
            text.contains("抖音") -> note = "抖音消费"
            else -> note = "截屏记账"
        }
    }

    return Triple(amount, type, note)
}
