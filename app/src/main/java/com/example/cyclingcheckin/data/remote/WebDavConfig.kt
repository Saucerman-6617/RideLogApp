package com.example.cyclingcheckin.data.remote

/** WebDAV 连接配置。密码只保存在加密存储中。 */
data class WebDavConfig(
    val serverUrl: String,
    val username: String,
    val password: String,
    val remoteDir: String
)

enum class WebDavError {
    NOT_CONFIGURED,
    INSECURE,
    UNAUTHORIZED,
    FORBIDDEN,
    NOT_FOUND,
    SERVER,
    HTTP,
    TIMEOUT,
    NETWORK,
    INVALID_RESPONSE,
    INVALID_DATA
}

class WebDavException(
    val kind: WebDavError,
    val httpCode: Int? = null,
    cause: Throwable? = null
) : Exception(kind.name, cause)

/** 远端备份文件。 */
data class RemoteBackupFile(
    val name: String,
    val href: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val recordCount: Int? = null
) {
    fun withRecordCount(count: Int?): RemoteBackupFile = copy(recordCount = count)
}
