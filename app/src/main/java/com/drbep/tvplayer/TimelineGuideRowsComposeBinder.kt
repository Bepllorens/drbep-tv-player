package com.drbep.tvplayer

import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
internal fun TimelineGuideRows(
    model: TimelineGuideRowsUiModel,
    imageBinder: TimelineGuideChannelImageBinder,
    modifier: Modifier = Modifier.fillMaxWidth(),
    windowStart: Long = 0, windowEnd: Long = 0, labelWidth: Int = 0
) {
    val rows = model.rows ?: emptyList()
    val preferredIndex = rows.indexOfFirst { row ->
        (row.blocks ?: emptyList()).any { block -> block.preferred }
    }.coerceAtLeast(0)
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (preferredIndex - 2).coerceAtLeast(0)
    )
    var clock by remember { mutableStateOf(System.currentTimeMillis()) }
    androidx.compose.runtime.LaunchedEffect(windowStart) { while (true) { clock = System.currentTimeMillis(); kotlinx.coroutines.delay(15000) } }
    Box(modifier) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth().graphicsLayer(),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        itemsIndexed(
            items = rows,
            key = { index, row -> "${row.channelName}|${row.logoUrl}|$index" }
        ) { _, row ->
            TimelineGuideRow(row, imageBinder)
        }
    }
    if (windowEnd > windowStart) {
        androidx.compose.foundation.Canvas(Modifier.matchParentSize()) {
            // Read the ticking clock only in drawing: keep the grid out of the restart scope.
            val now = clock
            if (now !in windowStart..windowEnd) return@Canvas
            val fraction = (now-windowStart).toFloat()/(windowEnd-windowStart)
            val x = labelWidth + (size.width-labelWidth)*fraction
            drawLine(Color(0xFFFFC83D), androidx.compose.ui.geometry.Offset(x,0f), androidx.compose.ui.geometry.Offset(x,size.height), strokeWidth=2.dp.toPx())
            drawCircle(Color(0xFFFFC83D),4.dp.toPx(),androidx.compose.ui.geometry.Offset(x,4.dp.toPx()))
        }
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
                .background(OfflineTvTheme.Colors.guideTrack),
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
            .clip(OfflineTvTheme.Menu.shape)
            .background(OfflineTvTheme.Colors.guideChannel)
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
                            contentDescription = row.channelName
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
            style = TextStyle(color = OfflineTvTheme.Colors.textPrimary, fontSize = OfflineTvTheme.Typography.guide, fontWeight = FontWeight.Bold),
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
    TvRequestFocus(requester, block.preferred, block.preferred)
    val genreColor = remember(block.genre) { Color(android.graphics.Color.parseColor(ProgrammePresentation.color(block.genre))) }
    val background = when {
        block.empty -> OfflineTvTheme.Colors.guideProgramEmpty
        block.scheduled -> genreColor.copy(alpha = 0.25f)
        block.live -> genreColor.copy(alpha = 0.23f)
        else -> genreColor.copy(alpha = 0.16f)
    }
    Column(
        modifier = Modifier
            .width(width)
            .height(62.dp)
            .padding(end = 2.dp)
            .drawBehind {
                // Focus changes repaint only this cell; text and layout stay retained.
                val radius = CornerRadius(3.dp.toPx())
                drawRoundRect(if (focused) OfflineTvTheme.Colors.focusSurface else background, cornerRadius = radius)
                val stroke = if (focused) OfflineTvTheme.Control.focusBorder.toPx() else 1.dp.toPx()
                val inset = stroke / 2f
                if (focused || !block.empty) drawRoundRect(
                    if (focused) OfflineTvTheme.Colors.focus else genreColor.copy(alpha = 0.55f),
                    topLeft = Offset(inset, inset),
                    size = Size((size.width - stroke).coerceAtLeast(0f), (size.height - stroke).coerceAtLeast(0f)),
                    cornerRadius = radius, style = Stroke(stroke))
            }
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
            .tvButtonSemantics(
                block.onClick != null,
                listOf(block.title, block.time, block.status).filter { it.isNotBlank() }.joinToString(". "),
                block.preferred
            )
            .combinedClickable(enabled = block.onClick != null, onClick = { block.onClick?.run() })
            .padding(horizontal = 9.dp, vertical = 7.dp)
    ) {
        BasicText(
            text = block.title,
            style = TextStyle(color = OfflineTvTheme.Colors.textPrimary, fontSize = OfflineTvTheme.Typography.guide, fontWeight = FontWeight.Bold),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (block.time.isNotBlank() || block.status.isNotBlank()) {
            Spacer(modifier = Modifier.height(3.dp))
            BasicText(
                text = listOf(block.time, block.status).filter { it.isNotBlank() }.joinToString("  ·  "),
                style = TextStyle(color = OfflineTvTheme.Colors.textSoft, fontSize = 10.sp, fontWeight = if (block.status.isNotBlank()) FontWeight.Bold else FontWeight.Normal),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
