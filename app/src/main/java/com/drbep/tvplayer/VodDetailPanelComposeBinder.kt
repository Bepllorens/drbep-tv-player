package com.drbep.tvplayer

import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

object VodDetailPanelComposeBinder {
    @JvmStatic
    fun bind(composeView: ComposeView?, model: VodDetailPanelUiModel, posterBinder: VodPosterBinder?) {
        if (composeView == null) return
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        composeView.setContent {
            VodDetailPanel(model, posterBinder)
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun VodDetailPanel(model: VodDetailPanelUiModel, posterBinder: VodPosterBinder?) {
    val compact = LocalConfiguration.current.screenWidthDp < 600
    val primaryFocusEnabled = model.primaryActions.any { it.onClick != null }
    val secondaryFocusEnabled = !primaryFocusEnabled && model.secondaryActions.any { it.onClick != null }
    val firstActionRequester = rememberTvInitialFocusRequester(primaryFocusEnabled || secondaryFocusEnabled, model)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .tvPanelBackHandler(model.onBack)
            .padding(horizontal = if (compact) 12.dp else 56.dp, vertical = if (compact) 18.dp else 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(if (compact) 1f else 0.82f)
                .clip(RoundedCornerShape(if (compact) 18.dp else 24.dp))
                .background(OfflineTvTheme.Colors.panelGlass)
                .border(1.dp, OfflineTvTheme.Colors.chipSelected.copy(alpha = 0.4f), RoundedCornerShape(if (compact) 18.dp else 24.dp))
                .padding(if (compact) 14.dp else 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            VodHeader(model, posterBinder, compact, firstActionRequester)
            if (model.secondaryActions.isNotEmpty()) {
                SectionTitle(model.secondaryTitle, compact)
                if (model.hint.isNotEmpty()) {
                    BasicText(
                        text = model.hint,
                        modifier = Modifier.padding(bottom = if (compact) 8.dp else 10.dp),
                        style = TextStyle(color = OfflineTvTheme.Colors.textSoft, fontSize = if (compact) 12.sp else 13.sp)
                    )
                }
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(if (compact) 7.dp else 9.dp),
                    verticalArrangement = Arrangement.spacedBy(if (compact) 7.dp else 9.dp)
                ) {
                    model.secondaryActions.forEachIndexed { index, action ->
                        VodPanelAction(action, compact, if (secondaryFocusEnabled && index == 0) firstActionRequester else null)
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun VodHeader(
    model: VodDetailPanelUiModel,
    posterBinder: VodPosterBinder?,
    compact: Boolean,
    firstActionRequester: FocusRequester
) {
    BasicText(
        text = model.title,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(
            color = Color.White,
            fontSize = if (compact) 22.sp else 30.sp,
            fontWeight = FontWeight.Bold
        )
    )
    if (model.meta.isNotEmpty()) {
        Spacer(modifier = Modifier.height(if (compact) 5.dp else 7.dp))
        BasicText(
            text = model.meta,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                color = OfflineTvTheme.Colors.accentCyan,
                fontSize = if (compact) 13.sp else 15.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
    Spacer(modifier = Modifier.height(if (compact) 12.dp else 18.dp))
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        AndroidView(
            modifier = Modifier
                .size(width = if (compact) 112.dp else 160.dp, height = if (compact) 168.dp else 240.dp)
                .clip(RoundedCornerShape(if (compact) 12.dp else 16.dp))
                .background(OfflineTvTheme.Colors.backdrop),
            factory = { context ->
                FrameLayout(context).apply {
                    addView(
                        AppCompatImageView(context).apply {
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            scaleType = ImageView.ScaleType.CENTER_CROP
                            contentDescription = null
                        }
                    )
                }
            },
            update = { frame ->
                posterBinder?.bind(frame.getChildAt(0) as ImageView, VodDetailHeaderUiModel(model.title, model.meta, model.description, model.progressLabel, model.posterUrl))
            }
        )
        Spacer(modifier = Modifier.width(if (compact) 12.dp else 18.dp))
        Column(modifier = Modifier.weight(1f)) {
            if (model.progressLabel.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF2F3A25))
                        .border(1.dp, OfflineTvTheme.Colors.focus, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 9.dp)
                ) {
                    BasicText(
                        text = model.progressLabel,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(color = OfflineTvTheme.Colors.focus, fontSize = if (compact) 12.sp else 14.sp, fontWeight = FontWeight.Bold)
                    )
                }
            }
            if (model.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(if (compact) 10.dp else 14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF121B26))
                        .padding(horizontal = if (compact) 12.dp else 16.dp, vertical = if (compact) 10.dp else 14.dp)
                ) {
                    BasicText(
                        text = model.description,
                        maxLines = if (compact) 5 else 4,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(
                            color = OfflineTvTheme.Colors.textSoft,
                            fontSize = if (compact) 12.sp else 14.sp
                        )
                    )
                }
            }
            if (model.primaryActions.isNotEmpty()) {
                SectionTitle(model.primaryTitle, compact)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp),
                    verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp)
                ) {
                    model.primaryActions.forEachIndexed { index, action ->
                        VodPanelAction(action, compact, if (index == 0) firstActionRequester else null)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, compact: Boolean) {
    if (title.isEmpty()) return
    BasicText(
        text = title,
        modifier = Modifier.padding(top = if (compact) 16.dp else 22.dp, bottom = if (compact) 8.dp else 10.dp),
        style = TextStyle(color = Color.White, fontSize = if (compact) 14.sp else 16.sp, fontWeight = FontWeight.Bold)
    )
}

@Composable
private fun VodPanelAction(action: VodPanelActionUiModel, compact: Boolean, focusRequester: FocusRequester?) {
    var focused by remember { mutableStateOf(false) }
    var nowMs by remember(action) { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(action) {
        if (action.availableAtMs > 0L && nowMs < action.availableAtMs) {
            while (nowMs < action.availableAtMs) {
                delay(minOf(1_000L, maxOf(100L, action.availableAtMs - nowMs)))
                nowMs = System.currentTimeMillis()
            }
        }
    }
    val enabled = action.isEnabledAt(nowMs)
    val actionLabel = action.labelAt(nowMs)
    val liveAction = action.tone == "live"
    val startOverAction = action.tone == "start_over"
    val fill = when {
        focused -> OfflineTvTheme.Colors.focus
        liveAction -> Color(0xFFD5202A)
        startOverAction -> Color(0xFFF4F5F7)
        action.primary -> OfflineTvTheme.Colors.chipSelected
        else -> OfflineTvTheme.Colors.chip
    }
    val textColor = if (focused || startOverAction) OfflineTvTheme.Colors.backdropAccent else Color.White
    val stroke = when {
        focused -> Color.White
        liveAction -> Color(0xFFFF6670)
        startOverAction -> Color.White
        action.primary -> OfflineTvTheme.Colors.focus
        else -> OfflineTvTheme.Colors.card
    }
    Box(
        modifier = Modifier
            .fillMaxWidth(
                if (action.primary) {
                    if (compact) 1f else 0.48f
                } else {
                    if (compact) 1f else 0.32f
                }
            )
            .height(if (action.primary) if (compact) 48.dp else 54.dp else if (compact) 42.dp else 48.dp)
            .clip(RoundedCornerShape(if (action.primary) 12.dp else 10.dp))
            .background(fill)
            .border(1.dp, stroke, RoundedCornerShape(if (action.primary) 12.dp else 10.dp))
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .tvButtonSemantics(enabled)
            .clickable(enabled = enabled) { action.onClick?.run() }
            .padding(horizontal = if (compact) 12.dp else 16.dp),
        contentAlignment = if (action.primary) Alignment.Center else Alignment.CenterStart
    ) {
        BasicText(
            text = actionLabel,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(color = textColor, fontSize = if (compact) 14.sp else 16.sp, fontWeight = FontWeight.Bold)
        )
    }
}
