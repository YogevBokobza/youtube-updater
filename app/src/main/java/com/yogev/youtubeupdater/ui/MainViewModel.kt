package com.yogev.youtubeupdater.ui

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yogev.youtubeupdater.data.AppStatus
import com.yogev.youtubeupdater.data.Prefs
import com.yogev.youtubeupdater.data.UpdateRepository
import com.yogev.youtubeupdater.install.ApkInstaller
import com.yogev.youtubeupdater.install.Downloader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    data class UiState(
        val items: List<AppStatus> = emptyList(),
        val loading: Boolean = false,
        val error: String? = null,
        val busyKey: String? = null,
        val progress: Float = 0f,
        val token: String = "",
        val includePrereleases: Boolean = false,
        /** Set once to tell the UI to fire a system uninstall prompt for this package. */
        val pendingUninstallPackage: String? = null,
        /** Source key currently mid "download → uninstall → auto-install" replace flow. */
        val replacingKey: String? = null,
    )

    private val repo = UpdateRepository(app)
    private val prefs = Prefs(app)
    private val downloader = Downloader(app)

    // Mirrors state.replacingKey; used by refresh() to notice the uninstall completed.
    private var pendingReplaceKey: String? = null

    private val _state = MutableStateFlow(
        UiState(
            token = prefs.token ?: "",
            includePrereleases = prefs.includePrereleases,
        )
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    // A refresh requested while one is running; replayed when it finishes so a
    // package change that lands mid-refresh isn't lost.
    private var rerun = false
    private var rerunForce = false

    /**
     * The screen refreshes on resume, but the install dialog closes before the
     * package manager finishes installing, so that refresh still sees the old
     * version. Listening for the package broadcasts updates the card the moment
     * the install, update or uninstall actually lands.
     */
    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            refresh()
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addDataScheme("package")
        }
        app.registerReceiver(packageReceiver, filter)
        refresh()
    }

    override fun onCleared() {
        getApplication<Application>().unregisterReceiver(packageReceiver)
    }

    /** [force] = true hits GitHub now (the refresh button); false uses the cache
     *  unless it is stale, so returning to the screen doesn't spend rate limit. */
    fun refresh(force: Boolean = false) {
        if (_state.value.loading) {
            rerun = true
            rerunForce = rerunForce || force
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val items = repo.loadStatuses(force)
            _state.update { it.copy(items = items, loading = false) }
            if (rerun) {
                val again = rerunForce
                rerun = false
                rerunForce = false
                refresh(again)
            }

            // If we're waiting for the user to confirm an uninstall (as part of a
            // signature-mismatch replace), and the package is now gone, finish the job.
            val key = pendingReplaceKey
            if (key != null) {
                val status = items.firstOrNull { it.source.key == key }
                if (status != null && !status.isInstalled) {
                    pendingReplaceKey = null
                    _state.update { it.copy(replacingKey = null) }
                    performInstall(status)
                }
            }
        }
    }

    /** Download (if needed) and install/update — routes through [replaceApp] first
     *  when the installed build has the wrong signature. */
    fun updateApp(status: AppStatus) {
        if (status.signatureMismatch) replaceApp(status) else performInstall(status)
    }

    fun consumePendingUninstall() {
        _state.update { it.copy(pendingUninstallPackage = null) }
    }

    private fun performInstall(status: AppStatus) {
        val remote = status.remote ?: return
        if (_state.value.busyKey != null) return
        viewModelScope.launch {
            _state.update { it.copy(busyKey = status.source.key, progress = 0f, error = null) }
            try {
                val apk = downloader.download(remote) { p ->
                    _state.update { it.copy(progress = p) }
                }
                ApkInstaller.install(getApplication(), apk, status.source.displayName)
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "שגיאה בהתקנה") }
            } finally {
                _state.update { it.copy(busyKey = null, progress = 0f) }
            }
        }
    }

    /** Downloads the new build, then asks the UI to prompt an uninstall of the
     *  wrongly-signed installed one; [refresh] auto-installs once it's gone. */
    private fun replaceApp(status: AppStatus) {
        val remote = status.remote ?: return
        if (_state.value.busyKey != null) return
        viewModelScope.launch {
            _state.update {
                it.copy(busyKey = status.source.key, progress = 0f, error = null, replacingKey = status.source.key)
            }
            try {
                downloader.download(remote) { p -> _state.update { it.copy(progress = p) } }
                pendingReplaceKey = status.source.key
                _state.update { it.copy(pendingUninstallPackage = status.source.packageName) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "שגיאה בהורדה", replacingKey = null) }
            } finally {
                _state.update { it.copy(busyKey = null, progress = 0f) }
            }
        }
    }

    fun setToken(value: String) {
        prefs.token = value
        _state.update { it.copy(token = value) }
    }

    fun setIncludePrereleases(value: Boolean) {
        prefs.includePrereleases = value
        _state.update { it.copy(includePrereleases = value) }
        refresh()
    }
}
