package com.tinyledger.app.ui.viewmodel

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tinyledger.app.BuildConfig
import com.tinyledger.app.domain.repository.ReleaseInfo
import com.tinyledger.app.domain.repository.UpdateCheckRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class UpdateCheckUiState(
    val isChecking: Boolean = false,
    val latestRelease: ReleaseInfo? = null,
    val hasNewVersion: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadError: String? = null,
    val showDialog: Boolean = false
)

@HiltViewModel
class UpdateCheckViewModel @Inject constructor(
    private val updateCheckRepository: UpdateCheckRepository
) : ViewModel() {
    private companion object {
        const val TAG = "UpdateCheckVM"
    }

    private val _uiState = MutableStateFlow(UpdateCheckUiState())
    val uiState: StateFlow<UpdateCheckUiState> = _uiState.asStateFlow()

    private var downloadReceiver: BroadcastReceiver? = null
    private var currentDownloadId: Long = -1L

    fun checkForUpdate() {
        _uiState.update { it.copy(isChecking = true) }
        viewModelScope.launch {
            try {
                val releaseInfo = updateCheckRepository.getLatestReleaseInfo()
                
                if (releaseInfo != null) {
                    val hasNewVersion = updateCheckRepository.compareVersion(
                        BuildConfig.VERSION_NAME,
                        releaseInfo.versionName
                    )
                    
                    _uiState.update {
                        it.copy(
                            latestRelease = releaseInfo,
                            hasNewVersion = hasNewVersion,
                            showDialog = hasNewVersion,
                            isChecking = false
                        )
                    }
                    
                    Log.d(TAG, "Local: ${BuildConfig.VERSION_NAME}, Remote: ${releaseInfo.versionName}, HasNew: $hasNewVersion")
                } else {
                    _uiState.update {
                        it.copy(
                            isChecking = false,
                            downloadError = "无法获取版本信息"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Check update failed", e)
                _uiState.update {
                    it.copy(
                        isChecking = false,
                        downloadError = e.message ?: "检查失败"
                    )
                }
            }
        }
    }

    /**
     * 静默检查更新，只更新红点提示状态，不自动弹出更新对话框
     */
    fun silentCheckForUpdate() {
        viewModelScope.launch {
            try {
                val releaseInfo = updateCheckRepository.getLatestReleaseInfo()
                
                if (releaseInfo != null) {
                    val hasNewVersion = updateCheckRepository.compareVersion(
                        BuildConfig.VERSION_NAME,
                        releaseInfo.versionName
                    )
                    
                    _uiState.update {
                        it.copy(
                            latestRelease = releaseInfo,
                            hasNewVersion = hasNewVersion,
                            isChecking = false
                        )
                    }
                    
                    Log.d(TAG, "Silent check - Local: ${BuildConfig.VERSION_NAME}, Remote: ${releaseInfo.versionName}, HasNew: $hasNewVersion")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Silent check failed", e)
            }
        }
    }

    fun downloadAndInstall(context: Context) {
        val releaseInfo = _uiState.value.latestRelease ?: return
        
        _uiState.update { it.copy(isDownloading = true) }
        
        try {
            val fileName = "TinyLedger-v${releaseInfo.versionName}-release.apk"
            val request = DownloadManager.Request(Uri.parse(releaseInfo.downloadUrl))
                .setTitle("小小记账本更新")
                .setDescription("正在下载 v${releaseInfo.versionName}")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                .setMimeType("application/vnd.android.package-archive")

            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = dm.enqueue(request)
            currentDownloadId = downloadId

            // 注册下载完成广播接收器
            downloadReceiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id == downloadId) {
                        try {
                            ctx.unregisterReceiver(this)
                        } catch (_: Exception) {}
                        _uiState.update { it.copy(isDownloading = false) }
                        installApk(ctx, fileName)
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    downloadReceiver,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                    Context.RECEIVER_EXPORTED
                )
            } else {
                context.registerReceiver(
                    downloadReceiver,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                )
            }

            Log.d(TAG, "Download started: $downloadId")
        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            _uiState.update {
                it.copy(
                    isDownloading = false,
                    downloadError = "下载失败: ${e.message}"
                )
            }
        }
    }

    private fun installApk(context: Context, fileName: String) {
        try {
            val file = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                fileName
            )
            
            if (!file.exists()) {
                Log.e(TAG, "APK file not found: ${file.absolutePath}")
                return
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } else {
                    setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive")
                }
            }

            context.startActivity(intent)
            Log.d(TAG, "Install started")
        } catch (e: Exception) {
            Log.e(TAG, "Install failed", e)
            _uiState.update {
                it.copy(
                    downloadError = "安装失败: ${e.message}"
                )
            }
        }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(showDialog = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(downloadError = null) }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            if (downloadReceiver != null) {
                // 不能这里注销，因为已经onCleared了
                downloadReceiver = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing resources", e)
        }
    }
}
