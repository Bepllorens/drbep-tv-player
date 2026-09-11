package com.drbep.tvplayer

import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import java.util.function.BiConsumer
import java.util.function.Consumer

internal object PrivateVodCardsBinder {
    @JvmStatic fun bind(view: ComposeView, title: String, message: String,
                       cards: List<PrivateVodBrowser.Card>, labels: List<String>, actions: List<Runnable>,
                       perform: Consumer<Runnable>, image: BiConsumer<ImageView, String>) {
        view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        view.setContent {
            val selectedIndex = cards.indexOfFirst { it.preferredFocus }
            val first = rememberTvInitialFocusRequester(labels.isNotEmpty() || selectedIndex >= 0, title)
            val gridState = rememberLazyGridState(initialFirstVisibleItemIndex = selectedIndex.coerceAtLeast(0))
            Column(Modifier.fillMaxSize().background(Color(0xFF081321)).padding(22.dp)) {
                BasicText(title, style = TextStyle(color=Color.White, fontSize=24.sp, fontWeight=FontWeight.Bold), maxLines=2)
                BasicText(message, style=TextStyle(color=Color(0xFFB8CADA), fontSize=14.sp))
                LazyRow(Modifier.padding(vertical=12.dp), horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    items(labels.indices.toList()) { index ->
                        FocusTile(Modifier.then(if(index==0 && selectedIndex < 0) Modifier.focusRequester(first) else Modifier), { perform.accept(actions[index]) }) {
                            BasicText(labels[index], Modifier.padding(10.dp), style=TextStyle(color=Color.White, fontSize=15.sp))
                        }
                    }
                }
                if(cards.isEmpty()) BasicText("No hay resultados", style=TextStyle(color=Color.White))
                LazyVerticalGrid(GridCells.Adaptive(300.dp), Modifier.weight(1f),
                    state=gridState,
                    horizontalArrangement=Arrangement.spacedBy(12.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    items(cards, key={it.posterQuery}) { card ->
                        FocusTile(Modifier.fillMaxWidth().then(if(card.preferredFocus) Modifier.focusRequester(first) else Modifier), { perform.accept(card.action) }) {
                            Row(Modifier.padding(10.dp).height(150.dp), horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                                AndroidView(factory={ context -> ImageView(context).apply { scaleType=ImageView.ScaleType.FIT_CENTER } },
                                    modifier=Modifier.width(96.dp).fillMaxHeight(),
                                    update={ image.accept(it, card.posterQuery) },
                                    onRelease={ Glide.with(it).clear(it) })
                                Column(Modifier.weight(1f)) {
                                    BasicText(card.title, style=TextStyle(color=Color.White, fontSize=16.sp, fontWeight=FontWeight.Bold), maxLines=2, overflow=TextOverflow.Ellipsis)
                                    Spacer(Modifier.height(8.dp))
                                    if(card.synopsis.isNotBlank()) BasicText(card.synopsis,
                                        style=TextStyle(color=Color(0xFFD2DDE8), fontSize=13.sp), maxLines=5, overflow=TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun FocusTile(modifier: Modifier, click: () -> Unit, content: @Composable () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(modifier.onFocusChanged { focused=it.isFocused }
        .border(if(focused) 3.dp else 1.dp, if(focused) Color(0xFF58BDFF) else Color(0xFF34465B), RoundedCornerShape(10.dp))
        .background(if(focused) Color(0xFF244465) else Color(0xFF15263A), RoundedCornerShape(10.dp))
        .clickable(onClick=click)) { content() }
}
