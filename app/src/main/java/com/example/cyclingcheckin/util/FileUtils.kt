package com.example.cyclingcheckin.util

import android.content.Context
import android.net.Uri
import com.example.cyclingcheckin.data.remote.BackupCodec
import com.example.cyclingcheckin.domain.model.Record
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object FileUtils {

    private val FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")

    fun backupFileName(now: LocalDateTime = LocalDateTime.now()): String =
        "cycling-backup-${now.format(FILE_STAMP)}.json"

    fun snapshotFileName(now: LocalDateTime = LocalDateTime.now()): String =
        "snapshot-${now.format(FILE_STAMP)}.json"

    fun readText(context: Context, uri: Uri): String {
        return context.contentResolver.openInputStream(uri)?.use { input ->
            input.bufferedReader(Charsets.UTF_8).readText()
        } ?: throw IllegalStateException("无法读取文件")
    }

    fun writeText(context: Context, uri: Uri, text: String) {
        context.contentResolver.openOutputStream(uri)?.use { output ->
            output.bufferedWriter(Charsets.UTF_8).use { it.write(text) }
        } ?: throw IllegalStateException("无法写入文件")
    }

    fun csvOf(records: List<Record>): String {
        val header = "date,time,distance,routeStart,routeEnd,note"
        val rows = records.map { record ->
            listOf(
                TimeUtils.formatDate(record.date),
                TimeUtils.formatTime(record.time),
                String.format(Locale.US, "%.1f", record.distance),
                escapeCsv(record.routeStart),
                escapeCsv(record.routeEnd),
                escapeCsv(record.note)
            ).joinToString(",")
        }
        return (listOf(header) + rows).joinToString("\n")
    }

    fun writeSnapshot(context: Context, records: List<Record>): File {
        val dir = File(context.filesDir, "snapshots")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, snapshotFileName())
        file.writeText(BackupCodec.encode(records, android.os.Build.MODEL), Charsets.UTF_8)
        return file
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }
}
