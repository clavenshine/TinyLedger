package com.tinyledger.app.domain.repository

import kotlinx.coroutines.flow.Flow

data class ReleaseInfo(
    val tagName: String,          // v2.2.0
    val versionName: String,      // 2.2.0
    val versionCode: Int,         // 29 (extracted from tag)
    val releaseBody: String,      // 发布说明
    val downloadUrl: String,      // GitHub APK下载链接
    val publishedAt: String       // 发布时间
)

interface UpdateCheckRepository {
    /**
     * 检查GitHub latest release信息
     */
    suspend fun getLatestReleaseInfo(): ReleaseInfo?

    /**
     * 比较版本号
     * @return true if remoteVersion > localVersion
     */
    fun compareVersion(localVersion: String, remoteVersion: String): Boolean
}
