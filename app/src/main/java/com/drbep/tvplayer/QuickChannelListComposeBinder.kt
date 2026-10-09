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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat

object QuickChannelListComposeBinder {
    @JvmStatic
    fun bind(composeView: ComposeView?, model: QuickChannelListUiModel, imageBinder: QuickChannelImageBinder) {
        if (composeView == null) return
        if (model.items.isNotEmpty() && model.items.all { it.vod }) {
            val originals = linkedMapOf<VodVisualItemUiModel, QuickChannelRowUiModel>()
            model.items.forEach { row ->
                originals[VodVisualItemUiModel(row.title, row.meta, "", row.logoUrl, row.onClick, null)] = row
            }
            val actions = model.actions.map { VodVisualActionUiModel(it.label, it.selected,
                if (it.enabled) it.onClick else null) }
            val carousel = VodVisualPanelUiModel(model.title, model.subtitle, "", "Sin títulos",
                actions, listOf(VodVisualSectionUiModel("Títulos", "", originals.keys.toList())))
            carousel.onBack = model.onBack
            VodVisualPanelComposeBinder.bind(composeView, carousel) { image, item ->
                originals[item]?.let { imageBinder.bind(image, it) }
            }
            return
        }
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        composeView.setContent {
            QuickChannelPanel(model, imageBinder)
        }
    }
}

@Composable
private fun QuickChannelPanel(model: QuickChannelListUiModel, imageBinder: QuickChannelImageBinder) {
    val compact = LocalConfiguration.current.screenWidthDp < 600
    val firstRowRequester = rememberTvInitialFocusRequester(model.items.isNotEmpty(), model)
    val actionRequesters = remember(model) { List(model.actions.size) { FocusRequester() } }
    val topActionIndex = QuickChannelFocusPolicy.topActionIndex(model.actions)
    val bottomActionIndex = QuickChannelFocusPolicy.bottomActionIndex(model.actions)
    val topActionRequester = actionRequesters.getOrNull(topActionIndex)
    val bottomActionRequester = actionRequesters.getOrNull(bottomActionIndex)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OfflineTvTheme.Menu.background)
            .tvPanelBackHandler(model.onBack)
            .padding(
                horizontal = if (compact) 18.dp else 56.dp,
                vertical = if (compact) 18.dp else 42.dp
        )
    ) {
        PanelHeader(model.title, model.subtitle, compact)
        QuickChannelActions(model.actions, actionRequesters, compact)
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            itemsIndexed(
                items = model.items,
                key = { index, item -> item.channelId.ifEmpty { "${item.channelName}:$index" } },
                contentType = { _, item -> if (item.vod) "vod" else "live" }
            ) { index, item ->
                QuickChannelRow(
                    item,
                    imageBinder,
                    compact,
                    if (index == 0) firstRowRequester else null,
                    if (index == 0) topActionRequester else null,
                    if (index == model.items.lastIndex) bottomActionRequester else null
                )
            }
        }
    }
}

@Composable
private fun QuickChannelActions(actions: List<ZapActionItem>, focusRequesters: List<FocusRequester>, compact: Boolean) {
    if (actions.isEmpty()) return
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = if (compact) 10.dp else 14.dp),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp)
    ) {
        itemsIndexed(actions) { index, action ->
            QuickChannelActionChip(action, focusRequesters.getOrNull(index), compact)
        }
    }
}

@Composable
private fun QuickChannelActionChip(action: ZapActionItem, focusRequester: FocusRequester?, compact: Boolean) {
    var focused by remember { mutableStateOf(false) }
    val enabled = action.enabled && action.onClick != null
    val background = when {
        focused -> OfflineTvTheme.Colors.focus
        action.selected || action.highlighted -> OfflineTvTheme.Menu.selected
        enabled -> OfflineTvTheme.Menu.row
        else -> OfflineTvTheme.Menu.row
    }
    val textColor = when {
        focused -> OfflineTvTheme.Colors.focusInk
        enabled -> Color.White
        else -> OfflineTvTheme.Colors.textMuted
    }
    Box(
        modifier = Modifier
            .background(background, OfflineTvTheme.Menu.shape)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .tvButtonSemantics(enabled)
            .clickable(enabled = enabled) { action.onClick?.run() }
            .padding(
                horizontal = if (compact) 14.dp else 18.dp,
                vertical = if (compact) 8.dp else 10.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = action.label,
            style = TextStyle(
                color = textColor,
                fontSize = if (compact) 12.sp else 14.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
private fun PanelHeader(title: String, subtitle: String, compact: Boolean) {
    if (title.isEmpty() && subtitle.isEmpty()) return
    Column(modifier = Modifier.padding(bottom = if (compact) 12.dp else 18.dp)) {
        if (title.isNotEmpty()) {
            BasicText(
                text = title,
                style = TextStyle(color = OfflineTvTheme.Colors.accentGold, fontSize = if (compact) 20.sp else 28.sp, fontWeight = FontWeight.Bold)
            )
        }
        if (subtitle.isNotEmpty()) {
            BasicText(
                text = subtitle,
                modifier = Modifier.padding(top = if (compact) 5.dp else 8.dp),
                style = TextStyle(color = OfflineTvTheme.Colors.textSoft, fontSize = if (compact) 12.sp else 14.sp)
            )
        }
    }
}

@Composable
private fun QuickChannelRow(
    item: QuickChannelRowUiModel,
    imageBinder: QuickChannelImageBinder,
    compact: Boolean,
    focusRequester: FocusRequester?,
    upEdgeRequester: FocusRequester?,
    downEdgeRequester: FocusRequester?
) {
    var focused by remember(item.channelId) { mutableStateOf(false) }
    val shape = OfflineTvTheme.Menu.shape
    val rowBackground = if (focused) OfflineTvTheme.Colors.focus else OfflineTvTheme.Menu.row
    val titleColor = if (focused) OfflineTvTheme.Colors.focusInk else Color.White
    val metaColor = if (focused) OfflineTvTheme.Colors.focusInk else OfflineTvTheme.Colors.textSoft
    val badgeBackground = if (focused) Color(0xCCFFFFFF) else OfflineTvTheme.Menu.row
    val badgeTextColor = if (focused) OfflineTvTheme.Colors.focusInk else OfflineTvTheme.Colors.accentCyan
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = if (compact) 6.dp else 8.dp)
            .then(
                if (focused) {
                    Modifier.border(3.dp, OfflineTvTheme.Colors.accentGold, shape)
                } else {
                    Modifier
                }
            )
            .background(rowBackground, shape)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) {
                    return@onPreviewKeyEvent false
                }
                val target = when (event.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_DPAD_UP -> upEdgeRequester
                    KeyEvent.KEYCODE_DPAD_DOWN -> downEdgeRequester
                    else -> null
                }
                if (target == null) {
                    false
                } else {
                    runCatching { target.requestFocus() }.isSuccess
                }
            }
            .tvButtonSemantics(item.onClick != null)
            .clickable(enabled = item.onClick != null) { item.onClick?.run() }
            .padding(if (compact) 10.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (item.playing) {
            Box(
                modifier = Modifier
                    .width(if (compact) 4.dp else 5.dp)
                    .height(if (compact) 34.dp else 38.dp)
                    .background(
                        if (focused) OfflineTvTheme.Colors.focusInk else OfflineTvTheme.Colors.accentCyan,
                        OfflineTvTheme.Menu.shape
                    )
            )
            Spacer(modifier = Modifier.width(if (compact) 8.dp else 10.dp))
        }
        QuickChannelLogo(item, imageBinder, compact)
        Spacer(modifier = Modifier.width(if (compact) 10.dp else 12.dp))
        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                text = item.title,
                style = TextStyle(color = titleColor, fontSize = if (compact) 15.sp else 17.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(if (compact) 4.dp else 6.dp))
            BasicText(
                text = item.meta,
                style = TextStyle(color = metaColor, fontSize = if (compact) 11.sp else 13.sp)
            )
        }
        if (item.typeLabel.isNotEmpty()) {
            Spacer(modifier = Modifier.width(if (compact) 8.dp else 10.dp))
            Box(
                modifier = Modifier
                    .background(badgeBackground, OfflineTvTheme.Menu.shape)
                    .padding(horizontal = if (compact) 8.dp else 10.dp, vertical = if (compact) 5.dp else 6.dp),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    text = item.typeLabel,
                    style = TextStyle(color = badgeTextColor, fontSize = if (compact) 10.sp else 11.sp, fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
private fun QuickChannelLogo(item: QuickChannelRowUiModel, imageBinder: QuickChannelImageBinder, compact: Boolean) {
    val size = if (compact) 38.dp else 42.dp
    val bindingKey = remember(item.logoUrl, item.channelName, item.vod) {
        QuickChannelLogoBindingKey(item.logoUrl, item.channelName, item.vod)
    }
    AndroidView(
        modifier = Modifier.size(size),
        factory = { context ->
            FrameLayout(context).apply {
                background = ContextCompat.getDrawable(context, R.drawable.channel_logo_plate_bg)
                setPadding(3, 3, 3, 3)
                addView(
                    AppCompatImageView(context).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        contentDescription = null
                    }
                )
            }
        },
        update = { frame ->
            if (frame.tag != bindingKey) {
                frame.tag = bindingKey
                val imageView = frame.getChildAt(0) as ImageView
                imageBinder.bind(imageView, item)
            }
        }
    )
}

private data class QuickChannelLogoBindingKey(
    val logoUrl: String,
    val channelName: String,
    val vod: Boolean
)
