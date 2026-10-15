package com.drbep.tvplayer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
object LiveChannelGuideComposeBinder {
 @JvmStatic fun bind(view: ComposeView, model: LiveChannelGuideUiModel) {
  view.setStableContent("live-channel-guide",model) { m ->
   Column(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xEE101D29),Color(0xBF101D29),Color(0x66101D29)))).padding(18.dp)) {
    BasicText(m.channel,style=TextStyle(Color.White,20.sp,FontWeight.Bold),maxLines=2,overflow=TextOverflow.Ellipsis)
    BasicText("↑ ↓ Cambiar canal · → o Atrás Cerrar guía",Modifier.padding(vertical=10.dp),style=TextStyle(Color(0xFFACBECF),10.sp))
    if(m.message.isNotBlank()) BasicText(m.message,style=TextStyle(Color(0xFFACBECF),13.sp))
    LazyColumn(verticalArrangement=Arrangement.spacedBy(14.dp)) {
     items(m.rows) { row ->
      Column {
       if(row.day.isNotBlank())BasicText(row.day,Modifier.padding(bottom=8.dp),style=TextStyle(Color(0xFF78D6EB),13.sp,FontWeight.Bold))
       BasicText((if(row.current) "● En emisión · " else "")+row.time,style=TextStyle(if(row.current) ContentStatusColors.live else ContentStatusColors.upcoming,11.sp))
       BasicText(row.title,Modifier.padding(top=3.dp),style=TextStyle(Color.White,15.sp,FontWeight.SemiBold),maxLines=3,overflow=TextOverflow.Ellipsis)
       if(row.current) {
        ProgrammeMetadataStrip(row.programmeMetadata,true)
        Box(Modifier.fillMaxWidth().padding(vertical=8.dp).height(2.dp).background(Color(0x444F6678))) { Box(Modifier.fillMaxWidth(row.progress.coerceIn(0f,1f)).fillMaxHeight().background(ContentStatusColors.live)) }
        if(row.description.isNotBlank())BasicText(row.description,style=TextStyle(Color(0xFFC7D2DC),12.sp),maxLines=5,overflow=TextOverflow.Ellipsis)
       }
      }
     }
    }
   }
  }
 }
}
