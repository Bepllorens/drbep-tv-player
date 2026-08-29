package com.drbep.tvplayer

import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

object StartupHomeHubComposeBinder {
    @JvmStatic
    fun bind(composeView: ComposeView?, model: StartupHomeHubUiModel, artworkBinder: StartupHomeHubArtworkBinder?) {
        if (composeView == null) return
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        composeView.setContent { StartupHomeHub(model, artworkBinder) }
    }
}

@Composable
private fun StartupHomeHub(model: StartupHomeHubUiModel, artworkBinder: StartupHomeHubArtworkBinder?) {
    val compact = LocalConfiguration.current.screenWidthDp < 600
    val firstCardRequester = rememberTvInitialFocusRequester(model.primaryCards.isNotEmpty(), model)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .tvPanelBackHandler(model.onBack)
            .background(
                Brush.linearGradient(
                    listOf(OfflineTvTheme.Colors.backdrop, OfflineTvTheme.Colors.backdropAccent, OfflineTvTheme.Colors.surfaceDeep)
                )
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .fillMaxHeight(0.56f)
                .align(Alignment.TopEnd)
                .background(
                    Brush.radialGradient(
                        listOf(OfflineTvTheme.Colors.chipSelected.copy(alpha = 0.33f), Color.Transparent)
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (compact) 18.dp else 40.dp, vertical = if (compact) 18.dp else 28.dp)
        ) {
            StartupTopBar(model, compact)
            Spacer(Modifier.height(if (compact) 20.dp else 24.dp))
            BasicText(
                text = model.title,
                style = TextStyle(Color.White, if (compact) 29.sp else 36.sp, FontWeight.Black)
            )
            if (model.subtitle.isNotBlank()) {
                Spacer(Modifier.height(5.dp))
                BasicText(
                    text = model.subtitle,
                    style = TextStyle(OfflineTvTheme.Colors.textMuted, if (compact) 12.sp else 14.sp)
                )
            }
            Spacer(Modifier.height(if (compact) 16.dp else 20.dp))
            if (compact) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    model.primaryCards.forEachIndexed { index, item ->
                        StartupPrimaryCard(item, Modifier.fillMaxWidth(), if (index == 0) firstCardRequester else null, true)
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    model.primaryCards.forEachIndexed { index, item ->
                        StartupPrimaryCard(item, Modifier.weight(1f), if (index == 0) firstCardRequester else null, false)
                    }
                }
            }
            if (model.continueCards.isNotEmpty()) {
                Spacer(Modifier.height(if (compact) 20.dp else 24.dp))
                StartupSectionTitle("CONTINUAR VIENDO", "${model.continueCards.size} disponibles", compact)
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 7.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    model.continueCards.forEach { StartupContinueCard(it, artworkBinder, compact) }
                }
            }
            if (model.recommendationCards.isNotEmpty()) {
                Spacer(Modifier.height(if (compact) 20.dp else 24.dp))
                StartupSectionTitle("PARA TI", "", compact)
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 7.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    model.recommendationCards.forEach { StartupContinueCard(it, artworkBinder, compact) }
                }
            }
            Spacer(Modifier.height(if (compact) 20.dp else 24.dp))
            StartupSectionTitle("ACCESOS RÁPIDOS", "", compact)
            Spacer(Modifier.height(10.dp))
            StartupShortcuts(model.shortcuts, compact)
            if (!compact) {
                Spacer(Modifier.height(18.dp))
                BasicText(
                    text = "← → elegir  ·  OK abrir  ·  Atrás cerrar",
                    style = TextStyle(OfflineTvTheme.Colors.textMuted, 11.sp, FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
private fun StartupTopBar(model: StartupHomeHubUiModel, compact: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(R.drawable.drbep_launcher_icon),
            contentDescription = model.brand,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(if (compact) 38.dp else 44.dp).clip(RoundedCornerShape(13.dp))
        )
        Spacer(Modifier.width(11.dp))
        BasicText(model.brand, style = TextStyle(Color.White, if (compact) 13.sp else 15.sp, FontWeight.Black, letterSpacing = 1.5.sp))
        Spacer(Modifier.weight(1f))
        if (!compact) {
            BasicText(model.catalogSummary, style = TextStyle(OfflineTvTheme.Colors.textSoft, 12.sp, FontWeight.Bold))
            Spacer(Modifier.width(16.dp))
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(11.dp))
                .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(11.dp))
                .background(Color(0x16000000))
                .padding(horizontal = 12.dp, vertical = 9.dp)
        ) { BasicText(model.clock, style = TextStyle(Color.White, 14.sp, FontWeight.Black)) }
        Spacer(Modifier.width(9.dp))
        StartupIconButton("⌕", "Buscar", model.onSearch, compact)
        Spacer(Modifier.width(7.dp))
        StartupIconButton("⚙", "Ajustes", model.onSettings, compact)
    }
}

@Composable
private fun StartupIconButton(icon: String, label: String, action: Runnable?, compact: Boolean) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .size(if (compact) 38.dp else 42.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(if (focused) 2.dp else 1.dp, if (focused) OfflineTvTheme.Colors.focus else Color(0x22FFFFFF), RoundedCornerShape(12.dp))
            .background(if (focused) OfflineTvTheme.Colors.card else Color(0x22FFFFFF))
            .onFocusChanged { focused = it.isFocused }
            .tvButtonSemantics(action != null, label)
            .clickable(enabled = action != null) { action?.run() },
        contentAlignment = Alignment.Center
    ) { BasicText(icon, style = TextStyle(Color.White, if (compact) 18.sp else 20.sp, FontWeight.Bold)) }
}

@Composable
private fun StartupPrimaryCard(item: StartupHomeHubUiModel.PrimaryCard, modifier: Modifier, requester: FocusRequester?, compact: Boolean) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(if (compact) 18.dp else 22.dp)
    val colors = if (item.vod) {
        listOf(OfflineTvTheme.Colors.card, OfflineTvTheme.Colors.chip)
    } else {
        listOf(OfflineTvTheme.Colors.backdropAccent, OfflineTvTheme.Colors.surfaceDeep)
    }
    Column(
        modifier = modifier
            .height(if (compact) 138.dp else 170.dp)
            .scale(if (focused) 1.018f else 1f)
            .clip(shape)
            .background(Brush.linearGradient(colors))
            .border(if (focused) 3.dp else 1.dp, if (focused) OfflineTvTheme.Colors.focus else OfflineTvTheme.Colors.accentSecondary.copy(alpha = 0.47f), shape)
            .then(if (requester != null) Modifier.focusRequester(requester) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .tvButtonSemantics(
                item.onClick != null,
                listOf(item.title, item.subtitle, item.metric).filter { it.isNotBlank() }.joinToString(". ")
            )
            .clickable(enabled = item.onClick != null) { item.onClick?.run() }
            .padding(horizontal = if (compact) 18.dp else 24.dp, vertical = if (compact) 15.dp else 20.dp)
    ) {
        BasicText(item.eyebrow, style = TextStyle(OfflineTvTheme.Colors.accentGold, 10.sp, FontWeight.Black, letterSpacing = 1.3.sp))
        Spacer(Modifier.height(if (compact) 9.dp else 14.dp))
        BasicText(item.title, style = TextStyle(Color.White, if (compact) 29.sp else 36.sp, FontWeight.Black))
        BasicText(item.subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis, style = TextStyle(OfflineTvTheme.Colors.textSoft, if (compact) 11.sp else 13.sp))
        Spacer(Modifier.weight(1f))
        BasicText(item.metric, style = TextStyle(OfflineTvTheme.Colors.textPrimary, if (compact) 11.sp else 13.sp, FontWeight.Bold))
    }
}

@Composable
private fun StartupSectionTitle(title: String, meta: String, compact: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        BasicText(title, style = TextStyle(Color.White, if (compact) 11.sp else 12.sp, FontWeight.Black, letterSpacing = 1.5.sp))
        if (meta.isNotBlank()) {
            Spacer(Modifier.width(10.dp))
            BasicText(meta, style = TextStyle(OfflineTvTheme.Colors.textMuted, if (compact) 10.sp else 11.sp, FontWeight.Bold))
        }
    }
}

@Composable
private fun StartupContinueCard(item: StartupHomeHubUiModel.ContinueCard, artworkBinder: StartupHomeHubArtworkBinder?, compact: Boolean) {
    var focused by remember { mutableStateOf(false) }
    val width = if (compact) 228.dp else 260.dp
    Column(
        modifier = Modifier
            .width(width)
            .scale(if (focused) 1.025f else 1f)
            .clip(RoundedCornerShape(16.dp))
            .border(if (focused) 3.dp else 1.dp, if (focused) OfflineTvTheme.Colors.focus else OfflineTvTheme.Colors.accentSecondary.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .background(OfflineTvTheme.Colors.surfaceDeep.copy(alpha = 0.85f))
            .onFocusChanged { focused = it.isFocused }
            .tvButtonSemantics(
                item.onClick != null,
                listOf(if (item.livePreview) "En directo" else "", item.title, item.subtitle).filter { it.isNotBlank() }.joinToString(". ")
            )
            .clickable(enabled = item.onClick != null) { item.onClick?.run() }
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(if (compact) 100.dp else 112.dp).background(OfflineTvTheme.Colors.surfaceDeep)) {
            if (item.livePreview || item.imageUrl.isNotBlank()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        FrameLayout(context).apply {
                            addView(AppCompatImageView(context).apply {
                                layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                                scaleType = if (item.poster || item.livePreview) ImageView.ScaleType.CENTER_CROP else ImageView.ScaleType.CENTER_INSIDE
                                contentDescription = item.imageLabel
                            })
                        }
                    },
                    update = { frame ->
                        val image = frame.getChildAt(0) as ImageView
                        if (item.livePreview) artworkBinder?.bindLivePreview(image, item.imageUrl, item.imageLabel, width.value.toInt(), if (compact) 100 else 112)
                        else if (item.poster) artworkBinder?.bindPoster(image, item.imageUrl)
                        else artworkBinder?.bindLogo(image, item.imageUrl, item.imageLabel, width.value.toInt(), if (compact) 100 else 112)
                    }
                )
            } else {
                BasicText(item.imageLabel.take(2).uppercase(), modifier = Modifier.align(Alignment.Center), style = TextStyle(OfflineTvTheme.Colors.textMuted, 22.sp, FontWeight.Black))
            }
            if (item.livePreview) {
                BasicText(
                    text = "●  EN DIRECTO",
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .background(OfflineTvTheme.Colors.panelGlass, RoundedCornerShape(999.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    style = TextStyle(Color(0xFFFFD6D6), 9.sp, FontWeight.Black)
                )
            }
            if (item.progress > 0f) {
                Box(Modifier.fillMaxWidth().height(4.dp).align(Alignment.BottomStart).background(Color(0x55FFFFFF))) {
                    Box(Modifier.fillMaxWidth(item.progress).fillMaxHeight().background(Brush.horizontalGradient(listOf(OfflineTvTheme.Colors.accentCyan, OfflineTvTheme.Colors.chipSelected))))
                }
            }
        }
        Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
            BasicText(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = TextStyle(Color.White, if (compact) 13.sp else 14.sp, FontWeight.Bold))
            BasicText(item.subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis, style = TextStyle(OfflineTvTheme.Colors.textSoft, if (compact) 10.sp else 11.sp))
        }
    }
}

@Composable
private fun StartupShortcuts(items: List<StartupHomeHubUiModel.Shortcut>, compact: Boolean) {
    val columns = if (compact) 2 else 4
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        items.chunked(columns).forEach { rowItems ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                rowItems.forEach { StartupShortcut(it, Modifier.weight(1f), compact) }
                repeat(columns - rowItems.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun StartupShortcut(item: StartupHomeHubUiModel.Shortcut, modifier: Modifier, compact: Boolean) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .height(if (compact) 62.dp else 68.dp)
            .clip(RoundedCornerShape(15.dp))
            .border(if (focused) 2.dp else 1.dp, if (focused) OfflineTvTheme.Colors.focus else OfflineTvTheme.Colors.accentSecondary.copy(alpha = 0.2f), RoundedCornerShape(15.dp))
            .background(if (focused) OfflineTvTheme.Colors.card else OfflineTvTheme.Colors.panelGlass.copy(alpha = 0.6f))
            .onFocusChanged { focused = it.isFocused }
            .tvButtonSemantics(
                item.onClick != null,
                listOf(item.title, item.subtitle).filter { it.isNotBlank() }.joinToString(". ")
            )
            .clickable(enabled = item.onClick != null) { item.onClick?.run() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicText(item.icon, style = TextStyle(OfflineTvTheme.Colors.accentCyan, if (compact) 19.sp else 21.sp, FontWeight.Black))
        Spacer(Modifier.width(10.dp))
        Column {
            BasicText(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = TextStyle(Color.White, if (compact) 12.sp else 13.sp, FontWeight.Bold))
            BasicText(item.subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis, style = TextStyle(OfflineTvTheme.Colors.textMuted, if (compact) 9.sp else 10.sp))
        }
    }
}
