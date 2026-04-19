package com.tinyledger.app.ui.screens.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.tinyledger.app.data.notification.TransactionNotificationService
import com.tinyledger.app.domain.model.Account
import com.tinyledger.app.domain.model.AccountType
import com.tinyledger.app.domain.model.TransactionType
import com.tinyledger.app.ui.components.CategorySelector
import com.tinyledger.app.ui.viewmodel.AddTransactionViewModel
import com.tinyledger.app.ui.viewmodel.LendingSubType
import kotlinx.coroutines.flow.drop
import com.tinyledger.app.util.DateUtils
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAccounts: () -> Unit = {},
    onNavigateToCategoryManage: () -> Unit = {},
    transactionId: Long? = null,
    initialCreditAccountId: Long? = null,
    initialSelectedAccountId: Long? = null,
    initialTransactionType: TransactionType? = null,
    initialFromAccountId: Long? = null,
    viewModel: AddTransactionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }
    var showAccountSelector by remember { mutableStateOf(false) }
    var showFromAccountSelector by remember { mutableStateOf(false) }
    var showToAccountSelector by remember { mutableStateOf(false) }
    var showNoAccountDialog by remember { mutableStateOf(false) }
    var showImagePicker by remember { mutableStateOf(false) }
    var imagePickerOption by remember { mutableStateOf("") } // "camera" or "gallery"
    var showPermissionDialog by remember { mutableStateOf(false) }
    var permissionType by remember { mutableStateOf("") } // "camera" or "storage"

    // 相机启动器
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                // 根据交易类型决定最大图片数量
                val maxImages = if (uiState.transactionType == TransactionType.LENDING) 9 else 3
                viewModel.addImage("camera_${System.currentTimeMillis()}.jpg", maxImages)
            }
            showImagePicker = false
        }
    )

    // 相册启动器
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                // 根据交易类型决定最大图片数量
                val maxImages = if (uiState.transactionType == TransactionType.LENDING) 9 else 3
                viewModel.addImage(it.toString(), maxImages)
            }
            showImagePicker = false
        }
    )

    // 权限请求启动器
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                // 权限已授予，执行相应操作
                if (permissionType == "camera") {
                    cameraLauncher.launch(
                        androidx.core.content.FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            java.io.File(context.cacheDir, "photo_${System.currentTimeMillis()}.jpg")
                        )
                    )
                } else if (permissionType == "storage") {
                    galleryLauncher.launch("image/*")
                }
            } else {
                // 权限被拒绝，显示提示
                showPermissionDialog = true
            }
        }
    )

    // Read vibration setting from SharedPreferences
    val vibrationEnabled = remember {
        TransactionNotificationService.isVibrationEnabled(context)
    }

    // Feature 3: Save button animation state
    var isSaveAnimating by remember { mutableStateOf(false) }

    LaunchedEffect(transactionId) {
        if (transactionId != null && transactionId > 0) {
            viewModel.loadTransaction(transactionId)
        }
    }

    // Trigger credit repayment mode when navigating from credit account
    LaunchedEffect(initialCreditAccountId) {
        if (initialCreditAccountId != null && initialCreditAccountId > 0) {
            viewModel.setCreditRepayModeById(initialCreditAccountId)
        }
    }
    
    // Handle initial parameters from AccountDetail
    LaunchedEffect(initialSelectedAccountId, initialTransactionType, initialFromAccountId, uiState.accounts) {
        if (initialTransactionType != null && uiState.accounts.isNotEmpty()) {
            // 设置交易类型
            viewModel.setTransactionType(initialTransactionType)
            
            // 设置账户
            if (initialFromAccountId != null) {
                // 转账模式：设置转出账户
                val fromAccount = uiState.accounts.find { it.id == initialFromAccountId }
                if (fromAccount != null) {
                    viewModel.selectFromAccount(fromAccount)
                }
            } else if (initialSelectedAccountId != null) {
                // 支出/收入模式：设置默认账户
                val account = uiState.accounts.find { it.id == initialSelectedAccountId }
                if (account != null) {
                    viewModel.selectAccount(account)
                }
            }
        }
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isEditing) "编辑记账" else "添加记账",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Transaction Type Selector - 4 types
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.transactionType == TransactionType.EXPENSE,
                        onClick = { viewModel.setTransactionType(TransactionType.EXPENSE) },
                        label = { Text("支出", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                            selectedLabelColor = MaterialTheme.colorScheme.error
                        )
                    )
                    FilterChip(
                        selected = uiState.transactionType == TransactionType.INCOME,
                        onClick = { viewModel.setTransactionType(TransactionType.INCOME) },
                        label = { Text("收入", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    FilterChip(
                        selected = uiState.transactionType == TransactionType.TRANSFER,
                        onClick = { viewModel.setTransactionType(TransactionType.TRANSFER) },
                        label = { Text("转账", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFF9800).copy(alpha = 0.2f),
                            selectedLabelColor = Color(0xFFFF9800)
                        )
                    )
                    FilterChip(
                        selected = uiState.transactionType == TransactionType.LENDING,
                        onClick = { viewModel.setTransactionType(TransactionType.LENDING) },
                        label = { Text("借贷", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF9C27B0).copy(alpha = 0.2f),
                            selectedLabelColor = Color(0xFF9C27B0)
                        )
                    )
                }
            }

            // Amount Input with dynamic zoom effect (Feature 1: font size auto-enlarges when typing)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Dynamic font size: grows from 28sp to 36sp as user types, animated smoothly
                    val targetFontSize = if (uiState.amount.isNotEmpty()) 36f else 28f
                    val animatedFontSize by animateFloatAsState(
                        targetValue = targetFontSize,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "amount_font_size"
                    )

                    OutlinedTextField(
                        value = uiState.amount,
                        onValueChange = { viewModel.setAmount(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("0.00", fontSize = 28.sp, fontWeight = FontWeight.Bold) },
                        prefix = { Text(uiState.currencySymbol, fontSize = animatedFontSize.sp, fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = animatedFontSize.sp
                        )
                    )
                }
            }

            // Account & Date Selector on same row (for EXPENSE/INCOME) - moved right after Amount
            if (uiState.transactionType == TransactionType.EXPENSE || uiState.transactionType == TransactionType.INCOME) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Account Selector
                    Card(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.refreshAccounts()
                                    if (uiState.accounts.isEmpty()) showNoAccountDialog = true else showAccountSelector = true
                                }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AccountSelectorRow(
                                account = uiState.selectedAccount,
                                placeholder = "选择账户"
                            )
                        }
                    }
                    // Date Selector
                    Card(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { showDatePicker = true }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = "选择日期",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                        .format(Date(uiState.date)),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Date Selector (for TRANSFER/LENDING - full width)
            if (uiState.transactionType == TransactionType.TRANSFER || uiState.transactionType == TransactionType.LENDING) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "选择日期",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                    .format(Date(uiState.date)),
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp)
                            )
                        }
                    }
                }
            }

            // Lending SubType Selector (only for LENDING) - redesigned to match category style
            if (uiState.transactionType == TransactionType.LENDING) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        LendingTypeButton(
                            icon = Icons.Default.SouthWest,
                            label = "借入",
                            isSelected = uiState.lendingSubType == LendingSubType.BORROW_IN,
                            selectedColor = Color(0xFF9C27B0),
                            onClick = { viewModel.setLendingSubType(LendingSubType.BORROW_IN) }
                        )
                        LendingTypeButton(
                            icon = Icons.Default.NorthEast,
                            label = "借出",
                            isSelected = uiState.lendingSubType == LendingSubType.BORROW_OUT,
                            selectedColor = Color(0xFF9C27B0),
                            onClick = { viewModel.setLendingSubType(LendingSubType.BORROW_OUT) }
                        )
                        LendingTypeButton(
                            icon = Icons.Default.CreditCardOff,
                            label = "还款",
                            isSelected = uiState.lendingSubType == LendingSubType.REPAY,
                            selectedColor = Color(0xFF9C27B0),
                            onClick = { viewModel.setLendingSubType(LendingSubType.REPAY) }
                        )
                        LendingTypeButton(
                            icon = Icons.Default.CreditScore,
                            label = "收款",
                            isSelected = uiState.lendingSubType == LendingSubType.COLLECT,
                            selectedColor = Color(0xFF9C27B0),
                            onClick = { viewModel.setLendingSubType(LendingSubType.COLLECT) }
                        )
                    }
                }
            }

            // Category Selector (only for EXPENSE/INCOME)
            if (uiState.transactionType == TransactionType.EXPENSE || uiState.transactionType == TransactionType.INCOME) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        CategorySelector(
                            categories = uiState.categories,
                            selectedCategory = uiState.selectedCategory,
                            onCategorySelected = { viewModel.selectCategory(it) },
                            onAddCategory = { name -> viewModel.addCategory(name) },
                            onDeleteCategory = { category -> viewModel.deleteCategory(category) },
                            onRenameCategory = { category, newName -> viewModel.renameCategory(category, newName) },
                            showAddButton = true,
                            transactionType = uiState.transactionType,
                            vibrationEnabled = vibrationEnabled,
                            onNavigateToCategoryManage = onNavigateToCategoryManage
                        )
                    }
                }
            }

            // From/To Account Selector for TRANSFER
            if (uiState.transactionType == TransactionType.TRANSFER) {
                // From Account (转出账户)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "转出账户", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        AccountSelectorRow(
                            account = uiState.selectedFromAccount,
                            placeholder = "请选择转出账户",
                            onClick = {
                                viewModel.refreshAccounts()
                                if (uiState.accounts.isEmpty()) showNoAccountDialog = true else showFromAccountSelector = true
                            }
                        )
                    }
                }
                // To Account (转入账户)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "转入账户", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        AccountSelectorRow(
                            account = uiState.selectedToAccount,
                            placeholder = "请选择转入账户",
                            onClick = {
                                viewModel.refreshAccounts()
                                if (uiState.accounts.isEmpty()) showNoAccountDialog = true else showToAccountSelector = true
                            }
                        )
                    }
                }
            }

            // From/To Account Selector for LENDING
            if (uiState.transactionType == TransactionType.LENDING) {
                val fromLabel = when (uiState.lendingSubType) {
                    LendingSubType.BORROW_IN -> "负债账户"
                    LendingSubType.BORROW_OUT -> "出账账户"
                    LendingSubType.REPAY -> "出账账户"
                    LendingSubType.COLLECT -> "债权账户"
                }
                val toLabel = when (uiState.lendingSubType) {
                    LendingSubType.BORROW_IN -> "入账账户"
                    LendingSubType.BORROW_OUT -> "债权账户"
                    LendingSubType.REPAY -> "负债账户"
                    LendingSubType.COLLECT -> "入账账户"
                }
                // From Account
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = fromLabel, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        AccountSelectorRow(
                            account = uiState.selectedFromAccount,
                            placeholder = "请选择${fromLabel}",
                            onClick = {
                                viewModel.refreshAccounts()
                                if (uiState.accounts.isEmpty()) showNoAccountDialog = true else showFromAccountSelector = true
                            }
                        )
                    }
                }
                // To Account
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = toLabel, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        AccountSelectorRow(
                            account = uiState.selectedToAccount,
                            placeholder = "请选择${toLabel}",
                            onClick = {
                                viewModel.refreshAccounts()
                                if (uiState.accounts.isEmpty()) showNoAccountDialog = true else showToAccountSelector = true
                            }
                        )
                    }
                }
            }

            // 添加相册卡片
            if (uiState.transactionType == TransactionType.EXPENSE || 
                uiState.transactionType == TransactionType.INCOME || 
                uiState.transactionType == TransactionType.LENDING) {
                
                // 借贷类型最多9张，其他类型最多3张
                val maxImages = if (uiState.transactionType == TransactionType.LENDING) 9 else 3
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "添加相册",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // 显示已添加的图片
                        if (uiState.imagePaths.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                uiState.imagePaths.forEachIndexed { index, imagePath ->
                                    Card(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clickable { viewModel.removeImage(index) },
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                        )
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize()) {
                                            Icon(
                                                imageVector = Icons.Default.Image,
                                                contentDescription = "图片$index",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .align(Alignment.Center)
                                            )
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "删除图片",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .align(Alignment.TopEnd)
                                                    .padding(4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // 添加图片按钮
                        if (uiState.imagePaths.size < maxImages) {
                            Card(
                                modifier = Modifier
                                    .clickable { showImagePicker = true },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddPhotoAlternate,
                                        contentDescription = "添加图片",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "添加图片 (${uiState.imagePaths.size}/$maxImages)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Note Input - Feature 3: Floating borderless input with gradient border on focus
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "备注",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val noteInteractionSource = remember { MutableInteractionSource() }
                    val isNoteFocused by noteInteractionSource.collectIsFocusedAsState()

                    // Gradient border animation
                    val borderAlpha by animateFloatAsState(
                        targetValue = if (isNoteFocused) 1f else 0f,
                        animationSpec = tween(400),
                        label = "note_border_alpha"
                    )
                    val gradientColors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary,
                        MaterialTheme.colorScheme.secondary
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (borderAlpha > 0f) {
                                    Modifier.border(
                                        width = 1.5.dp,
                                        brush = Brush.linearGradient(
                                            colors = gradientColors.map { it.copy(alpha = borderAlpha) }
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                } else Modifier
                            )
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        BasicTextField(
                            value = uiState.note,
                            onValueChange = { viewModel.setNote(it) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            maxLines = 3,
                            interactionSource = noteInteractionSource,
                            decorationBox = { innerTextField ->
                                if (uiState.note.isEmpty()) {
                                    Text(
                                        "添加备注...",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                }
            }

            // Error Message
            uiState.errorMessage?.let { error ->
                // 期初余额日期校验错误用美化弹窗
                if (error == "存在期初余额日期之前的记录，请核实") {
                    // 由下方弹窗处理
                } else {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Save Button - Feature 3: Color deepens on press with brief loading animation
            val saveButtonColor by animateColorAsState(
                targetValue = if (isSaveAnimating || uiState.isSaving)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                else
                    MaterialTheme.colorScheme.primary,
                animationSpec = tween(300),
                label = "save_button_color"
            )
            val saveButtonScale by animateFloatAsState(
                targetValue = if (isSaveAnimating) 0.96f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "save_button_scale"
            )

            Button(
                onClick = {
                    isSaveAnimating = true
                    viewModel.saveTransaction()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .scale(saveButtonScale),
                enabled = !uiState.isSaving && !isSaveAnimating,
                colors = ButtonDefaults.buttonColors(
                    containerColor = saveButtonColor
                )
            ) {
                if (uiState.isSaving || isSaveAnimating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "保存中...",
                        style = MaterialTheme.typography.titleMedium
                    )
                } else {
                    Text(
                        text = "保存",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            // Reset save animation after a brief delay if save didn't trigger navigation
            LaunchedEffect(isSaveAnimating) {
                if (isSaveAnimating) {
                    delay(1500)
                    isSaveAnimating = false
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Date Picker Bottom Sheet
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.date
        )
        // Monitor for actual date changes from user interaction
        LaunchedEffect(Unit) {
            snapshotFlow { datePickerState.selectedDateMillis }
                .drop(1)  // Skip initial value to avoid race condition on first click
                .collect { selectedMillis ->
                    if (selectedMillis != null && selectedMillis != uiState.date) {
                        viewModel.setDate(selectedMillis)
                        showDatePicker = false
                    }
                }
        }
        ModalBottomSheet(
            onDismissRequest = { showDatePicker = false },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            windowInsets = WindowInsets(0, 0, 0, 0),
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = null // 去掉拖拽指示器
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
            ) {
                DatePicker(
                    state = datePickerState,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }

    // Account Selector Bottom Sheet
    if (showAccountSelector) {
        AccountSelectorSheet(
            title = "选择账户",
            accounts = uiState.accounts,
            selectedAccountId = uiState.selectedAccount?.id,
            onSelect = { account ->
                viewModel.selectAccount(account)
                showAccountSelector = false
            },
            onDismiss = { showAccountSelector = false }
        )
    }

    // From Account Selector Bottom Sheet
    if (showFromAccountSelector) {
        val dialogTitle = when (uiState.transactionType) {
            TransactionType.TRANSFER -> "选择转出账户"
            TransactionType.LENDING -> when (uiState.lendingSubType) {
                LendingSubType.BORROW_IN -> "选择负债账户"
                LendingSubType.BORROW_OUT -> "选择出账账户"
                LendingSubType.REPAY -> "选择出账账户"
                LendingSubType.COLLECT -> "选择债权账户"
            }
            else -> "选择账户"
        }
        AccountSelectorSheet(
            title = dialogTitle,
            accounts = uiState.accounts,
            selectedAccountId = uiState.selectedFromAccount?.id,
            onSelect = { account ->
                viewModel.selectFromAccount(account)
                showFromAccountSelector = false
            },
            onDismiss = { showFromAccountSelector = false }
        )
    }

    // To Account Selector Bottom Sheet
    if (showToAccountSelector) {
        val dialogTitle = when (uiState.transactionType) {
            TransactionType.TRANSFER -> "选择转入账户"
            TransactionType.LENDING -> when (uiState.lendingSubType) {
                LendingSubType.BORROW_IN -> "选择入账账户"
                LendingSubType.BORROW_OUT -> "选择债权账户"
                LendingSubType.REPAY -> "选择负债账户"
                LendingSubType.COLLECT -> "选择入账账户"
            }
            else -> "选择账户"
        }
        AccountSelectorSheet(
            title = dialogTitle,
            accounts = uiState.accounts,
            selectedAccountId = uiState.selectedToAccount?.id,
            onSelect = { account ->
                viewModel.selectToAccount(account)
                showToAccountSelector = false
            },
            onDismiss = { showToAccountSelector = false }
        )
    }

    // 未添加账户提示对话框 - 精美卡片样式
    if (showNoAccountDialog) {
        Dialog(onDismissRequest = { showNoAccountDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // 图标
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // 标题
                    Text(
                        text = "账户未建立",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    // 说明文字
                    Text(
                        text = "记账前需要先建立账户\n您可以在账户管理中添加银行卡、微信、支付宝等",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 确认按钮
                    Button(
                        onClick = {
                            showNoAccountDialog = false
                            onNavigateToAccounts()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "去建立账户",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // 取消按钮
                    TextButton(
                        onClick = { showNoAccountDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "返回",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // 图片选择器弹窗
    if (showImagePicker) {
        Dialog(onDismissRequest = { showImagePicker = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "添加图片",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    // 拍照按钮
                    Button(
                        onClick = {
                            // 检查相机权限
                            val hasCameraPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED

                            if (hasCameraPermission) {
                                // 已有权限，直接打开相机
                                val photoFile = java.io.File(context.cacheDir, "photo_${System.currentTimeMillis()}.jpg")
                                val photoUri = androidx.core.content.FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    photoFile
                                )
                                cameraLauncher.launch(photoUri)
                            } else {
                                // 请求权限
                                permissionType = "camera"
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("拍照", style = MaterialTheme.typography.titleMedium)
                    }
                    
                    // 相册按钮
                    Button(
                        onClick = {
                            // 检查存储权限
                            val hasStoragePermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.READ_EXTERNAL_STORAGE
                            ) == PackageManager.PERMISSION_GRANTED

                            if (hasStoragePermission) {
                                // 已有权限，直接打开相册
                                galleryLauncher.launch("image/*")
                            } else {
                                // 请求权限
                                permissionType = "storage"
                                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("从相册选择", style = MaterialTheme.typography.titleMedium)
                    }
                    
                    // 取消按钮
                    TextButton(
                        onClick = { showImagePicker = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "取消",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // 权限被拒绝提示对话框
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("需要权限") },
            text = { 
                Text(
                    if (permissionType == "camera") 
                        "需要相机权限才能拍照，请在设置中授权"
                    else 
                        "需要存储权限才能访问相册，请在设置中授权"
                ) 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionDialog = false
                        // 打开应用设置页面
                        val intent = android.content.Intent(
                            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            android.net.Uri.fromParts("package", context.packageName, null)
                        )
                        context.startActivity(intent)
                    }
                ) { Text("去设置") }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) { Text("取消") }
            }
        )
    }

    // 期初余额日期校验弹窗 - 美化设计
    if (uiState.errorMessage == "存在期初余额日期之前的记录，请核实") {
        Dialog(
            onDismissRequest = { viewModel.clearError() },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                // 半透明遮罩
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { viewModel.clearError() }
                        )
                )
                // 对话框容器
                Box(
                    modifier = Modifier.fillMaxWidth(0.85f),
                    contentAlignment = Alignment.TopCenter
                ) {
                    // 主卡片
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 36.dp)
                            .shadow(
                                elevation = 24.dp,
                                shape = RoundedCornerShape(24.dp),
                                spotColor = Color(0xFFFF9800).copy(alpha = 0.25f)
                            )
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 28.dp)
                            .padding(top = 40.dp, bottom = 28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "日期校验提示",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "存在期初余额日期之前的记录，请核实",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(28.dp))
                            // 知道了按钮
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(
                                        elevation = 10.dp,
                                        shape = RoundedCornerShape(14.dp),
                                        spotColor = Color(0xFFFF9800).copy(alpha = 0.3f)
                                    )
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(Color(0xFFFFB74D), Color(0xFFF57C00))
                                        )
                                    )
                                    .clickable { viewModel.clearError() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "知道了",
                                    color = Color.White,
                                    modifier = Modifier.padding(vertical = 14.dp),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    // 警告图标
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .size(72.dp)
                            .shadow(
                                elevation = 20.dp,
                                shape = CircleShape,
                                spotColor = Color(0xFFFF9800).copy(alpha = 0.5f)
                            )
                            .clip(CircleShape)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0xFFFFB74D), Color(0xFFF57C00)),
                                    center = Offset(0.5f, 0.35f)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
            }
        }
    }
}

// 账户类型图标映射
private fun getAccountIcon(type: AccountType): ImageVector {
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

// 解析颜色字符串
private fun parseColor(colorString: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorString))
    } catch (e: Exception) {
        Color(0xFF10B981) // 默认绿色
    }
}

/**
 * 获取银行名称首字
 */
private fun getBankInitial(name: String): String? {
    return when {
        name.contains("工商") -> "工"
        name.contains("建设") -> "建"
        name.contains("农业") -> "农"
        name.contains("中国") && name.contains("银行") -> "中"
        name.contains("交通") -> "交"
        name.contains("招商") -> "招"
        name.contains("邮储") || name.contains("邮政") -> "邮"
        name.contains("民生") -> "民"
        name.contains("兴业") -> "兴"
        name.contains("中信") -> "信"
        name.contains("光大") -> "光"
        name.contains("平安") -> "平"
        name.contains("浦发") -> "浦"
        name.contains("华夏") -> "华"
        name.contains("广发") -> "广"
        name.contains("银行") -> name.first().toString()
        else -> null
    }
}

/**
 * 统一的账户选择器 BottomSheet
 * 宽度与屏幕一致，展示所有未停用账户
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountSelectorSheet(
    title: String,
    accounts: List<Account>,
    selectedAccountId: Long?,
    onSelect: (Account) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        windowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null // 去掉拖拽指示器
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // 标题栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${accounts.size}个账户",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 账户列表
            if (accounts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "暂无已启用的账户",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    accounts.forEach { account ->
                        AccountItemModern(
                            account = account,
                            isSelected = account.id == selectedAccountId,
                            onClick = { onSelect(account) }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * 美化版账户列表项
 * 样式参照"全部账户"页面的AccountCardSimple
 */
@Composable
private fun AccountItemModern(
    account: Account,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 账户图标
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(parseColor(account.color)),
                contentAlignment = Alignment.Center
            ) {
                val bankInitial = getBankInitial(account.name)
                if (bankInitial != null) {
                    Text(
                        text = bankInitial,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        imageVector = getAccountIcon(account.type),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(14.dp))
            
            // 账户信息
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 类别前缀
                    val categoryPrefix = when (account.attribute) {
                        com.tinyledger.app.domain.model.AccountAttribute.CASH -> "【现金账户】"
                        com.tinyledger.app.domain.model.AccountAttribute.CREDIT_ACCOUNT -> "【信用账户】"
                        com.tinyledger.app.domain.model.AccountAttribute.CREDIT -> "【外部往来】"
                    }
                    Text(
                        text = "$categoryPrefix${account.name}",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                    )
                    if (!account.cardNumber.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "(${account.cardNumber.takeLast(4)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = account.type.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 右侧：余额和选中状态
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "\u00A5${com.tinyledger.app.util.CurrencyUtils.formatAmount(account.currentBalance)}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (account.currentBalance >= 0) 
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    else 
                        Color(0xFFFF3B30)
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "已选择",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountItem(
    account: Account,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clickable { onClick() },
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
            ),
            border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(parseColor(account.color)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getAccountIcon(account.type),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // 添加账户类别前缀
                        val categoryPrefix = when (account.attribute) {
                            com.tinyledger.app.domain.model.AccountAttribute.CASH -> "【现金账户】"
                            com.tinyledger.app.domain.model.AccountAttribute.CREDIT_ACCOUNT -> "【信用账户】"
                            com.tinyledger.app.domain.model.AccountAttribute.CREDIT -> "【外部往来】"
                        }
                        Text(
                            text = "$categoryPrefix${account.name}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (!account.cardNumber.isNullOrBlank()) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "(${account.cardNumber.takeLast(4)})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = "\u00A5${String.format("%.2f", account.currentBalance)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "已选择",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountSelectorRow(
    account: Account?,
    placeholder: String,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (account != null) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(parseColor(account.color)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getAccountIcon(account.type),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(text = account.name, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = "\u4F59\u989D: \u00A5${String.format("%.2f", account.currentBalance)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "选择",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LendingTypeButton(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) selectedColor.copy(alpha = 0.2f)
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) selectedColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            color = if (isSelected) selectedColor else MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}
