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
    private const val GITHUB_API_URL = "https://api.github.com/repos/clavenshine/TinyLedger/releases/latest"

    suspend fun checkUpdate(): VersionInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URL(GITHUB_API_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(response)

                // Extract version from tag_name (e.g., "v2.2.1" -> "2.2.1")
                val tagName = json.optString("tag_name", "")
                val versionName = tagName.removePrefix("v")
                
                // Extract APK download URL from assets
                val assets = json.optJSONArray("assets") ?: return@withContext null
                var downloadUrl = ""
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk")) {
                        downloadUrl = asset.optString("browser_download_url", "")
                        break
                    }
                }

                if (downloadUrl.isBlank()) {
                    Log.e(TAG, "No APK asset found in release")
                    return@withContext null
                }

                // Extract release notes
                val updateLog = json.optString("body", "")

                // Compare versions: use version code or semantic versioning
                val serverVersionCode = versionStringToCode(versionName)
                val localVersionCode = BuildConfig.VERSION_CODE

                Log.d(TAG, "GitHub version: $versionName (code: $serverVersionCode), Local: ${BuildConfig.VERSION_NAME} (code: $localVersionCode)")

                if (serverVersionCode > localVersionCode && downloadUrl.isNotBlank()) {
                    VersionInfo(serverVersionCode, versionName, downloadUrl, updateLog)
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

    /**
     * Convert version string (e.g., "2.2.1") to version code (e.g., 30)
     * Format: major * 10000 + minor * 100 + patch
     */
    private fun versionStringToCode(versionString: String): Int {
        return try {
            val parts = versionString.split(".").map { it.toIntOrNull() ?: 0 }
            val major = parts.getOrNull(0) ?: 0
            val minor = parts.getOrNull(1) ?: 0
            val patch = parts.getOrNull(2) ?: 0
            major * 10000 + minor * 100 + patch
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse version string: $versionString", e)
            0
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
