package com.tinyledger.app.ui.screens.reimbursement

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.tinyledger.app.domain.model.ReimbursementStatus
import com.tinyledger.app.ui.components.DeleteConfirmationDialog
import com.tinyledger.app.ui.viewmodel.ReimbursementViewModel
import com.tinyledger.app.util.CurrencyUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ReimbursementDetailScreen(
    transactionId: Long,
    onNavigateBack: () -> Unit = {},
    reimbursementViewModel: ReimbursementViewModel = hiltViewModel()
) {
    val uiState by reimbursementViewModel.uiState.collectAsState()
    // 从列表中找到当前交易
    val transaction = remember(uiState.pendingTransactions, uiState.reimbursedTransactions) {
        (uiState.pendingTransactions + uiState.reimbursedTransactions).find { it.id == transactionId }
    }
    val scrollState = rememberScrollState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showStatusConfirmDialog by remember { mutableStateOf(false) }
    var showImagePreview by remember { mutableStateOf(false) }
    var previewImageIndex by remember { mutableIntStateOf(0) }

    // 解析多图片路径（imagePath 以 "||" 分隔存储多张图片）
    val imageList = remember(transaction?.imagePath) {
        transaction?.imagePath?.takeIf { it.isNotBlank() }
            ?.split("||")?.filter { it.isNotBlank() } ?: emptyList()
    }

    // 账户下拉框状态
    var accountDropdownExpanded by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val currencySymbol = uiState.currencySymbol

    // 查找当前账户名称
    val currentAccount = remember(transaction, uiState.accounts) {
        transaction?.accountId?.let { accountId ->
            uiState.accounts.find { it.id == accountId }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "报销详情",
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
            if (transaction != null) {
                BottomActionBar(
                    status = transaction.reimbursementStatus,
                    onStatusToggle = { showStatusConfirmDialog = true },
                    onDelete = { showDeleteDialog = true }
                )
            }
        }
    ) { paddingValues ->
        if (transaction == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val statusColor = when (transaction.reimbursementStatus) {
                ReimbursementStatus.PENDING -> MaterialTheme.colorScheme.error
                ReimbursementStatus.REIMBURSED -> Color(0xFF2E7D32)
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            val statusText = when (transaction.reimbursementStatus) {
                ReimbursementStatus.PENDING -> "待报销"
                ReimbursementStatus.REIMBURSED -> "已报销"
                else -> "未标记"
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InfoRow(label = "支出时间", value = dateFormat.format(Date(transaction.date)))
                    InfoRow(label = "支出分类", value = transaction.category.name)
                    InfoRow(
                        label = "支出金额",
                        value = "$currencySymbol ${CurrencyUtils.formatAmount(kotlin.math.abs(transaction.amount))}",
                        valueColor = MaterialTheme.colorScheme.error
                    )
                    // 记账账户下拉框
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "记账账户",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        ExposedDropdownMenuBox(
                            expanded = accountDropdownExpanded,
                            onExpandedChange = { accountDropdownExpanded = it }
                        ) {
                            val accountDisplayText = remember(currentAccount) {
                                val last4 = currentAccount?.cardNumber?.takeIf { it.length >= 4 }?.takeLast(4)
                                if (last4 != null) "${currentAccount.name}-$last4" else (currentAccount?.name ?: "未指定")
                            }
                            TextField(
                                value = accountDisplayText,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountDropdownExpanded) },
                                modifier = Modifier.menuAnchor(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = accountDropdownExpanded,
                                onDismissRequest = { accountDropdownExpanded = false }
                            ) {
                                uiState.accounts.forEach { account ->
                                    val last4 = account.cardNumber?.takeIf { it.length >= 4 }?.takeLast(4)
                                    val display = if (last4 != null) "${account.name}-$last4" else account.name
                                    DropdownMenuItem(
                                        text = { Text(display) },
                                        onClick = {
                                            reimbursementViewModel.updateTransactionAccount(transactionId, account.id)
                                            accountDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    InfoRow(label = "备注", value = transaction.note ?: "无备注")
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "发票管理",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )

                    if (imageList.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            imageList.forEachIndexed { index, path ->
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                        .clickable {
                                            previewImageIndex = index
                                            showImagePreview = true
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = File(path),
                                        contentDescription = "发票图片${index + 1}",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                        Text(
                            text = "点击图片预览",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable { },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = "点击上传发票图片",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            title = "删除报销记录？",
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                reimbursementViewModel.removeReimbursementStatus(transactionId)
                showDeleteDialog = false
                onNavigateBack()
            }
        )
    }

    if (showStatusConfirmDialog && transaction != null) {
        val isPending = transaction.reimbursementStatus == ReimbursementStatus.PENDING
        DeleteConfirmationDialog(
            title = if (isPending) "确认已报销？" else "撤销已报销？",
            message = if (isPending) {
                "确认已报销？将自动生成对应报销回款收入记录。"
            } else {
                "撤销已报销？将删除对应报销回款收入记录。"
            },
            icon = if (isPending) Icons.Default.CheckCircle else Icons.Default.Replay,
            iconTint = if (isPending) Color(0xFF2E7D32) else Color(0xFFFF9500),
            confirmButtonColor = if (isPending) Color(0xFF2E7D32) else Color(0xFFFF9500),
            confirmButtonText = if (isPending) "确认已报销" else "确认撤销",
            onDismiss = { showStatusConfirmDialog = false },
            onConfirm = {
                if (isPending) {
                    reimbursementViewModel.markAsReimbursed(transactionId)
                } else {
                    reimbursementViewModel.markAsPending(transactionId)
                }
                showStatusConfirmDialog = false
            }
        )
    }

    // 图片全屏预览（支持多图滑动浏览+双指放大）
    if (showImagePreview && imageList.isNotEmpty()) {
        val scale = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }

        Dialog(
            onDismissRequest = { showImagePreview = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                if (imageList.size == 1) {
                    var imageScale by remember { mutableFloatStateOf(1f) }
                    var offsetX by remember { mutableFloatStateOf(0f) }
                    var offsetY by remember { mutableFloatStateOf(0f) }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    imageScale = (imageScale * zoom).coerceIn(1f, 5f)
                                    if (imageScale > 1f) {
                                        offsetX += pan.x
                                        offsetY += pan.y
                                    } else {
                                        offsetX = 0f
                                        offsetY = 0f
                                    }
                                }
                            }
                            .clickable { showImagePreview = false },
                        contentAlignment = Alignment.Center
                    ) {
                        // 柔润白色描边容器
                        Box(
                            modifier = Modifier
                                .fillMaxSize(0.92f)
                                .clip(RoundedCornerShape(20.dp))
                                .border(3.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = File(imageList[0]),
                                contentDescription = "大图预览",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(17.dp))
                                    .scale(scale.value)
                                    .graphicsLayer {
                                        scaleX = imageScale
                                        scaleY = imageScale
                                        translationX = offsetX
                                        translationY = offsetY
                                    },
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                } else {
                    val pagerState = rememberPagerState(
                        initialPage = previewImageIndex,
                        pageCount = { imageList.size }
                    )
                    // 每个页面的独立缩放状态
                    val perPageScale = remember { mutableStateMapOf<Int, Float>() }
                    val perPageOffsetX = remember { mutableStateMapOf<Int, Float>() }
                    val perPageOffsetY = remember { mutableStateMapOf<Int, Float>() }

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    val page = pagerState.currentPage
                                    val currentScale = perPageScale[page] ?: 1f
                                    val newScale = (currentScale * zoom).coerceIn(1f, 5f)
                                    perPageScale[page] = newScale
                                    if (newScale > 1f) {
                                        perPageOffsetX[page] = (perPageOffsetX[page] ?: 0f) + pan.x
                                        perPageOffsetY[page] = (perPageOffsetY[page] ?: 0f) + pan.y
                                    } else {
                                        perPageOffsetX[page] = 0f
                                        perPageOffsetY[page] = 0f
                                    }
                                }
                            }
                    ) { page ->
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            // 柔润白色描边容器
                            Box(
                                modifier = Modifier
                                    .fillMaxSize(0.92f)
                                    .clip(RoundedCornerShape(20.dp))
                                    .border(3.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(20.dp))
                                    .graphicsLayer {
                                        scaleX = perPageScale[page] ?: 1f
                                        scaleY = perPageScale[page] ?: 1f
                                        translationX = perPageOffsetX[page] ?: 0f
                                        translationY = perPageOffsetY[page] ?: 0f
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = File(imageList[page]),
                                    contentDescription = "大图预览${page + 1}",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(17.dp))
                                        .scale(scale.value),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }

                    Text(
                        text = "${pagerState.currentPage + 1} / ${imageList.size}",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 48.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                IconButton(
                    onClick = { showImagePreview = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = valueColor
            )
        )
    }
}

@Composable
private fun BottomActionBar(
    status: ReimbursementStatus,
    onStatusToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val isPending = status == ReimbursementStatus.PENDING
    val buttonText = if (isPending) "确认已报销" else "撤销为待报销"
    val buttonColor = if (isPending) Color(0xFF2E7D32) else Color(0xFFFF9500)

    Surface(
        tonalElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onStatusToggle,
                modifier = Modifier.weight(2f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Text(
                    text = buttonText,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Button(
                onClick = onDelete,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Text(
                    text = "删除",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}
