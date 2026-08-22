package com.drbep.tvplayer

import android.view.KeyEvent
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.util.WeakHashMap

object OverlayChannelListComposeBinder {
    private val models = WeakHashMap<ComposeView, MutableState<OverlayChannelListUiModel>>()

    @JvmStatic
    fun bind(composeView: ComposeView?, model: OverlayChannelListUiModel, imageBinder: OverlayChannelImageBinder) {
        if (composeView == null) return
        val existing = models[composeView]
        if (existing != null) {
            existing.value = model
            return
        }
        val modelState = mutableStateOf(model)
        models[composeView] = modelState
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        composeView.setContent {
            OverlayChannelList(modelState.value, imageBinder)
        }
    }
}

@Composable
private fun OverlayChannelList(model: OverlayChannelListUiModel, imageBinder: OverlayChannelImageBinder) {
    val compact = LocalConfiguration.current.screenWidthDp < 600
    val listState = rememberLazyListState()
    LaunchedEffect(model.scrollRequestToken, model.items.size) {
        if (model.scrollToIndex >= 0 && model.items.isNotEmpty()) {
            val headerOffset = if (model.filterLabel.isNotBlank()) 1 else 0
            val anchorPadding = if (compact) 1 else 2
            val targetIndex = (model.scrollToIndex - anchorPadding + headerOffset).coerceIn(0, model.items.lastIndex + headerOffset)
            withFrameNanos { }
            listState.scrollToItem(targetIndex)
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(OfflineTvTheme.Colors.panelSoft, RoundedCornerShape(18.dp))
            .border(1.dp, OfflineTvTheme.Colors.chipSelected.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
            .padding(if (compact) 8.dp else 10.dp)
    ) {
        if (model.listTitle.isNotBlank()) {
            BasicText(
                text = model.listTitle.uppercase(),
                style = TextStyle(
                    color = OfflineTvTheme.Colors.accentGold,
                    fontSize = if (compact) 9.sp else 10.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .onPreviewKeyEvent {
                    if (it.nativeKeyEvent.action != KeyEvent.ACTION_DOWN) {
                        return@onPreviewKeyEvent false
                    }
                    when (it.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            model.onMoveSelectionUp?.run()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            model.onMoveSelectionDown?.run()
                            true
                        }
                        else -> false
                    }
                }
                .padding(top = 2.dp, bottom = 4.dp)
            ,
            contentPadding = PaddingValues(bottom = if (compact) 28.dp else 36.dp)
        ) {
            if (model.filterLabel.isNotBlank()) {
                item {
                    OverlayFilterSwitcher(model, compact)
                }
            }
            if (model.items.isEmpty()) {
                item {
                    OverlayEmptyMessage(model.emptyMessage, compact)
                }
            }
            itemsIndexed(model.items) { _, item ->
                OverlayChannelRow(item, imageBinder, compact)
            }
        }
    }
}

@Composable
private fun OverlayEmptyMessage(message: String, compact: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (compact) 14.dp else 20.dp, bottom = 14.dp)
            .background(OfflineTvTheme.Colors.surfaceDeep, RoundedCornerShape(16.dp))
            .padding(horizontal = if (compact) 12.dp else 16.dp, vertical = if (compact) 18.dp else 22.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = message,
            style = TextStyle(
                color = OfflineTvTheme.Colors.textSoft,
                fontSize = if (compact) 13.sp else 14.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
private fun OverlayFilterSwitcher(model: OverlayChannelListUiModel, compact: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .background(OfflineTvTheme.Colors.surfaceDeep, RoundedCornerShape(16.dp))
            .padding(horizontal = if (compact) 8.dp else 10.dp, vertical = if (compact) 8.dp else 10.dp)
    ) {
        BasicText(
            text = model.filterTitle,
            style = TextStyle(
                color = OfflineTvTheme.Colors.accentCyan,
                fontSize = if (compact) 10.sp else 11.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterNavButton("◀", model.onPreviousFilterClick, compact)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(if (compact) 42.dp else 46.dp)
                    .background(OfflineTvTheme.Colors.card, RoundedCornerShape(14.dp))
                    .tvButtonSemantics(model.onNextFilterClick != null)
                    .clickable(enabled = model.onNextFilterClick != null) { model.onNextFilterClick?.run() }
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    text = model.filterLabel,
                    style = TextStyle(
                        color = Color.White,
                        fontSize = if (compact) 13.sp else 14.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
            }
            FilterNavButton("▶", model.onNextFilterClick, compact)
        }
    }
}

@Composable
private fun FilterNavButton(label: String, action: Runnable?, compact: Boolean) {
    Box(
        modifier = Modifier
            .width(if (compact) 48.dp else 54.dp)
            .height(if (compact) 42.dp else 46.dp)
            .background(OfflineTvTheme.Colors.chipSelected, RoundedCornerShape(14.dp))
            .tvButtonSemantics(action != null)
            .clickable(enabled = action != null) { action?.run() },
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = label,
            style = TextStyle(
                color = Color.White,
                fontSize = if (compact) 18.sp else 20.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
private fun OverlayChannelRow(item: OverlayChannelRowUiModel, imageBinder: OverlayChannelImageBinder, compact: Boolean) {
    val bg = when {
        item.selected -> OfflineTvTheme.Colors.focusSurface
        item.tuned -> OfflineTvTheme.Colors.card.copy(alpha = 0.8f)
        else -> OfflineTvTheme.Colors.surfaceDeep
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .background(Color.Transparent)
            .tvButtonSemantics(item.onClick != null)
            .clickable(enabled = item.onClick != null) { item.onClick?.run() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(bg, RoundedCornerShape(14.dp))
                .padding(horizontal = if (compact) 8.dp else 10.dp, vertical = if (compact) 7.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OverlayChannelLogo(item, imageBinder, compact)
            Spacer(modifier = Modifier.width(if (compact) 6.dp else 8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BasicText(
                        text = highlightQuery(item.name, item.query),
                        style = TextStyle(color = Color.White, fontSize = if (compact) 14.sp else 15.sp, fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(1f)
                    )
                    if (item.badgeVisible) {
                        Chip(item.badge, Color(item.badgeTextColor), compact)
                    }
                    if (item.favoriteVisible) {
                        Box(
                            modifier = Modifier
                                .size(if (compact) 30.dp else 34.dp)
                                .background(OfflineTvTheme.Colors.card.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                .tvButtonSemantics(item.onFavoriteClick != null)
                                .clickable(enabled = item.onFavoriteClick != null) { item.onFavoriteClick?.run() },
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText(
                                text = item.favoriteText,
                                style = TextStyle(color = Color(item.favoriteTextColor), fontSize = if (compact) 14.sp else 16.sp, fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                BasicText(
                    text = highlightQuery(item.meta, item.query),
                    style = TextStyle(color = OfflineTvTheme.Colors.textSoft, fontSize = if (compact) 10.sp else 11.sp)
                )
            }
        }
    }
}

@Composable
private fun OverlayChannelLogo(item: OverlayChannelRowUiModel, imageBinder: OverlayChannelImageBinder, compact: Boolean) {
    val width = if (item.vod) (if (compact) 48.dp else 54.dp) else (if (compact) 40.dp else 44.dp)
    val height = if (item.vod) (if (compact) 64.dp else 72.dp) else (if (compact) 40.dp else 44.dp)
    val padding = if (item.vod) 0 else if (compact) 2 else 3
    AndroidView(
        modifier = Modifier
            .width(width)
            .height(height),
        factory = { context ->
            FrameLayout(context).apply {
                background = ContextCompat.getDrawable(context, R.drawable.channel_logo_plate_bg)
                setPadding(padding, padding, padding, padding)
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
            frame.setPadding(padding, padding, padding, padding)
            val image = frame.getChildAt(0) as ImageView
            imageBinder.bind(image, item)
        }
    )
}

@Composable
private fun Chip(text: String, textColor: Color, compact: Boolean) {
    Box(
        modifier = Modifier
            .background(OfflineTvTheme.Colors.card.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(horizontal = if (compact) 7.dp else 8.dp, vertical = if (compact) 4.dp else 5.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = text,
            style = TextStyle(color = textColor, fontSize = if (compact) 10.sp else 11.sp, fontWeight = FontWeight.Bold)
        )
    }
}

private fun highlightQuery(text: String, query: String) = buildAnnotatedString {
    append(text)
    val normalizedQuery = query.trim()
    if (normalizedQuery.isEmpty()) {
        return@buildAnnotatedString
    }
    val lowerText = text.lowercase()
    val lowerQuery = normalizedQuery.lowercase()
    var start = 0
    while (start < lowerText.length) {
        val index = lowerText.indexOf(lowerQuery, start)
        if (index < 0) break
        addStyle(
            SpanStyle(color = OfflineTvTheme.Colors.accentCyan, fontWeight = FontWeight.Bold),
            index,
            index + lowerQuery.length
        )
        start = index + lowerQuery.length
    }
}
