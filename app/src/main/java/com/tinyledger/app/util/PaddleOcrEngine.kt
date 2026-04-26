package com.tinyledger.app.util

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.equationl.paddleocr4android.CpuPowerMode
import com.equationl.paddleocr4android.OCR
import com.equationl.paddleocr4android.OcrConfig
import com.equationl.paddleocr4android.bean.OcrResult
import com.equationl.paddleocr4android.callback.OcrInitCallback
import com.equationl.paddleocr4android.callback.OcrRunCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Paddle-Lite OCR 引擎封装
 * 基于PPOCRv4模型（det_v4_arm.nb + rec_v4_arm.nb），
 * 提供高精度的银行截屏文字识别
 *
 * 使用 paddleocr4android 库（OCR class），封装了：
 * - 模型初始化（det + rec，不启用cls方向分类）
 * - 异步OCR识别
 * - 结果转换为 OcrElement 列表，与 ML Kit 输出格式统一
 */
object PaddleOcrEngine {

    private const val TAG = "PaddleOcrEngine"
    private const val MODEL_PATH = "ocr"  // assets/ocr/

    private var ocr: OCR? = null
    private var isInitialized = false
    private var initError: String? = null

    fun isAvailable(): Boolean = isInitialized

    fun getInitError(): String? = initError

    /**
     * 异步初始化 PaddleOCR 引擎
     */
    suspend fun init(context: Context): Boolean {
        if (isInitialized) return true
        if (ocr != null) return isInitialized

        return try {
            suspendCancellableCoroutine { continuation ->
                val config = OcrConfig().apply {
                    modelPath = MODEL_PATH
                    detModelFilename = "det_v4_arm.nb"
                    recModelFilename = "rec_v4_arm.nb"
                    clsModelFilename = "cls.nb"   // PPOCRv2 文本方向分类模型
                    isRunDet = true
                    isRunCls = false   // 银行截屏方向固定，不需要方向分类
                    isRunRec = true
                    cpuPowerMode = CpuPowerMode.LITE_POWER_FULL
                    isDrwwTextPositionBox = false
                }

                val engine = OCR(context)
                ocr = engine

                Log.d(TAG, "开始初始化 PaddleOCR 引擎...")
                engine.initModel(config, object : OcrInitCallback {
                    override fun onSuccess() {
                        Log.d(TAG, "PaddleOCR 引擎初始化成功")
                        isInitialized = true
                        initError = null
                        if (continuation.isActive) {
                            continuation.resume(true)
                        }
                    }

                    override fun onFail(e: Throwable) {
                        Log.e(TAG, "PaddleOCR 引擎初始化失败", e)
                        isInitialized = false
                        initError = e.message ?: "未知初始化错误"
                        ocr = null
                        if (continuation.isActive) {
                            continuation.resume(false)
                        }
                    }
                })
            }
        } catch (e: Exception) {
            Log.e(TAG, "PaddleOCR 初始化异常", e)
            isInitialized = false
            initError = e.message ?: "初始化异常"
            ocr = null
            false
        }
    }

    /**
     * 异步执行 OCR 识别
     * @return OcrElement 列表，失败返回空列表
     */
    suspend fun recognize(bitmap: Bitmap): List<ScreenshotTransactionParser.OcrElement> {
        val engine = ocr
        if (engine == null || !isInitialized) {
            Log.w(TAG, "PaddleOCR 引擎未就绪，跳过")
            return emptyList()
        }

        return try {
            suspendCancellableCoroutine { continuation ->
                engine.run(bitmap, object : OcrRunCallback {
                    override fun onSuccess(result: OcrResult) {
                        val elements = convertToOcrElements(result)
                        Log.d(TAG, "PaddleOCR 识别成功: ${elements.size} 个文本元素, 耗时 ${result.inferenceTime}ms")
                        if (continuation.isActive) {
                            continuation.resume(elements)
                        }
                    }

                    override fun onFail(e: Throwable) {
                        Log.e(TAG, "PaddleOCR 识别失败", e)
                        if (continuation.isActive) {
                            continuation.resume(emptyList())
                        }
                    }
                })
            }
        } catch (e: Exception) {
            Log.e(TAG, "PaddleOCR 识别异常", e)
            emptyList()
        }
    }

    /**
     * 将 PaddleOCR 的 OcrResult 转换为 OcrElement 列表
     *
     * OcrResultModel 提供：
     * - getLabel(): 已解码的识别文本（无需手动查字典）
     * - getPoints(): List<Point> 四点坐标（android.graphics.Point）
     * - getConfidence(): 识别置信度
     */
    private fun convertToOcrElements(result: OcrResult): List<ScreenshotTransactionParser.OcrElement> {
        val elements = mutableListOf<ScreenshotTransactionParser.OcrElement>()

        for (model in result.outputRawResult) {
            val label = model.label?.trim() ?: ""
            if (label.isEmpty()) continue

            // 转换四点坐标为矩形边界
            val points = model.points
            if (points.isEmpty()) continue

            var left = Int.MAX_VALUE
            var top = Int.MAX_VALUE
            var right = Int.MIN_VALUE
            var bottom = Int.MIN_VALUE

            for (point in points) {
                if (point.x < left) left = point.x
                if (point.x > right) right = point.x
                if (point.y < top) top = point.y
                if (point.y > bottom) bottom = point.y
            }

            if (right > left && bottom > top) {
                elements.add(
                    ScreenshotTransactionParser.OcrElement(
                        text = label,
                        left = left,
                        top = top,
                        right = right,
                        bottom = bottom
                    )
                )
            }
        }

        return elements
    }

    /**
     * 释放 PaddleOCR 引擎资源
     */
    fun release() {
        ocr?.releaseModel()
        ocr = null
        isInitialized = false
        initError = null
        Log.d(TAG, "PaddleOCR 引擎已释放")
    }
}
