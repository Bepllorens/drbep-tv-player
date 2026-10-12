package com.drbep.tvplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.delay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import java.util.WeakHashMap

object TimelineGuidePanelComposeBinder {
    private val rowStates = WeakHashMap<ComposeView, MutableState<TimelineGuideRowsUiModel>>()
    @JvmStatic fun updateRows(view: ComposeView?, rows: TimelineGuideRowsUiModel) { rowStates[view]?.value = rows }
    private val detailStates = WeakHashMap<ComposeView, MutableState<TimelineProgramDetailUiModel>>()

    @JvmStatic
    fun bind(
        composeView: ComposeView?,
        model: TimelineGuidePanelUiModel,
        channelImageBinder: TimelineGuideChannelImageBinder,
        detailImageBinder: TimelineProgramDetailImageBinder
    ) {
        if (composeView == null) return
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        composeView.setContent {
            val detailState = remember { mutableStateOf(model.detail) }
            detailStates[composeView] = detailState
            val rows = remember { mutableStateOf(model.rows) }
            rowStates[composeView] = rows
            TimelineGuidePanel(model, rows.value, { detailState.value }, channelImageBinder, detailImageBinder)
        }
    }

    @JvmStatic
    fun updateDetail(composeView: ComposeView?, detail: TimelineProgramDetailUiModel?) {
        if (composeView == null || detail == null) return
        detailStates[composeView]?.value = detail
    }

    @JvmStatic
    fun clear(composeView: ComposeView?) {
        if (composeView != null) {
            detailStates.remove(composeView)
            rowStates.remove(composeView)
        }
    }
}

@Composable
private fun TimelineGuidePanel(
    model: TimelineGuidePanelUiModel,
    rows: TimelineGuideRowsUiModel,
    detail: () -> TimelineProgramDetailUiModel,
    channelImageBinder: TimelineGuideChannelImageBinder,
    detailImageBinder: TimelineProgramDetailImageBinder
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF509111B))
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF101C29))
                .padding(12.dp)
        ) {
            if (model.header != null) {
                TimelineHeader(model.header)
            }
            androidx.compose.foundation.lazy.LazyRow(state = androidx.compose.foundation.lazy.rememberLazyListState(initialFirstVisibleItemIndex = (model.days.indexOfFirst { it.label.startsWith("●") } - 1).coerceAtLeast(0)), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                items(model.days.size) { index -> TimelineHeaderAction(model.days[index], false) }
            }
            if (model.scale != null) {
                Spacer(modifier = Modifier.height(4.dp))
                TimelineScaleRow(model.scale)
            }
            TimelineGuideRows(
                rows,
                channelImageBinder,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                windowStart = model.windowStartMs, windowEnd = model.windowEndMs,
                labelWidth = model.scale?.leadingWidthPx ?: 0
            )
            Spacer(modifier = Modifier.height(4.dp))
            TimelineGuideDetail(detail, detailImageBinder)
        }
    }
}

// The focused programme invalidates this scope, not the header and channel grid.
@Composable
private fun TimelineGuideDetail(detail: () -> TimelineProgramDetailUiModel,
    imageBinder: TimelineProgramDetailImageBinder) {
    var displayed by remember { mutableStateOf(detail()) }
    LaunchedEffect(detail) {
        androidx.compose.runtime.snapshotFlow { detail() }.collectLatest {
            delay(120)
            displayed = it
        }
    }
    TimelineProgramDetailCard(displayed, imageBinder)
}
