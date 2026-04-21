package com.tinyledger.app.ui.screens.detail

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.tinyledger.app.domain.model.Transaction
import com.tinyledger.app.domain.model.TransactionType
import com.tinyledger.app.ui.components.DeleteConfirmationDialog
import com.tinyledger.app.ui.components.getCategoryIcon
import com.tinyledger.app.ui.theme.ExpenseRed
import com.tinyledger.app.ui.theme.IncomeGreen
import com.tinyledger.app.ui.viewmodel.TransactionDetailViewModel
import com.tinyledger.app.util.CurrencyUtils
import com.tinyledger.app.util.DateUtils
import com.tinyledger.app.util.SoundFeedbackManager
import androidx.hilt.navigation.compose.hiltViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    onNavigateBack: () -> Unit,
    onEditTransaction: (Long) -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    viewModel: TransactionDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    if (uiState.isLoading) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("加载中...") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        return
    }
    
    if (uiState.error != null || uiState.transaction == null) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("错误") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.error ?: "交易记录不存在",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }
    
    TransactionDetailContent(
        transaction = uiState.transaction!!,
        accountName = uiState.accountName,
        onNavigateBack = onNavigateBack,
        onEditTransaction = onEditTransaction,
        onDeleteTransaction = onDeleteTransaction
    )
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun TransactionDetailContent(
    transaction: Transaction,
    accountName: String?,
    onNavigateBack: () -> Unit,
    onEditTransaction: (Long) -> Unit,
    onDeleteTransaction: (Long) -> Unit
) {
    
    val context = androidx.compose.ui.platform.LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showImagePreview by remember { mutableStateOf(false) }
    var previewImageIndex by remember { mutableIntStateOf(0) }
    
    // 解析多图片路径（imagePath 以 "||" 分隔存储多张图片）
    val imageList = remember(transaction.imagePath) {
        if (transaction.imagePath.isNullOrBlank()) emptyList()
        else transaction.imagePath.split("||").filter { it.isNotBlank() }
    }
    
    val transactionType = transaction.type
    val titleText = when (transactionType) {
        TransactionType.EXPENSE -> "支出详情"
        TransactionType.INCOME -> "收入详情"
        TransactionType.TRANSFER -> "转账详情"
        TransactionType.LENDING -> "借贷详情"
    }
    
    val amountColor = when (transactionType) {
        TransactionType.EXPENSE -> ExpenseRed
        TransactionType.INCOME -> IncomeGreen
        else -> if (transaction.amount > 0) IncomeGreen else ExpenseRed
    }
    val amountPrefix = when (transactionType) {
        TransactionType.EXPENSE -> "-"
        TransactionType.INCOME -> "+"
        else -> if (transaction.amount > 0) "+" else "-"
    }
    val displayAmount = kotlin.math.abs(transaction.amount)
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            // 底部按钮栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 删除按钮
                Button(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF5B7FD6)
                    )
                ) {
                    Text(
                        text = "删除",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
                
                // 编辑按钮
                Button(
                    onClick = { onEditTransaction(transaction.id) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0D1117)
                    )
                ) {
                    Text(
                        text = "编辑",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            // 分类金额卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 分类图标
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getCategoryIcon(transaction.category.icon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // 分类名称
                    Text(
                        text = transaction.category.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // 金额
                    Text(
                        text = "$amountPrefix ${CurrencyUtils.format(displayAmount, "¥")}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = amountColor
                    )
                }
            }
            
            // 详细信息卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // 时间
                    DetailRow(
                        label = "时间",
                        value = DateUtils.formatDetailDateTime(transaction.date)
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
                    
                    // 账户
                    DetailRow(
                        label = "账户",
                        value = accountName ?: "不计入账户"
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
                    
                    // 预算
                    DetailRow(
                        label = "预算",
                        value = "计入预算"
                    )
                }
            }
            
            // 备注卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Text(
                    text = if (transaction.note.isNullOrBlank()) "无备注" else transaction.note,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (transaction.note.isNullOrBlank()) 
                        MaterialTheme.colorScheme.onSurfaceVariant 
                    else 
                        MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(20.dp)
                )
            }
            
            // 图片缩略图卡片
            if (imageList.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        imageList.forEachIndexed { index, path ->
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .clickable {
                                        previewImageIndex = index
                                        showImagePreview = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = File(path),
                                    contentDescription = "交易图片${index + 1}",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }
            
            // 记录方式
            Text(
                text = "记录方式：手动记账",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(80.dp)) // 为底部按钮留出空间
        }
    }
    
    // 删除确认对话框
    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            title = "删除账单记录？",
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                // 播放删除成功音效和震动
                SoundFeedbackManager.onDeleted(context)
                onDeleteTransaction(transaction.id)
                showDeleteDialog = false
                onNavigateBack()
            }
        )
    }
    
    // 图片全屏预览（支持多图滑动浏览）
    if (showImagePreview && imageList.isNotEmpty()) {
        Dialog(onDismissRequest = { showImagePreview = false }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                if (imageList.size == 1) {
                    // 单图：点击关闭
                    AsyncImage(
                        model = File(imageList[0]),
                        contentDescription = "大图预览",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showImagePreview = false },
                        contentScale = ContentScale.Fit
                    )
                } else {
                    // 多图：HorizontalPager 浏览
                    val pagerState = rememberPagerState(
                        initialPage = previewImageIndex,
                        pageCount = { imageList.size }
                    )
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        AsyncImage(
                            model = File(imageList[page]),
                            contentDescription = "大图预览${page + 1}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showImagePreview = false },
                            contentScale = ContentScale.Fit
                        )
                    }
                    // 页码指示器
                    Text(
                        text = "${pagerState.currentPage + 1} / ${imageList.size}",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 48.dp)
                    )
                }
                // 关闭按钮
                IconButton(
                    onClick = { showImagePreview = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
