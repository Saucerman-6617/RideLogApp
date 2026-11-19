package com.example.cyclingcheckin.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cyclingcheckin.AppContainer
import com.example.cyclingcheckin.R
import com.example.cyclingcheckin.data.remote.RemoteBackupFile
import com.example.cyclingcheckin.data.remote.WebDavError
import com.example.cyclingcheckin.ui.components.webDavErrorMessage
import com.example.cyclingcheckin.ui.theme.RideLogTheme
import com.example.cyclingcheckin.util.TimeUtils
import java.util.Locale

@Composable
fun BackupRestoreScreen(
    container: AppContainer,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onOpenWebDav: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: BackupRestoreViewModel =
        viewModel(factory = BackupRestoreViewModel.factory(container))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var confirmFile by remember { mutableStateOf<RemoteBackupFile?>(null) }

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is BackupRestoreEvent.Message -> snackbarHostState.showSnackbar(event.text)
                is BackupRestoreEvent.BackupDone -> snackbarHostState.showSnackbar(
                    context.getString(R.string.settings_backup_done, event.count)
                )
                is BackupRestoreEvent.RestoreDone -> snackbarHostState.showSnackbar(
                    context.getString(R.string.settings_restore_done, event.imported, event.skipped)
                )
                is BackupRestoreEvent.WebDavFailure -> {
                    val message = webDavErrorMessage(context, event.kind, event.httpCode)
                    val label = if (event.kind == WebDavError.NOT_CONFIGURED) {
                        context.getString(R.string.action_configure)
                    } else null
                    val result = snackbarHostState.showSnackbar(
                        message = message,
                        actionLabel = label,
                        duration = SnackbarDuration.Long
                    )
                    if (result == SnackbarResult.ActionPerformed) onOpenWebDav()
                }
            }
        }
    }

    // 首次进入自动刷新一次
    LaunchedEffect(uiState.webDavConfigured) {
        if (uiState.webDavConfigured && !uiState.loadedOnce) {
            viewModel.refreshFiles()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.cd_back),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = stringResource(R.string.settings_backup_restore_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // —— 顶部：自动备份开关 ——
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_bike),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_auto_backup_title),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.settings_auto_backup_sub),
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = RideLogTheme.colors.secondaryText
                            )
                        }
                        Switch(
                            checked = uiState.autoBackupAfterRecord,
                            onCheckedChange = viewModel::setAutoBackupAfterRecord,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                }
            }

            // —— 手动备份按钮 + 上次备份时间 ——
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.settings_backup_files),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = viewModel::refreshFiles) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.action_refresh),
                            tint = RideLogTheme.colors.secondaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    TextButton(
                        onClick = viewModel::backupNow,
                        enabled = uiState.webDavConfigured && !uiState.busy
                    ) {
                        Text(
                            text = stringResource(R.string.action_backup),
                            color = if (uiState.webDavConfigured) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                RideLogTheme.colors.secondaryText.copy(alpha = 0.5f)
                            }
                        )
                    }
                }
            }

            // 上次备份时间（如果有）
            if (uiState.lastBackupAt != null) {
                item {
                    Text(
                        text = stringResource(
                            R.string.settings_last_backup,
                            TimeUtils.formatTimestamp(uiState.lastBackupAt!!)
                        ),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = RideLogTheme.colors.secondaryText
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(4.dp)) }

            // —— WebDAV 未配置提示 ——
            if (!uiState.webDavConfigured) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.settings_not_configured),
                                style = MaterialTheme.typography.bodyMedium,
                                color = RideLogTheme.colors.secondaryText
                            )
                            TextButton(onClick = onOpenWebDav) {
                                Text(stringResource(R.string.action_configure))
                            }
                        }
                    }
                }
            }

            // —— 加载中 ——
            if (uiState.busy) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 30.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.settings_busy),
                            style = MaterialTheme.typography.bodySmall,
                            color = RideLogTheme.colors.secondaryText
                        )
                    }
                }
            }

            // —— 空列表 ——
            if (uiState.webDavConfigured && !uiState.busy && uiState.loadedOnce && uiState.files.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.settings_restore_empty_title),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Text(
                                text = stringResource(R.string.settings_restore_empty_sub),
                                style = MaterialTheme.typography.bodySmall,
                                color = RideLogTheme.colors.secondaryText
                            )
                        }
                    }
                }
            }

            // —— 文件列表 ——
            if (uiState.files.isNotEmpty()) {
                items(items = uiState.files, key = { it.name }) { file ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { confirmFile = file }
                                .padding(horizontal = 14.dp, vertical = 11.dp)
                        ) {
                            Text(
                                text = file.name,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = fileMeta(file),
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = RideLogTheme.colors.secondaryText
                            )
                        }
                    }
                }
            }
        }
    }

    // 恢复确认对话框
    val file = confirmFile
    if (file != null) {
        AlertDialog(
            onDismissRequest = { confirmFile = null },
            title = { Text(stringResource(R.string.settings_restore_confirm_title)) },
            text = {
                Column {
                    Text(
                        stringResource(R.string.settings_restore_confirm_body, file.name)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = fileMeta(file),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = RideLogTheme.colors.secondaryText
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmFile = null
                        viewModel.restore(file)
                    },
                    enabled = !uiState.busy
                ) {
                    Text(stringResource(R.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmFile = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun fileMeta(file: RemoteBackupFile): String {
    val size = formatFileSize(file.sizeBytes)
    val timestamp = if (file.lastModified > 0) {
        TimeUtils.formatTimestamp(file.lastModified)
    } else {
        "—"
    }
    return if (file.recordCount != null) {
        stringResource(R.string.settings_file_meta, size, file.recordCount.toString(), timestamp)
    } else {
        stringResource(R.string.settings_file_no_count, size, timestamp)
    }
}

private fun formatFileSize(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> String.format(Locale.US, "%.1f MB", bytes / 1024.0 / 1024.0)
    bytes >= 1024L -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
    else -> "$bytes B"
}
