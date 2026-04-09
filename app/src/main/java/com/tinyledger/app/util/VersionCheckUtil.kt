package com.tinyledger.app.util

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
import com.tinyledger.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class VersionInfo(
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String,
    val updateLog: String = ""
)

object VersionCheckUtil {
    private const val TAG = "VersionCheck"
    private const val VERSION_URL = "https://www.clawlab.org.cn/download/version.json"

    suspend fun checkUpdate(): VersionInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URL(VERSION_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.requestMethod = "GET"

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(response)

                val serverVersionCode = json.optInt("versionCode", 0)
                val serverVersionName = json.optString("versionName", "")
                val downloadUrl = json.optString("downloadUrl", "")
                val updateLog = json.optString("updateLog", "")

                Log.d(TAG, "Server version: $serverVersionCode, Local: ${BuildConfig.VERSION_CODE}")

                if (serverVersionCode > BuildConfig.VERSION_CODE && downloadUrl.isNotBlank()) {
                    VersionInfo(serverVersionCode, serverVersionName, downloadUrl, updateLog)
                } else {
                    null
                }
            } else {
                Log.e(TAG, "HTTP ${connection.responseCode}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Version check failed", e)
            null
        }
    }

    fun downloadAndInstall(context: Context, versionInfo: VersionInfo) {
        try {
            val fileName = "TinyLedger-v${versionInfo.versionName}-release.apk"
            val request = DownloadManager.Request(Uri.parse(versionInfo.downloadUrl))
                .setTitle("小小记账本更新")
                .setDescription("正在下载 v${versionInfo.versionName}")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                .setMimeType("application/vnd.android.package-archive")

            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = dm.enqueue(request)

            // Register receiver to install after download
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id == downloadId) {
                        try {
                            ctx.unregisterReceiver(this)
                        } catch (_: Exception) {}
                        installApk(ctx, fileName)
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    receiver,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                    Context.RECEIVER_EXPORTED
                )
            } else {
                context.registerReceiver(
                    receiver,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
        }
    }

    private fun installApk(context: Context, fileName: String) {
        try {
            val file = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                fileName
            )
            if (!file.exists()) return

            val intent = Intent(Intent.ACTION_VIEW)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                intent.setDataAndType(uri, "application/vnd.android.package-archive")
            } else {
                intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive")
            }

            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Install failed", e)
        }
    }
}
