package com.drbep.tvplayer

import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
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
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat

object VodVisualPanelComposeBinder {
    @JvmStatic
    fun bind(composeView: ComposeView?, model: VodVisualPanelUiModel, imageBinder: VodVisualPosterImageBinder) {
        if (composeView == null) return
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        composeView.setContent {
            VodVisualPanel(model, imageBinder)
        }
    }
}

@Composable
private fun VodVisualPanel(model: VodVisualPanelUiModel, imageBinder: VodVisualPosterImageBinder) {
    val compact = LocalConfiguration.current.screenWidthDp < 720
    val firstActionRequester = rememberTvInitialFocusRequester(!model.actions.isNullOrEmpty(), model)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(Color(0xCC000000)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(if (compact) 0.94f else 0.96f)
                .fillMaxHeight(if (compact) 0.94f else 0.91f)
                .clip(RoundedCornerShape(if (compact) 20.dp else 28.dp))
                .background(Brush.verticalGradient(listOf(OfflineTvTheme.Colors.chip.copy(alpha = 0.95f), OfflineTvTheme.Colors.backdrop.copy(alpha = 0.98f))))
                .padding(if (compact) 14.dp else 20.dp)
        ) {
            BasicText(
                text = model.title,
                style = TextStyle(color = Color.White, fontSize = if (compact) 22.sp else 28.sp, fontWeight = FontWeight.Black),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(5.dp))
            BasicText(
                text = model.subtitle,
                style = TextStyle(color = OfflineTvTheme.Colors.textSoft, fontSize = if (compact) 12.sp else 14.sp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(if (compact) 8.dp else 10.dp))
            BasicText(
                text = model.help,
                style = TextStyle(color = Color(0xFFFFD887), fontSize = if (compact) 11.sp else 12.sp, fontWeight = FontWeight.Bold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(if (compact) 10.dp else 12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(if (compact) 7.dp else 9.dp)) {
                items((model.actions ?: emptyList()).withIndex().toList()) { indexedAction ->
                    VodVisualActionChip(indexedAction.value, compact, if (indexedAction.index == 0) firstActionRequester else null)
                }
            }
            Spacer(modifier = Modifier.height(if (compact) 12.dp else 16.dp))
            if ((model.sections ?: emptyList()).isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    BasicText(
                        text = model.emptyLabel,
                        style = TextStyle(color = OfflineTvTheme.Colors.textSoft, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(if (compact) 16.dp else 20.dp)
                ) {
                    items(model.sections ?: emptyList()) { section ->
                        VodVisualSection(section, imageBinder, compact)
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun VodVisualActionChip(action: VodVisualActionUiModel, compact: Boolean, focusRequester: FocusRequester?) {
    var focused by remember { mutableStateOf(false) }
    val background = if (focused) OfflineTvTheme.Colors.focus else if (action.filter) OfflineTvTheme.Colors.chipSelected else OfflineTvTheme.Colors.chip
    val textColor = if (focused) OfflineTvTheme.Colors.backdropAccent else Color.White
    Box(
        modifier = Modifier
            .height(if (compact) 38.dp else 42.dp)
            .width(if (compact) 138.dp else 170.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(background)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .tvButtonSemantics(action.onClick != null)
            .combinedClickable(enabled = action.onClick != null, onClick = { action.onClick?.run() })
            .padding(horizontal = if (compact) 10.dp else 12.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = action.label,
            style = TextStyle(color = textColor, fontSize = if (compact) 11.sp else 13.sp, fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun VodVisualSection(section: VodVisualSectionUiModel, imageBinder: VodVisualPosterImageBinder, compact: Boolean) {
    Column(modifier = Modifier.fillMaxWidth()) {
        BasicText(
            text = section.title,
            style = TextStyle(color = Color.White, fontSize = if (compact) 16.sp else 18.sp, fontWeight = FontWeight.Black),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (section.subtitle.isNotBlank()) {
            Spacer(modifier = Modifier.height(3.dp))
            BasicText(
                text = section.subtitle,
                style = TextStyle(color = OfflineTvTheme.Colors.textMuted, fontSize = if (compact) 10.sp else 12.sp, fontWeight = FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.height(if (compact) 8.dp else 10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp)) {
            items(section.items ?: emptyList()) { item ->
                VodVisualPoster(item, imageBinder, compact)
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun VodVisualPoster(item: VodVisualItemUiModel, imageBinder: VodVisualPosterImageBinder, compact: Boolean) {
    var focused by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .width(if (compact) 124.dp else 150.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (focused) OfflineTvTheme.Colors.focusSurface else OfflineTvTheme.Colors.surfaceDeep)
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent {
                if (it.nativeKeyEvent.action == KeyEvent.ACTION_DOWN && it.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_MENU) {
                    item.onMenu?.run()
                    true
                } else {
                    false
                }
            }
            .tvButtonSemantics(item.onClick != null)
            .combinedClickable(enabled = item.onClick != null, onClick = { item.onClick?.run() }, onLongClick = item.onMenu?.let { { it.run() } })
            .padding(if (compact) 8.dp else 10.dp)
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 150.dp else 184.dp)
                .clip(RoundedCornerShape(12.dp)),
            factory = { context ->
                FrameLayout(context).apply {
                    background = ContextCompat.getDrawable(context, R.drawable.channel_logo_plate_bg)
                    addView(
                        AppCompatImageView(context).apply {
                            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                            scaleType = ImageView.ScaleType.FIT_CENTER
                            contentDescription = null
                        }
                    )
                }
            },
            update = { frame ->
                imageBinder.bind(frame.getChildAt(0) as ImageView, item)
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        BasicText(
            text = item.title,
            style = TextStyle(color = Color.White, fontSize = if (compact) 12.sp else 13.sp, fontWeight = FontWeight.Bold),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (item.meta.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            BasicText(
                text = item.meta,
                style = TextStyle(color = OfflineTvTheme.Colors.textSoft, fontSize = if (compact) 10.sp else 11.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (item.progressLabel.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF2C6B58))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                BasicText(
                    text = item.progressLabel,
                    style = TextStyle(color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
