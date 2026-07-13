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
            .padding(horizontal = if (compact) 12.dp else 56.dp, vertical = if (compact) 18.dp else 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(if (compact) 1f else 0.72f)
                .clip(RoundedCornerShape(if (compact) 18.dp else 24.dp))
                .background(Color(0xF0181E28))
                .border(1.dp, Color(0xFF31445A), RoundedCornerShape(if (compact) 18.dp else 24.dp))
                .padding(if (compact) 14.dp else 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            VodHeader(model, posterBinder, compact)
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
            if (model.secondaryActions.isNotEmpty()) {
                SectionTitle(model.secondaryTitle, compact)
                if (model.hint.isNotEmpty()) {
                    BasicText(
                        text = model.hint,
                        modifier = Modifier.padding(bottom = if (compact) 8.dp else 10.dp),
                        style = TextStyle(color = Color(0xFFB7C4D6), fontSize = if (compact) 12.sp else 13.sp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(if (compact) 7.dp else 9.dp)) {
                    model.secondaryActions.forEachIndexed { index, action ->
                        VodPanelAction(action, compact, if (secondaryFocusEnabled && index == 0) firstActionRequester else null)
                    }
                }
            }
        }
    }
}

@Composable
private fun VodHeader(model: VodDetailPanelUiModel, posterBinder: VodPosterBinder?, compact: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        AndroidView(
            modifier = Modifier
                .size(width = if (compact) 104.dp else 150.dp, height = if (compact) 146.dp else 204.dp)
                .clip(RoundedCornerShape(if (compact) 12.dp else 16.dp))
                .background(Color(0xFF0E1820)),
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
            BasicText(
                text = model.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(color = Color.White, fontSize = if (compact) 20.sp else 26.sp, fontWeight = FontWeight.Bold)
            )
            if (model.meta.isNotEmpty()) {
                Spacer(modifier = Modifier.height(if (compact) 6.dp else 8.dp))
                BasicText(
                    text = model.meta,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(color = Color(0xFF9BD0FF), fontSize = if (compact) 13.sp else 15.sp, fontWeight = FontWeight.Bold)
                )
            }
            if (model.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(if (compact) 10.dp else 14.dp))
                BasicText(
                    text = model.description,
                    maxLines = if (compact) 5 else 4,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(color = Color(0xFFD5E6F8), fontSize = if (compact) 12.sp else 14.sp)
                )
            }
            if (model.progressLabel.isNotEmpty()) {
                Spacer(modifier = Modifier.height(if (compact) 10.dp else 14.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF2F3A25))
                        .border(1.dp, Color(0xFFFFD782), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 9.dp)
                ) {
                    BasicText(
                        text = model.progressLabel,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(color = Color(0xFFFFD782), fontSize = if (compact) 12.sp else 14.sp, fontWeight = FontWeight.Bold)
                    )
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
    val fill = when {
        focused -> Color(0xFFFFD782)
        action.primary -> Color(0xFF3A6EA5)
        else -> Color(0xFF172536)
    }
    val textColor = if (focused) Color(0xFF111820) else Color.White
    val stroke = when {
        focused -> Color.White
        action.primary -> Color(0xFFFFD782)
        else -> Color(0xFF2B4057)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth(if (action.primary) if (compact) 1f else 0.31f else 1f)
            .height(if (action.primary) if (compact) 48.dp else 54.dp else if (compact) 42.dp else 48.dp)
            .clip(RoundedCornerShape(if (action.primary) 12.dp else 10.dp))
            .background(fill)
            .border(1.dp, stroke, RoundedCornerShape(if (action.primary) 12.dp else 10.dp))
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .tvButtonSemantics(action.onClick != null)
            .clickable(enabled = action.onClick != null) { action.onClick?.run() }
            .padding(horizontal = if (compact) 12.dp else 16.dp),
        contentAlignment = if (action.primary) Alignment.Center else Alignment.CenterStart
    ) {
        BasicText(
            text = action.label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(color = textColor, fontSize = if (compact) 14.sp else 16.sp, fontWeight = FontWeight.Bold)
        )
    }
}
