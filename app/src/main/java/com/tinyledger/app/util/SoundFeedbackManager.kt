package com.tinyledger.app.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.tinyledger.app.R
import com.tinyledger.app.data.notification.TransactionNotificationService

/**
 * 统一的音效和震动反馈管理器
 * 
 * 用于所有"保存"和"删除"操作的反馈
 */
object SoundFeedbackManager {
    
    private var mediaPlayer: MediaPlayer? = null
    
    /**
     * 播放保存成功音效（水滴声）
     */
    fun playSaveSound(context: Context) {
        if (!TransactionNotificationService.isSoundEnabled(context)) return
        
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(context, android.net.Uri.parse("android.resource://${context.packageName}/${R.raw.waterdrop_notificationevening1}"))
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 播放删除成功音效
     */
    fun playDeleteSound(context: Context) {
        if (!TransactionNotificationService.isSoundEnabled(context)) return
        
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(context, android.net.Uri.parse("android.resource://${context.packageName}/${R.raw.system_delete}"))
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 触发震动反馈
     */
    fun vibrate(context: Context) {
        if (!TransactionNotificationService.isVibrationEnabled(context)) return
        
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(50)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 保存成功反馈（音效 + 震动）
     */
    fun onSaved(context: Context) {
        if (TransactionNotificationService.isSoundEnabled(context)) {
            playSaveSound(context)
        }
        if (TransactionNotificationService.isVibrationEnabled(context)) {
            vibrate(context)
        }
    }
    
    /**
     * 删除成功反馈（音效 + 震动）
     */
    fun onDeleted(context: Context) {
        if (TransactionNotificationService.isSoundEnabled(context)) {
            playDeleteSound(context)
        }
        if (TransactionNotificationService.isVibrationEnabled(context)) {
            vibrate(context)
        }
    }
    
    /**
     * 释放资源
     */
    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
