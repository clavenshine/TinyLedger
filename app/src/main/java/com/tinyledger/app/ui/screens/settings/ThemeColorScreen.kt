package com.tinyledger.app.ui.screens.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tinyledger.app.domain.model.ColorTheme
import com.tinyledger.app.ui.theme.ThemeColorPreview
import com.tinyledger.app.ui.theme.ThemeColorPreviews
import com.tinyledger.app.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeColorScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentTheme = uiState.settings.colorTheme

    val groupedThemes = ThemeColorPreviews.byGroup()
    val groupOrder = ThemeColorPreviews.groupOrder
    val groupDescriptions = mapOf(
        "经典" to "iOS 风格经典配色",
        "系统" to "跟随手机品牌风格",
        "商务" to "专业商务，数据可视化",
        "生活" to "清新自然，轻松记账",
        "极简" to "高对比度，数字一目了然",
        "渐变" to "年轻趣味，游戏化记账"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("选择主题模式") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 标题与说明
            item {
                Text(
                    text = "共 ${ThemeColorPreviews.themes.size} 种主题可选",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    textAlign = TextAlign.Center
                )
            }

            // 主题分组
            groupOrder.forEach { group ->
                val themesInGroup = groupedThemes[group] ?: return@forEach

                item {
                    Column(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                        Text(
                            text = group,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        groupDescriptions[group]?.let { desc ->
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                val rows = themesInGroup.chunked(3)
                rows.forEach { rowItems ->
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { preview ->
                                val isSelected = preview.name == currentTheme.displayName
                                val themeColor = ColorTheme.entries.find { it.displayName == preview.name }
                                CompactThemeItem(
                                    preview = preview,
                                    isSelected = isSelected,
                                    currentTheme = currentTheme,
                                    onClick = { themeColor?.let { viewModel.updateColorTheme(it) } },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(3 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun CompactThemeItem(
    preview: ThemeColorPreview,
    isSelected: Boolean,
    currentTheme: ColorTheme,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSystemDark = isSystemInDarkTheme()
    // 判断这个预览卡片是否是深色主题（根据背景色亮度判断）
    val previewBackgroundBrightness = preview.background.red * 0.299f + preview.background.green * 0.587f + preview.background.blue * 0.114f
    val isDarkPreview = previewBackgroundBrightness < 0.5f
    // 判断当前主题是否是深色主题
    val isDarkTheme = currentTheme == ColorTheme.DARK_MIDNIGHT || currentTheme == ColorTheme.DARK_OCEAN
    // 使用深色卡片的条件：系统深色模式 OR 当前主题是深色主题 OR 预览卡片本身是深色主题
    val shouldUseDarkCard = isSystemDark || isDarkTheme || isDarkPreview

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) preview.primary else Color.Transparent,
        label = "border_color"
    )

    // 卡片背景色：如果是深色预览卡片，使用预览的深色背景；否则根据系统模式决定
    val cardBackgroundColor = if (isDarkPreview) {
        // 深色主题预览卡片：使用预览的深色背景
        preview.background
    } else if (isSystemDark || isDarkTheme) {
        // 系统深色模式或当前主题是深色主题：使用主题surface色
        MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
    } else {
        // 浅色模式下的浅色主题：使用预览背景色
        preview.background
    }

    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardBackgroundColor
        ),
        border = if (isSelected) BorderStroke(2.dp, borderColor) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Color dots row
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(preview.primary)
                )
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(preview.primaryLight)
                )
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(preview.secondary)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 文字颜色：根据卡片背景色的亮度决定
            val textColor = if (isDarkPreview) {
                // 深色背景卡片：使用浅色文字
                if (isSelected) preview.primary else Color.White.copy(alpha = 0.9f)
            } else {
                // 浅色背景卡片：使用主题文字色
                if (isSelected) preview.primary else MaterialTheme.colorScheme.onSurface
            }

            Text(
                text = preview.name,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 10.sp
                ),
                color = textColor,
                maxLines = 1
            )

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = preview.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
