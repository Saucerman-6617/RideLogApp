package com.example.cyclingcheckin.data.remote

import android.content.Context
import android.os.Build
import com.example.cyclingcheckin.data.prefs.SecureStore
import com.example.cyclingcheckin.data.prefs.SettingsStore
import com.example.cyclingcheckin.data.repo.RecordRepository
import com.example.cyclingcheckin.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

enum class ConflictPolicy {
    MERGE,
    OVERWRITE
}

data class BackupSummary(
    val count: Int,
    val bytes: Int,
    val fileName: String,
    val timestamp: Long
)

data class RestoreSummary(
    val imported: Int,
    val skipped: Int,
    val snapshot: File
)

class BackupService(
    private val repository: RecordRepository,
    private val settingsStore: SettingsStore,
    private val secureStore: SecureStore,
    private val client: WebDavClient,
    private val context: Context
) {

    suspend fun currentConfig(): WebDavConfig? {
        val settings = settingsStore.settings.first()
        val password = secureStore.webDavPassword
        if (settings.webDavAccount.isBlank() || password.isBlank()) return null
        return WebDavConfig(
            serverUrl = settings.webDavServer,
            username = settings.webDavAccount,
            password = password,
            remoteDir = settings.webDavRemoteDir
        )
    }

    suspend fun testConnection(config: WebDavConfig): Result<Unit> =
        client.testConnection(config)

    /** 全量备份：上传成功后把所有记录标记为已备份，并记录备份时间。 */
    suspend fun backup(): Result<BackupSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val config = currentConfig() ?: throw WebDavException(WebDavError.NOT_CONFIGURED)
            val records = repository.getAll()
            val json = BackupCodec.encode(records, Build.MODEL)
            val fileName = FileUtils.backupFileName()
            client.upload(config, fileName, json).getOrThrow()
            repository.markAllClean()
            val timestamp = System.currentTimeMillis()
            settingsStore.setLastBackupAt(timestamp)
            BackupSummary(
                count = records.size,
                bytes = json.toByteArray(Charsets.UTF_8).size,
                fileName = fileName,
                timestamp = timestamp
            )
        }.toWebDavResult()
    }

    /**
     * 列出远端备份，并读取每个文件的记录条数（限制 2MB 以内，失败时条数为 null）。
     */
    suspend fun listBackups(): Result<List<RemoteBackupFile>> = withContext(Dispatchers.IO) {
        runCatching {
            val config = currentConfig() ?: throw WebDavException(WebDavError.NOT_CONFIGURED)
            client.listBackups(config).getOrThrow().map { file ->
                if (file.sizeBytes in 1..MAX_COUNT_FILE_SIZE) {
                    val text = client.download(config, file).getOrNull()
                    file.withRecordCount(text?.let { BackupCodec.decodeCount(it) })
                } else {
                    file
                }
            }
        }.toWebDavResult()
    }

    /**
     * 恢复：下载并校验备份，恢复前先写本机快照，再按合并/覆盖策略导入。
     */
    suspend fun restore(
        file: RemoteBackupFile,
        policy: ConflictPolicy
    ): Result<RestoreSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val config = currentConfig() ?: throw WebDavException(WebDavError.NOT_CONFIGURED)
            val text = client.download(config, file).getOrThrow()
            val records = try {
                BackupCodec.decode(text)
            } catch (e: IllegalArgumentException) {
                throw WebDavException(WebDavError.INVALID_DATA, cause = e)
            }
            val snapshot = FileUtils.writeSnapshot(context, repository.getAll())
            val result = repository.importRecords(records, overwrite = policy == ConflictPolicy.OVERWRITE)
            RestoreSummary(
                imported = result.imported,
                skipped = result.skipped,
                snapshot = snapshot
            )
        }.toWebDavResult()
    }

    private fun <T> Result<T>.toWebDavResult(): Result<T> = fold(
        onSuccess = { Result.success(it) },
        onFailure = { error ->
            when (error) {
                is WebDavException -> Result.failure(error)
                is IllegalArgumentException -> Result.failure(
                    WebDavException(WebDavError.INVALID_DATA, cause = error)
                )
                else -> Result.failure(
                    WebDavException(WebDavError.NETWORK, cause = error)
                )
            }
        }
    )

    private companion object {
        const val MAX_COUNT_FILE_SIZE = 2_000_000L
    }
}
