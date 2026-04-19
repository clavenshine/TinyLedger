package com.tinyledger.app.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * 美化的删除确认对话框
 * 
 * 布局：图标突破顶部 → 标题 → 副标题 → 确认按钮 → 取消按钮 → 微提示
 * 图标在最上层，一半在卡片内一半在卡片外
 *
 * @param title 标题（如"删除账单记录？"）
 * @param onDismiss 取消回调
 * @param onConfirm 确认删除回调
 */
@Composable
fun DeleteConfirmationDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val view = LocalView.current

    var isCancelPressed by remember { mutableStateOf(false) }
    var isConfirmPressed by remember { mutableStateOf(false) }

    val cancelScale by animateFloatAsState(targetValue = if (isCancelPressed) 0.97f else 1f, label = "cancel")
    val confirmScale by animateFloatAsState(targetValue = if (isConfirmPressed) 0.97f else 1f, label = "confirm")

    Dialog(
        onDismissRequest = onDismiss,
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
                        onClick = onDismiss
                    )
            )

            // 对话框容器 - 包含卡片和图标
            Box(
                modifier = Modifier.fillMaxWidth(0.85f),
                contentAlignment = Alignment.TopCenter
            ) {
                // ===== 主卡片（先绘制，在下层）=====
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 36.dp) // 为图标留出突破空间
                        .shadow(
                            elevation = 24.dp,
                            shape = RoundedCornerShape(24.dp),
                            spotColor = Color(0xFFFF453A).copy(alpha = 0.25f)
                        )
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF2C2C3E),
                                    Color(0xFF1E1E2E)
                                )
                            )
                        )
                        .padding(horizontal = 28.dp)
                        .padding(top = 40.dp, bottom = 28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 标题 - 使用Color.White确保在深色背景上可见
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            lineHeight = 32.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // 副标题
                        Text(
                            text = "删除后将无法恢复，请谨慎操作",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(36.dp))

                        // 确认删除按钮 - 红色渐变填充
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .scale(confirmScale)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                        onConfirm()
                                    }
                                )
                                .shadow(
                                    elevation = 14.dp,
                                    shape = RoundedCornerShape(14.dp),
                                    spotColor = Color(0xFFFF453A).copy(alpha = 0.5f)
                                )
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFFFF6B5B),
                                            Color(0xFFE8453A)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "确认删除",
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 取消按钮 - 灰白描边
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .scale(cancelScale)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        onDismiss()
                                    }
                                )
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    color = Color(0xFF3A3A4C).copy(alpha = 0.5f)
                                )
                                .border(
                                    width = 1.5.dp,
                                    color = Color.White.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "取消",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 底部微提示
                        Text(
                            text = "此操作不可撤销",
                            color = Color.White.copy(alpha = 0.3f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // ===== 红色删除图标（后绘制，在最上层）=====
                // 图标一半在卡片内，一半在卡片外
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = 0.dp) // 图标顶部对齐卡片顶部（卡片有36dp padding）
                        .size(72.dp)
                        .shadow(
                            elevation = 20.dp,
                            shape = CircleShape,
                            spotColor = Color(0xFFFF453A).copy(alpha = 0.6f)
                        )
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFF6B5B),
                                    Color(0xFFE8453A)
                                ),
                                center = Offset(0.5f, 0.35f)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
        }
    }
}
