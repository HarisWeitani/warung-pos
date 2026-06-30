package com.wfx.warungpos.feature.reports

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wfx.warungpos.core.util.DateUtil
import com.wfx.warungpos.domain.model.ReportData
import com.wfx.warungpos.domain.usecase.report.ExportReportUseCase
import com.wfx.warungpos.domain.usecase.report.GetReportDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class DateRangeMode { DAY, WEEK, MONTH, CUSTOM }

data class ReportUiState(
    val mode: DateRangeMode = DateRangeMode.DAY,
    val customStart: Long? = null,
    val customEnd: Long? = null,
    val reportData: ReportData? = null,
    val isLoading: Boolean = true,
)

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val getReportDataUseCase: GetReportDataUseCase,
    private val exportReportUseCase: ExportReportUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    private val _shareEvent = Channel<Uri>(Channel.BUFFERED)
    val shareEvent: Flow<Uri> = _shareEvent.receiveAsFlow()

    init {
        load()
    }

    fun selectMode(mode: DateRangeMode) {
        if (mode == DateRangeMode.CUSTOM) {
            _uiState.update { it.copy(mode = mode) }
        } else {
            _uiState.update { it.copy(mode = mode, customStart = null, customEnd = null) }
            load()
        }
    }

    fun selectCustomRange(startEpoch: Long, endEpoch: Long) {
        _uiState.update { it.copy(mode = DateRangeMode.CUSTOM, customStart = startEpoch, customEnd = endEpoch) }
        load()
    }

    private fun computeRange(state: ReportUiState): Pair<Long, Long> {
        val now = DateUtil.nowEpochMs()
        val dayMs = 24L * 60 * 60 * 1000
        return when (state.mode) {
            DateRangeMode.DAY -> DateUtil.startOfDay(now) to DateUtil.endOfDay(now)
            DateRangeMode.WEEK -> DateUtil.startOfDay(now - 6 * dayMs) to DateUtil.endOfDay(now)
            DateRangeMode.MONTH -> DateUtil.startOfDay(now - 29 * dayMs) to DateUtil.endOfDay(now)
            DateRangeMode.CUSTOM -> {
                val start = state.customStart ?: DateUtil.startOfDay(now)
                val end = state.customEnd ?: DateUtil.endOfDay(now)
                DateUtil.startOfDay(start) to DateUtil.endOfDay(end)
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val (start, end) = computeRange(_uiState.value)
            getReportDataUseCase(start, end).onSuccess { data ->
                _uiState.update { it.copy(reportData = data, isLoading = false) }
            }
        }
    }

    fun share() {
        val data = _uiState.value.reportData ?: return
        viewModelScope.launch {
            val uri = exportReportUseCase(data, _uiState.value.mode.name)
            _shareEvent.send(uri)
        }
    }
}
