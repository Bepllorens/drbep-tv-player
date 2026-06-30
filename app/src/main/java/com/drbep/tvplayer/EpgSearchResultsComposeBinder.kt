package com.drbep.tvplayer

import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

object EpgSearchResultsComposeBinder {
    @JvmStatic
    fun bind(composeView: ComposeView?, model: EpgSearchResultListUiModel, imageBinder: EpgSearchImageBinder) {
        if (composeView == null) return
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        composeView.setContent {
            EpgSearchResultsPanel(model, imageBinder)
        }
    }
}

@Composable
private fun EpgSearchResultsPanel(model: EpgSearchResultListUiModel, imageBinder: EpgSearchImageBinder) {
    val compact = LocalConfiguration.current.screenWidthDp < 600
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF20B111A))
            .padding(
                horizontal = if (compact) 18.dp else 56.dp,
                vertical = if (compact) 18.dp else 42.dp
            )
    ) {
        PanelHeader(model.title, model.subtitle, compact)
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            itemsIndexed(model.items) { _, item ->
                EpgSearchRow(item, imageBinder)
            }
        }
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
private fun EpgSearchRow(item: EpgSearchResultRowUiModel, imageBinder: EpgSearchImageBinder) {
    val compact = LocalConfiguration.current.screenWidthDp < 600
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = if (compact) 6.dp else 8.dp)
            .background(Color(0xFF1C2733), RoundedCornerShape(14.dp))
            .tvButtonSemantics(item.onClick != null)
            .clickable(enabled = item.onClick != null) { item.onClick?.run() }
            .padding(if (compact) 10.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EpgSearchPoster(item, imageBinder, compact)
        Spacer(modifier = Modifier.width(if (compact) 10.dp else 12.dp))
        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                text = item.title,
                style = TextStyle(color = Color.White, fontSize = if (compact) 15.sp else 17.sp, fontWeight = FontWeight.Bold)
            )
            if (item.meta.isNotEmpty()) {
                BasicText(
                    text = item.meta,
                    modifier = Modifier.padding(top = if (compact) 4.dp else 6.dp),
                    style = TextStyle(color = Color(0xFFC4D0DF), fontSize = if (compact) 11.sp else 13.sp)
                )
            }
        }
        Spacer(modifier = Modifier.width(if (compact) 8.dp else 10.dp))
        Box(
            modifier = Modifier
                .background(Color(item.badgeColor), RoundedCornerShape(12.dp))
                .padding(horizontal = if (compact) 8.dp else 10.dp, vertical = if (compact) 5.dp else 6.dp),
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                text = item.badge,
                style = TextStyle(color = Color(0xFFDCE7F5), fontSize = if (compact) 10.sp else 11.sp, fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
private fun EpgSearchPoster(item: EpgSearchResultRowUiModel, imageBinder: EpgSearchImageBinder, compact: Boolean) {
    AndroidView(
        modifier = Modifier.size(width = if (compact) 50.dp else 58.dp, height = if (compact) 64.dp else 72.dp),
        factory = { context ->
            FrameLayout(context).apply {
                setBackgroundColor(0xFF16202A.toInt())
                addView(
                    AppCompatImageView(context).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        scaleType = ImageView.ScaleType.CENTER_CROP
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
