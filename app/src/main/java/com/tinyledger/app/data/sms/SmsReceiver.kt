package com.tinyledger.app.data.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log

/**
 * 短信接收器 - 用于实时监听新收到的短信
 * 当收到金融类短信时，可以自动识别并提示用户是否要记账
 */
class SmsReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "SmsReceiver"
        
        // 金融类短信关键词
        private val financialKeywords = listOf(
            "银行", "转账", "到账", "收款", "支付", "消费", "扣款",
            "工商银行", "建设银行", "农业银行", "招商银行", "交通银行",
            "支付宝", "微信支付", "财付通"
        )
    }
    
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }
        
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        
        messages?.forEach { message ->
            val address = message.originatingAddress ?: return@forEach
            val body = message.messageBody ?: return@forEach
            
            // 检查是否是金融类短信
            val isFinancial = financialKeywords.any { body.contains(it) }
            
            if (isFinancial) {
                Log.d(TAG, "收到金融短信: $address - ${body.take(50)}")
                
                // TODO: 发送通知或通过EventBus/Channel通知UI层
                // 这里可以实现自动弹出记账提示的逻辑
            }
        }
    }
}
