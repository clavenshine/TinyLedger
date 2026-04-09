package com.tinyledger.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 通知栏捕获的银行短信
 *
 * 在国产 ROM 上，1069xxxx 等通知号码发出的银行短信不会出现在
 * content://sms 标准短信数据库中，只能通过 NotificationListenerService
 * 从通知栏实时捕获。此表用于持久化这些短信，以便用户在"导入短信收支记录"
 * 界面中进行选择和导入。
 *
 * uniqueHash 用于去重：同一条通知可能被多次推送，hash(address+body+date_minute) 去重。
 */
@Entity(
    tableName = "notification_sms",
    indices = [Index(value = ["uniqueHash"], unique = true)]
)
data class NotificationSmsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val address: String,         // 发件人（通知标题 / 包名标识）
    val body: String,            // 短信正文（通知文本）
    val date: Long,              // 捕获时间戳
    val packageName: String,     // 来源应用包名（如 com.android.mms）
    val uniqueHash: String,      // 去重哈希
    val createdAt: Long = System.currentTimeMillis()
)
