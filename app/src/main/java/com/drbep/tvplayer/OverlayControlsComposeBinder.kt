package com.drbep.tvplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object OverlayControlsComposeBinder {
    @JvmStatic
    fun bind(composeView: ComposeView?, model: OverlayControlsUiModel) {
        if (composeView == null) return
        composeView.setStableContent("overlay-controls", model) { currentModel ->
            OverlayControlsCard(currentModel)
        }
    }
}

@Composable
private fun OverlayControlsCard(model: OverlayControlsUiModel) {
    val compact = LocalConfiguration.current.screenWidthDp < 600
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OfflineTvTheme.Colors.panelSoft, RoundedCornerShape(OfflineTvTheme.Radius.panel))
            .padding(if (compact) 8.dp else 10.dp)
    ) {
        BasicText(
            text = model.sectionTitle,
            style = TextStyle(color = OfflineTvTheme.Colors.textMuted, fontSize = if (compact) 9.sp else 10.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            model.filterActions.getOrNull(0)?.let {
                OverlayActionChip(it, modifier = Modifier.width(if (compact) 54.dp else 58.dp), centered = true, compact = compact)
                Spacer(modifier = Modifier.width(if (compact) 6.dp else 8.dp))
            }
            OverlayActionChip(
                item = ZapActionItem(model.filterLabel, true, false, false, model.filterActions.getOrNull(1)?.onClick, model.filterActions.getOrNull(1)?.onLongClick),
                modifier = Modifier.weight(1f),
                centered = false,
                compact = compact
            )
            model.filterActions.getOrNull(2)?.let {
                Spacer(modifier = Modifier.width(if (compact) 6.dp else 8.dp))
                OverlayActionChip(it, modifier = Modifier.width(if (compact) 60.dp else 68.dp), centered = true, compact = compact)
            }
            model.filterActions.getOrNull(3)?.let {
                Spacer(modifier = Modifier.width(if (compact) 6.dp else 8.dp))
                OverlayActionChip(it, modifier = Modifier.width(if (compact) 54.dp else 58.dp), centered = true, compact = compact)
            }
        }
        if (model.primaryActions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(if (compact) 5.dp else 6.dp))
            OverlayActionRow(model.primaryActions, compact)
        }
        if (model.secondaryActions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(if (compact) 5.dp else 6.dp))
            OverlayActionRow(model.secondaryActions, compact)
        }
        Spacer(modifier = Modifier.height(if (compact) 6.dp else 7.dp))
        OverlaySearchField(model, compact)
    }
}

@Composable
private fun OverlaySearchField(model: OverlayControlsUiModel, compact: Boolean) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var text by remember(model.searchQuery) { mutableStateOf(model.searchQuery) }

    LaunchedEffect(model.searchFocusRequestToken) {
        if (model.searchFocusRequestToken > 0) {
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }
    LaunchedEffect(model.searchClearFocusRequestToken) {
        if (model.searchClearFocusRequestToken > 0) {
            focusManager.clearFocus()
            keyboard?.hide()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (compact) 38.dp else 42.dp)
            .background(OfflineTvTheme.Colors.card, RoundedCornerShape(OfflineTvTheme.Radius.chip))
            .padding(horizontal = 10.dp, vertical = if (compact) 8.dp else 9.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (text.isBlank()) {
            BasicText(
                text = model.searchHint,
                style = TextStyle(color = OfflineTvTheme.Colors.textMuted, fontSize = if (compact) 12.sp else 13.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        BasicTextField(
            value = text,
            onValueChange = {
                text = it
                model.onSearchQueryChange?.onSearchQueryChanged(it)
            },
            singleLine = true,
            textStyle = TextStyle(color = Color.White, fontSize = if (compact) 12.sp else 13.sp, fontWeight = FontWeight.Bold),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                focusManager.clearFocus()
                keyboard?.hide()
            }),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged {
                    if (it.isFocused) {
                        model.onSearchFocused?.run()
                    }
                }
        )
    }
}

@Composable
private fun OverlayActionRow(items: List<ZapActionItem>, compact: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp)
    ) {
        items.forEach { item ->
            OverlayActionChip(
                item = item,
                modifier = Modifier.weight(1f),
                centered = true,
                compact = compact
            )
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun OverlayActionChip(item: ZapActionItem, modifier: Modifier, centered: Boolean, compact: Boolean) {
    TvActionChip(
        label = item.label,
        enabled = item.enabled,
        selected = item.selected,
        highlighted = item.highlighted,
        modifier = modifier,
        centered = centered,
        compact = compact,
        onClick = item.onClick,
        onLongClick = item.onLongClick
    )
}
