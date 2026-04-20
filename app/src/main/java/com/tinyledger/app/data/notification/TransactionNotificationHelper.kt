package com.tinyledger.app.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.tinyledger.app.MainActivity
import com.tinyledger.app.domain.model.Transaction
import com.tinyledger.app.domain.model.TransactionType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 共享的交易通知工具类
 *
 * 提供确认通知构建和反馈播放功能，供 TransactionNotificationService 和 SmsReceiver 共用。
 */
@Singleton
class TransactionNotificationHelper @Inject constructor(
    @ApplicationContext private val appContext: Context
) {
    companion object {
        private const val TAG = "TxNotifHelper"
        private const val CHANNEL_ID = "transaction_confirmation"
        private const val NOTIFICATION_ID_BASE = 10000

        /**
         * 播放"咻"的滑降提示音
         * 使用AudioTrack生成从高频滑降到低频的频率扫描音效
         */
        fun playWhooshSound() {
            Thread {
                try {
                    val sampleRate = 44100
                    val durationSec = 0.18
                    val numSamples = (sampleRate * durationSec).toInt()
                    val startFreq = 1400.0  // 起始高频
                    val endFreq = 350.0      // 结束低频

                    val samples = ShortArray(numSamples)
                    var phase = 0.0
                    for (i in 0 until numSamples) {
                        val progress = i.toDouble() / numSamples
                        val freq = startFreq + (endFreq - startFreq) * progress
                        phase += 2.0 * Math.PI * freq / sampleRate
                        // 包络线：渐弱效果，让声音更自然
                        val envelope = (1.0 - progress * progress) * 0.5
                        samples[i] = (envelope * Short.MAX_VALUE * Math.sin(phase)).toInt().toShort()
                    }

                    val audioTrack = AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        numSamples * 2,
                        AudioTrack.MODE_STATIC
                    )
                    audioTrack.write(samples, 0, numSamples)
                    audioTrack.play()
                    Thread.sleep(500)
                    try { audioTrack.release() } catch (_: Exception) {}
                } catch (e: Exception) {
                    Log.e(TAG, "播放咻声失败", e)
                }
            }.start()
        }
    }

    /**
     * 记账成功后播放提示音/震动
     */
    fun playFeedback(context: Context = appContext) {
        try {
            // 声音
            if (TransactionNotificationService.isSoundEnabled(context)) {
                val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
                toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 200)
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ toneGenerator.release() }, 500)
            }

            // 震动
            if (TransactionNotificationService.isVibrationEnabled(context)) {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    vm.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(200)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "播放反馈失败", e)
        }
    }

    /**
     * 显示交易确认通知（非无感模式时调用）
     * 通知包含三个操作按钮：编辑、删除、确认
     *
     * @param context 上下文
     * @param pendingId 待确认交易ID
     * @param transaction 交易对象
     * @param sourceLabel 来源标签（如 "微信"、"建设银行" 等）
     */
    fun showTransactionConfirmationNotification(
        context: Context = appContext,
        pendingId: Long,
        transaction: Transaction,
        sourceLabel: String
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 创建通知渠道（高优先级，显示为浮动横幅）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "记账确认",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "自动记账确认通知"
                enableVibration(true)
                enableLights(true)
                lightColor = android.graphics.Color.parseColor("#3F51B5")
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val isIncome = transaction.type == TransactionType.INCOME
        val amountPrefix = if (isIncome) "+" else "-"
        val typeLabel = if (isIncome) "收入" else "支出"
        val amountText = "$amountPrefix¥${String.format("%.2f", kotlin.math.abs(transaction.amount))}"
        val categoryName = transaction.category.name

        // 构建确认操作 PendingIntent
        val confirmIntent = Intent(context, TransactionActionReceiver::class.java).apply {
            action = TransactionActionReceiver.ACTION_CONFIRM
            putExtra(TransactionActionReceiver.EXTRA_PENDING_ID, pendingId)
            putExtra(TransactionActionReceiver.EXTRA_NOTIFICATION_ID, NOTIFICATION_ID_BASE + pendingId.toInt())
        }
        val confirmPendingIntent = PendingIntent.getBroadcast(
            context,
            pendingId.toInt(),
            confirmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 构建删除操作 PendingIntent
        val deleteIntent = Intent(context, TransactionActionReceiver::class.java).apply {
            action = TransactionActionReceiver.ACTION_DELETE
            putExtra(TransactionActionReceiver.EXTRA_PENDING_ID, pendingId)
            putExtra(TransactionActionReceiver.EXTRA_NOTIFICATION_ID, NOTIFICATION_ID_BASE + pendingId.toInt())
        }
        val deletePendingIntent = PendingIntent.getBroadcast(
            context,
            (pendingId + 100000).toInt(),
            deleteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 构建编辑操作 PendingIntent → 打开APP的记账界面
        val editIntent = Intent(context, MainActivity::class.java).apply {
            action = TransactionActionReceiver.ACTION_EDIT
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(TransactionActionReceiver.EXTRA_PENDING_ID, pendingId)
            putExtra(TransactionActionReceiver.EXTRA_NOTIFICATION_ID, NOTIFICATION_ID_BASE + pendingId.toInt())
        }
        val editPendingIntent = PendingIntent.getActivity(
            context,
            (pendingId + 200000).toInt(),
            editIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 构建通知
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle("$typeLabel $amountText")
            .setContentText("$sourceLabel · $categoryName")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$typeLabel: $amountText\n分类: $categoryName\n来源: $sourceLabel${if (!transaction.note.isNullOrBlank()) "\n备注: ${transaction.note.take(40)}" else ""}")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(false)
            .setOngoing(false)
            .setContentIntent(editPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_edit,
                "编辑",
                editPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_delete,
                "删除",
                deletePendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_agenda,
                "确认",
                confirmPendingIntent
            )
            .build()

        notificationManager.notify(NOTIFICATION_ID_BASE + pendingId.toInt(), notification)
        Log.d(TAG, "确认通知已显示: pendingId=$pendingId $typeLabel ¥${transaction.amount}")
    }
}
