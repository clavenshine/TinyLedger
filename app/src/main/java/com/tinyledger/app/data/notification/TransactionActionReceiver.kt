package com.tinyledger.app.data.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.tinyledger.app.data.local.dao.PendingTransactionDao
import com.tinyledger.app.data.local.entity.PendingTransactionEntity
import com.tinyledger.app.domain.model.Category
import com.tinyledger.app.domain.model.Transaction
import com.tinyledger.app.domain.model.TransactionType
import com.tinyledger.app.domain.repository.TransactionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 交易确认通知的操作接收器
 *
 * 处理三个操作：
 * - ACTION_CONFIRM: 确认记账 → 将待确认记录移入正式交易表
 * - ACTION_DELETE: 删除 → 直接删除待确认记录
 * - ACTION_EDIT: 编辑 → 通过 MainActivity 打开记账编辑界面
 */
@AndroidEntryPoint
class TransactionActionReceiver : BroadcastReceiver() {

    @Inject lateinit var pendingTransactionDao: PendingTransactionDao
    @Inject lateinit var transactionRepository: TransactionRepository

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val TAG = "TxActionReceiver"
        const val ACTION_CONFIRM = "com.tinyledger.app.ACTION_CONFIRM_TRANSACTION"
        const val ACTION_DELETE = "com.tinyledger.app.ACTION_DELETE_TRANSACTION"
        const val ACTION_EDIT = "com.tinyledger.app.ACTION_EDIT_TRANSACTION"
        const val EXTRA_PENDING_ID = "pending_id"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val pendingId = intent.getLongExtra(EXTRA_PENDING_ID, -1)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

        if (pendingId == -1L) {
            Log.e(TAG, "Invalid pending ID")
            return
        }

        // Cancel the notification
        if (notificationId > 0) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(notificationId)
        }

        when (intent.action) {
            ACTION_CONFIRM -> handleConfirm(pendingId)
            ACTION_DELETE -> handleDelete(pendingId)
            ACTION_EDIT -> handleEdit(context, pendingId)
        }
    }

    private fun handleConfirm(pendingId: Long) {
        scope.launch {
            try {
                val entity = pendingTransactionDao.getById(pendingId) ?: run {
                    Log.e(TAG, "Pending transaction not found: $pendingId")
                    return@launch
                }

                val transactionType = TransactionType.fromInt(entity.type)
                val transaction = Transaction(
                    type = transactionType,
                    category = Category.fromId(entity.category, transactionType),
                    amount = entity.amount,
                    note = entity.note,
                    date = entity.date,
                    accountId = entity.accountId
                )

                transactionRepository.insertTransaction(transaction)
                pendingTransactionDao.deleteById(pendingId)
                Log.d(TAG, "交易已确认: pendingId=$pendingId ¥${entity.amount}")
            } catch (e: Exception) {
                Log.e(TAG, "确认交易失败", e)
            }
        }
    }

    private fun handleDelete(pendingId: Long) {
        scope.launch {
            try {
                pendingTransactionDao.deleteById(pendingId)
                Log.d(TAG, "待确认交易已删除: pendingId=$pendingId")
            } catch (e: Exception) {
                Log.e(TAG, "删除待确认交易失败", e)
            }
        }
    }

    private fun handleEdit(context: Context, pendingId: Long) {
        // The Edit action is handled by launching MainActivity with extras.
        // The notification's contentIntent already points to MainActivity,
        // so for the Edit button we also launch MainActivity with the pending ID.
        // MainActivity will read the extras and navigate to AddTransactionScreen.
        val editIntent = Intent(context, com.tinyledger.app.MainActivity::class.java).apply {
            action = ACTION_EDIT
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_PENDING_ID, pendingId)
        }
        context.startActivity(editIntent)
        Log.d(TAG, "编辑待确认交易: pendingId=$pendingId")
    }
}

