package com.drbep.tvplayer

import android.app.Activity
import android.app.Dialog
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.function.Consumer

internal object SportsAgendaBrowser{
 interface Host{
  fun valid():Boolean
  fun allowed(id:String):Boolean
  fun channelLabel(id:String):String
  fun play(id:String,dialog:Dialog?)
  fun back(dialog:Dialog?)
  fun closed()
  fun input(value:String,submit:Consumer<String>)
 }
 @JvmStatic fun open(activity:Activity,view:ComposeView,base:String,headers:Map<String,String>,account:String,host:Host){
  view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
  var dialog:Dialog?=null
  view.setContent{SportsAgendaScreen(activity,base,headers,account,host,{host.back(dialog)},{host.play(it,dialog)})}
  dialog=ComposeDialogHost.showFullscreen(activity,view,{host.back(dialog)},{host.closed()})
 }
}
private suspend fun sportsRequest(base:String,headers:Map<String,String>,day:LocalDate,zone:ZoneId,q:String,offset:Int):JSONObject=coroutineScope{
 val (start,end)=SportsAgendaModel.window(day,zone)
 val query=Uri.Builder().appendQueryParameter("start",start.toString()).appendQueryParameter("end",end.toString()).appendQueryParameter("q",q).appendQueryParameter("offset",offset.toString()).build().encodedQuery
 val c=URL(base.trimEnd('/')+"/api/epg/sports?"+query).openConnection() as HttpURLConnection
 val cancellation=launch(start=CoroutineStart.UNDISPATCHED){try{awaitCancellation()}finally{c.disconnect()}}
 try{withContext(Dispatchers.IO){
  c.connectTimeout=8000;c.readTimeout=15000;c.instanceFollowRedirects=false
  headers.forEach{(k,v)->c.setRequestProperty(k,v)}
  val status=c.responseCode
  check(status==200){if(status==401||status==403)"No tienes acceso a la guía. Revisa los permisos." else "No se pudo cargar la agenda. Pulsa Actualizar para reintentar."}
  val bytes=c.inputStream.use{input->val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192)
   while(true){val n=input.read(buffer);if(n<0)break;check(out.size()+n<=2*1024*1024){"Respuesta demasiado grande"};out.write(buffer,0,n)};out.toByteArray()}
  JSONObject(String(bytes,Charsets.UTF_8))
 }}finally{cancellation.cancel();c.disconnect()}
}
@Composable private fun SportsAction(label:String,modifier:Modifier=Modifier,selected:Boolean=false,enabled:Boolean=true,action:()->Unit){
 var focus by remember{mutableStateOf(false)}
 BasicText(label,modifier.onFocusChanged{focus=it.isFocused}
  .background(if(focus)Color(0xCC263542)else Color.Transparent)
  .border(1.dp,if(focus)Color.White else if(selected)OfflineTvTheme.Colors.accentCyan else Color.Transparent)
  .tvButtonSemantics(enabled,label,selected).clickable(enabled=enabled,onClick=action).padding(horizontal=14.dp,vertical=12.dp),
  style=TextStyle(color=if(enabled)Color.White else Color.Gray,fontSize=14.sp,fontWeight=if(selected)FontWeight.Bold else FontWeight.Normal),maxLines=1)
}
@Composable private fun SportsAgendaScreen(activity:Activity,base:String,headers:Map<String,String>,account:String,host:SportsAgendaBrowser.Host,back:()->Unit,play:(String)->Unit){
 val zone=remember{ZoneId.systemDefault()}
 val initialClock=remember{Instant.now()}
 val now=rememberVisibleProgramme("sports-clock",initialClock,{Instant.now()}){a,b->a.epochSecond/15==b.epochSecond/15}
 val today=now.atZone(zone).toLocalDate()
 var day by remember{mutableStateOf(today)};var query by remember{mutableStateOf("")};var offset by remember{mutableStateOf(0)}
 var generation by remember{mutableStateOf(0)};var retry by remember{mutableStateOf(0)};var loading by remember{mutableStateOf(true)};var error by remember{mutableStateOf("")}
 var rows by remember{mutableStateOf<List<JSONObject>>(emptyList())};var more by remember{mutableStateOf(false)}
 val prefs=remember{activity.getSharedPreferences("sports_agenda",0)};val prefKey=remember(account){SportsAgendaModel.preferenceKey(account)}
 var favorites by remember(account){mutableStateOf(SportsAgendaModel.decode(prefs.getString(prefKey,"[]")?:"[]"))}
 var saving by remember{mutableStateOf(false)}
 val scope=rememberCoroutineScope();val backFocus=remember{FocusRequester()};val favoriteFocus=remember{FocusRequester()};val list=rememberLazyListState()
 LaunchedEffect(Unit){backFocus.requestFocus()}
 LaunchedEffect(today){if(day.isBefore(today)){day=today;offset=0}}
 LaunchedEffect(day,query,offset,retry){
  val request=++generation
  loading=true;error="";rows=emptyList();more=false
  try{
   check(host.valid()){"La cuenta o los permisos han cambiado. Vuelve a abrir la agenda."}
   val page=sportsRequest(base,headers,day,zone,query,offset)
   ensureActive()
   check(host.valid()){"La cuenta o los permisos han cambiado. Vuelve a abrir la agenda."}
   val a=page.optJSONArray("items")
   rows=if(a==null)emptyList()else (0 until minOf(a.length(),60)).mapNotNull{a.optJSONObject(it)}.filter{host.allowed(it.optString("channel_id"))}
   more=page.optBoolean("has_more");list.scrollToItem(0)
  }catch(e:CancellationException){throw e}catch(e:Exception){error=e.message?:"No se pudo cargar la agenda"}finally{if(request==generation)loading=false}
 }
 val clock=remember{DateTimeFormatter.ofPattern("HH:mm",Locale("es")).withZone(zone)}
 fun time(raw:String)=runCatching{clock.format(SportsAgendaModel.timestamp(raw))}.getOrDefault("")
 Column(Modifier.fillMaxSize().background(CachedMenuFade.horizontal(0f to Color(0xF009121C),0.8f to Color(0xED09121C),1f to Color(0xB009121C))).padding(24.dp)){
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)){
   SportsAction("Volver",Modifier.focusRequester(backFocus),action=back)
   BasicText("Agenda deportiva",Modifier.weight(1f).padding(12.dp),style=TextStyle(color=Color.White,fontSize=25.sp,fontWeight=FontWeight.Bold))
   SportsAction("Actualizar"){retry++}
  }
  Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){
   (0..6).forEach{n->val d=today.plusDays(n.toLong());SportsAction(if(n==0)"Hoy"else if(n==1)"Mañana"else d.format(DateTimeFormatter.ofPattern("EEE d",Locale("es"))),selected=day==d){day=d;offset=0}}
  }
  Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){
   SportsAction(if(query.isBlank())"Buscar equipo o competición"else query){host.input(query,Consumer{query=SportsAgendaModel.normalize(it);offset=0})}
   SportsAction("Todos",selected=query.isBlank()){query="";offset=0}
   SportsAction(if(favorites.any{it.equals(query,true)})"★ Dejar de seguir"else "☆ Seguir búsqueda",Modifier.focusRequester(favoriteFocus),enabled=query.isNotBlank()&&(favorites.size<32||favorites.any{it.equals(query,true)})){
    if(!saving){
    val next=SportsAgendaModel.toggle(favorites,query);saving=true
    scope.launch{try{
     check(host.valid());val ok=withContext(Dispatchers.IO){prefs.edit().putString(prefKey,SportsAgendaModel.encode(next)).commit()}
     if(ok)favorites=next else error="No se pudo guardar el favorito"
    }catch(e:CancellationException){throw e}catch(e:Exception){error="No se pudo guardar el favorito"}finally{saving=false}
     withFrameNanos{};withFrameNanos{};runCatching{favoriteFocus.requestFocus()}
    }
    }
   }
  }
  if(favorites.isNotEmpty())Row(Modifier.horizontalScroll(rememberScrollState())){favorites.forEach{term->key(term){SportsAction("★ "+term,selected=query.equals(term,true)){query=term;offset=0}}}}
  BasicText(if(loading)"Cargando agenda…"else if(error.isNotEmpty())error else if(rows.isEmpty())"No hay emisiones disponibles para este día y filtro."else "EPG disponible · "+rows.size+" emisiones en esta página",
   Modifier.padding(vertical=10.dp),style=TextStyle(color=OfflineTvTheme.Colors.accentCyan,fontSize=13.sp))
  LazyColumn(Modifier.weight(1f).fillMaxWidth(),state=list,verticalArrangement=Arrangement.spacedBy(2.dp)){
   items(rows,key={it.optString("id")+"|"+it.optString("channel_id")}){event->
    val start=SportsAgendaModel.timestamp(event.optString("start_time"));val end=SportsAgendaModel.timestamp(event.optString("end_time"))
    val live=start!=null&&end!=null&&!now.isBefore(start)&&now.isBefore(end)
    val ended=end!=null&&!now.isBefore(end)
    Column(Modifier.fillMaxWidth()){
     SportsAction(time(event.optString("start_time"))+"–"+time(event.optString("end_time"))+"  ·  "+event.optString("title"),Modifier.fillMaxWidth(),enabled=!loading){
      val liveNow=start!=null&&end!=null&&!Instant.now().isBefore(start)&&Instant.now().isBefore(end)
      if(host.valid()&&host.allowed(event.optString("channel_id"))&&liveNow)play(event.optString("channel_id"))
      else error=if(live)"El canal ya no está disponible."else if(end!=null&&!Instant.now().isBefore(end))"Esta emisión ya ha terminado."else "Emisión programada; podrás abrir el canal cuando esté en directo."
     }
     BasicText(host.channelLabel(event.optString("channel_id")).ifBlank{event.optString("channel_name")}+" · "+event.optString("category")+(if(live)" · EN DIRECTO"else if(ended)" · Finalizado"else " · Próximamente"),Modifier.padding(start=14.dp,bottom=8.dp),
      style=TextStyle(color=if(live)Color(0xFFFFB45B)else Color(0xFFB7C4D1),fontSize=12.sp),maxLines=1,overflow=TextOverflow.Ellipsis)
    }
   }
  }
  Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){
   SportsAction("Anterior",enabled=offset>0&&!loading){backFocus.requestFocus();offset=maxOf(0,offset-60)}
   BasicText("Página "+(offset/60+1),Modifier.padding(12.dp),style=TextStyle(color=Color.White,fontSize=13.sp))
   SportsAction("Siguiente",enabled=more&&!loading){backFocus.requestFocus();offset+=60}
  }
 }
}
