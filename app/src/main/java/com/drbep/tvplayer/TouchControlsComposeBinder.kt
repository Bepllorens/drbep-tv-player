package com.drbep.tvplayer

import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt

object TouchControlsComposeBinder {
    @JvmStatic
    fun bind(
        composeView: ComposeView?,
        model: TouchControlsBarUiModel,
        artworkBinder: TouchControlsArtworkBinder? = null
    ) {
        if (composeView == null) return
        composeView.setStableContent("touch-controls", model) { currentModel ->
            TouchControlsBar(model = currentModel, artworkBinder = artworkBinder)
        }
    }
}

@Composable
private fun TouchControlsBar(model: TouchControlsBarUiModel, artworkBinder: TouchControlsArtworkBinder?) {
    if (model.modernStyle) {
        ModernTouchControlsBar(model, artworkBinder)
        return
    }
    ClassicTouchControlsBar(model, artworkBinder)
}

@Composable
private fun ClassicTouchControlsBar(model: TouchControlsBarUiModel, artworkBinder: TouchControlsArtworkBinder?) {
    val compact = LocalConfiguration.current.screenWidthDp < 600
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OfflineTvTheme.Colors.panelGlass, RoundedCornerShape(22.dp))
            .padding(horizontal = 8.dp, vertical = 7.dp)
    ) {
        if (model.nowPlaying.visible) {
            TouchNowPlayingHeader(model = model.nowPlaying, artworkBinder = artworkBinder)
        } else if (model.contextTitle.isNotBlank() || model.contextSubtitle.isNotBlank()) {
            TouchContextHeader(model)
        }
        if (!model.expanded) {
            BasicText(
                text = stringResource(id = R.string.playback_hud_expand_hint),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                style = TextStyle(
                    color = OfflineTvTheme.Colors.textSoft,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End
                )
            )
        }
        if (model.expanded) BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val actionCount = model.actions.size.coerceAtLeast(1)
            val firstEnabledIndex = model.actions.indexOfFirst { it.enabled }
            val focusedIndex = model.focusedActionIndex.coerceIn(0, model.actions.lastIndex.coerceAtLeast(0))
            val scrollState = rememberScrollState()
            val density = LocalDensity.current
            val firstButtonFocusRequester = rememberTvInitialFocusRequester(
                enabled = firstEnabledIndex >= 0,
                model.actions.size,
                model.contextSubtitle,
                focusedIndex
            )
            val minimumChipWidth = if (compact) 104.dp else 112.dp
            val maximumCenteredChipWidth = if (compact) 132.dp else 156.dp
            val minimumContentWidth =
                (minimumChipWidth * actionCount) + (8.dp * (actionCount - 1).coerceAtLeast(0)) + 24.dp
            val shouldCenter = minimumContentWidth <= maxWidth
            val chipWidth = if (shouldCenter) {
                ((maxWidth - 24.dp - (8.dp * (actionCount - 1).coerceAtLeast(0))) / actionCount)
                    .coerceIn(minimumChipWidth, maximumCenteredChipWidth)
            } else {
                0.dp
            }
            LaunchedEffect(focusedIndex, actionCount, shouldCenter, compact) {
                if (!shouldCenter && actionCount > 1) {
                    val chipPx = with(density) { minimumChipWidth.roundToPx() }
                    val gapPx = with(density) { 8.dp.roundToPx() }
                    val leadingPx = with(density) { 8.dp.roundToPx() }
                    val target = leadingPx + ((chipPx + gapPx) * focusedIndex) - chipPx
                    scrollState.animateScrollTo(target.coerceIn(0, scrollState.maxValue))
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                horizontalArrangement = if (shouldCenter) Arrangement.Center else Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(8.dp))
                model.actions.forEachIndexed { index, item ->
                    TouchControlChip(
                        item = item,
                        modifier = if (shouldCenter) Modifier.width(chipWidth) else Modifier.defaultMinSize(minWidth = minimumChipWidth),
                        focused = index == focusedIndex,
                        focusRequester = if (index == focusedIndex) firstButtonFocusRequester else null
                    )
                    if (index < model.actions.lastIndex) {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
            }
        }
    }
}

@Composable
private fun ModernTouchControlsBar(model: TouchControlsBarUiModel, artworkBinder: TouchControlsArtworkBinder?) {
    val compact = LocalConfiguration.current.screenWidthDp < 600
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(OfflineTvTheme.Colors.backdropAccent.copy(alpha = 0.77f), OfflineTvTheme.Colors.backdrop.copy(alpha = 0.95f))
                ),
                RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            )
            .border(1.dp, OfflineTvTheme.Colors.chipSelected.copy(alpha = 0.33f), RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .padding(horizontal = if (compact) 12.dp else 20.dp, vertical = if (compact) 10.dp else 14.dp)
    ) {
        if (model.nowPlaying.visible) {
            ModernNowPlaying(model.nowPlaying, model.integratedTimeshift, artworkBinder)
        } else if (model.contextTitle.isNotBlank() || model.contextSubtitle.isNotBlank()) {
            ModernContextHeader(model)
        }
        if (!model.expanded) {
            BasicText(
                text = stringResource(id = R.string.playback_hud_expand_hint),
                modifier = Modifier.fillMaxWidth().padding(top = 7.dp),
                style = TextStyle(color = OfflineTvTheme.Colors.textSoft, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
            )
        } else {
            ModernActionStrip(model, artworkBinder)
        }
    }
}

@Composable
private fun ModernNowPlaying(model: TouchControlsNowPlayingUiModel, timeshift: TimeshiftBarUiModel?, artworkBinder: TouchControlsArtworkBinder?) {
    val compact = LocalConfiguration.current.screenWidthDp < 600
    var timeshiftDragging by remember { mutableStateOf(false) }
    var displayedTimeshiftProgress by remember { mutableIntStateOf(timeshift?.progress ?: 0) }
    var displayedTimeshiftLabel by remember { mutableStateOf(timeshift?.statusLabel.orEmpty()) }
    val latestTimeshift by rememberUpdatedState(timeshift)
    LaunchedEffect(timeshift?.progress, timeshift?.statusLabel, timeshiftDragging) {
        if (!timeshiftDragging) {
            displayedTimeshiftProgress = timeshift?.progress ?: 0
            displayedTimeshiftLabel = timeshift?.statusLabel.orEmpty()
        }
    }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TouchChannelLogo(model, artworkBinder, compact)
        Spacer(Modifier.width(if (compact) 10.dp else 16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (model.channelBadge.isNotBlank()) {
                    ModernBadge(model.channelBadge)
                    Spacer(Modifier.width(8.dp))
                }
                BasicText(
                    text = model.channelName,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(color = OfflineTvTheme.Colors.accentGold, fontSize = if (compact) 12.sp else 14.sp, fontWeight = FontWeight.Black)
                )
                if (model.remainingText.isNotBlank()) {
                    BasicText(model.remainingText, style = TextStyle(color = OfflineTvTheme.Colors.textSoft, fontSize = 11.sp, fontWeight = FontWeight.Bold))
                }
            }
            Spacer(Modifier.height(3.dp))
            BasicText(
                text = model.programTitle,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(color = Color.White, fontSize = if (compact) 17.sp else 22.sp, fontWeight = FontWeight.Black)
            )
            if (model.programMeta.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                BasicText(model.programMeta, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = TextStyle(color = OfflineTvTheme.Colors.textSoft, fontSize = if (compact) 11.sp else 12.sp))
            }
            if (model.nextProgramVisible) {
                Spacer(Modifier.height(2.dp))
                BasicText(model.nextProgram, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = TextStyle(color = OfflineTvTheme.Colors.focus, fontSize = 11.sp, fontWeight = FontWeight.Bold))
            }
            if (model.progressVisible || timeshift != null) {
                Spacer(Modifier.height(8.dp))
                if (timeshift != null) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        BasicText(displayedTimeshiftLabel, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                            style = TextStyle(color = OfflineTvTheme.Colors.textSoft, fontSize = 10.sp, fontWeight = FontWeight.Bold))
                        if (timeshift.liveVisible) {
                            BasicText(
                                text = stringResource(R.string.timeshift_live_button),
                                modifier = Modifier.background(OfflineTvTheme.Colors.card, RoundedCornerShape(99.dp))
                                    .clickable(enabled = timeshift.onLiveClick != null) { timeshift.onLiveClick?.run() }
                                    .padding(horizontal = 9.dp, vertical = 3.dp),
                                style = TextStyle(color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val integratedProgress = (if (timeshift != null) displayedTimeshiftProgress else null)?.coerceIn(0, 1000)?.div(1000f)
                        ?: (model.progress.coerceIn(0, 100) / 100f)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(if (timeshift != null) 32.dp else 5.dp)
                            .then(
                                if (timeshift?.seekCommitHandler != null) {
                                    Modifier
                                        .pointerInput(Unit) {
                                            detectTapGestures { offset ->
                                                val active = latestTimeshift ?: return@detectTapGestures
                                                val progress = ((offset.x / size.width.coerceAtLeast(1)) * 1000f).roundToInt().coerceIn(0, 1000)
                                                timeshiftDragging = true
                                                active.onSeekStart?.run()
                                                displayedTimeshiftProgress = progress
                                                displayedTimeshiftLabel = active.previewLabelProvider?.provide(progress) ?: active.statusLabel
                                                active.seekCommitHandler?.seekTo(progress)
                                                active.onSeekEnd?.run()
                                                timeshiftDragging = false
                                            }
                                        }
                                        .pointerInput(Unit) {
                                            detectHorizontalDragGestures(
                                                onDragStart = { offset ->
                                                    val active = latestTimeshift ?: return@detectHorizontalDragGestures
                                                    timeshiftDragging = true
                                                    active.onSeekStart?.run()
                                                    val progress = ((offset.x / size.width.coerceAtLeast(1)) * 1000f).roundToInt().coerceIn(0, 1000)
                                                    displayedTimeshiftProgress = progress
                                                    displayedTimeshiftLabel = active.previewLabelProvider?.provide(progress) ?: active.statusLabel
                                                },
                                                onDragEnd = {
                                                    val active = latestTimeshift
                                                    active?.seekCommitHandler?.seekTo(displayedTimeshiftProgress)
                                                    active?.onSeekEnd?.run()
                                                    timeshiftDragging = false
                                                },
                                                onDragCancel = {
                                                    latestTimeshift?.onSeekEnd?.run()
                                                    timeshiftDragging = false
                                                }
                                            ) { change, _ ->
                                                val active = latestTimeshift ?: return@detectHorizontalDragGestures
                                                change.consume()
                                                val progress = ((change.position.x / size.width.coerceAtLeast(1)) * 1000f).roundToInt().coerceIn(0, 1000)
                                                displayedTimeshiftProgress = progress
                                                displayedTimeshiftLabel = active.previewLabelProvider?.provide(progress) ?: active.statusLabel
                                            }
                                        }
                                } else Modifier
                            ),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Box(Modifier.fillMaxWidth().height(5.dp).background(OfflineTvTheme.Colors.card.copy(alpha = 0.33f), RoundedCornerShape(99.dp)))
                        Box(Modifier.fillMaxWidth(integratedProgress).height(5.dp)
                            .background(Brush.horizontalGradient(listOf(OfflineTvTheme.Colors.accentCyan, OfflineTvTheme.Colors.accentSecondary)), RoundedCornerShape(99.dp)))
                    }
                    if (model.endTimeText.isNotBlank()) {
                        Spacer(Modifier.width(10.dp))
                        BasicText(model.endTimeText, style = TextStyle(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
        if (!compact && model.posterUrl.isNotBlank() && artworkBinder != null) {
            Spacer(Modifier.width(16.dp))
            TouchProgramPoster(model, artworkBinder)
        }
    }
}

@Composable
private fun ModernBadge(text: String) {
    BasicText(
        text = text.uppercase(), maxLines = 1,
        modifier = Modifier.background(OfflineTvTheme.Colors.card, RoundedCornerShape(7.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
        style = TextStyle(color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
    )
}

@Composable
private fun ModernContextHeader(model: TouchControlsBarUiModel) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(enabled = model.onContextClick != null) { model.onContextClick?.run() }
            .tvButtonSemantics(model.onContextClick != null).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            BasicText(model.contextTitle, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = TextStyle(color = OfflineTvTheme.Colors.accentGold, fontSize = 12.sp, fontWeight = FontWeight.Black))
            BasicText(model.contextSubtitle, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = TextStyle(color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold))
        }
        if (model.onContextClick != null) ModernBadge(stringResource(R.string.touch_context_change))
    }
}

@Composable
private fun ModernActionStrip(model: TouchControlsBarUiModel, artworkBinder: TouchControlsArtworkBinder?) {
    val compact = LocalConfiguration.current.screenWidthDp < 600
    val scrollState = rememberScrollState()
    val focusedIndex = model.focusedActionIndex.coerceIn(0, model.actions.lastIndex.coerceAtLeast(0))
    val density = LocalDensity.current
    val focusRequester = rememberTvInitialFocusRequester(model.actions.any { it.enabled }, model.actions.size, model.contextSubtitle, focusedIndex)
    Spacer(Modifier.height(if (compact) 8.dp else 12.dp))
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val chipWidth = if (compact) 82.dp else 100.dp
        val gap = 8.dp
        val contentWidth = (chipWidth * model.actions.size) + (gap * (model.actions.size - 1).coerceAtLeast(0))
        val shouldCenter = contentWidth <= maxWidth
        LaunchedEffect(focusedIndex, model.actions.size, shouldCenter) {
            if (shouldCenter) {
                scrollState.scrollTo(0)
            } else {
                val itemPx = with(density) { (chipWidth + gap).roundToPx() }
                scrollState.animateScrollTo((focusedIndex * itemPx - itemPx).coerceIn(0, scrollState.maxValue))
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState),
            horizontalArrangement = if (shouldCenter) {
                Arrangement.spacedBy(gap, Alignment.CenterHorizontally)
            } else {
                Arrangement.spacedBy(gap)
            }
        ) {
            model.actions.forEachIndexed { index, item ->
                ModernActionChip(item, index == focusedIndex, if (index == focusedIndex) focusRequester else null, compact, artworkBinder)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ModernActionChip(item: ZapActionItem, focused: Boolean, focusRequester: FocusRequester?, compact: Boolean, artworkBinder: TouchControlsArtworkBinder?) {
    val surface = when {
        focused -> OfflineTvTheme.Colors.focus
        item.selected -> OfflineTvTheme.Colors.card
        else -> OfflineTvTheme.Colors.chip.copy(alpha = 0.8f)
    }
    Column(
        modifier = Modifier
            .width(if (compact) 82.dp else 100.dp)
            .alpha(if (item.enabled) 1f else .42f)
            .background(surface, RoundedCornerShape(18.dp))
            .border(1.dp, if (focused) Color.White else OfflineTvTheme.Colors.chipSelected.copy(alpha = 0.33f), RoundedCornerShape(18.dp))
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .tvButtonSemantics(item.enabled)
            .combinedClickable(enabled = item.enabled, onClick = { item.onClick?.run() }, onLongClick = item.onLongClick?.let { { it.run() } })
            .padding(horizontal = 7.dp, vertical = if (compact) 7.dp else 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ModernActionIcon(item = item, focused = focused, artworkBinder = artworkBinder)
        Spacer(Modifier.height(2.dp))
        BasicText(item.label, maxLines = 1, overflow = TextOverflow.Ellipsis,
            style = TextStyle(color = if (focused) OfflineTvTheme.Colors.focusInk else Color.White, fontSize = if (compact) 9.sp else 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center))
    }
}

private enum class ModernActionIconKind { CHANNELS, PLATFORM, GUIDE, U7D, VOD, PREVIOUS, INFO, RECORD, REWIND, PLAY_PAUSE, FORWARD, OTHER }

private fun modernActionIconKind(item: ZapActionItem): ModernActionIconKind {
    if (item.iconHint.equals("platform", ignoreCase = true)) return ModernActionIconKind.PLATFORM
    val value = item.label.lowercase()
    return when {
        "canal" in value -> ModernActionIconKind.CHANNELS
        "plataforma" in value -> ModernActionIconKind.PLATFORM
        "guía" in value || "guia" in value -> ModernActionIconKind.GUIDE
        "u7d" in value -> ModernActionIconKind.U7D
        "vod" in value || "biblioteca" in value -> ModernActionIconKind.VOD
        "anterior" in value -> ModernActionIconKind.PREVIOUS
        "info" in value || "ficha" in value -> ModernActionIconKind.INFO
        "grab" in value -> ModernActionIconKind.RECORD
        "rebob" in value || "-30" in value || "−30" in value -> ModernActionIconKind.REWIND
        "pausa" in value || "reanudar" in value || "play" in value -> ModernActionIconKind.PLAY_PAUSE
        "avanz" in value || "+30" in value -> ModernActionIconKind.FORWARD
        else -> ModernActionIconKind.OTHER
    }
}

@Composable
private fun ModernActionIcon(item: ZapActionItem, focused: Boolean, artworkBinder: TouchControlsArtworkBinder?) {
    val kind = modernActionIconKind(item)
    val normalColor = if (focused) OfflineTvTheme.Colors.focusInk else OfflineTvTheme.Colors.accentCyan
    val recordingColor = Color(0xFFFF4D67)
    val iconColor = if (kind == ModernActionIconKind.RECORD) recordingColor else normalColor
    Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
        if (kind == ModernActionIconKind.PLATFORM && item.iconUrl.isNotBlank() && artworkBinder != null) {
            AndroidView(
                modifier = Modifier.size(30.dp),
                factory = { context ->
                    AppCompatImageView(context).apply {
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        adjustViewBounds = true
                        contentDescription = item.label
                    }
                },
                update = { imageView ->
                    imageView.contentDescription = item.label
                    artworkBinder.bindLogo(imageView, item.iconUrl, item.iconText, 30, 30)
                }
            )
        } else if (kind == ModernActionIconKind.PLATFORM && item.iconText.isNotBlank()) {
            BasicText(
                text = item.iconText,
                maxLines = 1,
                style = TextStyle(
                    color = normalColor,
                    fontSize = if (item.iconText.length > 3) 12.sp else 15.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
            )
        } else if (kind == ModernActionIconKind.OTHER) {
            BasicText(
                modernActionFallbackGlyph(item.label),
                style = TextStyle(color = normalColor, fontSize = 22.sp, fontWeight = FontWeight.Black)
            )
        } else {
            Canvas(Modifier.size(30.dp)) {
                val w = size.width
                val h = size.height
                val strokeWidth = 2.15.dp.toPx()
                val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                when (kind) {
                    ModernActionIconKind.CHANNELS -> {
                        drawRoundRect(iconColor, Offset(w * .09f, h * .18f), Size(w * .82f, h * .64f), CornerRadius(w * .09f), style = stroke)
                        val cell = w * .115f
                        listOf(.34f to .39f, .55f to .39f, .34f to .60f, .55f to .60f).forEach { (x, y) ->
                            drawRoundRect(iconColor, Offset(w * x, h * y), Size(cell, cell), CornerRadius(w * .025f))
                        }
                        drawLine(iconColor, Offset(w * .39f, h * .86f), Offset(w * .31f, h * .92f), strokeWidth, StrokeCap.Round)
                        drawLine(iconColor, Offset(w * .61f, h * .86f), Offset(w * .69f, h * .92f), strokeWidth, StrokeCap.Round)
                    }
                    ModernActionIconKind.GUIDE -> {
                        drawRoundRect(iconColor, Offset(w * .12f, h * .17f), Size(w * .76f, h * .70f), CornerRadius(w * .08f), style = stroke)
                        drawLine(iconColor, Offset(w * .12f, h * .36f), Offset(w * .88f, h * .36f), strokeWidth, StrokeCap.Round)
                        drawLine(iconColor, Offset(w * .31f, h * .10f), Offset(w * .31f, h * .25f), strokeWidth, StrokeCap.Round)
                        drawLine(iconColor, Offset(w * .69f, h * .10f), Offset(w * .69f, h * .25f), strokeWidth, StrokeCap.Round)
                        for (y in listOf(.50f, .64f, .78f)) drawLine(iconColor, Offset(w * .28f, h * y), Offset(w * .73f, h * y), strokeWidth, StrokeCap.Round)
                    }
                    ModernActionIconKind.U7D -> {
                        drawArc(iconColor, 35f, 285f, false, Offset(w * .12f, h * .12f), Size(w * .76f, h * .76f), style = stroke)
                        val arrow = Path().apply {
                            moveTo(w * .11f, h * .29f); lineTo(w * .13f, h * .50f); lineTo(w * .31f, h * .39f)
                        }
                        drawPath(arrow, iconColor, style = stroke)
                    }
                    ModernActionIconKind.VOD -> {
                        drawRoundRect(iconColor, Offset(w * .12f, h * .31f), Size(w * .76f, h * .57f), CornerRadius(w * .07f), style = stroke)
                        drawLine(iconColor, Offset(w * .13f, h * .31f), Offset(w * .84f, h * .13f), strokeWidth, StrokeCap.Round)
                        drawLine(iconColor, Offset(w * .19f, h * .17f), Offset(w * .29f, h * .28f), strokeWidth, StrokeCap.Round)
                        drawLine(iconColor, Offset(w * .42f, h * .11f), Offset(w * .52f, h * .22f), strokeWidth, StrokeCap.Round)
                        drawLine(iconColor, Offset(w * .65f, h * .08f), Offset(w * .75f, h * .17f), strokeWidth, StrokeCap.Round)
                        val play = Path().apply {
                            moveTo(w * .43f, h * .47f); lineTo(w * .43f, h * .75f); lineTo(w * .66f, h * .61f); close()
                        }
                        drawPath(play, iconColor)
                    }
                    ModernActionIconKind.PREVIOUS -> {
                        drawLine(iconColor, Offset(w * .19f, h * .35f), Offset(w * .82f, h * .35f), strokeWidth, StrokeCap.Round)
                        drawLine(iconColor, Offset(w * .82f, h * .35f), Offset(w * .68f, h * .22f), strokeWidth, StrokeCap.Round)
                        drawLine(iconColor, Offset(w * .82f, h * .35f), Offset(w * .68f, h * .48f), strokeWidth, StrokeCap.Round)
                        drawLine(iconColor, Offset(w * .81f, h * .67f), Offset(w * .18f, h * .67f), strokeWidth, StrokeCap.Round)
                        drawLine(iconColor, Offset(w * .18f, h * .67f), Offset(w * .32f, h * .54f), strokeWidth, StrokeCap.Round)
                        drawLine(iconColor, Offset(w * .18f, h * .67f), Offset(w * .32f, h * .80f), strokeWidth, StrokeCap.Round)
                    }
                    ModernActionIconKind.INFO -> drawCircle(iconColor, radius = w * .37f, center = center, style = stroke)
                    ModernActionIconKind.RECORD -> {
                        drawCircle(iconColor, radius = w * .37f, center = center, style = stroke)
                        drawCircle(iconColor, radius = w * .18f, center = center)
                    }
                    ModernActionIconKind.REWIND -> {
                        drawArc(iconColor, 35f, 285f, false, Offset(w * .12f, h * .12f), Size(w * .76f, h * .76f), style = stroke)
                        val arrow = Path().apply {
                            moveTo(w * .11f, h * .29f); lineTo(w * .13f, h * .50f); lineTo(w * .31f, h * .39f)
                        }
                        drawPath(arrow, iconColor, style = stroke)
                    }
                    ModernActionIconKind.PLAY_PAUSE -> {
                        val play = Path().apply {
                            moveTo(w * .14f, h * .25f); lineTo(w * .14f, h * .75f); lineTo(w * .48f, h * .50f); close()
                        }
                        drawPath(play, iconColor)
                        drawRoundRect(iconColor, Offset(w * .59f, h * .25f), Size(w * .10f, h * .50f), CornerRadius(w * .025f))
                        drawRoundRect(iconColor, Offset(w * .77f, h * .25f), Size(w * .10f, h * .50f), CornerRadius(w * .025f))
                    }
                    ModernActionIconKind.FORWARD -> {
                        drawArc(iconColor, 220f, 285f, false, Offset(w * .12f, h * .12f), Size(w * .76f, h * .76f), style = stroke)
                        val arrow = Path().apply {
                            moveTo(w * .89f, h * .29f); lineTo(w * .87f, h * .50f); lineTo(w * .69f, h * .39f)
                        }
                        drawPath(arrow, iconColor, style = stroke)
                    }
                    else -> Unit
                }
            }
            if (kind == ModernActionIconKind.U7D) {
                BasicText("7", style = TextStyle(color = iconColor, fontSize = 11.sp, fontWeight = FontWeight.Black))
            } else if (kind == ModernActionIconKind.INFO) {
                BasicText("i", style = TextStyle(color = iconColor, fontSize = 17.sp, fontWeight = FontWeight.Black))
            } else if (kind == ModernActionIconKind.REWIND) {
                BasicText("−30", style = TextStyle(color = iconColor, fontSize = 8.sp, fontWeight = FontWeight.Black))
            } else if (kind == ModernActionIconKind.FORWARD) {
                BasicText("+30", style = TextStyle(color = iconColor, fontSize = 8.sp, fontWeight = FontWeight.Black))
            }
        }
    }
}

private fun modernActionFallbackGlyph(label: String): String {
    val value = label.lowercase()
    return when {
        "ajuste" in value -> "⚙"
        "giro" in value || "rot" in value -> "↻"
        else -> "•"
    }
}

@Composable
private fun TouchNowPlayingHeader(model: TouchControlsNowPlayingUiModel, artworkBinder: TouchControlsArtworkBinder?) {
    val compact = LocalConfiguration.current.screenWidthDp < 600
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .background(OfflineTvTheme.Colors.panelSoft, RoundedCornerShape(18.dp))
            .border(1.dp, OfflineTvTheme.Colors.accentSecondary.copy(alpha = 0.22f), RoundedCornerShape(18.dp))
            .padding(horizontal = if (compact) 8.dp else 10.dp, vertical = if (compact) 7.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TouchChannelLogo(model = model, artworkBinder = artworkBinder, compact = compact)
        Spacer(modifier = Modifier.width(if (compact) 7.dp else 9.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (model.channelBadge.isNotBlank()) {
                    TouchMiniChip(text = model.channelBadge)
                    Spacer(modifier = Modifier.width(6.dp))
                }
                BasicText(
                    text = model.channelName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                    style = TextStyle(
                        color = Color.White,
                        fontSize = if (compact) 12.sp else 13.sp,
                        fontWeight = FontWeight.Black
                    )
                )
                if (model.remainingText.isNotBlank()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    BasicText(
                        text = model.remainingText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(
                            color = OfflineTvTheme.Colors.textSoft,
                            fontSize = if (compact) 9.sp else 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            BasicText(
                text = model.programTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    color = Color.White,
                    fontSize = if (compact) 14.sp else 15.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            if (model.programMeta.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                BasicText(
                    text = model.programMeta,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        color = OfflineTvTheme.Colors.textSoft,
                        fontSize = if (compact) 10.sp else 11.sp
                    )
                )
            }
            if (model.nextProgramVisible) {
                Spacer(modifier = Modifier.height(2.dp))
                BasicText(
                    text = model.nextProgram,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        color = OfflineTvTheme.Colors.focus,
                        fontSize = if (compact) 10.sp else 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            if (model.progressVisible) {
                Spacer(modifier = Modifier.height(5.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .background(OfflineTvTheme.Colors.card.copy(alpha = 0.4f), RoundedCornerShape(999.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(model.progress.coerceIn(0, 100) / 100f)
                                .height(4.dp)
                                .background(OfflineTvTheme.Colors.accentSecondary, RoundedCornerShape(999.dp))
                        )
                    }
                    if (model.endTimeText.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        BasicText(
                            text = model.endTimeText,
                            maxLines = 1,
                            style = TextStyle(
                                color = OfflineTvTheme.Colors.textPrimary,
                                fontSize = if (compact) 9.sp else 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
        if (!compact && model.posterUrl.isNotBlank() && artworkBinder != null) {
            Spacer(modifier = Modifier.width(10.dp))
            TouchProgramPoster(model = model, artworkBinder = artworkBinder)
        }
    }
}

@Composable
private fun TouchChannelLogo(
    model: TouchControlsNowPlayingUiModel,
    artworkBinder: TouchControlsArtworkBinder?,
    compact: Boolean
) {
    AndroidView(
        modifier = Modifier.size(if (compact) 44.dp else 50.dp),
        factory = { context ->
            FrameLayout(context).apply {
                background = ContextCompat.getDrawable(context, R.drawable.channel_logo_plate_bg)
                setPadding(1, 1, 1, 1)
                addView(
                    AppCompatImageView(context).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        contentDescription = context.getString(R.string.channel_logo_content_description)
                    }
                )
            }
        },
        update = { frame ->
            val imageView = frame.getChildAt(0) as ImageView
            val logoSize = if (compact) 42 else 48
            artworkBinder?.bindLogo(imageView, model.logoUrl, model.channelName, logoSize, logoSize)
        }
    )
}

@Composable
private fun TouchProgramPoster(model: TouchControlsNowPlayingUiModel, artworkBinder: TouchControlsArtworkBinder) {
    AndroidView(
        modifier = Modifier
            .width(60.dp)
            .height(54.dp)
            .background(OfflineTvTheme.Colors.card.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
        factory = { context ->
            AppCompatImageView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
        },
        update = { imageView ->
            artworkBinder.bindPoster(imageView, model.posterUrl)
        }
    )
}

@Composable
private fun TouchMiniChip(text: String) {
    BasicText(
        text = text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .background(OfflineTvTheme.Colors.accentSecondary.copy(alpha = 0.2f), RoundedCornerShape(999.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        style = TextStyle(
            color = OfflineTvTheme.Colors.textPrimary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black
        )
    )
}

@Composable
private fun TouchContextHeader(model: TouchControlsBarUiModel) {
    Row(
        modifier = Modifier
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .background(OfflineTvTheme.Colors.panelSoft, RoundedCornerShape(16.dp))
            .clickable(enabled = model.onContextClick != null) { model.onContextClick?.run() }
            .tvButtonSemantics(model.onContextClick != null)
            .padding(horizontal = 11.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                text = model.contextTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    color = OfflineTvTheme.Colors.accentCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
            )
            if (model.contextSubtitle.isNotBlank()) {
                BasicText(
                    text = model.contextSubtitle,
                    modifier = Modifier.padding(top = 2.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
        if (model.onContextClick != null) {
            Spacer(modifier = Modifier.width(10.dp))
            BasicText(
                text = stringResource(id = R.string.touch_context_change),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .background(OfflineTvTheme.Colors.chipSelected, RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                style = TextStyle(
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TouchControlChip(item: ZapActionItem, modifier: Modifier = Modifier, focused: Boolean = false, focusRequester: FocusRequester? = null) {
    val compact = LocalConfiguration.current.screenWidthDp < 600
    BasicText(
        text = item.label,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .alpha(if (item.enabled) 1f else 0.45f)
            .background(
                color = when {
                    focused -> OfflineTvTheme.Colors.focus
                    item.selected -> OfflineTvTheme.Colors.chipSelected
                    else -> OfflineTvTheme.Colors.chip
                },
                shape = RoundedCornerShape(18.dp)
            )
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .tvButtonSemantics(item.enabled)
            .combinedClickable(
                enabled = item.enabled,
                onClick = { item.onClick?.run() },
                onLongClick = item.onLongClick?.let { { it.run() } }
            )
            .padding(horizontal = if (compact) 10.dp else 12.dp, vertical = if (compact) 9.dp else 8.dp),
        style = TextStyle(
            color = if (focused) OfflineTvTheme.Colors.focusInk else Color.White,
            fontSize = if (compact) 12.sp else 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    )
}
