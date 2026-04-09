package com.tinyledger.app.ui.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 卡片进入动画修饰符
 * 
 * @param index 卡片索引
 * @param animationDelay 每张卡片之间的延迟（毫秒）
 * @param animationDuration 动画持续时间（毫秒）
 * @param initialOffsetX 初始X轴偏移（像素）
 * @param animationKey 动画触发键值，变化时重启动画
 */
@Composable
fun Modifier.cardEnterAnimation(
    index: Int,
    animationKey: Int,
    animationDelay: Int = 60,
    animationDuration: Int = 250,
    initialOffsetX: Float = -30f
): Modifier {
    var isVisible by remember(animationKey) { mutableStateOf(false) }
    
    LaunchedEffect(animationKey) {
        delay((index * animationDelay).toLong())
        isVisible = true
    }
    
    val offsetX = remember { Animatable(if (isVisible) 0f else initialOffsetX) }
    val alpha = remember { Animatable(if (isVisible) 1f else 0f) }
    
    LaunchedEffect(isVisible) {
        if (isVisible) {
            launch {
                offsetX.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(
                        durationMillis = animationDuration,
                        delayMillis = 0,
                        easing = FastOutSlowInEasing
                    )
                )
            }
            launch {
                alpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = animationDuration,
                        delayMillis = 0,
                        easing = FastOutSlowInEasing
                    )
                )
            }
        }
    }
    
    return this
        .offset { IntOffset(x = offsetX.value.roundToInt(), y = 0) }
        .alpha(alpha.value)
}

/**
 * 带重置功能的卡片进入动画状态
 */
class CardEnterAnimationState {
    private var animationKey by mutableStateOf(0)
    
    /**
     * 触发动画重放
     */
    fun resetAnimation() {
        animationKey++
    }
    
    /**
     * 获取当前动画键值，用于触发LaunchedEffect
     */
    val key: Int
        get() = animationKey
}

/**
 * 创建卡片进入动画状态
 */
@Composable
fun rememberCardEnterAnimationState(): CardEnterAnimationState {
    return remember { CardEnterAnimationState() }
}