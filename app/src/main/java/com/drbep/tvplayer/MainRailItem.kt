package com.drbep.tvplayer

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

/** Fixed hit target; focus decoration never remeasures the content beside the rail. */
@Composable
internal fun MainRailItem(label: String, @DrawableRes icon: Int, selected: Boolean,
                          modifier: Modifier = Modifier, action: () -> Unit) {
    val focused = remember { mutableStateOf(false) }
    Box(modifier.fillMaxWidth().height(48.dp)
        .onFocusChanged { focused.value = it.isFocused }
        .tvButtonSemantics(true, label, selected)
        .clickable(onClick = action), contentAlignment = Alignment.Center) {
        MainRailIcon(icon, selected, focused)
        MainRailFocusLabel(label, focused)
    }
}

/** Focus is read only by the drawing layer, leaving the icon composition intact. */
@Composable
private fun MainRailIcon(@DrawableRes icon: Int, selected: Boolean, focused: State<Boolean>) {
        Box(Modifier.size(40.dp).graphicsLayer {
                val factor = if (focused.value) 1.06f else 1f
                scaleX = factor
                scaleY = factor
            }.drawWithContent {
                if (selected || focused.value) drawCircle(Color(0xCC263542))
                drawContent()
                if (focused.value) {
                    val width = 1.5.dp.toPx()
                    drawCircle(Color(0xFFF1F5F9), radius = (size.minDimension - width) / 2f,
                        style = Stroke(width))
                }
            }, contentAlignment = Alignment.Center) {
            Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(23.dp))
            if (selected) Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp)
                .width(10.dp).height(3.dp).background(OfflineTvTheme.Colors.accentCyan, CircleShape))
        }
}

@Composable
private fun MainRailFocusLabel(label: String, focused: State<Boolean>) {
        if (focused.value) {
            val offset = with(LocalDensity.current) { 62.dp.roundToPx() }
            // Non-focusable overlay: labels do not push the preview or steal D-pad/Back.
            Popup(alignment = Alignment.CenterStart, offset = IntOffset(offset, 0),
                properties = PopupProperties(focusable = false)) {
                BasicText(label,
                    modifier = Modifier.background(Color(0xF01A2836), RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    style = TextStyle(color = Color.White, fontSize = 13.sp,
                        fontWeight = FontWeight.Medium),
                    maxLines = 1)
            }
        }
}
