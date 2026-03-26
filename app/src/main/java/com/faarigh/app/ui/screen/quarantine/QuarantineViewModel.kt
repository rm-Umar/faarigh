package com.faarigh.app.ui.screen.quarantine

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.faarigh.app.data.db.entity.AppSchedule
import com.faarigh.app.data.repository.AppScheduleRepository
import com.faarigh.app.util.InstalledApp
import com.faarigh.app.util.PackageUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QuarantineViewModel @Inject constructor(
    application: Application,
    private val repository: AppScheduleRepository,
) : AndroidViewModel(application) {

    val schedules: StateFlow<List<AppSchedule>> = repository.getAllSchedules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Installed apps for app picker
    private val _installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val installedApps: StateFlow<List<InstalledApp>> = _installedApps.asStateFlow()

    private val _showAppPicker = MutableStateFlow(false)
    val showAppPicker: StateFlow<Boolean> = _showAppPicker.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _installedApps.value = PackageUtils.getInstalledApps(getApplication())
        }
    }

    fun toggleAppPicker() { _showAppPicker.value = !_showAppPicker.value }

    fun selectApp(app: InstalledApp) {
        _formPackageName.value = app.packageName
        _formAppLabel.value = app.label
        _showAppPicker.value = false
    }

    // Form state for adding new schedule
    private val _showForm = MutableStateFlow(false)
    val showForm: StateFlow<Boolean> = _showForm.asStateFlow()

    private val _formPackageName = MutableStateFlow("")
    val formPackageName: StateFlow<String> = _formPackageName.asStateFlow()

    private val _formAppLabel = MutableStateFlow("")
    val formAppLabel: StateFlow<String> = _formAppLabel.asStateFlow()

    private val _formType = MutableStateFlow("schedule")
    val formType: StateFlow<String> = _formType.asStateFlow()

    private val _formStartHour = MutableStateFlow(22)
    val formStartHour: StateFlow<Int> = _formStartHour.asStateFlow()

    private val _formStartMin = MutableStateFlow(0)
    val formStartMin: StateFlow<Int> = _formStartMin.asStateFlow()

    private val _formEndHour = MutableStateFlow(7)
    val formEndHour: StateFlow<Int> = _formEndHour.asStateFlow()

    private val _formEndMin = MutableStateFlow(0)
    val formEndMin: StateFlow<Int> = _formEndMin.asStateFlow()

    fun toggleShowForm() {
        _showForm.value = !_showForm.value
    }

    fun setFormPackageName(value: String) { _formPackageName.value = value }
    fun setFormAppLabel(value: String) { _formAppLabel.value = value }
    fun setFormType(value: String) { _formType.value = value }
    fun setFormStartHour(value: Int) { _formStartHour.value = value.coerceIn(0, 23) }
    fun setFormStartMin(value: Int) { _formStartMin.value = value.coerceIn(0, 59) }
    fun setFormEndHour(value: Int) { _formEndHour.value = value.coerceIn(0, 23) }
    fun setFormEndMin(value: Int) { _formEndMin.value = value.coerceIn(0, 59) }

    fun addSchedule() {
        val pkg = _formPackageName.value.trim()
        val label = _formAppLabel.value.trim().ifEmpty { pkg.substringAfterLast('.') }
        if (pkg.isBlank()) return

        viewModelScope.launch {
            repository.addSchedule(
                AppSchedule(
                    packageName = pkg,
                    appLabel = label,
                    type = _formType.value,
                    startHour = _formStartHour.value,
                    startMin = _formStartMin.value,
                    endHour = _formEndHour.value,
                    endMin = _formEndMin.value,
                )
            )
            // Reset form
            _formPackageName.value = ""
            _formAppLabel.value = ""
            _formType.value = "schedule"
            _formStartHour.value = 22
            _formStartMin.value = 0
            _formEndHour.value = 7
            _formEndMin.value = 0
            _showForm.value = false
        }
    }

    fun toggleSchedule(id: Long, enabled: Boolean) {
        viewModelScope.launch { repository.toggleSchedule(id, enabled) }
    }

    fun deleteSchedule(id: Long) {
        viewModelScope.launch { repository.deleteSchedule(id) }
    }
}
