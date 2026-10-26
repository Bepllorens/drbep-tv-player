package com.drbep.tvplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal object MainRailSettingsComposeBinder {
    @JvmStatic
    fun bind(view: ComposeView, layout: MainRailLayout, move: java.util.function.BiConsumer<String,Int>,
             visibility: java.util.function.BiConsumer<String,Boolean>, reset: Runnable, close: Runnable) {
        view.setStableContent("main-rail-settings", layout) { current ->
            val compact = LocalConfiguration.current.screenWidthDp < 600
            Box(Modifier.fillMaxSize().tvPanelBackHandler(close).background(OfflineTvTheme.Menu.scrim)) {
                Column((if(compact)Modifier.fillMaxWidth() else Modifier.width((600f * OfflineTvTheme.menuWidthScale()).dp))
                    .fillMaxHeight().background(CachedMenuFade.horizontal(
                        0f to Color(0xFA0B141E), .85f to Color(0xE60B141E), 1f to Color.Transparent)).padding(24.dp)) {
                    BasicText("Menú lateral",style=TextStyle(Color.White,26.sp,FontWeight.Bold))
                    Spacer(Modifier.height(8.dp))
                    BasicText("Mueve las secciones u oculta las que no uses. Inicio y Ajustes siempre estarán disponibles.",
                        style=TextStyle(OfflineTvTheme.Colors.textSoft,13.sp))
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        RailSettingAction("Volver",true,true,Modifier.weight(1f)){close.run()}
                        RailSettingAction("Restaurar",true,false,Modifier.weight(1f)){reset.run()}
                    }
                    LazyColumn(Modifier.weight(1f).padding(top=12.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                        itemsIndexed(current.order,key={_,id->id}) { index,id ->
                            Column(Modifier.fillMaxWidth().padding(vertical=4.dp)) {
                                BasicText(MainRailLayout.label(id),maxLines=1,overflow=TextOverflow.Ellipsis,
                                    style=TextStyle(Color.White,15.sp,FontWeight.Medium))
                                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp),
                                    verticalAlignment=Alignment.CenterVertically) {
                                    RailSettingAction("Subir",index>0,false,Modifier.weight(1f),"Subir ${MainRailLayout.label(id)}"){move.accept(id,-1)}
                                    RailSettingAction("Bajar",index<current.order.lastIndex,false,Modifier.weight(1f),"Bajar ${MainRailLayout.label(id)}"){move.accept(id,1)}
                                    if(id==MainRailLayout.HOME) BasicText("Siempre visible",Modifier.weight(1.3f),
                                        style=TextStyle(OfflineTvTheme.Colors.textSoft,12.sp))
                                    else RailSettingAction(if(current.visible(id))"Visible" else "Oculta",true,false,Modifier.weight(1.3f),
                                        (if(current.visible(id))"Ocultar " else "Mostrar ")+MainRailLayout.label(id)){
                                        visibility.accept(id,!current.visible(id))
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    BasicText("Los cambios se guardan al momento. Ajustes y Salir permanecen al pie del lateral.",
                        style=TextStyle(OfflineTvTheme.Colors.textMuted,11.sp))
                }
            }
        }
    }
}

@Composable
private fun RailSettingAction(label:String,enabled:Boolean,initial:Boolean,modifier:Modifier,semanticLabel:String=label,action:()->Unit) {
    var focused by remember {mutableStateOf(false)}
    val focus=rememberTvInitialFocusRequester(initial)
    BasicText(label,maxLines=1,overflow=TextOverflow.Ellipsis,
        style=TextStyle(if(focused)OfflineTvTheme.Colors.focusInk else if(!enabled)OfflineTvTheme.Colors.textMuted else Color.White,13.sp),
        modifier=modifier.heightIn(min=48.dp)
            .then(if(initial)Modifier.focusRequester(focus) else Modifier)
            .onFocusChanged{focused=it.isFocused}
            .background(if(focused)Color.White else Color.Transparent,OfflineTvTheme.Menu.shape)
            .tvButtonSemantics(true,if(enabled)semanticLabel else "$semanticLabel · límite de la lista")
            .clickable { if(enabled)action() }.padding(horizontal=10.dp,vertical=14.dp))
}
