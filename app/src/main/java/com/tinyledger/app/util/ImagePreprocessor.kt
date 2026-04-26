package com.tinyledger.app.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import kotlin.math.max
import kotlin.math.min

/**
 * 截屏OCR图片预处理 — 灰度化 + 轻度对比度增强
 * v2: 移除降噪（避免破坏银行截图的细字体），对比度改为保守拉伸
 */
object ImagePreprocessor {

    private const val MAX_DIMENSION = 2048

    fun preprocess(source: Bitmap): Bitmap {
        var bitmap = source

        val scaled = resizeIfNeeded(bitmap)
        if (scaled != null) bitmap = scaled

        // 仅灰度化 + 轻度对比度增强；去掉降噪避免破坏细字体
        bitmap = toGrayscale(bitmap)
        bitmap = enhanceContrastLight(bitmap)

        return bitmap
    }

    private fun resizeIfNeeded(bitmap: Bitmap): Bitmap? {
        val width = bitmap.width
        val height = bitmap.height
        val maxSide = max(width, height)
        if (maxSide <= MAX_DIMENSION) return null

        val scale = MAX_DIMENSION.toFloat() / maxSide
        val newW = (width * scale).toInt()
        val newH = (height * scale).toInt()
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }

    private fun toGrayscale(bitmap: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        if (bitmap !== result) bitmap.recycle()
        return result
    }

    /**
     * 轻度对比度拉伸 — 5%-95% 分位线性拉伸（比之前的2%-98%更保守）
     */
    private fun enhanceContrastLight(bitmap: Bitmap): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val hist = IntArray(256)
        for (p in pixels) hist[(p shr 16) and 0xFF]++

        val total = w * h
        val lowThreshold = (total * 0.05).toInt()
        val highThreshold = (total * 0.95).toInt()
        var sum = 0
        var lowVal = 0
        var highVal = 255
        for (i in 0..255) {
            sum += hist[i]
            if (sum < lowThreshold) lowVal = i
            if (sum < highThreshold) highVal = i
        }
        if (highVal <= lowVal) { lowVal = 0; highVal = 255 }

        val scale = 255.0f / (highVal - lowVal).coerceAtLeast(1)
        for (i in pixels.indices) {
            val gray = (pixels[i] shr 16) and 0xFF
            val clamped = min(255, max(0, ((gray - lowVal) * scale).toInt()))
            pixels[i] = 0xFF000000.toInt() or (clamped shl 16) or (clamped shl 8) or clamped
        }

        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        result.setPixels(pixels, 0, w, 0, 0, w, h)
        bitmap.recycle()
        return result
    }
}
