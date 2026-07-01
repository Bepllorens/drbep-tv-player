package com.drbep.tvplayer

import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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

object VisualEpgSectionsComposeBinder {
    @JvmStatic
    fun bind(composeView: ComposeView?, sections: List<VisualEpgSectionUiModel>, imageBinder: VisualEpgCardImageBinder) {
        if (composeView == null) return
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        composeView.setContent {
            VisualEpgSections(sections, imageBinder)
        }
    }
}

@Composable
internal fun VisualEpgSections(
    sections: List<VisualEpgSectionUiModel>,
    imageBinder: VisualEpgCardImageBinder,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 8.dp)
    ) {
        sections.forEachIndexed { sectionIndex, section ->
            item(key = "title-$sectionIndex-${section.title}") {
                VisualEpgSectionTitleRow(section.title, sectionIndex == 0)
            }
            item(key = "row-$sectionIndex-${section.title}") {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = if (LocalConfiguration.current.screenWidthDp < 600) 8.dp else 12.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    itemsIndexed(section.entries ?: emptyList(), key = { index, _ -> "$sectionIndex-$index" }) { _, entry ->
                        VisualEpgSectionCard(entry, imageBinder)
                        Spacer(modifier = Modifier.width(if (LocalConfiguration.current.screenWidthDp < 600) 8.dp else 10.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun VisualEpgSectionTitleRow(title: String, first: Boolean) {
    val compact = LocalConfiguration.current.screenWidthDp < 600
    BasicText(
        text = title,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 2.dp,
                top = if (first) 2.dp else if (compact) 10.dp else 14.dp,
                end = 2.dp,
                bottom = if (compact) 6.dp else 8.dp
            ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(color = Color.White, fontSize = if (compact) 15.sp else 18.sp, fontWeight = FontWeight.Bold)
    )
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun VisualEpgSectionCard(entry: VisualEpgEntryUiModel, imageBinder: VisualEpgCardImageBinder) {
    val compact = LocalConfiguration.current.screenWidthDp < 600
    val requester = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(entry.preferred) {
        if (entry.preferred) {
            requester.requestFocus()
        }
    }
    val model = entry.card ?: VisualEpgCardUiModel("", "", "", "", false, focused)
    val effectiveModel = VisualEpgCardUiModel(
        model.title,
        model.timeLabel,
        model.badgeLabel,
        model.posterUrl,
        model.scheduled,
        focused
    )
    val shape = RoundedCornerShape(if (compact) 14.dp else 18.dp)
    val borderColor = when {
        focused -> Color(0xFF68B6FF)
        effectiveModel.scheduled -> Color(0xFFAF7A21)
        else -> Color(0xFF284156)
    }
    Column(
        modifier = Modifier
            .width(if (compact) 136.dp else 164.dp)
            .height(if (compact) 182.dp else 208.dp)
            .graphicsLayer {
                scaleX = if (focused) 1.03f else 1f
                scaleY = if (focused) 1.03f else 1f
            }
            .clip(shape)
            .background(if (focused) Color(0xFF213447) else Color(0xFF17232F))
            .border(2.dp, borderColor, shape)
            .focusRequester(requester)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) {
                    entry.onFocus?.run()
                }
            }
            .onPreviewKeyEvent {
                if (it.nativeKeyEvent.action == KeyEvent.ACTION_DOWN &&
                    (it.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_MENU || it.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_BUTTON_START)
                ) {
                    entry.onClick?.run()
                    true
                } else {
                    false
                }
            }
            .tvButtonSemantics(entry.onClick != null)
            .combinedClickable(enabled = entry.onClick != null, onClick = { entry.onClick?.run() })
            .padding(if (compact) 6.dp else 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 88.dp else 104.dp)
                .clip(RoundedCornerShape(if (compact) 10.dp else 12.dp))
                .background(Color(0xFF0E1820))
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    FrameLayout(context).apply {
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
                update = { frame -> imageBinder.bind(frame.getChildAt(0) as ImageView, effectiveModel) }
            )
            if (effectiveModel.badgeLabel.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(if (compact) 4.dp else 6.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (effectiveModel.scheduled) Color(0xCC8E5B16) else Color(0xCC214A72))
                        .padding(horizontal = if (compact) 6.dp else 7.dp, vertical = if (compact) 2.dp else 3.dp)
                ) {
                    BasicText(
                        text = effectiveModel.badgeLabel,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(color = Color.White, fontSize = if (compact) 9.sp else 10.sp, fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(if (compact) 6.dp else 8.dp))
        BasicText(
            text = effectiveModel.title,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(color = Color.White, fontSize = if (compact) 11.sp else 12.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(if (compact) 3.dp else 4.dp))
        BasicText(
            text = effectiveModel.timeLabel,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(color = Color(0xFFC9D8E8), fontSize = if (compact) 9.sp else 10.sp)
        )
    }
}
