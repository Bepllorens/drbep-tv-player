package com.drbep.tvplayer

import android.view.KeyEvent
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.*
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
import com.bumptech.glide.Glide

object VodVisualPanelComposeBinder {
    // Indices and public identities only: no callbacks, Views or Activity references.
    private val positions = object : LinkedHashMap<String, Pair<Int, String>>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Pair<Int, String>>?) = size > 16
    }
    @JvmStatic fun bind(view: ComposeView?, model: VodVisualPanelUiModel, binder: VodVisualPosterImageBinder) {
        view ?: return
        val key = model.title + "|" + model.subtitle + "|" + model.actions.orEmpty().joinToString { it.label }
        val saved = positions[key]
        view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        view.setContent { PlaylistImportIndicator.Surface { VodCarouselPanel(model, binder, saved) { row, item -> positions[key] = row to item } } }
    }
}

private fun artworkIdentity(item: VodVisualItemUiModel) = item.title + "|" + item.posterUrl

@Composable
private fun VodCarouselPanel(model: VodVisualPanelUiModel, binder: VodVisualPosterImageBinder,
    saved: Pair<Int, String>?, rememberSelection: (Int, String) -> Unit) {
    var restoredOnce by remember { mutableStateOf(false) }
    val compact = LocalConfiguration.current.screenWidthDp < 720
    val sections = model.sections.orEmpty()
    val restoreRow = saved?.first?.coerceIn(0, (sections.size - 1).coerceAtLeast(0)) ?: 0
    val restoreIndex = sections.getOrNull(restoreRow)?.items.orEmpty().indexOfFirst { artworkIdentity(it) == saved?.second }
    val restore = saved != null && restoreIndex >= 0
    var selected by remember(model) { mutableStateOf(sections.getOrNull(restoreRow)?.items?.getOrNull(restoreIndex.coerceAtLeast(0))) }
    val firstEnabledAction = model.actions.orEmpty().indexOfFirst { it.onClick != null }
    val firstAction = rememberTvInitialFocusRequester(!restore && firstEnabledAction >= 0, model)
    val columnState = rememberLazyListState(if (restore) restoreRow else 0)
    Column(Modifier.fillMaxSize().tvPanelBackHandler(model.onBack).background(OfflineTvTheme.Menu.scrim)
        .background(OfflineTvTheme.Menu.background).padding(horizontal = 24.dp, vertical = 16.dp)) {
        BasicText(model.title, maxLines = 1, style = TextStyle(Color.White, 24.sp, FontWeight.Bold))
        BasicText(model.subtitle, maxLines = 1, style = TextStyle(OfflineTvTheme.Colors.textSoft, 12.sp))
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(model.actions.orEmpty().size) { index ->
                val action = model.actions[index]
                var focused by remember { mutableStateOf(false) }
                BasicText(action.label, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = TextStyle(if (focused) OfflineTvTheme.Colors.focusInk else Color.White, 12.sp, FontWeight.Bold),
                    modifier = Modifier.widthIn(max = 185.dp)
                        .then(if (index == firstEnabledAction) Modifier.focusRequester(firstAction) else Modifier)
                        .onFocusChanged { focused = it.isFocused }
                        .background(if (focused) Color.White else Color.Transparent, RoundedCornerShape(3.dp))
                        .tvButtonSemantics(action.onClick != null, action.label)
                        .clickable(enabled = action.onClick != null) { action.onClick?.run() }
                        .padding(horizontal = 12.dp, vertical = 10.dp))
            }
        }
        Column(Modifier.fillMaxWidth().height(if (compact) 94.dp else 104.dp).padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            BasicText(selected?.title.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = TextStyle(Color.White, 20.sp, FontWeight.Bold))
            BasicText(listOf(selected?.meta.orEmpty(), selected?.progressLabel.orEmpty()).filter { it.isNotBlank() }.joinToString(" · "),
                maxLines = 1, overflow = TextOverflow.Ellipsis, style = TextStyle(OfflineTvTheme.Colors.accentCyan, 12.sp))
            BasicText(selected?.description.orEmpty(), maxLines = 2, overflow = TextOverflow.Ellipsis,
                style = TextStyle(OfflineTvTheme.Colors.textSoft, 12.sp))
        }
        if (sections.isEmpty()) BasicText(model.emptyLabel, style = TextStyle(Color.White, 16.sp))
        LazyColumn(Modifier.weight(1f), state = columnState, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(sections.size) { row ->
                val section = sections[row]
                Column {
                    BasicText(section.title, maxLines = 1, style = TextStyle(Color.White, 17.sp, FontWeight.Bold))
                    if (section.subtitle.isNotBlank()) BasicText(section.subtitle, maxLines = 1,
                        style = TextStyle(OfflineTvTheme.Colors.textMuted, 10.sp))
                    val rowState = rememberLazyListState(if (restore && row == restoreRow) restoreIndex else 0)
                    LazyRow(Modifier.fillMaxWidth().focusGroup(), state = rowState,
                        contentPadding = PaddingValues(horizontal = 5.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(section.items.orEmpty().size) { index ->
                            val item = section.items[index]
                            val requester = remember { FocusRequester() }
                            if (!restoredOnce && ((restore && row == restoreRow && index == restoreIndex) || (!restore && firstEnabledAction < 0 && row == 0 && index == 0))) LaunchedEffect(Unit) {
                                withFrameNanos { }; if (runCatching { requester.requestFocus() }.isSuccess) restoredOnce = true
                            }
                            VodCover(item, binder, compact, requester) {
                                selected = item; rememberSelection(row, artworkIdentity(item))
                            }
                        }
                    }
                }
            }
        }
        BasicText("← → Títulos   ·   ↑ ↓ Filas   ·   OK Ficha   ·   Menú Opciones", maxLines = 1,
            style = TextStyle(OfflineTvTheme.Colors.textMuted, 10.sp), modifier = Modifier.padding(top = 5.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VodCover(item: VodVisualItemUiModel, binder: VodVisualPosterImageBinder,
    compact: Boolean, requester: FocusRequester, onFocus: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Column(Modifier.width(if (compact) 94.dp else 110.dp).scale(if (focused) 1.035f else 1f)
        .focusRequester(requester).onFocusChanged { focused = it.isFocused; if (it.isFocused) onFocus() }
        .onPreviewKeyEvent {
            if (it.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_MENU && item.onMenu != null) {
                if (it.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) item.onMenu.run()
                true
            } else false
        }
        .tvButtonSemantics(item.onClick != null, "${item.title}. ${item.meta}")
        .combinedClickable(enabled = item.onClick != null, onClick = { onFocus(); item.onClick?.run() },
            onLongClick = item.onMenu?.let { { it.run() } }), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        AndroidView(modifier = Modifier.fillMaxWidth().height(if (compact) 132.dp else 156.dp),
            factory = { AppCompatImageView(it).apply { scaleType = ImageView.ScaleType.FIT_CENTER; contentDescription = null } },
            onReset = null, onRelease = { Glide.with(it).clear(it) }, update = { image ->
                val identity = artworkIdentity(item)
                if (image.getTag(R.id.startup_home_artwork_request) != identity) {
                    image.setTag(R.id.startup_home_artwork_request, identity); binder.bind(image, item)
                }
            })
        BasicText(item.title, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.height(34.dp),
            style = TextStyle(if (focused) OfflineTvTheme.Colors.accentCyan else Color.White, 12.sp, FontWeight.Bold))
    }
}
