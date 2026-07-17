package com.drbep.tvplayer

import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat

object TouchControlsComposeBinder {
    @JvmStatic
    fun bind(
        composeView: ComposeView?,
        model: TouchControlsBarUiModel,
        artworkBinder: TouchControlsArtworkBinder? = null
    ) {
        if (composeView == null) return
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        composeView.setContent {
            TouchControlsBar(model = model, artworkBinder = artworkBinder)
        }
    }
}

@Composable
private fun TouchControlsBar(model: TouchControlsBarUiModel, artworkBinder: TouchControlsArtworkBinder?) {
    val compact = LocalConfiguration.current.screenWidthDp < 600
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xD411161D), RoundedCornerShape(22.dp))
            .padding(horizontal = 8.dp, vertical = 7.dp)
    ) {
        if (model.nowPlaying.visible) {
            TouchNowPlayingHeader(model = model.nowPlaying, artworkBinder = artworkBinder)
        } else if (model.contextTitle.isNotBlank() || model.contextSubtitle.isNotBlank()) {
            TouchContextHeader(model)
        }
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
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
            val minimumContentWidth =
                (78.dp * actionCount) + (8.dp * (actionCount - 1).coerceAtLeast(0)) + 24.dp
            val shouldCenter = minimumContentWidth <= maxWidth
            val chipWidth = if (shouldCenter) {
                ((maxWidth - 24.dp - (8.dp * (actionCount - 1).coerceAtLeast(0))) / actionCount)
                    .coerceIn(78.dp, if (compact) 116.dp else 136.dp)
            } else {
                0.dp
            }
            LaunchedEffect(focusedIndex, actionCount, shouldCenter, compact) {
                if (!shouldCenter && actionCount > 1) {
                    val chipPx = with(density) { (if (compact) 86.dp else 92.dp).roundToPx() }
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
                        modifier = if (shouldCenter) Modifier.width(chipWidth) else Modifier.defaultMinSize(minWidth = if (compact) 86.dp else 92.dp),
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
private fun TouchNowPlayingHeader(model: TouchControlsNowPlayingUiModel, artworkBinder: TouchControlsArtworkBinder?) {
    val compact = LocalConfiguration.current.screenWidthDp < 600
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .background(Color(0xB51B2A38), RoundedCornerShape(18.dp))
            .border(1.dp, Color(0x247BAFD3), RoundedCornerShape(18.dp))
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
                            color = Color(0xFFA7C3DE),
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
                        color = Color(0xFFCFE0F4),
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
                        color = Color(0xFF94BCE2),
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
                            .background(Color(0x4058859F), RoundedCornerShape(999.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(model.progress.coerceIn(0, 100) / 100f)
                                .height(4.dp)
                                .background(Color(0xFF6AA8FF), RoundedCornerShape(999.dp))
                        )
                    }
                    if (model.endTimeText.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        BasicText(
                            text = model.endTimeText,
                            maxLines = 1,
                            style = TextStyle(
                                color = Color(0xFFDDEEFF),
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
            .background(Color(0x66233242), RoundedCornerShape(12.dp)),
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
            .background(Color(0x264F86A8), RoundedCornerShape(999.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        style = TextStyle(
            color = Color(0xFFDDEEFF),
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
            .background(Color(0xB51B2A38), RoundedCornerShape(16.dp))
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
                    color = Color(0xFF9BD0FF),
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
                    .background(Color(0xFF2D6EA3), RoundedCornerShape(999.dp))
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
        modifier = modifier
            .alpha(if (item.enabled) 1f else 0.45f)
            .background(
                color = when {
                    focused -> Color(0xFFFFD47A)
                    item.selected -> Color(0xFF2D6EA3)
                    else -> Color(0xFF203246)
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
            color = if (focused) Color(0xFF101722) else Color.White,
            fontSize = if (compact) 12.sp else 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    )
}
