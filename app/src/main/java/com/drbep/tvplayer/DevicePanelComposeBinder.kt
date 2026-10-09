package com.drbep.tvplayer

import android.graphics.Bitmap
import android.view.KeyEvent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object DevicePanelComposeBinder {
    @JvmStatic fun bind(view: ComposeView, address: String, pin: String, onBack: Runnable, onStop: Runnable) {
        view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        view.setContent {
            val qr = remember(address) {
                runCatching {
                    val matrix = DevicePanelQr.encode(address)
                    val pixels = IntArray(matrix.width * matrix.height) { i -> if (matrix[i % matrix.width, i / matrix.width]) android.graphics.Color.BLACK else android.graphics.Color.WHITE }
                    Bitmap.createBitmap(pixels, matrix.width, matrix.height, Bitmap.Config.ARGB_8888).asImageBitmap()
                }.getOrNull()
            }
            val initial = remember { FocusRequester() }
            LaunchedEffect(Unit) { withFrameNanos { }; initial.requestFocus() }
            Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xF5101D29),Color(0xDE101D29),Color(0x55101D29))))
                .onPreviewKeyEvent { e -> if(e.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_BACK) { if(e.nativeKeyEvent.action == KeyEvent.ACTION_UP) onBack.run(); true } else false }
                .padding(36.dp), contentAlignment=Alignment.CenterStart) {
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(30.dp)) {
                    if(qr != null) Image(qr,"Escanea para abrir el panel de DRBEP",Modifier.size(210.dp))
                    Column(Modifier.widthIn(max=390.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                        BasicText("Conecta con tu dispositivo",style=TextStyle(Color.White,26.sp,FontWeight.Bold))
                        BasicText("Escanea el QR con el móvil y escribe este PIN:",style=TextStyle(Color.White,16.sp))
                        BasicText(pin,style=TextStyle(Color(0xFF69DDEA),34.sp,FontWeight.Bold))
                        BasicText(address,style=TextStyle(Color.White,15.sp))
                        BasicText("Misma red local · sesión de 20 minutos\nPuedes volver a la televisión mientras utilizas el panel.",style=TextStyle(Color(0xFFB5C6D2),13.sp))
                        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                            PanelButton("Volver",Modifier.focusRequester(initial),onBack)
                            PanelButton("Cerrar panel web",Modifier,onStop)
                        }
                    }
                }
            }
        }
    }
}
@Composable private fun PanelButton(label: String, modifier: Modifier, action: Runnable) {
    var focused by remember { mutableStateOf(false) }
    BasicText(label,modifier.onFocusChanged { focused=it.isFocused }.background(if(focused)Color.White else Color(0x332E6672)).clickable { action.run() }.padding(14.dp),
        style=TextStyle(if(focused)Color(0xFF10202A) else Color.White,15.sp,FontWeight.Bold))
}
