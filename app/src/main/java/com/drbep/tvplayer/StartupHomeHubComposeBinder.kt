package com.drbep.tvplayer

import kotlinx.coroutines.launch

import kotlin.coroutines.resume
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.input.key.onPreviewKeyEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.focusGroup
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
import androidx.compose.runtime.key
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
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
    @Suppress("UNCHECKED_CAST")
    fun update(composeView: ComposeView, model: StartupHomeHubUiModel) {
        val state = composeView.getTag(R.id.startup_home_model_state) as? androidx.compose.runtime.MutableState<StartupHomeHubUiModel>
        state?.value = model
    }

    @JvmStatic
    fun bind(composeView: ComposeView?, model: StartupHomeHubUiModel, artworkBinder: StartupHomeHubArtworkBinder?) {
        if (composeView == null) return
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        val state = mutableStateOf(model)
        composeView.setTag(R.id.startup_home_model_state, state)
        composeView.setContent { PlaylistImportIndicator.Surface { if (state.value.discovery != null) DiscoveryHome(state.value, artworkBinder) else StartupHomeHub(state.value, artworkBinder) } }
    }
}

@Composable
private fun StartupHomeHub(model: StartupHomeHubUiModel, artworkBinder: StartupHomeHubArtworkBinder?) {
    val compact = LocalConfiguration.current.screenWidthDp < 600
    var secondaryReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        // Give the primary actions their own first draw; do not add a timer or
        // block the first usable screen on artwork/secondary Android views.
        withFrameNanos { }
        withFrameNanos { }
        secondaryReady = true
    }
    val firstCardRequester = rememberTvInitialFocusRequester(model.primaryCards.isNotEmpty(), model)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .tvPanelBackHandler(model.onBack)
            .background(
                Brush.linearGradient(
                    listOf(OfflineTvTheme.Colors.backdrop, OfflineTvTheme.Colors.backdropAccent, OfflineTvTheme.Menu.row)
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
                        listOf(OfflineTvTheme.Menu.selected.copy(alpha = 0.33f), Color.Transparent)
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
            if (secondaryReady) {
            for (live in listOf(true, false)) {
            val continuation = StartupHomeHubUiModel.continueGroup(model.continueCards, live)
            if (continuation.isNotEmpty()) {
                Spacer(Modifier.height(if (compact) 20.dp else 24.dp))
                StartupSectionTitle(if (live) "TELEVISIÓN" else "CONTINUAR VIENDO · PELÍCULAS Y SERIES", "${continuation.size} disponibles", compact)
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 7.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    continuation.forEach { StartupContinueCard(it, artworkBinder, compact) }
                }
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
}

@Composable
private fun StartupTopBar(model: StartupHomeHubUiModel, compact: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(R.drawable.drbep_menu_logo),
            contentDescription = model.brand,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(if (compact) 38.dp else 44.dp).clip(OfflineTvTheme.Menu.shape)
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
                .clip(OfflineTvTheme.Menu.shape)
                .border(1.dp, Color(0x22FFFFFF), OfflineTvTheme.Menu.shape)
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
            .clip(OfflineTvTheme.Menu.shape)
            .border(if (focused) 2.dp else 1.dp, if (focused) OfflineTvTheme.Colors.focus else Color(0x22FFFFFF), OfflineTvTheme.Menu.shape)
            .background(if (focused) OfflineTvTheme.Menu.row else Color(0x22FFFFFF))
            .onFocusChanged { focused = it.isFocused }
            .tvButtonSemantics(action != null, label)
            .clickable(enabled = action != null) { action?.run() },
        contentAlignment = Alignment.Center
    ) { BasicText(icon, style = TextStyle(Color.White, if (compact) 18.sp else 20.sp, FontWeight.Bold)) }
}

// Read focus during drawing, not composition: changing selection must not
// rebuild text measurement or the native preview/artwork subtree.
private fun Modifier.startupFocusBorder(isFocused: () -> Boolean, radius: Dp, idle: Color, focus: Color = OfflineTvTheme.Colors.focus): Modifier =
    drawWithContent {
        drawContent()
        val active = isFocused()
        val stroke = (if (active) 3.dp else 1.dp).toPx()
        val inset = stroke / 2f
        val corner = (radius.toPx() - inset).coerceAtLeast(0f)
        drawRoundRect(
            color = if (active) focus else idle,
            topLeft = Offset(inset, inset),
            size = Size((size.width - stroke).coerceAtLeast(0f), (size.height - stroke).coerceAtLeast(0f)),
            cornerRadius = CornerRadius(corner, corner),
            style = Stroke(stroke)
        )
    }

@Composable
private fun StartupPrimaryCard(item: StartupHomeHubUiModel.PrimaryCard, modifier: Modifier, requester: FocusRequester?, compact: Boolean) {
    val focused = remember { mutableStateOf(false) }
    val shape = OfflineTvTheme.Menu.shape
    val colors = if (item.vod) {
        listOf(OfflineTvTheme.Menu.row, OfflineTvTheme.Menu.row)
    } else {
        listOf(OfflineTvTheme.Colors.backdropAccent, OfflineTvTheme.Menu.row)
    }
    Column(
        modifier = modifier
            .height(if (compact) 138.dp else 170.dp)
            .graphicsLayer {
                val factor = if (focused.value) 1.018f else 1f
                scaleX = factor
                scaleY = factor
            }
            .clip(shape)
            .background(Brush.linearGradient(colors))
            .startupFocusBorder({ focused.value }, if (compact) 18.dp else 22.dp, OfflineTvTheme.Colors.accentSecondary.copy(alpha = 0.47f))
            .then(if (requester != null) Modifier.focusRequester(requester) else Modifier)
            .onFocusChanged { focused.value = it.isFocused }
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
    val artworkRequest = remember(item.imageUrl, item.imageLabel, item.poster, compact, artworkBinder) { Any() }
    val focused = remember { mutableStateOf(false) }
    val width = if (compact) 228.dp else 260.dp
    Column(
        modifier = Modifier
            .width(width)
            .graphicsLayer {
                val factor = if (focused.value) 1.025f else 1f
                scaleX = factor
                scaleY = factor
            }
            .clip(OfflineTvTheme.Menu.shape)
            .startupFocusBorder({ focused.value }, 16.dp, OfflineTvTheme.Colors.accentSecondary.copy(alpha = 0.2f))
            .background(OfflineTvTheme.Menu.row)
            .onFocusChanged { focused.value = it.isFocused }
            .tvButtonSemantics(
                item.onClick != null,
                listOf(if (item.livePreview) "En directo" else "", item.title, item.subtitle).filter { it.isNotBlank() }.joinToString(". ")
            )
            .clickable(enabled = item.onClick != null) { item.onClick?.run() }
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(if (compact) 100.dp else 112.dp).background(OfflineTvTheme.Menu.row)) {
            if (item.livePreview || item.imageUrl.isNotBlank()) {
                AndroidView(
                    modifier = if (item.poster || item.livePreview) Modifier.fillMaxSize()
                        else Modifier.align(Alignment.Center).padding(12.dp)
                            .width(144.dp).fillMaxHeight()
                            .clip(OfflineTvTheme.Menu.shape).background(Color(0xFFF1F5F9)).padding(12.dp),
                    factory = { context ->
                        FrameLayout(context).apply {
                            addView(AppCompatImageView(context).apply {
                                id = R.id.startup_home_artwork
                                layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                                scaleType = if (item.poster || item.livePreview) ImageView.ScaleType.CENTER_CROP else ImageView.ScaleType.CENTER_INSIDE
                                contentDescription = item.imageLabel
                            })
                        }
                    },
                    onReset = null,
                    onRelease = { frame ->
                        if (!item.livePreview) {
                            val image = frame.findViewById<ImageView>(R.id.startup_home_artwork)
                            artworkBinder?.releaseArtwork(image)
                            image.setTag(R.id.startup_home_artwork_request, null)
                        }
                    },
                    update = update@{ frame ->
                        val image = frame.findViewById<ImageView>(R.id.startup_home_artwork)
                        // Image loaders own the unkeyed tag. Keep request identity separate,
                        // and do not restart static image loads when remote focus changes.
                        if (!item.livePreview && image.getTag(R.id.startup_home_artwork_request) === artworkRequest) return@update
                        image.scaleType = if (item.poster || item.livePreview) ImageView.ScaleType.CENTER_CROP else ImageView.ScaleType.FIT_CENTER
                        image.contentDescription = item.imageLabel
                        if (item.livePreview) artworkBinder?.bindLivePreview(image, item.imageUrl, item.imageLabel, width.value.toInt(), if (compact) 100 else 112)
                        else if (item.poster) artworkBinder?.bindPoster(image, item.imageUrl, width.value.toInt(), if (compact) 100 else 112)
                        else artworkBinder?.bindLogo(image, item.imageUrl, item.imageLabel, 120, if (compact) 52 else 64)
                        if (artworkBinder != null) image.setTag(R.id.startup_home_artwork_request, artworkRequest)
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
                        .background(OfflineTvTheme.Menu.background, OfflineTvTheme.Menu.shape)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    style = TextStyle(Color(0xFFFFD6D6), 9.sp, FontWeight.Black)
                )
            }
            if (item.progress > 0f) {
                Box(Modifier.fillMaxWidth().height(4.dp).align(Alignment.BottomStart).background(Color(0x55FFFFFF))) {
                    Box(Modifier.fillMaxWidth(item.progress).fillMaxHeight().background(Brush.horizontalGradient(listOf(OfflineTvTheme.Colors.accentCyan, OfflineTvTheme.Menu.selected))))
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
            .clip(OfflineTvTheme.Menu.shape)
            .border(if (focused) 2.dp else 1.dp, if (focused) OfflineTvTheme.Colors.focus else OfflineTvTheme.Colors.accentSecondary.copy(alpha = 0.2f), OfflineTvTheme.Menu.shape)
            .background(if (focused) OfflineTvTheme.Menu.row else OfflineTvTheme.Colors.panelGlass.copy(alpha = 0.6f))
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

/** Discovery home: section changes never launch a player. */
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
private fun DiscoveryHome(model: StartupHomeHubUiModel, artwork: StartupHomeHubArtworkBinder?) {
    val discovery = model.discovery ?: return
    val touchLayout = (LocalConfiguration.current.uiMode and android.content.res.Configuration.UI_MODE_TYPE_MASK) != android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
    var section by remember { mutableStateOf(discovery.section.takeIf {
        it in listOf("Inicio", "En directo", "Mi biblioteca") && model.railLayout.visible(MainRailLayout.sectionId(it))
    } ?: "Inicio") }
    var provider by remember(section, model) { mutableStateOf(discovery.selectedProviders["En directo"] ?: "") }
    // Visible TV rows refresh EPG independently; explicit home changes publish a model.
    // A second timer rebuilt every channel card on the UI thread between those updates.
    val page = remember(model, section, provider) { discovery.page.apply(section, provider) }
    val initialFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        discovery.section = section
        withFrameNanos { }
        if (!(section == "Inicio" && !touchLayout && discovery.startupPreviewFocusPending && model.currentLive != null) && (!discovery.restoreLiveFocus || section != "En directo") && (section != "Inicio" || discovery.homeFocusedChannel.isBlank())) runCatching { initialFocus.requestFocus() }
    }
    val openSection: (String) -> Unit = { section = it; discovery.section = it; discovery.restoreLiveFocus = false; discovery.refreshHome?.run() }
    Box(Modifier.fillMaxSize().onPreviewKeyEvent {
        // Do not steal focus when a late startup channel arrives after navigation.
        if (it.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) discovery.startupPreviewFocusPending = false
        false
    }.focusRestorer().focusGroup().tvPanelBackHandler(Runnable { (discovery.cancelLiveOrder ?: model.onBack)?.run() })
        .background(if (section == "Inicio" || model.livePlaybackError) OfflineTvTheme.Colors.backdrop else Color.Transparent)
        .background(CachedMenuFade.horizontal(0f to Color(0xFA09121C), 0.38f to Color(0xE009121C), 0.75f to Color(0x7509121C), 1f to Color.Transparent))) {
        Box(Modifier.fillMaxSize().background(CachedMenuFade.vertical(0f to Color(0x1809121C), 0.45f to Color.Transparent, 1f to Color(0xF009121C))))
        Row(Modifier.fillMaxSize().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(if (section == "Inicio") 34.dp else 20.dp)) {
            Column(Modifier.width(68.dp).fillMaxHeight().focusGroup()
                .background(CachedMenuFade.horizontal(0f to Color(0xE609121C), 0.72f to Color(0xC009121C), 1f to Color.Transparent),
                    RoundedCornerShape(28.dp))
                .padding(horizontal = 6.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Image(painterResource(R.drawable.drbep_menu_logo), model.brand, Modifier.size(48.dp))
                Spacer(Modifier.height(10.dp))
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val sports = model.primaryCards.firstOrNull { it.navigationId == MainRailLayout.SPORTS }
                    val palantir = model.primaryCards.firstOrNull { it.navigationId == MainRailLayout.PALANTIR }
                    val alfaAdult = model.primaryCards.firstOrNull { it.navigationId == MainRailLayout.ALFA_ADULT }
 val movie = model.primaryCards.firstOrNull { it.navigationId == MainRailLayout.MOVIES }
                    val series = model.primaryCards.firstOrNull { it.navigationId == MainRailLayout.SERIES }
                    val available = buildList {
                        add(MainRailLayout.HOME); add(MainRailLayout.LIVE); add(MainRailLayout.LIBRARY)
                        if (model.onSearch != null) add(MainRailLayout.SEARCH)
                        if (movie != null) add(MainRailLayout.MOVIES)
                        if (series != null) add(MainRailLayout.SERIES)
 if(palantir!=null)add(MainRailLayout.PALANTIR)
 if(alfaAdult!=null)add(MainRailLayout.ALFA_ADULT)
 if(sports!=null)add(MainRailLayout.SPORTS)
                        if (model.onLiveGuide != null) add(MainRailLayout.GUIDE)
                        if (model.onLiveHistory != null) add(MainRailLayout.CATCHUP)
                    }
                    model.railLayout.visibleOrder(available).forEach { id ->
                        key(id) {
                            val icon = when(id) {
                                MainRailLayout.SEARCH -> R.drawable.rail_search
                                MainRailLayout.HOME -> R.drawable.rail_home
                                MainRailLayout.LIVE -> R.drawable.rail_tv
                                MainRailLayout.LIBRARY -> R.drawable.rail_library
                                MainRailLayout.MOVIES -> R.drawable.rail_movies
                                MainRailLayout.SERIES -> R.drawable.rail_series
 MainRailLayout.SPORTS -> R.drawable.rail_sports
 MainRailLayout.PALANTIR -> R.drawable.rail_palantir
 MainRailLayout.ALFA_ADULT -> R.drawable.rail_alfa_adult
                                MainRailLayout.GUIDE -> R.drawable.rail_guide
                                else -> R.drawable.rail_history
                            }
                            val selected = MainRailLayout.sectionId(section) == id
                            MainRailItem(MainRailLayout.label(id), icon, selected,
                                modifier = if (selected) Modifier.focusRequester(initialFocus) else Modifier,
                                action = {
                                    when(id) {
                                        MainRailLayout.SEARCH -> model.onSearch?.run()
                                        MainRailLayout.HOME -> openSection("Inicio")
                                        MainRailLayout.LIVE -> openSection("En directo")
                                        MainRailLayout.LIBRARY -> openSection("Mi biblioteca")
                                        MainRailLayout.MOVIES -> movie?.onClick?.run()
                                        MainRailLayout.SERIES -> series?.onClick?.run()
 MainRailLayout.SPORTS -> sports?.onClick?.run()
 MainRailLayout.PALANTIR -> palantir?.onClick?.run()
 MainRailLayout.ALFA_ADULT -> alfaAdult?.onClick?.run()
                                        MainRailLayout.GUIDE -> model.onLiveGuide?.run()
                                        MainRailLayout.CATCHUP -> model.onLiveHistory?.run()
                                    }
                                })
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                MainRailItem("Ajustes", R.drawable.rail_settings, false, action = { model.onSettings?.run() })
                discovery.requestExit?.let { action ->
                    MainRailItem("Salir", R.drawable.rail_power, false, action = { action.run() })
                }
                BasicText("BETA " + AppBuildInfo.versionCode(), Modifier.padding(top = 6.dp),
                    style = TextStyle(Color(0xFF8094A8), 9.sp))
            }
            Box(Modifier.weight(1f).fillMaxHeight()) {
                when (section) {
                    "En directo" -> DiscoveryLive(model, page, provider, artwork, Modifier.fillMaxSize()) { key ->
                        discovery.selectLiveProvider?.accept(key)
                        provider = key
                    }
                    "Inicio" -> LiveHomeLanding(model, artwork) { openSection("En directo") }
                    else -> DiscoveryLibrary(model, artwork)
                }
            }
        }
    }
}

/** The library only renders saved history; it never requests whole provider catalogs. */
@Composable
private fun DiscoveryLibrary(model: StartupHomeHubUiModel, artwork: StartupHomeHubArtworkBinder?) {
    var removed by remember { mutableStateOf(setOf<String>()) }
    fun identity(card: StartupHomeHubUiModel.ContinueCard) = card.libraryId.ifBlank { card.title + card.imageUrl }
    val cards = remember(model.continueCards, removed) {
        StartupHomeHubUiModel.continueGroup(model.continueCards, false).filter { identity(it) !in removed }.distinctBy { identity(it) }
    }
    var selectedId by remember { mutableStateOf(model.discovery?.librarySelectedId.orEmpty()) }
    val selected = cards.firstOrNull { identity(it) == selectedId } ?: cards.firstOrNull()
    var details by remember(selected?.libraryId, selected?.title) { mutableStateOf(arrayOf(selected?.description.orEmpty(), selected?.facts.orEmpty())) }
    LaunchedEffect(selected?.libraryId, selected?.title) {
        val card = selected ?: return@LaunchedEffect
        delay(350)
        if (artwork != null) details = kotlinx.coroutines.suspendCancellableCoroutine<Array<String>> { continuation ->
            val cancel = artwork.loadLibraryInfo(card) { info -> if (continuation.isActive) continuation.resume(info) }
            continuation.invokeOnCancellation { cancel.run() }
        }
    }
    var actions by remember { mutableStateOf(false) }
    val actionFocus = remember { FocusRequester() }
    val coverFocus = remember { FocusRequester() }
    androidx.compose.runtime.DisposableEffect(actions, model.discovery) {
        val dismiss = if (actions) Runnable { actions = false; runCatching { coverFocus.requestFocus() } } else null
        model.discovery?.dismissLibraryActions = dismiss
        onDispose { if (model.discovery?.dismissLibraryActions === dismiss) model.discovery?.dismissLibraryActions = null }
    }
    LaunchedEffect(removed) {
        if (removed.isNotEmpty() && cards.isNotEmpty()) { withFrameNanos { }; runCatching { coverFocus.requestFocus() } }
    }
    LaunchedEffect(actions) {
        if (actions) { withFrameNanos { }; runCatching { actionFocus.requestFocus() } }
    }
    Column(Modifier.fillMaxSize().padding(top = 8.dp, end = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        model.onSavedArchive?.let { action -> DiscoveryButton("Programas U7D guardados", action = { action.run() }) }
        BasicText("MI BIBLIOTECA", style = TextStyle(OfflineTvTheme.Colors.accentCyan, 12.sp, FontWeight.Bold, letterSpacing = 1.5.sp))
        Column(Modifier.fillMaxWidth().height(176.dp)
            .background(Brush.horizontalGradient(listOf(Color(0xB009121C), Color.Transparent)))
            .padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            BasicText(selected?.title ?: "Tu próxima historia empieza aquí", maxLines = 2, overflow = TextOverflow.Ellipsis,
                style = TextStyle(Color.White, 25.sp, FontWeight.Bold))
            BasicText(selected?.let { listOf(it.providerKey, details[1].ifBlank { it.kind }).filter(String::isNotBlank).joinToString("  ·  ") }
                ?: "Aquí encontrarás lo que dejes a medias.", maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = TextStyle(OfflineTvTheme.Colors.accentCyan, 12.sp))
            BasicText(details[0].takeIf { it.isNotBlank() } ?: if (selected == null)
                "Explora tus películas, series y grabaciones. El progreso aparecerá al volver a la biblioteca."
                else "No hay sinopsis guardada para este título. Puedes consultar su ficha en el proveedor.",
                Modifier.weight(1f).fillMaxWidth(0.94f), maxLines = 3, overflow = TextOverflow.Ellipsis,
                style = TextStyle(OfflineTvTheme.Colors.textSoft, 13.sp, lineHeight = 18.sp))
            if (selected != null) {
                if (actions) Row(Modifier.horizontalScroll(rememberScrollState()).focusGroup()
                    .tvPanelBackHandler(Runnable { actions = false; runCatching { coverFocus.requestFocus() } }),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    DiscoveryButton("Continuar", modifier = Modifier.focusRequester(actionFocus), action = { (selected.onResume ?: selected.onClick)?.run() })
                    selected.onStartOver?.let { DiscoveryButton("Desde el principio", action = { it.run() }) }
                    selected.onEpisodes?.let { DiscoveryButton("Ver episodios", action = { it.run() }) }
                    if (selected.onResume != null && selected.providerKey != "Netflix") DiscoveryButton("Ver ficha", action = { selected.onClick?.run() })
                    selected.onRemove?.let { remove -> DiscoveryButton("Quitar de la fila", action = {
                        remove.run(); removed = removed + identity(selected); actions = false
                    }) }
                } else BasicText("${if(selected.progress>=.95f) "Casi terminado" else if(selected.progress>0f) "En curso" else "Sin empezar"} · ${selected.subtitle}  ·  OK para ver opciones", maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = TextStyle(if(selected.progress>=.95f) ContentStatusColors.live else if(selected.progress>0f) ContentStatusColors.pending else OfflineTvTheme.Colors.textMuted, 11.sp))
            }
        }
        if (cards.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                BasicText("Continuar viendo", style = TextStyle(Color.White, 17.sp, FontWeight.Bold))
                BasicText("${cards.indexOf(selected) + 1} / ${cards.size}", style = TextStyle(OfflineTvTheme.Colors.textMuted, 11.sp))
            }
            androidx.compose.foundation.lazy.LazyRow(Modifier.fillMaxWidth().height(194.dp).focusGroup(),
                state = androidx.compose.foundation.lazy.rememberLazyListState(cards.indexOfFirst { identity(it) == selectedId }.coerceAtLeast(0)),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 3.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(cards.size, key = { identity(cards[it]) }) { index ->
                    val card = cards[index]
                    var focused by remember { mutableStateOf(false) }
                    Column(Modifier.width(106.dp)
                        .then(if (card == selected) Modifier.focusRequester(coverFocus) else Modifier)
                        .onFocusChanged { focused = it.isFocused; if (it.isFocused) { selectedId = identity(card); model.discovery?.librarySelectedId = selectedId; actions = false } }
                        .tvButtonSemantics(true, "${card.title}. ${card.subtitle}")
                        .clickable { selectedId = identity(card); model.discovery?.librarySelectedId = selectedId; actions = true },
                        verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Box(Modifier.fillMaxWidth().height(151.dp)
                            .border(if (focused) 2.dp else 0.dp, if (focused) Color.White else Color.Transparent, RoundedCornerShape(4.dp))
                            .padding(3.dp).clip(RoundedCornerShape(2.dp))) {
                            AndroidView(modifier = Modifier.fillMaxSize(), factory = { AppCompatImageView(it).apply {
                                scaleType = ImageView.ScaleType.FIT_CENTER
                                importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
                            } }, onReset = null, onRelease = { artwork?.releaseArtwork(it) }, update = { image ->
                                val request = identity(card) + card.imageUrl
                                if (image.getTag(R.id.startup_home_artwork_request) != request) {
                                    image.setTag(R.id.startup_home_artwork_request, request)
                                    artwork?.bindArtworkCard(image, card, true, 212, 302, java.util.function.Consumer {})
                                }
                            })
                            if (card.progress > 0f) Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(3.dp).background(Color(0x66000000))) {
                                Box(Modifier.fillMaxHeight().fillMaxWidth(card.progress.coerceIn(0f, 1f)).background(if(card.progress>=.95f) ContentStatusColors.live else ContentStatusColors.pending))
                            }
                        }
                        BasicText(card.title, maxLines = 2, overflow = TextOverflow.Ellipsis,
                            style = TextStyle(if (focused) Color.White else OfflineTvTheme.Colors.textSoft, 11.sp, if (focused) FontWeight.Bold else FontWeight.Normal))
                    }
                }
            }
        }
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) { DiscoveryShortcuts(model.shortcuts) }
    }
}

@Composable
private fun LiveHomeLanding(model: StartupHomeHubUiModel, artwork: StartupHomeHubArtworkBinder?, browse: () -> Unit) {
    val discovery = model.discovery ?: return
    val homeChannels = remember(model) { model.resolveHomeChannels() }
    val live = model.currentLive
    val previewFocus = remember { FocusRequester() }
    val television = (LocalConfiguration.current.uiMode and android.content.res.Configuration.UI_MODE_TYPE_MASK) == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
    LaunchedEffect(live != null) {
        if (television && live != null && (discovery.startupPreviewFocusPending || discovery.homeFocusedChannel == "@preview")) {
            withFrameNanos { }; withFrameNanos { }
            if (discovery.startupPreviewFocusPending || discovery.homeFocusedChannel == "@preview") {
                runCatching { previewFocus.requestFocus() }
            }
        }
    }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val requesters = remember { mutableMapOf<String, FocusRequester>() }
    val groupFocus = remember { FocusRequester() }
    var groupsOpen by remember { mutableStateOf(false) }
    var restoreGroup by remember { mutableStateOf(false) }
    val presentation = remember(live?.subtitle) { HomeProgramPresentation.of(live?.subtitle.orEmpty()) }
    val spacious = LocalConfiguration.current.screenHeightDp >= 650
    androidx.compose.runtime.DisposableEffect(groupsOpen) {
        val close=if(groupsOpen) Runnable { groupsOpen=false;restoreGroup=true } else null
        discovery.cancelLiveOrder=close
        onDispose { if(discovery.cancelLiveOrder===close)discovery.cancelLiveOrder=null }
    }
    LaunchedEffect(groupsOpen) {
        if (!groupsOpen && restoreGroup) { withFrameNanos { }; runCatching { groupFocus.requestFocus() }; restoreGroup=false }
    }
    LaunchedEffect(Unit) {
        val id = discovery.homeFocusedChannel
        val index = homeChannels.indexOfFirst { it.channelId == id }
        if (index >= 0) {
            listState.scrollToItem(index)
            withFrameNanos { }; withFrameNanos { }
            runCatching { requesters[id]?.requestFocus() }
        } else if (id.isNotBlank() && (id != "@preview" || live == null)) {
            // Playback of VOD removes the live preview; never restore focus to
            // its stale sentinel or to a channel removed from the current list.
            withFrameNanos { }; withFrameNanos { }
            runCatching { groupFocus.requestFocus() }
        }
    }
    Column(Modifier.fillMaxSize().padding(top=8.dp,end=14.dp), verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            BasicText(if(live==null) "TU TELEVISIÓN" else if(model.livePlaybackError) "REPRODUCCIÓN INTERRUMPIDA · ${live.title}" else "● EN DIRECTO · ${live.title}",
                Modifier.weight(1f),maxLines=1,overflow=TextOverflow.Ellipsis,style=TextStyle(OfflineTvTheme.Colors.accentCyan,12.sp,FontWeight.Bold))
            BasicText(model.clock,style=TextStyle(Color.White,14.sp))
        }
        Row(Modifier.fillMaxWidth().height(if(spacious) 230.dp else 180.dp),horizontalArrangement=Arrangement.spacedBy(24.dp)) {
            Column(Modifier.weight(1f).fillMaxHeight(),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                BasicText(presentation.title.ifBlank { "Todo listo para ver televisión" },maxLines=2,overflow=TextOverflow.Ellipsis,
                    style=TextStyle(Color.White,if(spacious) 27.sp else 24.sp,FontWeight.Bold))
                if(presentation.metadata.isNotBlank()) BasicText(presentation.metadata,maxLines=1,overflow=TextOverflow.Ellipsis,style=TextStyle(OfflineTvTheme.Colors.textSoft,12.sp))
                if(!live?.schedule.isNullOrBlank()) BasicText(live!!.schedule,style=TextStyle(Color(0xFFC6D8E4),12.sp))
                if(live!=null) Box(Modifier.fillMaxWidth().height(2.dp).background(Color(0x35FFFFFF))) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(live.progress.coerceIn(0f,1f)).background(OfflineTvTheme.Colors.accentCyan))
                }
                ProgrammeMetadataStrip(live?.programmeMetadata)
                if(!live?.description.isNullOrBlank()) BasicText(live!!.description,maxLines=2,overflow=TextOverflow.Ellipsis,style=TextStyle(Color(0xFFBDD0DE),12.sp))
                if(!live?.nextProgram.isNullOrBlank()) BasicText("Después · ${live!!.nextProgram}",maxLines=1,overflow=TextOverflow.Ellipsis,style=TextStyle(Color(0xFF9CB4C6),11.sp))
            }
            Box(Modifier.weight(1.1f).fillMaxHeight(),contentAlignment=Alignment.CenterEnd) {
                if(live!=null) {
                    var focused by remember { mutableStateOf(false) }
                    Box(Modifier.fillMaxHeight().aspectRatio(16f/9f,true)
                        .focusRequester(previewFocus)
                        .border(2.dp,if(focused) Color.White else Color.Transparent,RoundedCornerShape(6.dp))
                        .onFocusChanged { focused=it.isFocused; if(it.isFocused) { discovery.startupPreviewFocusPending=false; discovery.homeFocusedChannel="@preview" } }
                        .tvButtonSemantics(true,"Ver ${live.title} a pantalla completa")
                        .clickable { live.onClick?.run() }) {
                        DiscoveryLivePreview(live,artwork,Modifier.fillMaxSize())
                    }
                } else BasicText("Selecciona un canal",style=TextStyle(OfflineTvTheme.Colors.textMuted,14.sp))
            }
        }
        Row(Modifier.focusGroup(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            DiscoveryButton(if(live==null) "Elegir canal" else if(model.livePlaybackError) "Reintentar canal" else "Ver televisión",accent=Color(0xFF62DDED),action={ if(live==null) browse() else live.onClick?.run() })
            model.onLiveStartOver?.let { action -> DiscoveryButton("Desde el inicio",accent=Color(0xFFFFC56B),action={action.run()}) }
            model.onLiveRecord?.let { action -> DiscoveryButton("Grabar",accent=Color(0xFFFF8594),action={action.run()}) }
        }
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            BasicText("Tus canales",style=TextStyle(Color.White,17.sp,FontWeight.Bold))
            DiscoveryButton(model.liveGroupTitle.ifBlank { "Plataforma" }+" ▾",modifier=Modifier.focusRequester(groupFocus),action={groupsOpen=!groupsOpen;restoreGroup=true})
            Spacer(Modifier.weight(1f))
            DiscoveryButton("Explorar →",action=browse)
        }
        if(groupsOpen) {
            val first=rememberTvInitialFocusRequester(true)
            LazyColumn(Modifier.weight(1f).fillMaxWidth().background(OfflineTvTheme.Menu.background)
                .tvPanelBackHandler(Runnable { groupsOpen=false;restoreGroup=true })) {
                items(discovery.liveProviders.size,key={discovery.liveProviders[it].key}) { i ->
                    val provider=discovery.liveProviders[i]
                    DiscoveryButton(provider.title,selected=discovery.selectedProviders["En directo"]==provider.key,
                        modifier=if(i==0) Modifier.focusRequester(first) else Modifier,action={
                            discovery.homeFocusedChannel="";discovery.selectLiveProvider?.accept(provider.key);groupsOpen=false;restoreGroup=true
                        })
                }
            }
        } else LazyColumn(Modifier.weight(1f).fillMaxWidth().background(OfflineTvTheme.Menu.background),state=listState,
            verticalArrangement=Arrangement.spacedBy(2.dp),contentPadding=androidx.compose.foundation.layout.PaddingValues(bottom=12.dp)) {
            items(homeChannels.size,key={"channel:${homeChannels[it].channelId}"}) { index ->
                val channel=homeChannels[index]
                val focus=requesters.getOrPut(channel.channelId) { FocusRequester() }
                var focused by remember { mutableStateOf(false) }
                val playing=channel.channelId==live?.channelId
                Row(Modifier.fillMaxWidth().height(54.dp).focusRequester(focus)
                    .onFocusChanged { focused=it.isFocused;if(it.isFocused)discovery.homeFocusedChannel=channel.channelId }
                    .background(if(focused) OfflineTvTheme.Menu.selected else if(playing) Color(0x172FD2D9) else Color.Transparent)
                    .tvButtonSemantics(true,channel.title+". "+channel.subtitle,playing)
                    .clickable { channel.onClick?.run() }.padding(horizontal=8.dp,vertical=4.dp),verticalAlignment=Alignment.CenterVertically) {
                    Box(Modifier.width(2.dp).height(34.dp).background(if(focused||playing) OfflineTvTheme.Colors.accentCyan else Color.Transparent))
                    Spacer(Modifier.width(10.dp))
                    DiscoveryArtwork(channel.imageUrl,channel.title,false,artwork,Modifier.width(48.dp).height(34.dp),48,34)
                    Column(Modifier.weight(1f).padding(horizontal=12.dp),verticalArrangement=Arrangement.spacedBy(3.dp)) {
                        BasicText(HomeProgramPresentation.of(channel.subtitle).title,maxLines=1,overflow=TextOverflow.Ellipsis,style=TextStyle(Color.White,14.sp,FontWeight.SemiBold))
                        BasicText(channel.title,maxLines=1,overflow=TextOverflow.Ellipsis,style=TextStyle(if(playing) OfflineTvTheme.Colors.accentCyan else Color(0xFF9CB4C6),11.sp))
                    }
                    BasicText(channel.schedule,maxLines=1,style=TextStyle(Color(0xFF9CB4C6),11.sp))
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun DiscoveryLive(model: StartupHomeHubUiModel, page: StartupHomeHubUiModel.Page, provider: String,
    artwork: StartupHomeHubArtworkBinder?, modifier: Modifier, selectProvider: (String) -> Unit) {
    val discovery = model.discovery ?: return
    var groupsOpen by remember { mutableStateOf(false) }
    var restoreGroupButton by remember { mutableStateOf(false) }
    val groupButtonFocus = remember { FocusRequester() }
    var categories by remember(discovery.liveProviders.toList()) { mutableStateOf(discovery.liveProviders.toList()) }
    var moving by remember { mutableStateOf<String?>(null) }
    var beforeMove by remember { mutableStateOf(categories) }
    var longPressUsed by remember { mutableStateOf(false) }
    val closeGroups: () -> Unit = {
        if (moving != null) { categories = beforeMove; moving = null }
        groupsOpen = false; restoreGroupButton = true
    }
    LaunchedEffect(groupsOpen, restoreGroupButton) {
        if (!groupsOpen && restoreGroupButton) {
            withFrameNanos { }; groupButtonFocus.requestFocus(); restoreGroupButton = false
        }
    }
    val categoryState = androidx.compose.foundation.lazy.rememberLazyListState()
    val categoryFocus = remember { mutableMapOf<String, FocusRequester>() }
    LaunchedEffect(groupsOpen) {
        if (groupsOpen) {
            val index = categories.indexOfFirst { it.key == provider }.coerceAtLeast(0)
            categoryState.scrollToItem(index)
            withFrameNanos { }; withFrameNanos { }
            categoryFocus[categories.getOrNull(index)?.key]?.requestFocus()
        }
    }
    LaunchedEffect(categories, moving) {
        moving?.let { key ->
            val index = categories.indexOfFirst { it.key == key }
            if (index >= 0) {
                categoryState.scrollToItem(index)
                withFrameNanos { }
                categoryFocus[key]?.requestFocus()
            }
        }
    }
    val rowTextMeasurer = androidx.compose.ui.text.rememberTextMeasurer(cacheSize = 64)
    var query by remember { mutableStateOf(discovery.liveQuery) }
    var searchOpen by remember { mutableStateOf(false) }
    var restoreSearch by remember { mutableStateOf(false) }
    val searchFocus = remember { FocusRequester() }
    val searchButtonFocus = remember { FocusRequester() }
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val closeSearch: () -> Unit = { searchOpen = false; restoreSearch = true; keyboard?.hide() }
    LaunchedEffect(searchOpen, restoreSearch) {
        if (searchOpen) { withFrameNanos { }; searchFocus.requestFocus() }
        else if (restoreSearch) { withFrameNanos { }; searchButtonFocus.requestFocus(); restoreSearch = false }
    }
    var channelRows by remember(provider) { mutableStateOf(page.cards.toList()) }
    var movingChannel by remember(provider) { mutableStateOf<String?>(null) }
    var beforeChannels by remember(provider) { mutableStateOf(channelRows) }
    var channelLongPress by remember { mutableStateOf(false) }
    LaunchedEffect(page) { if (movingChannel == null) channelRows = page.cards.toList() }
    val channels = remember(channelRows, query) { channelRows.filter { it.title.contains(query, true) || it.subtitle.contains(query, true) } }
    val channelFocus = remember { mutableMapOf<String, FocusRequester>() }
    androidx.compose.runtime.SideEffect {
        discovery.cancelLiveOrder = when {
            groupsOpen -> Runnable { closeGroups() }
            searchOpen -> Runnable { closeSearch() }
            movingChannel != null -> Runnable { channelRows = beforeChannels; movingChannel = null }
            moving != null -> Runnable { categories = beforeMove; moving = null }
            else -> null
        }
    }
    androidx.compose.runtime.DisposableEffect(discovery) { onDispose { discovery.cancelLiveOrder = null } }

    var previewChannelId by remember { mutableStateOf(model.currentLive?.channelId ?: "") }
    var selectedId by remember(provider) { mutableStateOf(discovery.selectedLiveChannels[provider] ?: discovery.selectedLiveChannel) }
    val selectedChannel = { channels.firstOrNull { it.channelId == selectedId } }
    val navigationCadence = remember(provider, query) { TvNavigationCadence() }
    val channelNavigationScope = androidx.compose.runtime.rememberCoroutineScope()
    val channelFocusQueue = remember(provider, query, channels) { TvChannelFocusQueue() }
    val channelNavigationJob = remember(provider, query) { arrayOfNulls<kotlinx.coroutines.Job>(1) }
    androidx.compose.runtime.DisposableEffect(channelNavigationJob, channelFocusQueue) {
        onDispose { channelNavigationJob[0]?.cancel(); channelFocusQueue.cancel() }
    }
    val positionKey = "live:$provider:$query"
    val gridState = remember(positionKey) {
        // Start at the selected channel when there is no saved viewport, instead
        // of composing the first page and synchronously measuring another page.
        val initialIndex = discovery.scrollPositions[positionKey]
            ?: channels.indexOfFirst { it.channelId == selectedId }.coerceAtLeast(0)
        androidx.compose.foundation.lazy.LazyListState(initialIndex,
            discovery.scrollPositions["$positionKey:offset"] ?: 0)
    }
    androidx.compose.runtime.DisposableEffect(positionKey, gridState) {
        onDispose {
            discovery.scrollPositions[positionKey] = gridState.firstVisibleItemIndex
            discovery.scrollPositions["$positionKey:offset"] = gridState.firstVisibleItemScrollOffset
        }
    }
    LaunchedEffect(channelRows, movingChannel) {
        movingChannel?.let { id ->
            val index = channels.indexOfFirst { it.channelId == id }
            if (index >= 0) {
                if (gridState.layoutInfo.visibleItemsInfo.none { it.index == index }) gridState.scrollToItem(index)
                withFrameNanos { }
                channelFocus[id]?.requestFocus()
            }
        }
    }

    LaunchedEffect(provider, channels.isEmpty()) {
        if (searchOpen || groupsOpen || movingChannel != null) return@LaunchedEffect
        // Explore replaces the focused home button. Give the new screen a
        // concrete focus target instead of leaving focus on a removed node.
        val target = selectedChannel() ?: channels.getOrNull(gridState.firstVisibleItemIndex) ?: channels.firstOrNull()
        if (target == null) {
            withFrameNanos { }
            runCatching { groupButtonFocus.requestFocus() }
        } else {
            val index = channels.indexOf(target).coerceAtLeast(0)
            android.os.Trace.beginSection("DRBEP.tvEntryPosition")
            try {
                if (gridState.layoutInfo.visibleItemsInfo.none { it.index == index }) {
                    // Schedule positioning for the regular layout pass. Avoid
                    // a synchronous extra measure while the TV screen is built.
                    gridState.requestScrollToItem(index)
                }
            } finally { android.os.Trace.endSection() }
            withFrameNanos { }; withFrameNanos { }
            var restored = false
            repeat(3) {
                if (!restored) {
                    withFrameNanos { }
                    restored = runCatching {
                        val requester = channelFocus[target.channelId] ?: return@runCatching false
                        requester.requestFocus()
                        true
                    }.getOrDefault(false)
                }
            }
            if (!restored) runCatching { groupButtonFocus.requestFocus() }
        }
        discovery.restoreLiveFocus = false
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BasicText("En directo", style = TextStyle(Color.White, 21.sp, FontWeight.Bold))
        Box {
            DiscoveryButton((categories.firstOrNull { it.key == provider }?.title ?: "Elegir grupo") + " ▾",
                selected = groupsOpen, modifier = Modifier.focusRequester(groupButtonFocus), action = { groupsOpen = true })
            if (groupsOpen) androidx.compose.ui.window.Popup(
                alignment = Alignment.TopStart,
                offset = androidx.compose.ui.unit.IntOffset(0, with(androidx.compose.ui.platform.LocalDensity.current) { 44.dp.roundToPx() }),
                onDismissRequest = { closeGroups() },
                properties = androidx.compose.ui.window.PopupProperties(focusable = true)) {
            LazyColumn(Modifier.width(260.dp).height(360.dp).background(OfflineTvTheme.Menu.background, OfflineTvTheme.Menu.shape).padding(8.dp).focusGroup(), state = categoryState, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(categories.size, key = { categories[it].key }) { index ->
                    val category = categories[index]
                    val requester = categoryFocus.getOrPut(category.key) { FocusRequester() }
                    var focused by remember { mutableStateOf(false) }
                    Row(Modifier.fillMaxWidth().height(42.dp).clip(OfflineTvTheme.Menu.shape)
                        .background(if (moving == category.key) Color(0xFF916B22) else if (focused) Color.White else if (provider == category.key) Color(0xFF203747) else Color.Transparent)
                        .focusRequester(requester)
                        .onPreviewKeyEvent { event ->
                            val key = event.nativeKeyEvent
                            val down = key.action == android.view.KeyEvent.ACTION_DOWN
                            val ok = key.keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER || key.keyCode == android.view.KeyEvent.KEYCODE_ENTER
                            if (moving != null) {
                                when (key.keyCode) {
                                    android.view.KeyEvent.KEYCODE_DPAD_UP, android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                                        if (down) categories = LiveCategoryOrder.move(categories, moving, if (key.keyCode == android.view.KeyEvent.KEYCODE_DPAD_UP) -1 else 1)
                                    }
                                    android.view.KeyEvent.KEYCODE_BACK -> if (!down) { categories = beforeMove; moving = null }
                                    android.view.KeyEvent.KEYCODE_DPAD_CENTER, android.view.KeyEvent.KEYCODE_ENTER -> if (!down) {
                                        if (longPressUsed) longPressUsed = false
                                        else { discovery.saveLiveOrder?.accept(categories.map { it.key }); moving = null }
                                    }
                                }
                                true
                            } else if (ok) {
                                if (down && key.repeatCount > 0 && category.key != "ALL") {
                                    beforeMove = categories; moving = category.key; longPressUsed = true
                                } else if (!down) { if (provider != category.key) artwork?.onLiveGroupChanged(); selectProvider(category.key); closeGroups() }
                                true
                            } else false
                        }
                        .onFocusChanged { focused = it.isFocused }
                        .tvButtonSemantics(true, if (moving == category.key) "Moviendo ${category.title}" else category.title, provider == category.key)
                        .clickable { if (provider != category.key) artwork?.onLiveGroupChanged(); selectProvider(category.key); closeGroups() }.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (category.logo.isNotBlank()) DiscoveryArtwork(category.logo, category.title, false, artwork, Modifier.width(34.dp).height(30.dp), 34, 30)
                        BasicText(category.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = TextStyle(if (focused) Color(0xFF0D1520) else Color.White, 12.sp, FontWeight.SemiBold))
                    }
                }
            }
            }
        }
            BasicText("${channels.size} canales", style = TextStyle(Color(0xFF9BAFC1), 11.sp))
            Spacer(Modifier.weight(1f))
            if (searchOpen) {
                BasicTextField(query, { query = it; discovery.liveQuery = it }, singleLine = true,
                    textStyle = TextStyle(Color.White, 13.sp), cursorBrush = Brush.verticalGradient(listOf(Color.White, Color.White)),
                    modifier = Modifier.width(190.dp).focusRequester(searchFocus).background(Color(0xFF203747), OfflineTvTheme.Menu.shape).padding(horizontal = 12.dp, vertical = 9.dp),
                    decorationBox = { inner -> Box { if (query.isEmpty()) BasicText("Canal o programa", style = TextStyle(Color(0xFFCCE0EB), 12.sp)); inner() } })
                DiscoveryButton("Cerrar búsqueda", action = closeSearch)
            } else DiscoveryButton(if (query.isBlank()) "⌕ Buscar" else "⌕ Buscar · filtro activo", modifier = Modifier.focusRequester(searchButtonFocus), action = { searchOpen = true })
        }
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            if (channels.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    BasicText(if (provider.isBlank()) "Elige un grupo para ver sus canales." else if (query.isBlank()) "No hay canales disponibles para este usuario." else "No se encontraron canales.", style = TextStyle(Color.White, 16.sp))
                }
            } else {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.foundation.gestures.LocalBringIntoViewSpec provides remember(navigationCadence) {
                        object : androidx.compose.foundation.gestures.BringIntoViewSpec {
                            override val scrollAnimationSpec: androidx.compose.animation.core.AnimationSpec<Float>
                                get() = androidx.compose.animation.core.tween(navigationCadence.durationMs(), easing = androidx.compose.animation.core.LinearEasing)
                        }
                    }
                ) {
                LazyColumn(Modifier.weight(1.05f).fillMaxHeight().graphicsLayer().background(OfflineTvTheme.Menu.background, OfflineTvTheme.Menu.shape).focusGroup(), state = gridState,
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(3.dp)) {
                    items(channels.size, key = { channels[it].channelId }) { index ->
                        val channel = liveProgrammeCard(channels[index], reuseRecentSnapshot = true)
                        val requester = channelFocus.getOrPut(channel.channelId) { FocusRequester() }
                        var focused by remember { mutableStateOf(false) }
                        val playing = channel.channelId == previewChannelId
                        val hasProgramme = channel.subtitle.isNotBlank() && !channel.subtitle.equals(channel.title, true) && channel.subtitle != "Programación no disponible"
                        val hasSchedule = hasProgramme && channel.schedule.isNotBlank()
                        Column(Modifier.fillMaxWidth().height(58.dp)
                            .focusRequester(requester)
                            .onPreviewKeyEvent { event ->
                                val key = event.nativeKeyEvent
                                val down = key.action == android.view.KeyEvent.ACTION_DOWN
                                val ok = key.keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER || key.keyCode == android.view.KeyEvent.KEYCODE_ENTER
                                if (down && key.keyCode != android.view.KeyEvent.KEYCODE_DPAD_UP && key.keyCode != android.view.KeyEvent.KEYCODE_DPAD_DOWN) { channelNavigationJob[0]?.cancel(); channelFocusQueue.cancel() }
                                if (down && movingChannel == null &&
                                    (key.keyCode == android.view.KeyEvent.KEYCODE_DPAD_UP || key.keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN)) {
                                    navigationCadence.onNavigation(key.eventTime, key.repeatCount)
                                    val targetIndex = channelFocusQueue.move(index, if (key.keyCode == android.view.KeyEvent.KEYCODE_DPAD_UP) -1 else 1, channels.size)
                                    if (targetIndex in channels.indices) {
                                        channelNavigationJob[0]?.cancel()
                                        channelNavigationJob[0] = channelNavigationScope.launch {
                                            val target = channels[targetIndex].channelId
                                            val visible = gridState.layoutInfo.visibleItemsInfo
                                            android.os.Trace.beginSection("DRBEP.tvFocusScroll")
                                            try {
                                            if (visible.any { it.index == targetIndex }) {
                                                val ready = channelFocus[target]
                                                if (ready != null && runCatching { ready.requestFocus(); true }.getOrDefault(false)) return@launch
                                            } else {
                                                // A partially visible row is not a full slot. Reveal the
                                                // destination using pixel geometry, not visible-item count.
                                                val rowHeight = visible.firstOrNull { it.index == index }?.size
                                                    ?: visible.firstOrNull()?.size ?: 1
                                                val offset = if (targetIndex < index) 0 else
                                                    -(gridState.layoutInfo.viewportEndOffset - rowHeight).coerceAtLeast(0)
                                                gridState.requestScrollToItem(targetIndex, offset)
                                            }
                                            } finally { android.os.Trace.endSection() }
                                            repeat(6) {
                                                withFrameNanos { }
                                                val requester = channelFocus[target]
                                                if (requester != null && runCatching { requester.requestFocus(); true }.getOrDefault(false)) return@launch
                                            }
                                        }
                                        return@onPreviewKeyEvent true
                                    }
                                }
                                if (movingChannel != null) {
                                    val delta = when (key.keyCode) {
                                        android.view.KeyEvent.KEYCODE_DPAD_UP -> -1
                                        android.view.KeyEvent.KEYCODE_DPAD_DOWN -> 1
                                        android.view.KeyEvent.KEYCODE_DPAD_LEFT -> -1
                                        android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> 1
                                        else -> 0
                                    }
                                    if (down && delta != 0) {
                                        val from = channelRows.indexOfFirst { it.channelId == movingChannel }
                                        val to = (from + delta).coerceIn(0, channelRows.lastIndex)
                                        channelRows = channelRows.toMutableList().apply { add(to, removeAt(from)) }
                                    }
                                    if (ok && !down) {
                                        if (channelLongPress) channelLongPress = false
                                        else { discovery.saveChannelOrder?.accept(provider, channelRows.map { it.channelId }); movingChannel = null }
                                    }
                                    true
                                } else if (key.keyCode == android.view.KeyEvent.KEYCODE_DPAD_RIGHT) {
                                    if (!down) { discovery.restoreLiveFocus = true; channel.onClick?.run() }
                                    true
                                } else if (ok) {
                                    if (down && key.repeatCount > 0 && query.isBlank()) {
                                        beforeChannels = channelRows; movingChannel = channel.channelId; channelLongPress = true
                                    } else if (!down) channel.onClick?.run()
                                    true
                                } else false
                            }
                            .clip(OfflineTvTheme.Menu.shape)
                            // Focus changes only invalidate drawing, not the logo/text composition.
                            .drawBehind {
                                val radius = CornerRadius(3.dp.toPx())
                                if (focused || playing || movingChannel == channel.channelId) {
                                    drawRoundRect(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(
                                        if (movingChannel == channel.channelId) Color(0x996B4A18) else if (focused) Color(0xCC30414F) else Color(0x55202F3C),
                                        Color.Transparent)), cornerRadius = radius)
                                    if (focused) drawRoundRect(Color(0xFFFFA348), size = Size(3.dp.toPx(), size.height), cornerRadius = radius)
                                }
                            }
                            .onFocusChanged { focused = it.isFocused; if (it.isFocused) { channelFocusQueue.focused(index); selectedId = channel.channelId; discovery.selectedLiveChannel = channel.channelId; discovery.selectedLiveChannels[provider] = channel.channelId } }
                            .tvButtonSemantics(true, (if (movingChannel == channel.channelId) "Moviendo " else "") + channel.title + ". " + channel.subtitle)
                            .clickable { selectedId = channel.channelId; discovery.selectedLiveChannel = channel.channelId; discovery.selectedLiveChannels[provider] = channel.channelId; channel.onClick?.run() }
                            .padding(9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                BasicText(if (playing) "▶" else "${index + 1}", modifier = Modifier.width(18.dp), style = TextStyle(if (playing) Color(0xFF67DBEA) else Color(0xFF9BB4C5), 10.sp))
                                DiscoveryArtwork(channel.imageUrl, channel.title, false, artwork, Modifier.width(42.dp).height(34.dp).padding(3.dp), 42, 34)
                                TvChannelRowText(channel.title,
                                    if (hasProgramme) remember(channel.subtitle) { HomeProgramPresentation.of(channel.subtitle).title } else "Sin programación",
                                    if (hasSchedule) channel.progress else null, rowTextMeasurer,
                                    Modifier.weight(1f).fillMaxHeight())
                            }
                        }
                    }
                }
                }
                DiscoveryLiveDetails(selectedChannel, { gridState.isScrollInProgress }, provider, artwork, Modifier.weight(1f),
                    onPreviewChannel = { previewChannelId = it })
            }
        }
        BasicText(if (moving != null || movingChannel != null) "Flechas Mover · OK Guardar · Atrás Cancelar" else "Mantén OK para ordenar canales", style = TextStyle(Color(0xFF819BAE), 10.sp))
    }
}

/** Read selection in this restart scope, not in the parent containing the channel list. */
@Composable
private fun DiscoveryLiveDetails(selectedChannel: () -> StartupHomeHubUiModel.ContinueCard?, scrolling: () -> Boolean, provider: String,
    artwork: StartupHomeHubArtworkBinder?, modifier: Modifier, onPreviewChannel: (String) -> Unit) {
    var displayed by remember(provider) { mutableStateOf(selectedChannel()) }
    LaunchedEffect(provider, selectedChannel, scrolling) {
        // Observe focus without recomposing the detail panel for every intermediate row.
        androidx.compose.runtime.snapshotFlow { selectedChannel() to scrolling() }.collectLatest { (selected, moving) ->
            if (moving) return@collectLatest
            delay(150)
            displayed = selected
        }
    }
    Column(modifier.fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    if (displayed == null) Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(OfflineTvTheme.Menu.background, OfflineTvTheme.Menu.shape), contentAlignment = Alignment.Center) {
                        BasicText("Selecciona un canal", style = TextStyle(Color(0xFFCCE0EB), 16.sp))
                    }
                    displayed?.let { original ->
                        val channel = liveProgrammeCard(original)
                        Box {
                            androidx.compose.runtime.key(provider) { DiscoveryLivePreview(channel, artwork, onPreviewChannel = onPreviewChannel) }
                            DiscoveryLiveSelectionHint(channel.channelId, selectedChannel,
                                Modifier.align(Alignment.BottomStart).padding(8.dp))
                        }
                        DiscoveryLiveProgrammeDetails(channel)

                    }
                }
}


/** Metadata stays stable until the focused row settles. */
@Composable
private fun DiscoveryLiveProgrammeDetails(channel: StartupHomeHubUiModel.ContinueCard) {
    val programme = remember(channel.subtitle) { HomeProgramPresentation.of(channel.subtitle) }
    val hasProgramme = channel.subtitle.isNotBlank() && !channel.subtitle.equals(channel.title, true) && channel.subtitle != "Programación no disponible"
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        BasicText(channel.title, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, style = TextStyle(Color.White, 15.sp, FontWeight.SemiBold))
        BasicText("OK para pantalla completa", style = TextStyle(Color(0xFF9FBDCF), 10.sp))
    }
    BasicText(if (hasProgramme) programme.title else "Sin programación", maxLines = 2, overflow = TextOverflow.Ellipsis, style = TextStyle(Color.White, 20.sp, FontWeight.Bold))
    if (hasProgramme && programme.metadata.isNotBlank()) BasicText(programme.metadata, maxLines = 1, overflow = TextOverflow.Ellipsis, style = TextStyle(Color(0xFF9FBDCF), 12.sp))
    if (hasProgramme && channel.schedule.isNotBlank()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            BasicText(channel.schedule, style = TextStyle(Color(0xFF9FBDCF), 11.sp))
            BasicText("● EN DIRECTO", style = TextStyle(Color(0xFF67DBEA), 10.sp, FontWeight.Bold))
        }
        Box(Modifier.fillMaxWidth().height(2.dp).background(Color(0xFF354B5B))) {
            Box(Modifier.fillMaxWidth(channel.progress.coerceIn(0f,1f)).fillMaxHeight().background(Color(0xFF67DBEA)))
        }
    }
    if (hasProgramme) ProgrammeMetadataStrip(channel.programmeMetadata)
    if (hasProgramme && channel.description.isNotBlank()) BasicText(channel.description, maxLines = 2, overflow = TextOverflow.Ellipsis, style = TextStyle(Color(0xFFC5D6E2), 12.sp))
    if (channel.nextProgram.isNotBlank() && !channel.nextProgram.equals(channel.title,true)) BasicText("Después · ${channel.nextProgram}", maxLines = 1, overflow = TextOverflow.Ellipsis, style = TextStyle(Color(0xFF9FBDCF), 11.sp))

}

@Composable
private fun DiscoveryLiveSelectionHint(displayedId: String,
    selectedChannel: () -> StartupHomeHubUiModel.ContinueCard?, modifier: Modifier) {
    // Read selection only in the layer: no text remeasurement or video recomposition.
    BasicText("Seleccionando canal…", modifier.graphicsLayer {
        alpha = if (displayedId != selectedChannel()?.channelId) 1f else 0f
    }.clearAndSetSemantics { }, style = TextStyle(Color(0xFF9FBDCF), 12.sp))
}

@Composable
private fun DiscoveryLivePreview(channel: StartupHomeHubUiModel.ContinueCard, artwork: StartupHomeHubArtworkBinder?, modifier: Modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f), onPreviewChannel: (String) -> Unit = {}) {
    // The first channel is already selected; debounce only subsequent focus changes.
    var settled by remember { mutableStateOf<StartupHomeHubUiModel.ContinueCard?>(channel) }
    // Debounce rapid D-pad traversal without covering the existing video.
    LaunchedEffect(channel.channelId) { delay(650); settled = channel }
    Box(modifier.clip(OfflineTvTheme.Menu.shape).background(Color(0xFF1C3346))) {
        AndroidView(modifier = Modifier.fillMaxSize(), factory = { context -> FrameLayout(context).apply {
            descendantFocusability=ViewGroup.FOCUS_BLOCK_DESCENDANTS
            importantForAccessibility=android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
            addView(AppCompatImageView(context).apply { id = R.id.startup_home_artwork }, FrameLayout.LayoutParams(-1, -1))
        } }, onReset = null, onRelease = { frame -> artwork?.releaseLivePreview(frame.findViewById<ImageView>(R.id.startup_home_artwork)) }, update = { frame ->
            val ready = settled
            if (ready != null && frame.tag != ready.channelId) {
                val image = frame.findViewById<ImageView>(R.id.startup_home_artwork)
                frame.tag = ready.channelId
                // Wait until the dialog is shown; initial composition can run before show().
                frame.post {
                    if (frame.isAttachedToWindow && frame.tag == ready.channelId) {
                        artwork?.bindSelectedLivePreview(image, ready, 420, 236)
                        onPreviewChannel(ready.channelId)
                    }
                }
            }
        })
    }
}

@Composable
private fun DiscoveryButton(label: String, selected: Boolean = false, modifier: Modifier = Modifier,
    onFocus: (() -> Unit)? = null, accent: Color? = null, action: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(modifier.clip(OfflineTvTheme.Menu.shape)
        .background(if (focused) accent ?: Color.White else accent?.copy(alpha = 0.18f) ?: if (selected) OfflineTvTheme.Menu.selected else Color.Transparent)
        .border(if (focused && accent != null) 2.dp else 1.dp,
            if (focused && accent != null) Color.White else accent?.copy(alpha = 0.65f) ?: if (selected) Color.White else Color.Transparent, OfflineTvTheme.Menu.shape)
        .onFocusChanged { focused = it.isFocused; if (it.isFocused) onFocus?.invoke() }
        .tvButtonSemantics(true, label)
        .clickable(onClick = action).padding(horizontal = 15.dp, vertical = 9.dp)) {
        BasicText(label, style = TextStyle(if (focused) Color(0xFF121215) else accent ?: Color.White, 12.sp, FontWeight.Bold))
    }
}

@Composable
private fun DiscoveryRail(title: String, cards: List<StartupHomeHubUiModel.ContinueCard>, artwork: StartupHomeHubArtworkBinder?, compact: Boolean) {
    if (cards.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        BasicText(title, style = TextStyle(Color.White, 17.sp, FontWeight.Bold))
        LazyColumn(Modifier.fillMaxWidth().height(if (compact) 280.dp else 360.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items(cards.size) { index -> DiscoveryCoverCard(cards[index], artwork, false) }
        }
    }
}

@Composable
private fun DiscoveryArtwork(url: String, label: String, poster: Boolean, binder: StartupHomeHubArtworkBinder?, modifier: Modifier, width: Int, height: Int) {
    AndroidView(modifier = modifier, factory = { context ->
        android.os.Trace.beginSection("DRBEP.logoCreate")
        try { (if (poster) AppCompatImageView(context) else IntegratedChannelLogoView(context)).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
        } } finally { android.os.Trace.endSection() }
    }, onReset = { image ->
        binder?.releaseArtwork(image)
        image.setTag(R.id.startup_home_artwork_request, null)
    }, onRelease = { binder?.releaseArtwork(it) }, update = { image ->
        if (image.getTag(R.id.startup_home_artwork_request) != url) {
            android.os.Trace.beginSection("DRBEP.logoBind")
            try {
                if (poster) binder?.bindPoster(image, url, width, height) else binder?.bindLogo(image, url, label, width, height)
            } finally { android.os.Trace.endSection() }
            image.setTag(R.id.startup_home_artwork_request, url)
        }
    })
}

@Composable
private fun DiscoveryBackdrop(candidates: List<StartupHomeHubUiModel.ContinueCard>, artwork: StartupHomeHubArtworkBinder?,
    paused: Boolean, onColor: (Int) -> Unit, onSelected: (StartupHomeHubUiModel.ContinueCard?) -> Unit) {
    val identities = candidates.map { it.title + it.imageUrl }
    var cursor by remember { mutableStateOf(0) }
    var loaded by remember { mutableStateOf(-1) }
    var ready by remember { mutableStateOf(false) }
    val ordered = remember(identities, cursor) {
        if (candidates.isEmpty()) emptyList() else candidates.drop(cursor % candidates.size) + candidates.take(cursor % candidates.size)
    }
    LaunchedEffect(loaded, ready, paused, identities) {
        if (ready && !paused && candidates.size > 1) {
            delay(10_000)
            ready = false
            cursor = (loaded + 1) % candidates.size
        }
    }
    val request = ordered.map { it.title + it.imageUrl }
    if (ordered.isNotEmpty()) AndroidView(modifier = Modifier.fillMaxSize(),
        factory = { AppCompatImageView(it).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
        } }, onReset = null, onRelease = { artwork?.releaseArtwork(it) }, update = { image ->
            if (image.getTag(R.id.startup_home_artwork_request) != request) {
                image.setTag(R.id.startup_home_artwork_request, request)
                artwork?.bindHero(image, ordered, java.util.function.IntConsumer { index ->
                    if (image.getTag(R.id.startup_home_artwork_request) == request) {
                        val card = ordered.getOrNull(index)
                        loaded = candidates.indexOf(card).coerceAtLeast(0)
                        onColor(HomeArtworkStyle.color(image.drawable))
                        onSelected(card)
                        ready = true
                    }
                }, 1280, 720)
            }
        })
}

@Composable
private fun DiscoveryHero(hero: StartupHomeHubUiModel.ContinueCard?, page: StartupHomeHubUiModel.Page,
    section: String, compact: Boolean, rotating: Boolean, hasCarousel: Boolean,
    onFocus: (Boolean) -> Unit, onToggle: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(if (compact) 182.dp else 190.dp)) {
        Column(Modifier.align(Alignment.CenterStart).fillMaxWidth(0.68f).padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BasicText(if (section == "Inicio") "DESTACADO PARA TI" else section.uppercase(), style = TextStyle(Color(0xFFE0E6EC), 10.sp, FontWeight.Bold, letterSpacing = 1.6.sp))
            BasicText(hero?.title ?: page.heading, maxLines = 2, overflow = TextOverflow.Ellipsis,
                style = TextStyle(Color.White, if (compact) 25.sp else 30.sp, FontWeight.Bold))
            BasicText(hero?.subtitle?.takeIf { it.isNotBlank() } ?: page.description, maxLines = 1,
                overflow = TextOverflow.Ellipsis, style = TextStyle(Color(0xFFE0E6EC), 12.sp))
            Row(Modifier.onFocusChanged { onFocus(it.hasFocus) }.focusGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (hero?.onClick != null) DiscoveryButton(if (section == "En directo") "Ver canal" else if (hero.progress > 0f) "Continuar" else "Ver ficha", selected = true, action = { hero?.onClick?.run() })
                if (page.browse != null) DiscoveryButton(if (section == "En directo") "Guía TV" else "Explorar", action = { page.browse.run() })
                if (hasCarousel && section != "En directo") DiscoveryButton(if (rotating) "Pausar fondo" else "Rotar fondo", action = onToggle)
            }
        }
    }
}


@Composable
private fun DiscoveryCoverCard(item: StartupHomeHubUiModel.ContinueCard, artwork: StartupHomeHubArtworkBinder?, portrait: Boolean) {
    var focused by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().height(86.dp)
        .background(if (focused) Color.White else Color.Transparent, OfflineTvTheme.Menu.shape)
        .onFocusChanged { focused = it.isFocused }
        .tvButtonSemantics(item.onClick != null, listOf(item.title, item.subtitle).filter { it.isNotBlank() }.joinToString(". "))
        .clickable(enabled = item.onClick != null) { item.onClick?.run() }.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(108.dp).height(68.dp)) {
            if (item.poster) AndroidView(modifier = Modifier.fillMaxSize(),
                factory = { AppCompatImageView(it).apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
                } }, onReset = null, onRelease = { artwork?.releaseArtwork(it) }, update = { image ->
                    if (image.getTag(R.id.startup_home_artwork_request) != item.imageUrl) {
                        image.setTag(R.id.startup_home_artwork_request, item.imageUrl)
                        artwork?.bindArtworkCard(image, item, false, 216, 136, java.util.function.Consumer {})
                    }
                })
            else if (item.imageUrl.isNotBlank()) DiscoveryArtwork(item.imageUrl, item.title, false, artwork, Modifier.fillMaxSize(), 216, 136)
        }
        Column(Modifier.weight(1f).padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            BasicText(item.title, maxLines = 2, overflow = TextOverflow.Ellipsis,
                style = TextStyle(if (focused) OfflineTvTheme.Colors.focusInk else Color.White, 16.sp, FontWeight.Bold))
            if (item.subtitle.isNotBlank()) BasicText(item.subtitle, maxLines = 2, overflow = TextOverflow.Ellipsis,
                style = TextStyle(if (focused) OfflineTvTheme.Colors.focusInk else OfflineTvTheme.Colors.textSoft, 12.sp))
            if (item.progress > 0f) Box(Modifier.fillMaxWidth().height(2.dp).background(Color(0x33FFFFFF))) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(item.progress.coerceIn(0f, 1f))
                    .background(if (focused) OfflineTvTheme.Colors.focusInk else OfflineTvTheme.Colors.accentCyan))
            }
        }
    }
}

@Composable
private fun DiscoveryShortcuts(items: List<StartupHomeHubUiModel.Shortcut>) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        items.forEach { item ->
            var focused by remember { mutableStateOf(false) }
            Row(Modifier.fillMaxWidth().height(54.dp)
                .background(if (focused) Color.White else Color.Transparent, OfflineTvTheme.Menu.shape)
                .onFocusChanged { focused = it.isFocused }
                .tvButtonSemantics(item.onClick != null, item.title)
                .clickable(enabled = item.onClick != null) { item.onClick?.run() }
                .padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                BasicText(item.title, modifier = Modifier.weight(1f), maxLines = 1,
                    style = TextStyle(if (focused) OfflineTvTheme.Colors.focusInk else Color.White, 16.sp, FontWeight.Bold))
                BasicText(item.subtitle, modifier = Modifier.weight(1.4f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = TextStyle(if (focused) OfflineTvTheme.Colors.focusInk else OfflineTvTheme.Colors.textSoft, 12.sp))
            }
        }
    }
}

@Composable
private fun DiscoveryNavItem(label: String, icon: String, expanded: Boolean, selected: Boolean,
                             modifier: Modifier = Modifier, action: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(modifier.fillMaxWidth().height(44.dp).clip(OfflineTvTheme.Menu.shape)
        .background(if (focused) Color.White else if (selected) Color(0xFF263E50) else Color.Transparent)
        .onFocusChanged { focused = it.isFocused }
        .tvButtonSemantics(true, label, selected).clickable(onClick = action).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        BasicText(icon, Modifier.width(26.dp), style = TextStyle(if (focused) Color(0xFF101B28) else Color.White, 19.sp, FontWeight.Bold))
        if (expanded) BasicText(label, maxLines = 1, overflow = TextOverflow.Ellipsis,
            style = TextStyle(if (focused) Color(0xFF101B28) else Color.White, 13.sp, FontWeight.Medium))
    }
}
