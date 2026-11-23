package com.example.cyclingcheckin.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LooksOne
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cyclingcheckin.AppContainer
import com.example.cyclingcheckin.BuildConfig
import com.example.cyclingcheckin.R
import com.example.cyclingcheckin.data.remote.ConflictPolicy
import com.example.cyclingcheckin.data.remote.RemoteBackupFile
import com.example.cyclingcheckin.data.remote.WebDavError
import com.example.cyclingcheckin.domain.model.CalendarMarkStyle
import com.example.cyclingcheckin.domain.model.Record
import com.example.cyclingcheckin.domain.model.ThemeMode
import com.example.cyclingcheckin.ui.components.RideLogCard
import com.example.cyclingcheckin.ui.components.webDavErrorMessage
import com.example.cyclingcheckin.ui.theme.RideLogTheme
import com.example.cyclingcheckin.util.FileUtils
import com.example.cyclingcheckin.util.TimeUtils
import java.util.Locale

@Composable
fun SettingsScreen(
    container: AppContainer,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onOpenWebDav: () -> Unit,
    onOpenBackupRestore: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: SettingsViewModel =
        viewModel(factory = SettingsViewModel.factory(container, context))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showExportDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showMarkDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var clearInput by remember { mutableStateOf("") }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showLicenseDialog by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<List<Record>>(emptyList()) }
    var showImportPolicy by remember { mutableStateOf(false) }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let(viewModel::exportJson) }
    val exportCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let(viewModel::exportCsv) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(viewModel::readImportFile) }

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is SettingsEvent.Message -> snackbarHostState.showSnackbar(event.text)
                is SettingsEvent.BackupDone -> snackbarHostState.showSnackbar(
                    context.getString(R.string.settings_backup_done, event.count)
                )
                is SettingsEvent.ImportFileLoaded -> {
                    pendingImport = event.records
                    showImportPolicy = true
                }
                is SettingsEvent.RestoreDone -> snackbarHostState.showSnackbar(
                    context.getString(R.string.settings_restore_done, event.imported, event.skipped)
                )
                is SettingsEvent.WebDavFailure -> {
                    val message = webDavErrorMessage(context, event.kind, event.httpCode)
                    val result = snackbarHostState.showSnackbar(
                        message = message,
                        actionLabel = if (event.kind == WebDavError.NOT_CONFIGURED) {
                            context.getString(R.string.action_configure)
                        } else {
                            null
                        },
                        duration = SnackbarDuration.Long
                    )
                    if (result == SnackbarResult.ActionPerformed) onOpenWebDav()
                }
                is SettingsEvent.RemoteBackupsLoaded -> { /* now handled by BackupRestoreScreen */ }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SettingsHeader(onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { SettingsSectionTitle(stringResource(R.string.settings_section_data)) }
            item {
                SettingRow(
                    icon = { Icon(Icons.Filled.Cloud, null, tint = MaterialTheme.colorScheme.primary) },
                    title = stringResource(R.string.settings_webdav),
                    subtitle = if (uiState.settings.webDavConfigured) {
                        uiState.settings.webDavAccount
                    } else {
                        stringResource(R.string.settings_not_configured)
                    },
                    onClick = onOpenWebDav
                )
            }
            item {
                SettingRow(
                    icon = { Icon(Icons.Filled.Sync, null, tint = MaterialTheme.colorScheme.primary) },
                    title = stringResource(R.string.settings_backup_restore),
                    subtitle = if (uiState.settings.webDavConfigured) {
                        uiState.settings.lastBackupAt?.let {
                            stringResource(R.string.settings_last_backup, TimeUtils.formatTimestamp(it))
                        } ?: stringResource(R.string.settings_never_backup)
                    } else {
                        stringResource(R.string.settings_not_configured)
                    },
                    onClick = onOpenBackupRestore
                )
            }
            item {
                SettingRow(
                    icon = { Icon(Icons.Filled.FileDownload, null, tint = MaterialTheme.colorScheme.primary) },
                    title = stringResource(R.string.settings_export),
                    subtitle = stringResource(R.string.settings_export_sub),
                    onClick = { showExportDialog = true }
                )
            }
            item {
                SettingRow(
                    icon = { Icon(Icons.Filled.FileUpload, null, tint = MaterialTheme.colorScheme.primary) },
                    title = stringResource(R.string.settings_import),
                    subtitle = stringResource(R.string.settings_import_sub),
                    onClick = {
                        importLauncher.launch(
                            arrayOf("application/json", "text/plain", "application/octet-stream")
                        )
                    }
                )
            }
            item {
                SettingRow(
                    icon = {
                        Icon(
                            Icons.Filled.Warning,
                            null,
                            tint = RideLogTheme.colors.danger
                        )
                    },
                    title = stringResource(R.string.settings_clear),
                    subtitle = stringResource(R.string.settings_clear_sub),
                    danger = true,
                    onClick = {
                        clearInput = ""
                        showClearDialog = true
                    }
                )
            }

            item { SettingsSectionTitle(stringResource(R.string.settings_section_general)) }
            item {
                SettingRow(
                    icon = { Icon(Icons.Filled.Palette, null, tint = MaterialTheme.colorScheme.primary) },
                    title = stringResource(R.string.settings_theme),
                    subtitle = themeLabel(uiState.settings.themeMode),
                    onClick = { showThemeDialog = true }
                )
            }
            item {
                SettingRow(
                    icon = { Icon(Icons.Filled.Star, null, tint = MaterialTheme.colorScheme.primary) },
                    title = stringResource(R.string.settings_mark),
                    subtitle = markLabel(uiState.settings.calendarMarkStyle),
                    onClick = { showMarkDialog = true }
                )
            }
            item {
                SettingRow(
                    icon = { Icon(Icons.Filled.LooksOne, null, tint = MaterialTheme.colorScheme.primary) },
                    title = stringResource(R.string.settings_decimals),
                    subtitle = stringResource(R.string.settings_decimals_value),
                    enabled = false,
                    onClick = {}
                )
            }
            item {
                SettingRow(
                    icon = { Icon(Icons.Filled.Language, null, tint = MaterialTheme.colorScheme.primary) },
                    title = stringResource(R.string.settings_language),
                    subtitle = stringResource(R.string.settings_language_value),
                    enabled = false,
                    onClick = {}
                )
            }

            item { SettingsSectionTitle(stringResource(R.string.settings_section_about)) }
            item {
                SettingRow(
                    icon = { Icon(Icons.Filled.Info, null, tint = MaterialTheme.colorScheme.primary) },
                    title = stringResource(R.string.settings_version),
                    subtitle = BuildConfig.VERSION_NAME,
                    enabled = false,
                    onClick = {}
                )
            }
            item {
                SettingRow(
                    icon = { Icon(Icons.Filled.Shield, null, tint = MaterialTheme.colorScheme.primary) },
                    title = stringResource(R.string.settings_privacy),
                    subtitle = stringResource(R.string.settings_privacy_body),
                    onClick = { showPrivacyDialog = true }
                )
            }
            item {
                SettingRow(
                    icon = { Icon(Icons.Filled.Description, null, tint = MaterialTheme.colorScheme.primary) },
                    title = stringResource(R.string.settings_license),
                    subtitle = stringResource(R.string.settings_license_body),
                    onClick = { showLicenseDialog = true }
                )
            }
            item {
                Text(
                    text = stringResource(R.string.settings_privacy_body),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = RideLogTheme.colors.secondaryText,
                    modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp)
                )
            }
        }
    }

    if (uiState.busy) {
        Dialog(onDismissRequest = {}) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    Text(
                        text = stringResource(R.string.settings_busy),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text(stringResource(R.string.settings_export)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            showExportDialog = false
                            exportJsonLauncher.launch(FileUtils.backupFileName())
                        }
                    ) { Text(stringResource(R.string.settings_export_json)) }
                    TextButton(
                        onClick = {
                            showExportDialog = false
                            exportCsvLauncher.launch("cycling-records.csv")
                        }
                    ) { Text(stringResource(R.string.settings_export_csv)) }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(stringResource(R.string.settings_theme)) },
            text = {
                Column {
                    ThemeMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.settings.themeMode == mode,
                                onClick = {
                                    viewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                }
                            )
                            Text(text = themeLabel(mode))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showMarkDialog) {
        AlertDialog(
            onDismissRequest = { showMarkDialog = false },
            title = { Text(stringResource(R.string.settings_mark)) },
            text = {
                Column {
                    CalendarMarkStyle.entries.forEach { style ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setCalendarMarkStyle(style)
                                    showMarkDialog = false
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.settings.calendarMarkStyle == style,
                                onClick = {
                                    viewModel.setCalendarMarkStyle(style)
                                    showMarkDialog = false
                                }
                            )
                            Text(text = markLabel(style))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMarkDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showClearDialog) {
        val clearWord = stringResource(R.string.settings_clear_word)
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.settings_clear_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.settings_clear_message))
                    Spacer(modifier = Modifier.size(12.dp))
                    OutlinedTextField(
                        value = clearInput,
                        onValueChange = { clearInput = it.take(10) },
                        singleLine = true,
                        placeholder = { Text(stringResource(R.string.settings_clear_hint)) }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = clearInput == clearWord,
                    onClick = {
                        viewModel.clearAllData()
                        showClearDialog = false
                    }
                ) {
                    Text(
                        text = stringResource(R.string.settings_clear_confirm),
                        color = if (clearInput == clearWord) {
                            RideLogTheme.colors.danger
                        } else {
                            RideLogTheme.colors.secondaryText
                        }
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text(stringResource(R.string.settings_privacy)) },
            text = { Text(stringResource(R.string.settings_privacy_body)) },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text(stringResource(R.string.action_confirm))
                }
            }
        )
    }

    if (showLicenseDialog) {
        AlertDialog(
            onDismissRequest = { showLicenseDialog = false },
            title = { Text(stringResource(R.string.settings_license)) },
            text = { Text(stringResource(R.string.settings_license_body)) },
            confirmButton = {
                TextButton(onClick = { showLicenseDialog = false }) {
                    Text(stringResource(R.string.action_confirm))
                }
            }
        )
    }

    if (showImportPolicy) {
        AlertDialog(
            onDismissRequest = { showImportPolicy = false },
            title = { Text(stringResource(R.string.settings_import_title)) },
            text = { Text(stringResource(R.string.settings_import_message, pendingImport.size)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.importRecords(pendingImport, overwrite = false)
                        showImportPolicy = false
                    }
                ) { Text(stringResource(R.string.settings_merge)) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showImportPolicy = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                    TextButton(
                        onClick = {
                            viewModel.importRecords(pendingImport, overwrite = true)
                            showImportPolicy = false
                        }
                    ) { Text(stringResource(R.string.settings_overwrite)) }
                }
            }
        )
    }
}

@Composable
private fun SettingsHeader(onBack: () -> Unit) {
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
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SettingsSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp, start = 4.dp)
    )
}

@Composable
private fun SettingRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    danger: Boolean = false
) {
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
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        if (danger) {
                            RideLogTheme.colors.danger.copy(alpha = 0.12f)
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.size(18.dp)) { icon() }
            }
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = if (danger) {
                        RideLogTheme.colors.danger
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = RideLogTheme.colors.secondaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (enabled) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = RideLogTheme.colors.secondaryText,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun themeLabel(mode: ThemeMode): String = stringResource(
    when (mode) {
        ThemeMode.SYSTEM -> R.string.settings_theme_system
        ThemeMode.LIGHT -> R.string.settings_theme_light
        ThemeMode.DARK -> R.string.settings_theme_dark
    }
)

@Composable
private fun markLabel(style: CalendarMarkStyle): String = stringResource(
    when (style) {
        CalendarMarkStyle.DOT -> R.string.settings_mark_dot
        CalendarMarkStyle.DISTANCE -> R.string.settings_mark_distance
    }
)

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
