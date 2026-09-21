package com.yogev.youtubeupdater.ui

import android.app.Application
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
    )

    private val repo = UpdateRepository(app)
    private val prefs = Prefs(app)
    private val downloader = Downloader(app)

    private val _state = MutableStateFlow(
        UiState(
            token = prefs.token ?: "",
            includePrereleases = prefs.includePrereleases,
        )
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        refresh()
    }

    /** [force] = true hits GitHub now (the refresh button); false uses the cache
     *  unless it is stale, so returning to the screen doesn't spend rate limit. */
    fun refresh(force: Boolean = false) {
        if (_state.value.loading) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val items = repo.loadStatuses(force)
            _state.update { it.copy(items = items, loading = false) }
        }
    }

    /** Download the matching APK and hand it to the system installer. */
    fun updateApp(status: AppStatus) {
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
