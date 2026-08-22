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
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import java.util.WeakHashMap

object VisualEpgPanelComposeBinder {
    private val detailStates = WeakHashMap<ComposeView, MutableState<TimelineProgramDetailUiModel>>()

    @JvmStatic
    fun bind(
        composeView: ComposeView?,
        model: VisualEpgPanelUiModel,
        cardImageBinder: VisualEpgCardImageBinder,
        detailImageBinder: TimelineProgramDetailImageBinder
    ) {
        if (composeView == null) return
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        composeView.setContent {
            val detailState = remember { mutableStateOf(model.detail) }
            detailStates[composeView] = detailState
            VisualEpgPanel(model, detailState.value, cardImageBinder, detailImageBinder)
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
private fun VisualEpgPanel(
    model: VisualEpgPanelUiModel,
    detail: TimelineProgramDetailUiModel,
    cardImageBinder: VisualEpgCardImageBinder,
    detailImageBinder: TimelineProgramDetailImageBinder
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OfflineTvTheme.Colors.guideBackdrop)
            .padding(dimensionResource(id = R.dimen.visual_epg_outer_padding))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(OfflineTvTheme.Colors.guidePanel)
                .padding(dimensionResource(id = R.dimen.visual_epg_inner_padding))
        ) {
            if (model.header != null) {
                VisualEpgHeader(model.header)
            }
            VisualEpgSections(
                sections = model.sections ?: emptyList(),
                imageBinder = cardImageBinder,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            TimelineProgramDetailCard(detail, detailImageBinder)
        }
    }
}
