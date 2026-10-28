package com.drbep.tvplayer
import android.app.Activity
import android.app.Dialog
import android.net.Uri
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import kotlinx.coroutines.*
import org.json.JSONObject
import java.util.function.Consumer

@JvmName("alfaPlaybackItem") @JvmOverloads internal fun alfaPlaybackItem(data:JSONObject,liveKey:String="",renewed:Boolean=false):ChannelItem{
 val uri=runCatching{java.net.URI(data.optString("play_url"))}.getOrElse{throw IllegalArgumentException("Enlace de reproducción no válido.")}
 val domains=listOf("eporner.com","epornercdn.com","xvideos.com","xvideos-cdn.com","xvideos-cdn.net","xnxx.com","xnxx-cdn.com","xnxx-cdn.net","nubilefilm.xxx","pornstar4k.com","beeg.com","video.externulls.com","chaturbate.com","mmcdn.com","highwebmedia.com")
 require(uri.scheme=="https"&&uri.userInfo==null&&(uri.port==-1||uri.port==443)&&domains.any{uri.host==it||uri.host?.endsWith("."+it)==true}){"Enlace de reproducción no válido."}
 val id=data.optString("id")
 require(Regex("alfa-adult:[0-9a-f]{32}").matches(id)){"Título no válido."}
 val item=ChannelItem(id,data.optString("title"),"","","Alfa Adultos",uri.toString(),"",0,0,true,true,0,"Alfa Adultos",
  listOf("Alfa Adultos"),"","","vod:alfa-adult",true,"","",0L)
 val input=data.optJSONObject("headers")?:JSONObject()
 item.playlistMimeType=data.optString("mime").takeIf{it in listOf("video/mp4","application/x-mpegURL")}.orEmpty()
 require(!data.optBoolean("is_live")||item.playlistMimeType=="application/x-mpegURL"){"La emisión en directo requiere una fuente HLS válida."}
 val supplied=HashMap<String,String>()
 input.keys().forEach{key->supplied[key]=input.optString(key)}
 item.playlistStreamHeaders=AlfaAdultPrivacy.mediaHeaders(supplied)
 item.alfaChaturbateLive=data.optBoolean("is_live")&&Regex("[0-9a-f]{32}").matches(liveKey)
 if(item.alfaChaturbateLive)item.alfaLiveKey=liveKey
 item.alfaLiveRenewed=renewed
 return item
}
internal object AlfaAdultBrowser{
 interface Host{
  fun play(item:ChannelItem,dialog:Dialog?)
  fun back(dialog:Dialog?)
  fun closed()
  fun input(value:String,submit:Consumer<String>)
  fun pin(submit:Consumer<String>)
 }
 @JvmStatic fun open(activity:Activity,view:ComposeView,base:String,headers:Map<String,String>,host:Host){
  view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
  var dialog:Dialog?=null
  var back=Runnable{host.back(dialog)}
  val api=PalantirHttp(base,headers)
  view.setContent{AlfaLibrary(api,host,{host.back(dialog)},{back=it}){host.play(it,dialog)}}
  dialog=ComposeDialogHost.showFullscreen(activity,view,{back.run()},{host.closed()})
  dialog?.setOnKeyListener{_,code,event->if(code==android.view.KeyEvent.KEYCODE_BACK){if(event.action==android.view.KeyEvent.ACTION_UP)back.run();true}else false}
 }
}
@Composable private fun AlfaLibrary(api:PalantirHttp,host:AlfaAdultBrowser.Host,exit:()->Unit,setBack:(Runnable)->Unit,play:(ChannelItem)->Unit){
 var source by remember{mutableStateOf("")}
 var search by remember{mutableStateOf("")}
 var offset by remember{mutableStateOf(0)}
 var revision by remember{mutableStateOf(0)}
 var unlockedUntil by remember{mutableStateOf(0L)}
 var sources by remember{mutableStateOf<List<JSONObject>>(emptyList())}
 var page by remember{mutableStateOf<JSONObject?>(null)}
 var detail by remember{mutableStateOf<JSONObject?>(null)}
 var loading by remember{mutableStateOf(true)}
 var resolving by remember{mutableStateOf(false)}
 var error by remember{mutableStateOf("")}
 var focused by remember{mutableStateOf("")}
 var playJob by remember{mutableStateOf<Job?>(null)}
 val scope=rememberCoroutineScope()
 val fallbackFocus=remember{FocusRequester()}
 val itemFocus=remember{FocusRequester()}
 val grid=rememberLazyGridState()
 fun cancel(){playJob?.cancel();resolving=false}
 SideEffect{setBack(Runnable{when{resolving->cancel();detail!=null->detail=null;else->exit()}})}
 LaunchedEffect(source,search,offset,revision){
  loading=true;error=""
  try{
   if(sources.isEmpty()){
    val response=api.get("/api/vod/alfa-adult/sources")
    unlockedUntil=response.optLong("unlocked_until")
    val value=response.optJSONArray("items")
    sources=if(value==null)emptyList()else(0 until value.length()).map{value.getJSONObject(it)}
   }
   val query=Uri.Builder().appendQueryParameter("source",source).appendQueryParameter("q",search).appendQueryParameter("offset",offset.toString()).build().encodedQuery
   page=api.get("/api/vod/alfa-adult/catalog?"+query)
  }catch(c:CancellationException){throw c}catch(e:Exception){page=null;error=e.message.orEmpty()}
  finally{loading=false}
 }
 LaunchedEffect(unlockedUntil){
  if(unlockedUntil>0L){
   delay((unlockedUntil*1000L-System.currentTimeMillis()).coerceAtLeast(0L))
   page=null;detail=null;sources=emptyList();error="parental PIN required"
  }
 }
 val array=page?.optJSONArray("items")
 val items=if(array==null)emptyList()else(0 until array.length()).map{array.getJSONObject(it)}
 val target=focused.takeIf{key->items.any{it.optString("key")==key}}?:items.firstOrNull()?.optString("key").orEmpty()
 LaunchedEffect(page,loading,detail){
  if(!loading){
   if(detail==null&&items.isNotEmpty()){
    grid.scrollToItem(items.indexOfFirst{it.optString("key")==target}.coerceAtLeast(0))
    withFrameNanos{};withFrameNanos{};runCatching{itemFocus.requestFocus()}
   }else{withFrameNanos{};runCatching{fallbackFocus.requestFocus()}}
  }
 }
 Box(Modifier.fillMaxSize().background(OfflineTvTheme.Menu.background).padding(28.dp)){
  Column(Modifier.fillMaxSize().focusGroup(),verticalArrangement=Arrangement.spacedBy(12.dp)){
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
    BasicText("Alfa Adultos",style=TextStyle(Color.White,25.sp,FontWeight.Bold))
    AlfaAction(if(detail==null)"Volver"else"Volver al catálogo",Modifier.focusRequester(fallbackFocus)){if(detail!=null)detail=null else exit()}
   }
   if(error.isNotBlank()){
    BasicText(if(error.contains("parental PIN"))"Desbloquea el catálogo con el PIN de tu cuenta."else error,style=TextStyle(Color(0xFFFFBE80),14.sp))
    if(error.contains("parental PIN"))AlfaAction("Introducir PIN"){
     host.pin(Consumer{pin->scope.launch{
      try{api.post("/api/webapp/parental/unlock",JSONObject().put("pin",pin));sources=emptyList();page=null;revision++}
      catch(c:CancellationException){throw c}catch(e:Exception){error=e.message.orEmpty()}
     }})
    }
    AlfaAction("Reintentar"){revision++}
   }
   if(detail!=null){
    val item=detail!!
    BasicText(item.optString("title"),maxLines=3,overflow=TextOverflow.Ellipsis,style=TextStyle(Color.White,23.sp,FontWeight.Bold))
    AlfaImage(item.optString("poster"),Modifier.width(320.dp).aspectRatio(16f/9f))
    BasicText(listOf(item.optString("source"),item.optString("duration"),item.optString("quality")).filter{it.isNotBlank()}.joinToString(" · "),style=TextStyle(OfflineTvTheme.Colors.accentCyan,15.sp))
    BasicText(if(item.optBoolean("is_live"))"Emisión en directo. La disponibilidad se comprueba al abrir." else if(item.optString("published_at").isNotBlank())"Publicado: "+item.optString("published_at").take(10) else "Añadido a DRBEP: "+item.optString("added_at").take(10),style=TextStyle(Color(0xFFAABBCC),14.sp))
    AlfaAction(if(resolving)"Preparando…"else if(item.optBoolean("is_live"))"Ver directo"else"Reproducir",enabled=!resolving){
     resolving=true;error=""
     playJob=scope.launch{
      try{
       val result=api.post("/api/vod/alfa-adult/play",JSONObject().put("key",item.optString("key")))
       play(alfaPlaybackItem(result,item.optString("key")))
      }catch(c:CancellationException){throw c}catch(e:Exception){error=e.message.orEmpty()}
      finally{resolving=false}
     }
    }
    if(resolving)AlfaAction("Cancelar preparación"){cancel()}
   }else{
    LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){
     item{AlfaAction("Todas"){source="";offset=0;focused=""}}
     items(sources,key={it.optString("id")}){row->AlfaAction(row.optString("name")){source=row.optString("id");offset=0;focused=""}}
     item{AlfaAction("Buscar"){host.input(search,Consumer{search=it;offset=0;focused=""})}}
     item{AlfaAction("Ver todo"){source="";search="";offset=0;focused=""}}
    }
    BasicText(if(source=="chaturbate")"Salas públicas · Última actualización del catálogo" else "Más recientes · "+if(search.isBlank())"Catálogo almacenado"else search,style=TextStyle(Color(0xFFAABBCC),13.sp))
    if(loading)BasicText("Consultando catálogo…",style=TextStyle(Color.White,15.sp))
    if(!loading&&items.isEmpty())BasicText("Sin resultados. El administrador puede actualizar las fuentes desde el dashboard.",style=TextStyle(Color.White,15.sp))
    LazyVerticalGrid(columns=GridCells.Adaptive(176.dp),state=grid,modifier=Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
     items(items,key={it.optString("key")}){item->
      val key=item.optString("key")
      var focus by remember{mutableStateOf(false)}
      Column(Modifier.then(if(key==target)Modifier.focusRequester(itemFocus)else Modifier)
       .onFocusChanged{focus=it.isFocused;if(focus)focused=key}
       .border(if(focus)2.dp else 0.dp,if(focus)OfflineTvTheme.Colors.accentCyan else Color.Transparent,RoundedCornerShape(9.dp))
       .clickable{detail=item}.padding(4.dp)){
       AlfaImage(item.optString("poster"),Modifier.fillMaxWidth().aspectRatio(16f/9f))
       if(item.optBoolean("is_live"))BasicText("DIRECTO",style=TextStyle(OfflineTvTheme.Colors.accentCyan,11.sp,FontWeight.Bold))
       BasicText(item.optString("title"),maxLines=2,overflow=TextOverflow.Ellipsis,style=TextStyle(Color.White,14.sp,FontWeight.Medium),modifier=Modifier.height(42.dp))
      }
     }
    }
    Row(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically){
     AlfaAction("Anterior",enabled=offset>0&&!loading){offset=(offset-30).coerceAtLeast(0);focused=""}
     BasicText("Página "+(1+offset/30)+" · "+(page?.optInt("total")?:0)+" vídeos",style=TextStyle(Color.White,13.sp))
     AlfaAction("Siguiente",enabled=page?.optBoolean("has_more")==true&&!loading){offset+=30;focused=""}
    }
   }
  }
 }
}
@Composable private fun AlfaAction(label:String,modifier:Modifier=Modifier,enabled:Boolean=true,click:()->Unit){
 var focus by remember{mutableStateOf(false)}
 Box(modifier.onFocusChanged{focus=it.isFocused}.background(if(focus)Color(0xFF304A60)else Color(0x88202D3E),RoundedCornerShape(8.dp))
  .border(if(focus)2.dp else 0.dp,if(focus)OfflineTvTheme.Colors.accentCyan else Color.Transparent,RoundedCornerShape(8.dp))
  .clickable(enabled=enabled,onClick=click).padding(horizontal=16.dp,vertical=11.dp)){
  BasicText(label,style=TextStyle(if(enabled)Color.White else Color.Gray,14.sp))
 }
}
@Composable private fun AlfaImage(url:String,modifier:Modifier){
 if(url.isBlank()){Box(modifier.background(Color(0xFF203246)));return}
 AndroidView(factory={context->AppCompatImageView(context).apply{scaleType=ImageView.ScaleType.CENTER_CROP}},
 modifier=modifier,update={view->Glide.with(view).load(url).diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).into(view)},
 onRelease={view->Glide.with(view).clear(view)})
}
