package com.drbep.tvplayer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object PersonalListComposeBinder {
    @JvmStatic
    fun bind(composeView: ComposeView?, model: PersonalListManagerUiModel) {
        if (composeView == null) return
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        composeView.setContent {
            PersonalListContent(model)
        }
    }

    @JvmStatic
    fun bindPanel(
        composeView: ComposeView?,
        title: String,
        subtitle: String,
        createLabel: String,
        closeLabel: String,
        model: PersonalListManagerUiModel,
        onCreate: Runnable?,
        onClose: Runnable?
    ) {
        if (composeView == null) return
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        composeView.setContent {
            PersonalListPanel(title, subtitle, createLabel, closeLabel, model, onCreate, onClose)
        }
    }
}

@Composable
private fun PersonalListPanel(
    title: String,
    subtitle: String,
    createLabel: String,
    closeLabel: String,
    model: PersonalListManagerUiModel,
    onCreate: Runnable?,
    onClose: Runnable?
) {
    val compact = LocalConfiguration.current.screenWidthDp < 600
    val firstRowRequester = rememberTvInitialFocusRequester(model.items.isNotEmpty(), model)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .tvPanelBackHandler(onClose)
            .background(Color(0xCC07101A))
            .padding(if (compact) 14.dp else 36.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(if (compact) 1f else 0.72f)
                .background(Color(0xF0181E28), RoundedCornerShape(if (compact) 18.dp else 24.dp))
                .padding(if (compact) 14.dp else 22.dp)
        ) {
            BasicText(
                text = title,
                style = TextStyle(color = Color.White, fontSize = if (compact) 20.sp else 26.sp, fontWeight = FontWeight.Bold)
            )
            if (subtitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                BasicText(
                    text = subtitle,
                    style = TextStyle(color = Color(0xFFC4D0DF), fontSize = if (compact) 12.sp else 14.sp)
                )
            }
            Spacer(modifier = Modifier.height(if (compact) 12.dp else 16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = if (compact) 330.dp else 420.dp)
            ) {
                PersonalListContent(model, firstRowRequester)
            }
            Spacer(modifier = Modifier.height(if (compact) 12.dp else 16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp, Alignment.End)
            ) {
                PersonalListPanelButton(createLabel, true, Modifier.weight(1f), onCreate)
                PersonalListPanelButton(closeLabel, false, Modifier.weight(1f), onClose)
            }
        }
    }
}

@Composable
private fun PersonalListPanelButton(label: String, primary: Boolean, modifier: Modifier, onClick: Runnable?) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                when {
                    focused -> Color(0xFFFFD47A)
                    primary -> Color(0xFF5FA8FF)
                    else -> Color(0xFF1E2D3E)
                }
            )
            .onFocusChanged { focused = it.isFocused }
            .tvButtonSemantics(onClick != null)
            .clickable(enabled = onClick != null) { onClick?.run() },
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = label,
            style = TextStyle(
                color = if (focused || primary) Color(0xFF07101A) else Color(0xFFDCE7F5),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PersonalListContent(model: PersonalListManagerUiModel, firstRowRequester: FocusRequester? = null) {
    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        itemsIndexed(model.items) { index, item ->
            PersonalListRow(item, if (index == 0) firstRowRequester else null)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PersonalListRow(item: PersonalListRowUiModel, focusRequester: FocusRequester?) {
    val compact = LocalConfiguration.current.screenWidthDp < 600
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = if (compact) 6.dp else 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (focused) Color(0xFFFFD47A) else Color(0xFF1C2733))
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .tvButtonSemantics(item.onClick != null)
            .combinedClickable(
                enabled = item.onClick != null,
                onClick = { item.onClick?.run() },
                onLongClick = item.onLongClick?.let { { it.run() } }
            )
            .padding(if (compact) 10.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(if (compact) 40.dp else 46.dp)
                .background(if (focused) Color(0xFFFFFFFF) else Color(0xFF233647), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                text = item.badge,
                style = TextStyle(
                    color = if (focused) Color(0xFF172131) else Color(0xFFDDEAF7),
                    fontSize = if (compact) 11.sp else 12.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        Spacer(modifier = Modifier.width(if (compact) 10.dp else 12.dp))
        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                text = item.title,
                style = TextStyle(color = if (focused) Color(0xFF101722) else Color.White, fontSize = if (compact) 15.sp else 18.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (item.preview.isNotEmpty()) {
                BasicText(
                    text = item.preview,
                    modifier = Modifier.padding(top = if (compact) 4.dp else 6.dp),
                    style = TextStyle(color = if (focused) Color(0xFF26384B) else Color(0xFFC4D0DF), fontSize = if (compact) 11.sp else 13.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (item.actionLabel.isNotEmpty()) {
            Spacer(modifier = Modifier.width(if (compact) 8.dp else 10.dp))
            Box(
                modifier = Modifier
                    .background(if (focused) Color(0xFFFFFFFF) else Color(0xFF1E2D3E), RoundedCornerShape(12.dp))
                    .padding(horizontal = if (compact) 8.dp else 10.dp, vertical = if (compact) 5.dp else 6.dp),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    text = item.actionLabel,
                    style = TextStyle(color = if (focused) Color(0xFF172131) else Color(0xFFDCE7F5), fontSize = if (compact) 10.sp else 11.sp, fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
