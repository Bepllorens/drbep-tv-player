package com.drbep.tvplayer

import android.graphics.Color as AndroidColor
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

object RecordingsSurfaceComposeBinder {
    @JvmStatic
    fun bind(composeView: ComposeView?, model: RecordingsSurfaceUiModel, posterBinder: RecordingPosterBinder?) {
        if (composeView == null) return
        composeView.setStableContent("recordings-surface", model) { currentModel ->
            RecordingsSurface(currentModel, posterBinder)
        }
    }
}

@Composable
private fun RecordingsSurface(model: RecordingsSurfaceUiModel, posterBinder: RecordingPosterBinder?) {
    val compact = LocalConfiguration.current.screenWidthDp < 600
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xEF171B22))
            .padding(dimensionResource(id = R.dimen.recordings_panel_padding))
    ) {
        model.panel?.let { RecordingsHeader(it, posterBinder, compact) }
        Spacer(modifier = Modifier.height(if (compact) 12.dp else 14.dp))
        RecordingRows(
            model = model.list ?: RecordingListUiModel(emptyList(), -1),
            posterBinder = posterBinder,
            compact = compact,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun RecordingsHeader(model: RecordingsPanelUiModel, posterBinder: RecordingPosterBinder?, compact: Boolean) {
    val completedRequester = remember { FocusRequester() }
    val scheduledRequester = remember { FocusRequester() }
    val refreshRequester = remember { FocusRequester() }
    TvRequestFocus(completedRequester, model.focusedActionIndex == 0, model.scheduledMode, model.focusedActionIndex)
    TvRequestFocus(scheduledRequester, model.focusedActionIndex == 1, model.scheduledMode, model.focusedActionIndex)
    TvRequestFocus(refreshRequester, model.focusedActionIndex == 2, model.scheduledMode, model.focusedActionIndex)
    Column(modifier = Modifier.fillMaxWidth()) {
        BasicText(
            text = model.sectionTitle,
            style = TextStyle(color = Color.White, fontSize = if (compact) 20.sp else 24.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(4.dp))
        BasicText(
            text = model.summary,
            style = TextStyle(color = Color(0xFF9BD0FF), fontSize = if (compact) 11.sp else 12.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(if (compact) 6.dp else 8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RecordingChip(
                label = "Completadas",
                selected = !model.scheduledMode,
                modifier = Modifier.weight(1f),
                visuallyFocused = model.focusedActionIndex == 0,
                focusRequester = completedRequester,
                onLeft = null,
                onRight = { scheduledRequester.requestFocus() },
                onClick = model.onCompletedClick
            )
            RecordingChip(
                label = "Programadas",
                selected = model.scheduledMode,
                modifier = Modifier.weight(1f),
                visuallyFocused = model.focusedActionIndex == 1,
                focusRequester = scheduledRequester,
                onLeft = { completedRequester.requestFocus() },
                onRight = { refreshRequester.requestFocus() },
                onClick = model.onScheduledClick
            )
            RecordingChip(
                label = "Refrescar",
                selected = false,
                modifier = Modifier.width(if (compact) 84.dp else 96.dp),
                visuallyFocused = model.focusedActionIndex == 2,
                focusRequester = refreshRequester,
                onLeft = { scheduledRequester.requestFocus() },
                onRight = null,
                onClick = model.onRefreshClick
            )
        }
        Spacer(modifier = Modifier.height(if (compact) 8.dp else 10.dp))
        BasicText(
            text = model.hint,
            style = TextStyle(color = Color(0xFFB8C8D8), fontSize = if (compact) 12.sp else 14.sp)
        )
        Spacer(modifier = Modifier.height(if (compact) 10.dp else 12.dp))
        RecordingDetail(model, posterBinder, compact)
    }
}

@Composable
private fun RecordingDetail(model: RecordingsPanelUiModel, posterBinder: RecordingPosterBinder?, compact: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2F3540), RoundedCornerShape(16.dp))
            .padding(if (compact) 12.dp else 14.dp)
    ) {
        if (model.posterUrl.isNotEmpty()) {
            AndroidView(
                factory = { context ->
                    ImageView(context).apply {
                        scaleType = ImageView.ScaleType.CENTER_CROP
                    }
                },
                update = { imageView -> posterBinder?.bind(imageView, model.posterUrl) },
                modifier = Modifier.size(width = if (compact) 72.dp else 86.dp, height = if (compact) 108.dp else 128.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(width = if (compact) 72.dp else 86.dp, height = if (compact) 108.dp else 128.dp)
                    .background(Color(0xFF1F252F), RoundedCornerShape(10.dp))
            )
        }
        Spacer(modifier = Modifier.width(if (compact) 10.dp else 14.dp))
        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                text = model.detailTitle,
                style = TextStyle(color = Color.White, fontSize = if (compact) 17.sp else 20.sp, fontWeight = FontWeight.Bold)
            )
            if (model.detailMeta.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                BasicText(
                    text = model.detailMeta,
                    style = TextStyle(color = argb(model.detailMetaColor), fontSize = if (compact) 12.sp else 14.sp)
                )
            }
            if (model.detailPathVisible && model.detailPath.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                BasicText(
                    text = model.detailPath,
                    style = TextStyle(color = Color(0xFFC7D2E2), fontSize = if (compact) 11.sp else 12.sp)
                )
            }
            if (model.detailAction.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                BasicText(
                    text = model.detailAction,
                    style = TextStyle(color = Color(0xFF9BD0FF), fontSize = if (compact) 11.sp else 12.sp, fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
private fun RecordingRows(model: RecordingListUiModel, posterBinder: RecordingPosterBinder?, compact: Boolean, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    LaunchedEffect(model.scrollToIndex, model.items.size) {
        if (model.scrollToIndex >= 0 && model.items.isNotEmpty()) {
            listState.scrollToItem(model.scrollToIndex.coerceIn(0, model.items.lastIndex))
        }
    }
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth()
    ) {
        itemsIndexed(model.items) { _, item ->
            RecordingRow(item, posterBinder, compact)
        }
    }
}

@Composable
private fun RecordingRow(item: RecordingListRowUiModel, posterBinder: RecordingPosterBinder?, compact: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = if (compact) 6.dp else 8.dp)
            .background(if (item.selected) Color(0xFF80542A) else Color(0xFF2C2419), RoundedCornerShape(14.dp))
            .tvButtonSemantics(item.onClick != null)
            .clickable(enabled = item.onClick != null) { item.onClick?.run() }
            .padding(if (compact) 8.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AndroidView(
            factory = { context ->
                ImageView(context).apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                }
            },
            update = { imageView -> posterBinder?.bind(imageView, item.posterUrl) },
            modifier = Modifier.size(width = if (compact) 46.dp else 54.dp, height = if (compact) 62.dp else 72.dp)
        )
        Spacer(modifier = Modifier.width(if (compact) 10.dp else 12.dp))
        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                text = item.title,
                style = TextStyle(color = Color.White, fontSize = if (compact) 14.sp else 16.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(if (compact) 4.dp else 6.dp))
            BasicText(
                text = item.meta,
                style = TextStyle(color = argb(item.metaColor), fontSize = if (compact) 11.sp else 12.sp)
            )
        }
        Spacer(modifier = Modifier.width(if (compact) 8.dp else 10.dp))
        Box(
            modifier = Modifier
                .background(argb(item.statusBadgeColor), RoundedCornerShape(12.dp))
                .padding(horizontal = if (compact) 8.dp else 10.dp, vertical = if (compact) 5.dp else 6.dp),
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                text = item.statusLabel,
                style = TextStyle(color = Color(0xFFFFF3E0), fontSize = if (compact) 10.sp else 11.sp, fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
private fun RecordingChip(
    label: String,
    selected: Boolean,
    modifier: Modifier,
    visuallyFocused: Boolean,
    focusRequester: FocusRequester,
    onLeft: (() -> Unit)?,
    onRight: (() -> Unit)?,
    onClick: Runnable?
) {
    val compact = LocalConfiguration.current.screenWidthDp < 600
    var focused by remember { mutableStateOf(false) }
    val activeFocus = visuallyFocused || focused
    val background = when {
        activeFocus -> Color(0xFFFFD47A)
        selected -> Color(0xFF2A7C86)
        else -> Color(0xFF2B3642)
    }
    val textColor = if (activeFocus) Color(0xFF101722) else Color.White
    Box(
        modifier = modifier
            .height(if (compact) 36.dp else 40.dp)
            .background(background, RoundedCornerShape(14.dp))
            .focusRequester(focusRequester)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) {
                    return@onPreviewKeyEvent false
                }
                when (event.key) {
                    Key.DirectionLeft -> {
                        onLeft?.invoke()
                        onLeft != null
                    }
                    Key.DirectionRight -> {
                        onRight?.invoke()
                        onRight != null
                    }
                    Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> {
                        onClick?.run()
                        onClick != null
                    }
                    else -> false
                }
            }
            .tvButtonSemantics(onClick != null)
            .clickable(enabled = onClick != null) { onClick?.run() },
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = label,
            style = TextStyle(color = textColor, fontSize = if (compact) 12.sp else 13.sp, fontWeight = FontWeight.Bold)
        )
    }
}

private fun argb(value: Int): Color {
    return Color(
        AndroidColor.red(value),
        AndroidColor.green(value),
        AndroidColor.blue(value),
        AndroidColor.alpha(value)
    )
}
