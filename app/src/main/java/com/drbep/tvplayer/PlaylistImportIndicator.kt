package com.drbep.tvplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object PlaylistImportIndicator {
    private val text = mutableStateOf("")
    @JvmStatic fun update(value: String) { text.value = value }
    @JvmStatic fun bind(view: ComposeView) { view.setContent { Banner() } }
    @Composable fun Surface(content: @Composable () -> Unit) {
        Box(Modifier.fillMaxSize()) {
            content()
            Box(Modifier.align(Alignment.BottomEnd).padding(12.dp).widthIn(max=420.dp)) { Banner() }
        }
    }
    @Composable fun Banner() {
        if (text.value.isNotEmpty()) BasicText(text.value,
            Modifier.background(Brush.horizontalGradient(listOf(Color(0xEF132633), Color(0xB5132633)))).padding(12.dp),
            style=TextStyle(Color(0xFFBCEAF4),12.sp),maxLines=2,overflow=TextOverflow.Ellipsis)
    }
}
