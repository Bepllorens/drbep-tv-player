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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat

object TimelineGuideRowsComposeBinder {
    @JvmStatic
    fun bind(composeView: ComposeView?, model: TimelineGuideRowsUiModel, imageBinder: TimelineGuideChannelImageBinder) {
        if (composeView == null) return
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        composeView.setContent {
            TimelineGuideRows(model, imageBinder)
        }
    }
}

@Composable
internal fun TimelineGuideRows(model: TimelineGuideRowsUiModel, imageBinder: TimelineGuideChannelImageBinder) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        (model.rows ?: emptyList()).forEach { row ->
            TimelineGuideRow(row, imageBinder)
        }
    }
}

@Composable
private fun TimelineGuideRow(row: TimelineGuideRowUiModel, imageBinder: TimelineGuideChannelImageBinder) {
    val density = LocalDensity.current
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TimelineChannelCell(row, imageBinder, with(density) { row.labelWidthPx.toDp() })
        Row(
            modifier = Modifier
                .weight(1f)
                .height(62.dp)
                .background(Color(0xFF101820)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            (row.blocks ?: emptyList()).forEach { block ->
                val spacer = with(density) { block.spacerWidthPx.toDp() }
                if (spacer > 0.dp) {
                    Spacer(modifier = Modifier.width(spacer))
                }
                TimelineGuideProgramBlock(block, with(density) { block.blockWidthPx.toDp() })
            }
        }
    }
}

@Composable
private fun TimelineChannelCell(row: TimelineGuideRowUiModel, imageBinder: TimelineGuideChannelImageBinder, width: Dp) {
    Row(
        modifier = Modifier
            .width(width)
            .height(62.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF172333))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AndroidView(
            modifier = Modifier.size(30.dp),
            factory = { context ->
                FrameLayout(context).apply {
                    background = ContextCompat.getDrawable(context, R.drawable.channel_logo_plate_bg)
                    setPadding(2, 2, 2, 2)
                    addView(
                        AppCompatImageView(context).apply {
                            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                            scaleType = ImageView.ScaleType.FIT_CENTER
                            contentDescription = null
                        }
                    )
                }
            },
            update = { frame -> imageBinder.bind(frame.getChildAt(0) as ImageView, row) }
        )
        Spacer(modifier = Modifier.width(8.dp))
        BasicText(
            text = row.channelName,
            modifier = Modifier.weight(1f),
            style = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun TimelineGuideProgramBlock(block: TimelineGuideBlockUiModel, width: Dp) {
    var focused by remember { mutableStateOf(false) }
    val requester = remember { FocusRequester() }
    LaunchedEffect(block.preferred) {
        if (block.preferred) {
            requester.requestFocus()
        }
    }
    val background = when {
        focused && block.scheduled -> Color(0xFF9A6A1F)
        focused && block.live -> Color(0xFF49A06E)
        focused -> Color(0xFF4A6F98)
        block.empty -> Color(0xFF1E2630)
        block.scheduled -> Color(0xFF6E4A16)
        block.live -> Color(0xFF276B49)
        else -> Color(0xFF2B4056)
    }
    Column(
        modifier = Modifier
            .width(width)
            .height(62.dp)
            .padding(end = 2.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(background)
            .alpha(if (block.empty && !focused) 0.82f else 1f)
            .focusRequester(requester)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) block.onFocus?.run()
            }
            .onPreviewKeyEvent {
                if (it.nativeKeyEvent.action == KeyEvent.ACTION_DOWN && (it.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_MENU || it.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_BUTTON_START)) {
                    block.onMenu?.run()
                    true
                } else {
                    false
                }
            }
            .tvButtonSemantics(block.onClick != null)
            .combinedClickable(enabled = block.onClick != null, onClick = { block.onClick?.run() })
            .padding(horizontal = 9.dp, vertical = 7.dp)
    ) {
        BasicText(
            text = block.title,
            style = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (block.time.isNotBlank() || block.status.isNotBlank()) {
            Spacer(modifier = Modifier.height(3.dp))
            BasicText(
                text = listOf(block.time, block.status).filter { it.isNotBlank() }.joinToString("  ·  "),
                style = TextStyle(color = Color(0xFFD5E2F0), fontSize = 10.sp, fontWeight = if (block.status.isNotBlank()) FontWeight.Bold else FontWeight.Normal),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
