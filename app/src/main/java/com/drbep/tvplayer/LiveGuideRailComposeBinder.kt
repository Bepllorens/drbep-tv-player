package com.drbep.tvplayer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
object LiveGuideRailComposeBinder {
 @JvmStatic fun bind(view:ComposeView,channel:String,close:Runnable) {
  view.setContent {
   Column(Modifier.fillMaxSize().clickable { close.run() }.padding(vertical=16.dp)) {
    BasicText("‹",Modifier.padding(8.dp),style=TextStyle(Color.White,26.sp,FontWeight.Bold))
    BasicText(channel,Modifier.padding(4.dp),style=TextStyle(Color(0xFF78D6EB),11.sp),maxLines=4)
   }
  }
 }
}
