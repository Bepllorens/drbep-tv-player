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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF20B111A))
            .tvPanelBackHandler(model.onBack)
            .padding(
                horizontal = if (compact) 18.dp else 56.dp,
                vertical = if (compact) 18.dp else 42.dp
        )
    ) {
        PanelHeader(model.title, model.subtitle, compact)
        QuickChannelActions(model.actions, compact)
        LazyColumn(
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(model.items) { index, item ->
                QuickChannelRow(item, imageBinder, if (index == 0) firstRowRequester else null)
            }
        }
    }
}

@Composable
private fun QuickChannelActions(actions: List<ZapActionItem>, compact: Boolean) {
    if (actions.isEmpty()) return
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = if (compact) 10.dp else 14.dp),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp)
    ) {
        items(actions) { action ->
            QuickChannelActionChip(action, compact)
        }
    }
}

@Composable
private fun QuickChannelActionChip(action: ZapActionItem, compact: Boolean) {
    var focused by remember { mutableStateOf(false) }
    val enabled = action.enabled && action.onClick != null
    val background = when {
        focused -> Color(0xFFFFD47A)
        action.selected || action.highlighted -> Color(0xFF2B5D9E)
        enabled -> Color(0xFF1B3554)
        else -> Color(0xFF182230)
    }
    val textColor = when {
        focused -> Color(0xFF101722)
        enabled -> Color.White
        else -> Color(0xFF7F8EA1)
    }
    Box(
        modifier = Modifier
            .background(background, RoundedCornerShape(if (compact) 16.dp else 18.dp))
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
                style = TextStyle(color = Color.White, fontSize = if (compact) 20.sp else 28.sp, fontWeight = FontWeight.Bold)
            )
        }
        if (subtitle.isNotEmpty()) {
            BasicText(
                text = subtitle,
                modifier = Modifier.padding(top = if (compact) 5.dp else 8.dp),
                style = TextStyle(color = Color(0xFFB7C5D8), fontSize = if (compact) 12.sp else 14.sp)
            )
        }
    }
}

@Composable
private fun QuickChannelRow(item: QuickChannelRowUiModel, imageBinder: QuickChannelImageBinder, focusRequester: FocusRequester?) {
    val compact = LocalConfiguration.current.screenWidthDp < 600
    var focused by remember { mutableStateOf(false) }
    val rowBackground = if (focused) Color(0xFFFFD47A) else Color(0xFF1C2733)
    val titleColor = if (focused) Color(0xFF101722) else Color.White
    val metaColor = if (focused) Color(0xFF203044) else Color(0xFFC4D0DF)
    val badgeBackground = if (focused) Color(0xFFEAB85E) else Color(0xFF1E2D3E)
    val badgeTextColor = if (focused) Color(0xFF101722) else Color(0xFFDCE7F5)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = if (compact) 6.dp else 8.dp)
            .background(rowBackground, RoundedCornerShape(14.dp))
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .tvButtonSemantics(item.onClick != null)
            .clickable(enabled = item.onClick != null) { item.onClick?.run() }
            .padding(if (compact) 10.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
                    .background(badgeBackground, RoundedCornerShape(12.dp))
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
            val imageView = frame.getChildAt(0) as ImageView
            imageBinder.bind(imageView, item)
        }
    )
}
