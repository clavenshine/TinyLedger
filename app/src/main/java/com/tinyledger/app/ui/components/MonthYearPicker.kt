package com.tinyledger.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
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

/**
 * 通用的月份/年份选择器
 * 支持按月查看和按年查看两种模式
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthYearPicker(
    currentYear: Int,
    currentMonth: Int,
    onYearSelected: (Int) -> Unit = {},
    onMonthSelected: (Int, Int) -> Unit = { _, _ -> }, // (year, month)
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var viewMode by remember { mutableStateOf("month") } // "month" or "year"
    var selectedYear by remember { mutableStateOf(currentYear) }
    var selectedMonth by remember { mutableStateOf(currentMonth) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        windowInsets = WindowInsets(0, 0, 0, 0) // 移除底部内边距，使其与屏幕底端齐平
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp) // 只保留少量底部内边距
        ) {
            // 模式切换选项卡
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 按月查看选项卡
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewMode = "month" }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "按月查看",
                            fontWeight = if (viewMode == "month") FontWeight.Bold else FontWeight.Normal,
                            fontSize = 16.sp,
                            color = if (viewMode == "month") 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        if (viewMode == "month") {
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(1.5.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }
                
                // 按年查看选项卡
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewMode = "year" }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "按年查看",
                            fontWeight = if (viewMode == "year") FontWeight.Bold else FontWeight.Normal,
                            fontSize = 16.sp,
                            color = if (viewMode == "year") 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        if (viewMode == "year") {
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(1.5.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (viewMode == "month") {
                // 按月查看模式
                MonthView(
                    selectedYear = selectedYear,
                    selectedMonth = selectedMonth,
                    onYearChange = { selectedYear = it },
                    onMonthSelected = { month ->
                        selectedMonth = month
                        onMonthSelected(selectedYear, month)
                        onDismiss()
                    }
                )
            } else {
                // 按年查看模式
                YearView(
                    selectedYear = selectedYear,
                    onYearSelected = { year ->
                        selectedYear = year
                        onYearSelected(year)
                        onDismiss()
                    }
                )
            }
        }
    }
}

/**
 * 按月查看视图
 */
@Composable
private fun MonthView(
    selectedYear: Int,
    selectedMonth: Int,
    onYearChange: (Int) -> Unit,
    onMonthSelected: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        // 年份选择器
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onYearChange(selectedYear - 1) }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "上一年")
            }
            
            Text(
                text = "$selectedYear",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            IconButton(onClick = { onYearChange(selectedYear + 1) }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "下一年")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 月份网格
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(200.dp)
        ) {
            items((1..12).toList()) { month ->
                val isSelected = month == selectedMonth
                val isFuture = if (selectedYear > java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)) {
                    true
                } else if (selectedYear == java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)) {
                    month > java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1
                } else {
                    false
                }

                Box(
                    modifier = Modifier
                        .aspectRatio(1.5f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else Color.Transparent
                        )
                        .clickable(
                            enabled = !isFuture,
                            onClick = { onMonthSelected(month) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${month}月",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else if (isFuture) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * 按年查看视图
 */
@Composable
private fun YearView(
    selectedYear: Int,
    onYearSelected: (Int) -> Unit
) {
    val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
    val startYear = (selectedYear / 12) * 12
    val years = (startYear..startYear + 11).toList()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        // 年份范围选择器
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onYearSelected(startYear - 12) }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "上一组年份")
            }

            Text(
                text = "年份",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            IconButton(onClick = { onYearSelected(startYear + 12) }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "下一组年份")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 年份网格
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(200.dp)
        ) {
            items(years) { year ->
                val isSelected = year == selectedYear
                val isFuture = year > currentYear

                Box(
                    modifier = Modifier
                        .aspectRatio(1.5f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else Color.Transparent
                        )
                        .clickable(
                            enabled = !isFuture,
                            onClick = { onYearSelected(year) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$year",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else if (isFuture) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
