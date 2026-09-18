package com.drbep.tvplayer

import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

object VodOriginPickerComposeBinder {
    @JvmStatic fun bind(view: ComposeView, origins: List<VodVisualItemUiModel>, images: VodVisualPosterImageBinder) {
        view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        view.setContent {
            val first = rememberTvInitialFocusRequester(origins.isNotEmpty(), origins)
            Column(Modifier.fillMaxSize().background(OfflineTvTheme.Colors.backdrop).padding(32.dp)) {
                BasicText("Origen", style = TextStyle(Color.White, 28.sp, fontWeight = FontWeight.Bold))
                Spacer(Modifier.height(8.dp))
                BasicText("Tus plataformas · Atrás para volver", style = TextStyle(OfflineTvTheme.Colors.textSoft, 14.sp))
                Spacer(Modifier.height(20.dp))
                LazyVerticalGrid(columns = GridCells.Adaptive(180.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    itemsIndexed(origins, key = { _, item -> item.title }) { index, item ->
                        var focused by remember { mutableStateOf(false) }
                        Column(Modifier.height(128.dp).clip(RoundedCornerShape(16.dp))
                            .background(if (focused) OfflineTvTheme.Colors.focus else OfflineTvTheme.Colors.chip)
                            .then(if(index == 0) Modifier.focusRequester(first) else Modifier)
                            .onFocusChanged { focused = it.isFocused }
                            .tvButtonSemantics(true, item.title)
                            .clickable { item.onClick?.run() }.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            if(item.posterUrl.isNotBlank()) AndroidView(factory = { context -> AppCompatImageView(context).apply { scaleType = ImageView.ScaleType.FIT_CENTER } },
                                update = { image -> images.bind(image, item); image.scaleType = ImageView.ScaleType.FIT_CENTER }, modifier = Modifier.fillMaxWidth().height(64.dp))
                            else Box(Modifier.fillMaxWidth().height(64.dp), contentAlignment = Alignment.Center) {
                                BasicText(if(item.title == "Mis plataformas") "▦" else item.title, style = TextStyle(if(focused) Color.Black else Color.White, 24.sp, fontWeight = FontWeight.Bold))
                            }
                            BasicText(item.title, style = TextStyle(if(focused) Color.Black else Color.White, 16.sp, fontWeight = FontWeight.Bold))
                            if(item.meta.isNotBlank()) BasicText(item.meta, style = TextStyle(if(focused) Color.Black else OfflineTvTheme.Colors.textSoft, 12.sp))
                        }
                    }
                }
            }
        }
    }
}
