package com.drbep.tvplayer

import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat

object GlobalSearchListComposeBinder {
    @JvmStatic
    fun bind(composeView: ComposeView?, model: GlobalSearchListUiModel, imageBinder: GlobalSearchImageBinder) {
        if (composeView == null) return
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        composeView.setContent {
            GlobalSearchPanel(model, imageBinder)
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun GlobalSearchPanel(model: GlobalSearchListUiModel, imageBinder: GlobalSearchImageBinder) {
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
        BasicText(
            text = model.title,
            style = TextStyle(color = Color.White, fontSize = if (compact) 20.sp else 28.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(if (compact) 10.dp else 14.dp))
        GlobalSearchField(model, compact)
        if (model.filters.isNotEmpty()) {
            Spacer(modifier = Modifier.height(if (compact) 10.dp else 14.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(if (compact) 6.dp else 8.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(if (compact) 6.dp else 8.dp)
            ) {
                model.filters.forEach { filter ->
                    FilterChip(filter, compact)
                }
            }
        }
        Spacer(modifier = Modifier.height(if (compact) 12.dp else 18.dp))
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            itemsIndexed(model.items) { _, item ->
                GlobalSearchRow(item, imageBinder)
            }
        }
    }
}

@Composable
private fun GlobalSearchField(model: GlobalSearchListUiModel, compact: Boolean) {
    var value by remember { mutableStateOf(model.query) }
    LaunchedEffect(model.query) {
        if (model.query != value) {
            value = model.query
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF142235))
            .border(2.dp, Color(0xFF2E5D82), RoundedCornerShape(16.dp))
            .padding(horizontal = if (compact) 14.dp else 18.dp, vertical = if (compact) 11.dp else 14.dp)
    ) {
        if (value.isEmpty()) {
            BasicText(
                text = model.hint,
                style = TextStyle(color = Color(0xFF71859B), fontSize = if (compact) 16.sp else 19.sp, fontWeight = FontWeight.Bold)
            )
        }
        BasicTextField(
            value = value,
            onValueChange = {
                value = it
                model.onQueryChanged?.accept(it)
            },
            singleLine = true,
            textStyle = TextStyle(color = Color.White, fontSize = if (compact) 16.sp else 19.sp, fontWeight = FontWeight.Bold),
            cursorBrush = SolidColor(Color(0xFF74BFFF)),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun FilterChip(filter: GlobalSearchFilterUiModel, compact: Boolean) {
    val fill = if (filter.selected) Color(0xFF2A7C86) else Color(0xFF223249)
    val stroke = if (filter.selected) Color(0xFFB8F2FF) else Color(0xFF30455E)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(fill)
            .border(1.dp, stroke, RoundedCornerShape(999.dp))
            .tvButtonSemantics(filter.onClick != null)
            .clickable(enabled = filter.onClick != null) { filter.onClick?.run() }
            .padding(horizontal = if (compact) 12.dp else 15.dp, vertical = if (compact) 8.dp else 9.dp)
    ) {
        BasicText(
            text = filter.label,
            style = TextStyle(color = Color.White, fontSize = if (compact) 12.sp else 13.sp, fontWeight = FontWeight.Bold)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GlobalSearchRow(item: GlobalSearchRowUiModel, imageBinder: GlobalSearchImageBinder) {
    val compact = LocalConfiguration.current.screenWidthDp < 600
    if (item.header) {
        BasicText(
            text = item.title,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (compact) 8.dp else 10.dp, bottom = if (compact) 4.dp else 6.dp),
            style = TextStyle(color = Color(0xFF9BD0FF), fontSize = if (compact) 12.sp else 13.sp, fontWeight = FontWeight.Bold)
        )
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = if (compact) 6.dp else 8.dp)
            .background(Color(0xFF182638), RoundedCornerShape(14.dp))
            .tvButtonSemantics(item.onClick != null)
            .combinedClickable(
                enabled = item.onClick != null,
                onClick = { item.onClick?.run() },
                onLongClick = item.onLongClick?.let { { it.run() } }
            )
            .padding(if (compact) 10.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlobalSearchLogo(item, imageBinder, compact)
        Spacer(modifier = Modifier.width(if (compact) 10.dp else 12.dp))
        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                text = item.title,
                style = TextStyle(color = Color.White, fontSize = if (compact) 15.sp else 18.sp, fontWeight = FontWeight.Bold)
            )
            if (item.meta.isNotEmpty()) {
                BasicText(
                    text = item.meta,
                    modifier = Modifier.padding(top = if (compact) 4.dp else 6.dp),
                    style = TextStyle(color = Color(0xFFC4D0DF), fontSize = if (compact) 11.sp else 13.sp)
                )
            }
        }
        if (item.badge.isNotEmpty()) {
            Spacer(modifier = Modifier.width(if (compact) 8.dp else 10.dp))
            Box(
                modifier = Modifier
                    .background(Color(0xFF1E2D3E), RoundedCornerShape(12.dp))
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
}

@Composable
private fun GlobalSearchLogo(item: GlobalSearchRowUiModel, imageBinder: GlobalSearchImageBinder, compact: Boolean) {
    AndroidView(
        modifier = Modifier.size(if (compact) 38.dp else 42.dp),
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
