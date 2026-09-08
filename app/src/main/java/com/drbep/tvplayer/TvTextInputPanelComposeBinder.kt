package com.drbep.tvplayer

import android.graphics.Color as AndroidColor
import android.text.InputType
import android.view.ViewGroup
import android.widget.EditText
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.android.awaitFrame

object TvTextInputPanelComposeBinder {
    @JvmStatic
    fun bind(composeView: ComposeView?, model: TvTextInputPanelUiModel) {
        if (composeView == null) return
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        composeView.setContent {
            TvTextInputPanel(model)
        }
    }
}

@Composable
private fun TvTextInputPanel(model: TvTextInputPanelUiModel) {
    val compact = LocalConfiguration.current.screenWidthDp < 600
    val editTexts = remember(model) { mutableListOf<EditText>() }
    val submitRequester = remember { FocusRequester() }
    LaunchedEffect(model) {
        awaitFrame()
        editTexts.firstOrNull()?.requestFocus()
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(OfflineTvTheme.Colors.overlayScrim),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(if (compact) 0.94f else 0.48f)
                .clip(RoundedCornerShape(if (compact) 22.dp else 28.dp))
                .background(Brush.verticalGradient(listOf(OfflineTvTheme.Colors.chip.copy(alpha = 0.95f), OfflineTvTheme.Colors.backdrop.copy(alpha = 0.98f))))
                .padding(if (compact) 16.dp else 22.dp)
        ) {
            BasicText(
                text = model.title,
                style = TextStyle(color = Color.White, fontSize = if (compact) 21.sp else 26.sp, fontWeight = FontWeight.Black),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (model.message.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                BasicText(
                    text = model.message,
                    style = TextStyle(color = OfflineTvTheme.Colors.textSoft, fontSize = if (compact) 12.sp else 14.sp),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(if (compact) 14.dp else 18.dp))
            editTexts.clear()
            (model.fields ?: emptyList()).forEach { field ->
                TvAndroidTextField(field, editTexts, compact)
                Spacer(modifier = Modifier.height(if (compact) 10.dp else 12.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp)
            ) {
                if (model.negativeLabel.isNotBlank()) {
                    TvTextInputActionButton(model.negativeLabel, false, Modifier.weight(1f), null) {
                        model.onCancel?.run()
                    }
                }
                if (model.neutralLabel.isNotBlank()) {
                    TvTextInputActionButton(model.neutralLabel, true, Modifier.weight(1f), null) {
                        model.onNeutral?.run()
                    }
                }
                TvTextInputActionButton(model.positiveLabel.ifBlank { "OK" }, false, Modifier.weight(1f), submitRequester) {
                    model.onSubmit?.submit(editTexts.map { it.text?.toString() ?: "" })
                }
            }
        }
    }
}

@Composable
private fun TvAndroidTextField(field: TvTextInputFieldUiModel, editTexts: MutableList<EditText>, compact: Boolean) {
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (compact) 48.dp else 54.dp),
        factory = { context ->
            EditText(context).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                setSingleLine(true)
                hint = field.hint
                setText(field.initialValue)
                setSelectAllOnFocus(true)
                textSize = if (compact) 15f else 17f
                setTextColor(AndroidColor.WHITE)
                setHintTextColor(0xFF8FA8C2.toInt())
                setPadding(18, 0, 18, 0)
                inputType = when {
                    field.password && field.numeric -> InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
                    field.password -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                    field.numeric -> InputType.TYPE_CLASS_NUMBER
                    else -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                }
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = 14f
                    setColor(OfflineTvTheme.cardArgb())
                    setStroke(2, OfflineTvTheme.focusArgb())
                }
            }
        },
        update = { editText ->
            if (!editTexts.contains(editText)) {
                editTexts.add(editText)
            }
        }
    )
}

@Composable
private fun TvTextInputActionButton(label: String, destructive: Boolean, modifier: Modifier, requester: FocusRequester?, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (focused) OfflineTvTheme.Colors.focus else if (destructive) OfflineTvTheme.Colors.destructive else OfflineTvTheme.Colors.chip)
            .then(if (requester != null) Modifier.focusRequester(requester) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .tvButtonSemantics(true)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = label,
            style = TextStyle(color = if (focused) OfflineTvTheme.Colors.backdropAccent else Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
