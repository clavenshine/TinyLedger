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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.tinyledger.app.domain.model.Account
import com.tinyledger.app.domain.model.AccountType
import com.tinyledger.app.domain.model.Category
import com.tinyledger.app.domain.model.Transaction
import com.tinyledger.app.domain.model.TransactionType
import com.tinyledger.app.ui.theme.IOSColors
import kotlinx.coroutines.launch
import com.tinyledger.app.ui.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.*

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
    val saved: Boolean = false
)

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
                        parsedRecords = parseScreenshotTextMulti(result.text, homeState.accounts)
                        showResult = true
                        isProcessing = false
                        if (parsedRecords.isNotEmpty()) {
                            val expenseCount = parsedRecords.count { it.type == TransactionType.EXPENSE }
                            val incomeCount = parsedRecords.count { it.type == TransactionType.INCOME }
                            val msg = buildString {
                                if (expenseCount > 0) append("识别到 $expenseCount 笔支出")
                                if (incomeCount > 0) {
                                    if (isNotEmpty()) append("，")
                                    append("识别到 $incomeCount 笔收入")
                                }
                                append("，已自动填充")
                            }
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
                            }
                        }
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

    val multiImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            isProcessing = true
            showResult = false
            parsedRecords = emptyList()
            var allRecords = mutableListOf<ParsedRecord>()
            var processed = 0
            uris.forEach { uri ->
                try {
                    val image = InputImage.fromFilePath(context, uri)
                    val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
                    recognizer.process(image)
                        .addOnSuccessListener { result ->
                            allRecords.addAll(parseScreenshotTextMulti(result.text, homeState.accounts))
                            processed++
                            if (processed == uris.size) {
                                parsedRecords = allRecords
                                showResult = true
                                isProcessing = false
                                if (allRecords.isNotEmpty()) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("识别到 ${allRecords.size} 条记录，已自动填充", duration = SnackbarDuration.Short)
                                    }
                                }
                            }
                        }
                        .addOnFailureListener {
                            processed++
                            if (processed == uris.size) {
                                parsedRecords = allRecords
                                showResult = true
                                isProcessing = false
                            }
                        }
                } catch (e: Exception) {
                    processed++
                }
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
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            modifier = Modifier.size(24.dp).padding(start = 10.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFFF5F5F5)
                )
            )
        },
        containerColor = Color(0xFFF5F5F5),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Instruction card - redesigned
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
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
                            color = Color(0xFF757575),
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
                            color = Color(0xFFF0F0F0)
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
                                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.7f))
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                if (label == "微信支付") Icons.Default.Chat else Icons.Default.Payment,
                                                contentDescription = null,
                                                tint = Color(0xFFBDBDBD),
                                                modifier = Modifier.size(32.dp)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                "${label}截图示例",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFFBDBDBD)
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
                        accounts = homeState.accounts,
                        onUpdate = { updated ->
                            parsedRecords = parsedRecords.toMutableList().also { it[index] = updated }
                        },
                        onSave = {
                            val amount = record.amount.toDoubleOrNull()
                            if (amount != null && amount > 0) {
                                val category = record.category
                                    ?: if (record.type == TransactionType.INCOME)
                                        Category.fromId("redpacket", TransactionType.INCOME)
                                    else
                                        Category.fromId("other", TransactionType.EXPENSE)

                                val transaction = Transaction(
                                    type = record.type,
                                    category = category,
                                    amount = amount,
                                    note = record.note.ifBlank { "截屏记账" },
                                    date = record.date,
                                    accountId = record.accountId
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
                                        val category = record.category
                                            ?: if (record.type == TransactionType.INCOME)
                                                Category.fromId("redpacket", TransactionType.INCOME)
                                            else
                                                Category.fromId("other", TransactionType.EXPENSE)

                                        viewModel.insertTransaction(Transaction(
                                            type = record.type,
                                            category = category,
                                            amount = amount,
                                            note = record.note.ifBlank { "截屏记账" },
                                            date = record.date,
                                            accountId = record.accountId
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
        AlertDialog(
            onDismissRequest = { showAccountSelector = false },
            title = { Text("选择账户") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (accounts.isEmpty()) {
                        Text(
                            "暂无账户，请在\"账户\"管理中添加",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        accounts.forEach { account ->
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
        AccountType.ALIPAY -> Icons.Default.Payment
        AccountType.CASH -> Icons.Default.Wallet
        AccountType.OTHER -> Icons.Default.AccountBalanceWallet
    }
}

private fun String.containsAny(vararg keywords: String): Boolean =
    keywords.any { this.contains(it) }

/**
 * 从OCR文本智能识别日期
 * 支持：2024-01-15, 2024/01/15, 2024年1月15日, 01-15, 1月15日, etc.
 */
private fun extractDateFromText(text: String): Long? {
    // Full date: 2024-01-15 or 2024/01/15
    val fullDatePattern = Regex("(20\\d{2})[/\\-](\\d{1,2})[/\\-](\\d{1,2})")
    fullDatePattern.find(text)?.let { match ->
        val year = match.groupValues[1].toIntOrNull() ?: return@let
        val month = match.groupValues[2].toIntOrNull() ?: return@let
        val day = match.groupValues[3].toIntOrNull() ?: return@let
        if (month in 1..12 && day in 1..31) {
            val cal = Calendar.getInstance()
            cal.set(year, month - 1, day)
            return cal.timeInMillis
        }
    }

    // Chinese date: 2024年1月15日
    val cnFullDatePattern = Regex("(20\\d{2})年(\\d{1,2})月(\\d{1,2})日")
    cnFullDatePattern.find(text)?.let { match ->
        val year = match.groupValues[1].toIntOrNull() ?: return@let
        val month = match.groupValues[2].toIntOrNull() ?: return@let
        val day = match.groupValues[3].toIntOrNull() ?: return@let
        if (month in 1..12 && day in 1..31) {
            val cal = Calendar.getInstance()
            cal.set(year, month - 1, day)
            return cal.timeInMillis
        }
    }

    // Short date: 1月15日 or 01-15 or 01/15 (assume current year)
    val cnShortDatePattern = Regex("(\\d{1,2})月(\\d{1,2})日")
    cnShortDatePattern.find(text)?.let { match ->
        val month = match.groupValues[1].toIntOrNull() ?: return@let
        val day = match.groupValues[2].toIntOrNull() ?: return@let
        if (month in 1..12 && day in 1..31) {
            val cal = Calendar.getInstance()
            cal.set(Calendar.MONTH, month - 1)
            cal.set(Calendar.DAY_OF_MONTH, day)
            return cal.timeInMillis
        }
    }

    val shortDatePattern = Regex("(\\d{2})[/\\-](\\d{2})\\s")
    shortDatePattern.find(text)?.let { match ->
        val month = match.groupValues[1].toIntOrNull() ?: return@let
        val day = match.groupValues[2].toIntOrNull() ?: return@let
        if (month in 1..12 && day in 1..31) {
            val cal = Calendar.getInstance()
            cal.set(Calendar.MONTH, month - 1)
            cal.set(Calendar.DAY_OF_MONTH, day)
            return cal.timeInMillis
        }
    }

    // Time pattern: HH:mm(:ss)
    val timePattern = Regex("(\\d{1,2}):(\\d{2})(?::(\\d{2}))?")
    timePattern.find(text)?.let { match ->
        val hour = match.groupValues[1].toIntOrNull() ?: return@let
        val minute = match.groupValues[2].toIntOrNull() ?: return@let
        if (hour in 0..23 && minute in 0..59) {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
            return cal.timeInMillis
        }
    }

    return null
}

/**
 * 从OCR文本智能识别分类（参考短信导入的智能识别模式）
 */
private fun inferCategoryFromOcrText(text: String, type: TransactionType): Category? {
    // 先尝试提取商户名
    val merchantPatterns = listOf(
        Regex("向(.+?)支付"),
        Regex("在(.+?)消费"),
        Regex("付款给(.+?)"),
        Regex("支付给(.+?)[,，。]"),
        Regex("(?:商户|商家|收款方|店铺)[：:]*\\s*(.+?)(?:\\n|$)")
    )
    var merchant: String? = null
    for (pattern in merchantPatterns) {
        val match = pattern.find(text)
        if (match != null) {
            merchant = match.groupValues[1].trim()
            break
        }
    }

    if (type == TransactionType.INCOME) {
        return when {
            text.containsAny("工资", "薪资", "薪酬", "代发", "发薪") -> Category.fromId("salary", TransactionType.INCOME)
            text.containsAny("奖金", "绩效", "年终奖") -> Category.fromId("bonus", TransactionType.INCOME)
            text.containsAny("分红", "股息") -> Category.fromId("dividend", TransactionType.INCOME)
            text.containsAny("退款", "退还", "返还", "退货") -> Category.fromId("refund", TransactionType.INCOME)
            text.containsAny("押金退", "退押金", "退保证金") -> Category.fromId("deposit_back", TransactionType.INCOME)
            text.containsAny("报销", "报销款") -> Category.fromId("reimbursement", TransactionType.INCOME)
            text.containsAny("红包") -> Category.fromId("redpacket", TransactionType.INCOME)
            text.containsAny("收回借款", "还款", "还钱", "归还") -> Category.fromId("recover_loan", TransactionType.INCOME)
            text.containsAny("投资", "理财", "收益", "利息", "赎回") -> Category.fromId("investment", TransactionType.INCOME)
            text.containsAny("转账", "汇款", "转入") -> Category.fromId("income_transfer", TransactionType.INCOME)
            else -> null
        }
    }

    // 支出 - 先匹配商户名
    if (merchant != null) {
        val merchantCategory = when {
            merchant.containsAny("酒店", "宾馆", "民宿", "旅馆", "客栈", "公寓",
                "如家", "汉庭", "全季", "亚朵", "希尔顿", "万豪", "洲际",
                "锦江", "华住", "7天", "七天") ->
                Category.fromId("accommodation", TransactionType.EXPENSE)
            merchant.containsAny("基金", "慈善", "天使", "公益", "红十字",
                "捐赠", "捐款", "希望工程", "壹基金") ->
                Category.fromId("charity", TransactionType.EXPENSE)
            merchant.containsAny("米线", "豆腐", "烧烤", "面馆", "饺子", "包子",
                "粉丝", "麻辣", "串串", "炸鸡", "鸡排", "牛肉", "羊肉",
                "拉面", "馄饨", "煲仔", "粥", "寿司", "料理", "火锅",
                "餐厅", "饭店", "小吃", "快餐", "食堂", "茶餐厅",
                "肯德基", "麦当劳", "海底捞", "瑞幸", "星巴克") ->
                Category.fromId("food", TransactionType.EXPENSE)
            merchant.containsAny("水果", "果", "蔬", "小卖部", "盒马", "零食",
                "百货", "超市", "商店", "便利店", "杂货", "菜市场",
                "沃尔玛", "永辉", "华润", "大润发", "物美",
                "全家", "711", "罗森", "美宜佳") ->
                Category.fromId("shopping", TransactionType.EXPENSE)
            else -> null
        }
        if (merchantCategory != null) return merchantCategory
    }

    // 支出 - 关键词匹配
    return when {
        text.containsAny("酒店", "宾馆", "民宿", "旅馆", "客栈", "住宿",
            "如家", "汉庭", "全季", "亚朵") -> Category.fromId("accommodation", TransactionType.EXPENSE)
        text.containsAny("慈善", "捐赠", "捐款", "公益", "红十字",
            "希望工程", "壹基金", "天使") -> Category.fromId("charity", TransactionType.EXPENSE)
        text.containsAny("发红包", "派发红包", "发出红包") -> Category.fromId("send_redpacket", TransactionType.EXPENSE)
        text.containsAny("餐", "饭", "食", "外卖", "美团", "饿了么", "肯德基", "麦当劳",
            "海底捞", "奶茶", "咖啡", "瑞幸", "星巴克", "烧烤", "火锅", "小吃",
            "早餐", "午餐", "晚餐", "食堂", "饮料", "甜品", "蛋糕",
            "米线", "豆腐", "面馆", "饺子", "包子", "拉面") -> Category.fromId("food", TransactionType.EXPENSE)
        text.containsAny("打车", "滴滴", "地铁", "公交", "高铁", "机票", "加油",
            "停车", "高速", "出租", "曹操", "首汽", "T3出行", "花小猪",
            "铁路", "12306", "航空", "ETC", "过路费", "充电桩") -> Category.fromId("transport", TransactionType.EXPENSE)
        text.containsAny("购物", "京东", "淘宝", "天猫", "超市", "商场", "拼多多",
            "唯品会", "苏宁", "当当", "亚马逊", "沃尔玛", "永辉",
            "便利店", "全家", "711", "罗森",
            "水果", "蔬", "小卖部", "盒马", "零食", "百货") -> Category.fromId("shopping", TransactionType.EXPENSE)
        text.containsAny("水费", "电费", "燃气", "天然气", "煤气", "暖气", "宽带",
            "网费", "物业费", "供暖", "电力", "自来水", "国网") -> Category.fromId("utilities", TransactionType.EXPENSE)
        text.containsAny("房贷", "按揭", "月供", "公积金", "住房贷款") -> Category.fromId("mortgage", TransactionType.EXPENSE)
        text.containsAny("信用卡还款", "还信用卡", "信用卡", "账单还款") -> Category.fromId("credit_card_repay", TransactionType.EXPENSE)
        text.containsAny("花呗", "借呗", "蚂蚁") -> Category.fromId("alipay_repay", TransactionType.EXPENSE)
        text.containsAny("京东白条", "白条") -> Category.fromId("jd_repay", TransactionType.EXPENSE)
        text.containsAny("抖音月付", "放心借") -> Category.fromId("douyin_repay", TransactionType.EXPENSE)
        text.containsAny("转账", "汇款", "转出") -> Category.fromId("account_transfer", TransactionType.EXPENSE)
        text.containsAny("娱乐", "电影", "游戏", "视频", "音乐", "KTV", "网吧",
            "直播", "充值", "会员") -> Category.fromId("entertainment", TransactionType.EXPENSE)
        text.containsAny("医院", "药店", "医疗", "诊所", "挂号", "门诊", "体检",
            "药房", "药品", "看病") -> Category.fromId("medical", TransactionType.EXPENSE)
        text.containsAny("房租", "物业", "租房") -> Category.fromId("housing", TransactionType.EXPENSE)
        text.containsAny("话费", "流量", "通讯", "中国移动", "中国联通", "中国电信",
            "充话费", "手机费") -> Category.fromId("communication", TransactionType.EXPENSE)
        text.containsAny("学费", "培训", "教育", "书", "课程", "网课",
            "学校", "考试", "辅导") -> Category.fromId("education", TransactionType.EXPENSE)
        text.containsAny("保险", "保费", "社保", "医保", "车险", "人寿") -> Category.fromId("insurance", TransactionType.EXPENSE)
        text.containsAny("旅游", "景区", "门票", "携程", "去哪儿", "飞猪", "途牛") -> Category.fromId("travel", TransactionType.EXPENSE)
        text.containsAny("投资", "理财", "基金", "股票", "证券", "期货") -> Category.fromId("investment_expense", TransactionType.EXPENSE)
        else -> null
    }
}

/**
 * 从OCR文本智能识别账户
 */
private fun inferAccountFromOcrText(text: String, accounts: List<Account>): Long? {
    if (accounts.isEmpty()) return null

    // 匹配银行卡尾号
    val cardPattern = Regex("尾号(\\d{4})")
    cardPattern.find(text)?.let { match ->
        val lastFour = match.groupValues[1]
        val matched = accounts.find { account ->
            account.cardNumber?.endsWith(lastFour) == true ||
                    account.cardNumber == lastFour
        }
        if (matched != null) return matched.id
    }

    // 匹配支付方式
    return when {
        text.containsAny("微信支付", "微信", "零钱", "微信红包") ->
            accounts.find { it.type == AccountType.WECHAT }?.id
        text.containsAny("支付宝", "余额宝", "花呗", "蚂蚁") ->
            accounts.find { it.type == AccountType.ALIPAY }?.id
        text.containsAny("银行", "储蓄卡", "借记卡", "信用卡", "工商", "建设",
            "农业", "中国银行", "招商", "交通", "邮储", "民生",
            "光大", "兴业", "浦发", "中信") ->
            accounts.filter { it.type == AccountType.BANK }.let { bankAccounts ->
                // Try to match specific bank
                bankAccounts.find { account ->
                    text.contains(account.name.take(4))
                } ?: bankAccounts.firstOrNull()
            }?.id
        else -> accounts.firstOrNull()?.id
    }
}

/**
 * Parse screenshot OCR text to extract multiple records.
 * Each occurrence of "XX元" pattern becomes a separate record.
 * Now with smart recognition for date, category, and account.
 */
private fun parseScreenshotTextMulti(text: String, accounts: List<Account>): List<ParsedRecord> {
    val records = mutableListOf<ParsedRecord>()

    // Determine default type from overall text
    val incomeKeywords = listOf("收款", "到账", "转入", "收入", "退款", "红包")
    val expenseKeywords = listOf("付款", "支付", "消费", "扣款", "转出", "转账", "充值", "缴费")

    val isIncome = incomeKeywords.any { text.contains(it) }
    val isExpense = expenseKeywords.any { text.contains(it) }
    val defaultType = if (isIncome && !isExpense) TransactionType.INCOME else TransactionType.EXPENSE

    // Extract global date from text
    val globalDate = extractDateFromText(text) ?: System.currentTimeMillis()

    // Extract global account
    val globalAccountId = inferAccountFromOcrText(text, accounts)

    // Find all "数字+元" patterns
    val yuanPattern = Regex("([0-9]+(?:\\.[0-9]{1,2})?)\\s*元")
    val matches = yuanPattern.findAll(text).toList()

    if (matches.isEmpty()) return records

    // Deduplicate amounts that appear at very close positions (within 20 chars)
    val deduped = mutableListOf<MatchResult>()
    for (match in matches) {
        val value = match.groupValues[1].toDoubleOrNull() ?: continue
        if (value <= 0 || value >= 10_000_000) continue
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

        // Look at surrounding context (100 chars before, 50 after)
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

        // Smart category inference from context
        val category = inferCategoryFromOcrText(context, type)

        // Try to extract date from local context, fall back to global
        val localDate = extractDateFromText(context) ?: globalDate

        // Smart account inference from context, fall back to global
        val localAccountId = inferAccountFromOcrText(context, accounts) ?: globalAccountId

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

        records.add(ParsedRecord(
            amount = amountStr,
            type = type,
            note = note,
            category = category,
            accountId = localAccountId,
            date = localDate
        ))
    }

    return records
}
