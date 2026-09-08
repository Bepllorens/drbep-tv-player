package com.drbep.tvplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
            TimelineGuidePanel(model, detailState.value, channelImageBinder, detailImageBinder)
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
        }
    }
}

@Composable
private fun TimelineGuidePanel(
    model: TimelineGuidePanelUiModel,
    detail: TimelineProgramDetailUiModel,
    channelImageBinder: TimelineGuideChannelImageBinder,
    detailImageBinder: TimelineProgramDetailImageBinder
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OfflineTvTheme.Colors.guideBackdrop)
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(OfflineTvTheme.Colors.guidePanel)
                .padding(12.dp)
        ) {
            if (model.header != null) {
                TimelineHeader(model.header)
            }
            if (model.scale != null) {
                Spacer(modifier = Modifier.height(8.dp))
                TimelineScaleRow(model.scale)
            }
            TimelineGuideRows(
                model.rows ?: TimelineGuideRowsUiModel(emptyList()),
                channelImageBinder,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            TimelineProgramDetailCard(detail, detailImageBinder)
        }
    }
}
