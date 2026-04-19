package com.tinyledger.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 通用数字键盘控件
 * 参照财务App风格设计，4x4网格布局
 * 布局：
 * 1 2 3 ⌫
 * 4 5 6 +
 * 7 8 9 -
 * . 0 = 完成
 */
@Composable
fun NumericKeyboard(
    onKeyPress: (String) -> Unit,
    onBackspace: () -> Unit,
    onDone: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 第一行: 1 2 3 ⌫
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NumericKey("1", onKeyPress, Modifier.weight(1f))
            NumericKey("2", onKeyPress, Modifier.weight(1f))
            NumericKey("3", onKeyPress, Modifier.weight(1f))
            BackspaceKey(onBackspace, Modifier.weight(1f))
        }
        
        // 第二行: 4 5 6 +
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NumericKey("4", onKeyPress, Modifier.weight(1f))
            NumericKey("5", onKeyPress, Modifier.weight(1f))
            NumericKey("6", onKeyPress, Modifier.weight(1f))
            OperatorKey("+", onKeyPress, Modifier.weight(1f))
        }
        
        // 第三行: 7 8 9 -
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NumericKey("7", onKeyPress, Modifier.weight(1f))
            NumericKey("8", onKeyPress, Modifier.weight(1f))
            NumericKey("9", onKeyPress, Modifier.weight(1f))
            OperatorKey("-", onKeyPress, Modifier.weight(1f))
        }
        
        // 第四行: . 0 = 完成
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OperatorKey(".", onKeyPress, Modifier.weight(1f))
            NumericKey("0", onKeyPress, Modifier.weight(1f))
            OperatorKey("=", onKeyPress, Modifier.weight(1f))
            DoneKey(onDone, Modifier.weight(1f))
        }
    }
}

@Composable
private fun NumericKey(
    key: String,
    onKeyPress: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable { onKeyPress(key) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = key,
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun OperatorKey(
    key: String,
    onKeyPress: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .clickable { onKeyPress(key) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = key,
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun BackspaceKey(
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .clickable { onBackspace() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "⌫",
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DoneKey(
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.onSurface)
            .clickable { onDone() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "完成",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.surface,
            textAlign = TextAlign.Center
        )
    }
}
