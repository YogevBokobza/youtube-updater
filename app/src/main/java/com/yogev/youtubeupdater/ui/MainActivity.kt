package com.yogev.youtubeupdater.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yogev.youtubeupdater.data.AppStatus
import com.yogev.youtubeupdater.notify.Notifications

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Notifications.ensureChannel(this)
        setContent {
            MaterialTheme {
                UpdaterScreen(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpdaterScreen(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showSettings by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<AppStatus?>(null) }

    val installPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        val target = pending
        pending = null
        if (target != null && context.packageManager.canRequestPackageInstalls()) {
            viewModel.updateApp(target)
        }
    }

    fun startUpdate(status: AppStatus) {
        if (context.packageManager.canRequestPackageInstalls()) {
            viewModel.updateApp(status)
        } else {
            pending = status
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}"),
            )
            installPermissionLauncher.launch(intent)
        }
    }

    fun uninstall(packageName: String) {
        val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName"))
        context.startActivity(intent)
    }

    // The ViewModel sets this once a signature-mismatch replace has downloaded
    // the new build and needs the old (wrongly-signed) one removed first.
    LaunchedEffect(state.pendingUninstallPackage) {
        val pkg = state.pendingUninstallPackage
        if (pkg != null) {
            uninstall(pkg)
            viewModel.consumePendingUninstall()
        }
    }

    // Refresh installed/available state whenever the screen returns to foreground
    // (e.g. after an install or uninstall completes).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = androidx.compose.ui.res.stringResource(com.yogev.youtubeupdater.R.string.app_name)) },
                actions = {
                    IconButton(onClick = { viewModel.refresh(force = true) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "רענן")
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "הגדרות")
                    }
                },
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            if (state.loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            state.error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.items, key = { it.source.key }) { status ->
                    AppCard(
                        status = status,
                        busy = state.busyKey == status.source.key,
                        replacing = state.replacingKey == status.source.key,
                        progress = state.progress,
                        onUpdate = { startUpdate(status) },
                        onUninstall = { uninstall(it) },
                    )
                }
            }
        }
    }

    if (showSettings) {
        SettingsDialog(
            token = state.token,
            includePrereleases = state.includePrereleases,
            onToken = viewModel::setToken,
            onPrerelease = viewModel::setIncludePrereleases,
            onDismiss = { showSettings = false },
        )
    }
}

@Composable
private fun AppCard(
    status: AppStatus,
    busy: Boolean,
    replacing: Boolean,
    progress: Float,
    onUpdate: () -> Unit,
    onUninstall: (String) -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(status.source.displayName, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "מותקן: " + (status.installedVersion ?: "לא מותקן"),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "זמין: " + (status.remote?.version ?: "—"),
                style = MaterialTheme.typography.bodyMedium,
            )
            status.error?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, color = MaterialTheme.colorScheme.error, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }

            // Warn about a non-RE variant and offer to remove it before installing.
            status.conflicts.forEach { conflict ->
                Spacer(Modifier.height(8.dp))
                Text(
                    "מותקנת גרסה אחרת: ${conflict.label}. מומלץ להסיר לפני התקנת RE.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(4.dp))
                OutlinedButton(onClick = { onUninstall(conflict.packageName) }) {
                    Text("הסר ${conflict.label}")
                }
            }

            Spacer(Modifier.height(12.dp))

            if (busy) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(4.dp))
                Text("מוריד… ${(progress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
            } else if (replacing) {
                Text(
                    "הורד הושלם — אשר את מחיקת הגרסה הישנה במכשיר; ההתקנה תמשיך אוטומטית.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            } else {
                if (status.signatureMismatch && status.remote != null) {
                    Text(
                        "הגרסה המותקנת חתומה אחרת מהבילד הרשמי — נדרשת מחיקה והתקנה מחדש.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.height(4.dp))
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    when {
                        status.signatureMismatch && status.remote != null ->
                            Button(onClick = onUpdate) { Text("מחק והתקן") }
                        status.updateAvailable -> Button(onClick = onUpdate) {
                            Text(if (status.isInstalled) "עדכן" else "התקן")
                        }
                        status.isInstalled -> Text("מעודכן", style = MaterialTheme.typography.bodyMedium)
                        else -> Button(onClick = onUpdate, enabled = status.remote != null) {
                            Text("התקן")
                        }
                    }
                }
            }
        }
    }
}
