package com.tinyledger.app.data.repository

import android.util.Log
import com.tinyledger.app.domain.repository.ReleaseInfo
import com.tinyledger.app.domain.repository.UpdateCheckRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateCheckRepositoryImpl @Inject constructor() : UpdateCheckRepository {
    private companion object {
        const val TAG = "UpdateCheck"
        const val GITHUB_OWNER = "clavenshine"
        const val GITHUB_REPO = "TinyLedger"
        const val API_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
    }

    override suspend fun getLatestReleaseInfo(): ReleaseInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URL(API_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 10000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github+json")
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(response)

                val tagName = json.optString("tag_name", "") // "v2.2.0"
                if (tagName.isBlank()) return@withContext null

                val versionName = tagName.removePrefix("v") // "2.2.0"
                val versionCode = parseVersionCode(versionName)
                val releaseBody = json.optString("body", "")
                val publishedAt = json.optString("published_at", "")
                
                // 从assets中获取APK下载链接
                val assets = json.optJSONArray("assets")
                var downloadUrl = ""
                
                if (assets != null && assets.length() > 0) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk")) {
                            downloadUrl = asset.optString("browser_download_url", "")
                            break
                        }
                    }
                }

                Log.d(TAG, "Latest release: $tagName, URL: $downloadUrl")

                if (downloadUrl.isNotBlank()) {
                    ReleaseInfo(
                        tagName = tagName,
                        versionName = versionName,
                        versionCode = versionCode,
                        releaseBody = releaseBody,
                        downloadUrl = downloadUrl,
                        publishedAt = publishedAt
                    )
                } else {
                    null
                }
            } else {
                Log.e(TAG, "HTTP ${connection.responseCode}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check update", e)
            null
        }
    }

    override fun compareVersion(localVersion: String, remoteVersion: String): Boolean {
        return try {
            val localParts = localVersion.split(".").map { it.toIntOrNull() ?: 0 }
            val remoteParts = remoteVersion.split(".").map { it.toIntOrNull() ?: 0 }

            val maxParts = maxOf(localParts.size, remoteParts.size)
            val local = localParts + List(maxParts - localParts.size) { 0 }
            val remote = remoteParts + List(maxParts - remoteParts.size) { 0 }

            for (i in 0 until maxParts) {
                if (remote[i] > local[i]) return true
                if (remote[i] < local[i]) return false
            }
            false
        } catch (e: Exception) {
            Log.e(TAG, "Compare version failed", e)
            false
        }
    }

    private fun parseVersionCode(versionName: String): Int {
        return try {
            // 将版本号转换为版本代码，如 "2.2.0" -> 29 (2*10 + 2)*10 + 0 = 220
            // 为了保持简单，我们只使用主版本号
            // 实际可能需要从服务器获取或使用其他方式
            val parts = versionName.split(".").map { it.toIntOrNull() ?: 0 }
            when {
                parts.size >= 3 -> (parts[0] * 100) + (parts[1] * 10) + parts[2]
                parts.size >= 2 -> (parts[0] * 100) + (parts[1] * 10)
                else -> parts.getOrNull(0) ?: 0
            }
        } catch (e: Exception) {
            0
        }
    }
}
