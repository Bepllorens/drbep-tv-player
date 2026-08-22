package com.drbep.tvplayer

import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Brush
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

object ProgramInfoPanelComposeBinder {
    @JvmStatic
    fun bind(composeView: ComposeView?, model: ProgramInfoPanelUiModel, imageBinder: TimelineProgramDetailImageBinder) {
        if (composeView == null) return
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        composeView.setContent {
            ProgramInfoPanel(model, imageBinder)
        }
    }
}

@Composable
private fun ProgramInfoPanel(model: ProgramInfoPanelUiModel, imageBinder: TimelineProgramDetailImageBinder) {
    val compact = LocalConfiguration.current.screenWidthDp < 600
    val firstActionRequester = rememberTvInitialFocusRequester(!model.actions.isNullOrEmpty(), model)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OfflineTvTheme.Colors.guideBackdrop),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(if (compact) 0.94f else 0.58f)
                .fillMaxHeight(if (compact) 0.86f else 0.78f)
                .clip(RoundedCornerShape(if (compact) 22.dp else 30.dp))
                .background(Brush.verticalGradient(listOf(OfflineTvTheme.Colors.chip.copy(alpha = 0.95f), OfflineTvTheme.Colors.backdrop.copy(alpha = 0.98f))))
                .padding(if (compact) 16.dp else 22.dp)
        ) {
            BasicText(
                text = model.title,
                style = TextStyle(color = Color.White, fontSize = if (compact) 21.sp else 26.sp, fontWeight = FontWeight.Black),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(if (compact) 12.dp else 16.dp))
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(OfflineTvTheme.Colors.surfaceDeep.copy(alpha = 0.8f))
                    .padding(if (compact) 12.dp else 16.dp),
                verticalAlignment = Alignment.Top
            ) {
                if (model.detail.imageUrl.isNotEmpty()) {
                    AndroidView(
                        modifier = Modifier
                            .size(width = if (compact) 112.dp else 168.dp, height = if (compact) 154.dp else 226.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        factory = { context ->
                            FrameLayout(context).apply {
                                setBackgroundColor(OfflineTvTheme.surfaceDeepArgb())
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
                        update = { frame -> imageBinder.bind(frame.getChildAt(0) as ImageView, model.detail) }
                    )
                    Spacer(modifier = Modifier.size(if (compact) 12.dp else 16.dp))
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(if (compact) 7.dp else 9.dp)
                ) {
                    BasicText(
                        text = model.detail.title,
                        style = TextStyle(color = Color.White, fontSize = if (compact) 17.sp else 21.sp, fontWeight = FontWeight.Bold),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (model.detail.meta.isNotEmpty()) {
                        BasicText(
                            text = model.detail.meta,
                            style = TextStyle(color = OfflineTvTheme.Colors.accentCyan, fontSize = if (compact) 12.sp else 14.sp, fontWeight = FontWeight.SemiBold)
                        )
                    }
                    if (model.detail.description.isNotEmpty()) {
                        BasicText(
                            text = model.detail.description,
                            style = TextStyle(color = OfflineTvTheme.Colors.textSoft, fontSize = if (compact) 13.sp else 15.sp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(if (compact) 12.dp else 16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp)
            ) {
                (model.actions ?: emptyList()).forEachIndexed { index, action ->
                    ProgramInfoActionButton(
                        action = action,
                        compact = compact,
                        modifier = Modifier.weight(1f),
                        requester = if (index == 0) firstActionRequester else null
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgramInfoActionButton(action: TvMessageActionUiModel, compact: Boolean, modifier: Modifier, requester: FocusRequester?) {
    var focused by remember { mutableStateOf(false) }
    val background = when {
        focused -> OfflineTvTheme.Colors.focus
        action.destructive -> Color(0xFF643040)
        else -> OfflineTvTheme.Colors.chip
    }
    val textColor = if (focused) OfflineTvTheme.Colors.focusInk else Color.White
    Box(
        modifier = modifier
            .height(if (compact) 46.dp else 50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .then(if (requester != null) Modifier.focusRequester(requester) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .tvButtonSemantics(action.onClick != null)
            .clickable(enabled = action.onClick != null) { action.onClick?.run() },
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = action.label,
            style = TextStyle(color = textColor, fontSize = if (compact) 13.sp else 15.sp, fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
