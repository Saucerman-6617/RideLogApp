package com.example.cyclingcheckin.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cyclingcheckin.AppContainer
import com.example.cyclingcheckin.R
import com.example.cyclingcheckin.ui.components.webDavErrorMessage
import com.example.cyclingcheckin.ui.theme.RideLogTheme

@Composable
fun WebDavConfigScreen(
    container: AppContainer,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: WebDavConfigViewModel =
        viewModel(factory = WebDavConfigViewModel.factory(container))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var server by rememberSaveable { mutableStateOf("") }
    var account by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var remoteDir by rememberSaveable { mutableStateOf("") }
    var initialized by rememberSaveable { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var testMessage by remember { mutableStateOf<String?>(null) }
    var testSucceeded by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.loaded) {
        if (uiState.loaded && !initialized) {
            server = uiState.settings.webDavServer
            account = uiState.settings.webDavAccount
            password = uiState.password
            remoteDir = uiState.settings.webDavRemoteDir
            initialized = true
        }
    }

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                WebDavEvent.Saved -> {
                    testMessage = null
                    snackbarHostState.showSnackbar(context.getString(R.string.webdav_saved))
                }
                WebDavEvent.TestSuccess -> {
                    testSucceeded = true
                    testMessage = context.getString(R.string.webdav_success)
                }
                is WebDavEvent.TestFailure -> {
                    testSucceeded = false
                    testMessage = webDavErrorMessage(context, event.kind, event.httpCode)
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
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
                text = stringResource(R.string.webdav_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.webdav_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = server,
                    onValueChange = { server = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.webdav_server)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                )
            }
            item {
                OutlinedTextField(
                    value = account,
                    onValueChange = { account = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.webdav_account)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
            }
            item {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.webdav_password)) },
                    singleLine = true,
                    visualTransformation = if (showPassword) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        TextButton(onClick = { showPassword = !showPassword }) {
                            Text(
                                text = stringResource(
                                    if (showPassword) R.string.action_hide else R.string.action_show
                                )
                            )
                        }
                    }
                )
            }
            item {
                OutlinedTextField(
                    value = remoteDir,
                    onValueChange = { remoteDir = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.webdav_dir)) },
                    singleLine = true
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.testConnection(server, account, password, remoteDir)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.testing,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        if (uiState.testing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(stringResource(R.string.webdav_testing))
                        } else {
                            Text(stringResource(R.string.webdav_test))
                        }
                    }
                    Button(
                        onClick = { viewModel.save(server, account, password, remoteDir) },
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.saving,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(stringResource(R.string.webdav_save))
                    }
                }
            }
            item {
                testMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (testSucceeded) {
                            RideLogTheme.colors.success
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}
