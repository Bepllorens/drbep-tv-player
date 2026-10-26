package com.drbep.tvplayer

import android.app.Activity
import android.app.Dialog
import android.net.Uri
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.function.Consumer

internal data class PalantirFilter(val kind:String="",val category:String="",val genre:String="",val collection:String="",val search:String="",val sort:String="newest")
internal class PalantirHttp(private val base:String,private val headers:Map<String,String>){
    suspend fun get(path:String)=request("GET",path,null)
    suspend fun post(path:String,body:JSONObject)=request("POST",path,body)
    private suspend fun request(method:String,path:String,body:JSONObject?):JSONObject=coroutineScope{
        val connection=URL(base.trimEnd('/')+path).openConnection() as HttpURLConnection
        val cancellation=launch(start=CoroutineStart.UNDISPATCHED){try{awaitCancellation()}finally{connection.disconnect()}}
        try{withContext(Dispatchers.IO){
            connection.connectTimeout=8000;connection.readTimeout=if(method=="POST")180000 else 20000;connection.instanceFollowRedirects=false
            connection.requestMethod=method
            headers.forEach{(key,value)->connection.setRequestProperty(key,value)}
            if(body!=null){
                connection.doOutput=true;connection.setRequestProperty("Content-Type","application/json")
                connection.outputStream.use{it.write(body.toString().toByteArray(Charsets.UTF_8))}
            }
            val code=connection.responseCode
            if(code!=200){
                val error=runCatching{connection.errorStream?.use{JSONObject(String(it.readBytesBounded(16384),Charsets.UTF_8)).optString("error")}}.getOrNull()
                throw IllegalStateException(error?.takeIf{it.isNotBlank()} ?: "No se pudo consultar Palantir. Revisa el acceso y vuelve a intentarlo.")
            }
            val bytes=connection.inputStream.use{it.readBytesBounded(2*1024*1024)}
            JSONObject(String(bytes,Charsets.UTF_8))
        }}finally{cancellation.cancel();connection.disconnect()}
    }
    fun playbackItem(data:JSONObject):ChannelItem{
        val path=data.optString("play_url")
        require(Regex("/api/vod/palantir/stream/[A-Za-z0-9_-]{32}").matches(path)){"Sesión de reproducción no válida."}
        val id=data.optString("id");require(id.startsWith("palantir:")){"Título no válido."}
        val item=ChannelItem(id,data.optString("title"),"",data.optString("poster"),"Palantir",base.trimEnd('/')+path,"",0,0,true,false,0,"Palantir",
            listOf("Palantir"),"","","vod:palantir",true,data.optString("plot"),data.optString("year"),0L)
        item.playlistStreamHeaders=HashMap(headers)
        if(data.optString("kind")=="series"){item.vodSeriesTitle=data.optString("title");item.vodSeason=data.optInt("season");item.vodEpisode=data.optInt("episode")}
        return item
    }
}

private fun java.io.InputStream.readBytesBounded(limit:Int):ByteArray{
    val output=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192)
    while(true){val n=read(buffer);if(n<0)break;if(output.size()+n>limit)throw IllegalStateException("Respuesta del catálogo demasiado grande.");output.write(buffer,0,n)}
    return output.toByteArray()
}
internal object PalantirBrowser{
    interface Host{fun play(item:ChannelItem,dialog:Dialog?);fun back(dialog:Dialog?);fun closed();fun input(value:String,submit:Consumer<String>)}
    @JvmStatic fun open(activity:Activity,view:ComposeView,base:String,headers:Map<String,String>,host:Host){
        view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        var dialog:Dialog?=null
        var onBack=Runnable{host.back(dialog)}
        val exit={host.back(dialog)}
        val api=PalantirHttp(base,headers)
        view.setContent{PalantirLibrary(api,host,exit,{onBack=it}){item->host.play(item,dialog)}}
        dialog=ComposeDialogHost.showFullscreen(activity,view,{host.back(dialog)},{host.closed()})
        dialog?.setOnKeyListener{_,code,event->if(code==android.view.KeyEvent.KEYCODE_BACK){if(event.action==android.view.KeyEvent.ACTION_UP)onBack.run();true}else false}
    }
}
private fun JSONArray.objects():List<JSONObject> =(0 until length()).mapNotNull{optJSONObject(it)}
private fun JSONArray.numbers():List<Int> =(0 until length()).map{optInt(it)}
private fun query(filter:PalantirFilter,offset:Int)=Uri.Builder().appendQueryParameter("kind",filter.kind).appendQueryParameter("category",filter.category)
    .appendQueryParameter("genre",filter.genre).appendQueryParameter("collection",filter.collection).appendQueryParameter("q",filter.search)
    .appendQueryParameter("sort",filter.sort).appendQueryParameter("offset",offset.toString()).build().encodedQuery.orEmpty()
private fun categoryLabel(raw:String)=when(raw){"movies"->"Películas";"series"->"Series";"cartoons"->"Dibujos";"anime"->"Anime";"documentaries"->"Documentales";"music"->"Música";else->raw}
internal fun palantirFocusTarget(keys:List<String>, preferred:String):String =
    preferred.takeIf{it in keys} ?: keys.firstOrNull().orEmpty()
private val palantirAccent get()=OfflineTvTheme.Colors.accentCyan

@Composable private fun PalantirLibrary(api:PalantirHttp,host:PalantirBrowser.Host,exit:()->Unit,setBack:(Runnable)->Unit,play:(ChannelItem)->Unit){
    var filter by remember{mutableStateOf(PalantirFilter())}
    var offset by remember{mutableStateOf(0)}
    var page by remember{mutableStateOf<JSONObject?>(null)}
    var filters by remember{mutableStateOf<JSONObject?>(null)}
    var detail by remember{mutableStateOf<JSONObject?>(null)}
    var loading by remember{mutableStateOf(true)}
    var error by remember{mutableStateOf("")}
    var refresh by remember{mutableStateOf(0)}
    var focused by remember{mutableStateOf("")}
    var picker by remember{mutableStateOf<List<Pair<String,()->Unit>>?>(null)}
 var pickerEpoch by remember{mutableStateOf(0)}
    val scope=rememberCoroutineScope()
    val focusRequester=remember{FocusRequester()}
    val catalogBackFocus=remember{FocusRequester()}
 val posters=rememberLazyListState()
 var detailJob by remember{mutableStateOf<Job?>(null)}
 var detailEpoch by remember{mutableStateOf(0)}
    var detailLoading by remember{mutableStateOf(false)}
    var playJob by remember{mutableStateOf<Job?>(null)}
    var playEpoch by remember{mutableStateOf(0)}
    var resolving by remember{mutableStateOf(0L)}
    fun cancelPlayback(){playEpoch++;playJob?.cancel();resolving=0L}
    fun startPlayback(variant:JSONObject){
        val current=detail?:return
        val item=current.optJSONObject("item")?:return
        if(resolving!=0L)return
        val epoch=++playEpoch;resolving=variant.optLong("id");error=""
        playJob=scope.launch{
            try{
                val result=api.post("/api/vod/palantir/play",JSONObject().put("key",item.optString("key")).put("revision",current.optString("revision"))
                    .put("variant",variant.optLong("id")).put("season",current.optInt("season")).put("episode",current.optInt("episode")))
                if(playEpoch==epoch)play(api.playbackItem(result))
            }catch(c:CancellationException){throw c}catch(e:Exception){if(playEpoch==epoch)error=e.message.orEmpty()}
            finally{if(playEpoch==epoch)resolving=0L}
        }
    }
    fun closeDetail(){cancelPlayback();detailEpoch++;detailJob?.cancel();detailLoading=false;detail=null}
 fun openDetail(key:String,season:Int?=null,episode:Int?=null,episodeOffset:Int=0){
        if(detailLoading)return
        detailLoading=true
 val epoch=++detailEpoch
        detailJob=scope.launch{
            try{
                val q=Uri.Builder().appendQueryParameter("episode_offset",episodeOffset.toString()).apply{season?.let{appendQueryParameter("season",it.toString())};episode?.let{appendQueryParameter("episode",it.toString())}}.build().encodedQuery
                val loaded=api.get("/api/vod/palantir/items/"+Uri.encode(key)+(q?.let{"?$it"}?:""))
                if(detailEpoch==epoch){detail=loaded;error=""}
            }catch(c:CancellationException){throw c}catch(e:Exception){error=e.message.orEmpty()}
            finally{if(detailEpoch==epoch)detailLoading=false}
        }
    }
    fun openCollections(at:Int=0,search:String=""){
        val epoch=++pickerEpoch
        picker=listOf("Cargando colecciones…" to {})
        scope.launch{
            try{
                val q=Uri.Builder().appendQueryParameter("offset",at.toString()).appendQueryParameter("q",search).build().encodedQuery
                val result=api.get("/api/vod/palantir/collections?"+q)
                if(pickerEpoch!=epoch)return@launch
                val choices=mutableListOf<Pair<String,()->Unit>>(
                    "Todas las colecciones" to {filter=filter.copy(collection="");offset=0;focused="";picker=null},
                    "Buscar colección" to {picker=null;host.input(search,Consumer{openCollections(0,it)})})
                result.optJSONArray("items")?.objects().orEmpty().forEach{c->
                    choices+=c.optString("title") to {filter=filter.copy(collection=c.optString("id"));offset=0;focused="";picker=null}
                }
                if(at>0)choices+="Colecciones anteriores" to {openCollections((at-60).coerceAtLeast(0),search)}
                if(result.optBoolean("has_more"))choices+="Más colecciones" to {openCollections(at+60,search)}
                picker=choices
            }catch(c:CancellationException){throw c}catch(e:Exception){if(pickerEpoch==epoch){picker=null;error=e.message.orEmpty()}}
        }
    }
    SideEffect{setBack(Runnable{when{resolving!=0L->cancelPlayback();picker!=null->{pickerEpoch++;picker=null};detail!=null->closeDetail();else->exit()}})}
    LaunchedEffect(Unit){try{filters=api.get("/api/vod/palantir/filters")}catch(c:CancellationException){throw c}catch(_:Exception){}}
    LaunchedEffect(filter,offset,refresh){
        loading=true;error=""
        try{page=api.get("/api/vod/palantir/catalog?"+query(filter,offset))}
        catch(c:CancellationException){throw c}
        catch(e:Exception){page=null;error=e.message.orEmpty()}
        finally{loading=false}
    }
    val entries=page?.optJSONArray("items")?.objects().orEmpty()
    LaunchedEffect(page,detail,picker,loading){
        if(!loading&&detail==null&&picker==null){
            if(entries.isNotEmpty()){
                val target=palantirFocusTarget(entries.map{it.optString("key")},focused)
                focused=target
                posters.scrollToItem(entries.indexOfFirst{it.optString("key")==target}.coerceAtLeast(0))
                withFrameNanos{};withFrameNanos{}
                runCatching{focusRequester.requestFocus()}
            }else{
                withFrameNanos{};runCatching{catalogBackFocus.requestFocus()}
            }
        }
    }
    fun change(next:PalantirFilter){if(next!=filter||offset!=0){loading=true;page=null};filter=next;offset=0;focused=""}
    Box(Modifier.fillMaxSize().background(OfflineTvTheme.Menu.background).padding(horizontal=30.dp,vertical=18.dp)){
        Column(Modifier.fillMaxSize().focusGroup(),verticalArrangement=Arrangement.spacedBy(10.dp)){
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
                BasicText(if(detail==null)"Palantir" else detail!!.optJSONObject("item")?.optString("title").orEmpty(),maxLines=1,overflow=TextOverflow.Ellipsis,
                    style=TextStyle(Color.White,25.sp,FontWeight.Bold),modifier=Modifier.weight(1f))
                PAction(if(detail==null)"Volver" else "Volver al catálogo",initial=detail!=null,modifier=Modifier.focusRequester(catalogBackFocus)){if(detail!=null)closeDetail() else exit()}
            }
            if(error.isNotBlank())BasicText(error,style=TextStyle(Color(0xFFFFB86B),14.sp))
            if(detail!=null){
                val data=detail!!;val item=data.optJSONObject("item")?:JSONObject()
                Row(Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(24.dp)){
                    PImage(item.optString("poster"),Modifier.width(145.dp).height(216.dp))
                    LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(10.dp)){
                        item{BasicText(listOf(item.optInt("year").takeIf{it>0}?.toString().orEmpty(),item.optDouble("rating",0.0).takeIf{it>0}?.let{"★ %.1f".format(it)}.orEmpty(),
                            item.optJSONArray("genres")?.let{a->(0 until a.length()).map{a.optString(it)}.joinToString(" · ")}.orEmpty()).filter{it.isNotBlank()}.joinToString(" · "),style=TextStyle(palantirAccent,14.sp))}
                        item{BasicText(item.optString("plot").ifBlank{"Sin sinopsis disponible"},style=TextStyle(Color.White,15.sp,lineHeight=21.sp))}
                        if(item.optString("kind")=="series"){
                            item{LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){items(data.optJSONArray("seasons")?.numbers().orEmpty(),key={it}){number->PAction((if(number==data.optInt("season"))"✓ " else "")+"Temporada $number"){openDetail(item.optString("key"),number)}}}}
                            item{LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){items(data.optJSONArray("episodes")?.numbers().orEmpty(),key={it}){number->PAction((if(number==data.optInt("episode"))"✓ " else "")+"Episodio $number"){openDetail(item.optString("key"),data.optInt("season"),number,data.optInt("episode_offset"))}}}}
                        }
                        if(item.optString("kind")=="series"){
                            item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                                if(data.optInt("episode_offset")>0)PAction("Episodios anteriores"){openDetail(item.optString("key"),data.optInt("season"),null,(data.optInt("episode_offset")-60).coerceAtLeast(0))}
                                if(data.optBoolean("episodes_more"))PAction("Más episodios"){openDetail(item.optString("key"),data.optInt("season"),null,data.optInt("episode_offset")+60)}
                            }}
                        }
                        item{BasicText("Opciones del catálogo",style=TextStyle(palantirAccent,16.sp,FontWeight.Bold))}
                        items(data.optJSONArray("variants")?.objects().orEmpty(),key={it.optLong("id")}){variant->
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically){
                                BasicText(listOf(variant.optString("quality"),variant.optString("audio"),variant.optString("info")).filter{it.isNotBlank()}.joinToString(" · ").ifBlank{"Versión disponible"},
                                    style=TextStyle(Color.White,14.sp),modifier=Modifier.weight(1f))
                                PAction(if(resolving==variant.optLong("id"))"Preparando…" else "Reproducir",enabled=resolving==0L){startPlayback(variant)}
                            }
                        }
                        if(resolving!=0L)item{PAction("Cancelar preparación"){cancelPlayback()}}
                        item{BasicText("Las cuentas de reproducción se configuran por usuario en el dashboard.",style=TextStyle(Color(0xFFAAC0CE),13.sp))}
                    }
                }
            }else{
                LazyRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    item{PAction("Películas"){change(filter.copy(kind="movie",category=""))}}
                    item{PAction("Series"){change(filter.copy(kind="series",category=""))}}
                    item{PAction("Sección"){picker=listOf("Todas" to {change(filter.copy(category="",kind=""));picker=null})+
                        filters?.optJSONArray("categories")?.objects().orEmpty().map{f->categoryLabel(f.optString("name")) to {change(filter.copy(category=f.optString("name"),kind=""));picker=null}}}}
                    item{PAction("Género"){picker=listOf("Todos" to {change(filter.copy(genre=""));picker=null})+
                        filters?.optJSONArray("genres")?.objects().orEmpty().map{f->f.optString("name") to {change(filter.copy(genre=f.optString("name")));picker=null}}}}
                    item{PAction("Colecciones"){openCollections()}}
                    item{PAction("Orden"){picker=listOf("Fecha de estreno" to {change(filter.copy(sort="newest"));picker=null},"Título" to {change(filter.copy(sort="title"));picker=null},"Valoración" to {change(filter.copy(sort="rating"));picker=null})}}
                    item{PAction("Buscar"){host.input(filter.search,Consumer{change(filter.copy(search=it))})}}
                    item{PAction("Limpiar"){change(PalantirFilter())}}
                }
                BasicText(listOf(filter.search,categoryLabel(filter.category),filter.genre).filter{it.isNotBlank()}.joinToString(" · ").ifBlank{"Películas, series y colecciones"},style=TextStyle(palantirAccent,13.sp))
                val selected=entries.firstOrNull{it.optString("key")==focused}?:entries.firstOrNull()
                BasicText(selected?.optString("title").orEmpty(),maxLines=2,overflow=TextOverflow.Ellipsis,style=TextStyle(Color.White,19.sp,FontWeight.Bold),modifier=Modifier.height(44.dp))
                if(loading||detailLoading)BasicText("Cargando…",style=TextStyle(Color.White,14.sp))
                if(!loading&&entries.isEmpty())BasicText(if(error.isNotBlank())"No se pudo abrir Palantir." else if(page?.optString("revision").isNullOrBlank())"El catálogo de Palantir aún no se ha importado en el servidor." else "No hay títulos para estos filtros.",style=TextStyle(Color.White,15.sp))
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()){
                  val imageHeight=(maxHeight-52.dp).coerceIn(80.dp,214.dp)
                  LazyRow(state=posters,horizontalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(vertical=6.dp)){
                    items(entries,key={it.optString("key")}){item->
                        val key=item.optString("key");val target=palantirFocusTarget(entries.map{it.optString("key")},focused)
                        PPoster(item,imageHeight,Modifier.then(if(key==target)Modifier.focusRequester(focusRequester)else Modifier),{focused=key}){openDetail(key)}
                    }
                }
                }
                Row(horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically){
                    if(offset>0)PAction("Página anterior"){loading=true;page=null;offset=(offset-60).coerceAtLeast(0);focused=""}
                    BasicText("Página "+(offset/60+1),style=TextStyle(Color.White,13.sp))
                    if(page?.optBoolean("has_more")==true)PAction("Página siguiente"){loading=true;page=null;offset+=60;focused=""}
                    PAction("Actualizar"){refresh++}
                }
            }
        }
        picker?.let{options->
            Box(Modifier.fillMaxSize().background(Color(0xCC07101A)),contentAlignment=Alignment.Center){
                LazyColumn(Modifier.width(360.dp).heightIn(max=400.dp).background(OfflineTvTheme.Menu.background).padding(18.dp)){
                    itemsIndexed(options){index,option->PAction(option.first,initial=index==0,modifier=Modifier.fillMaxWidth()){option.second()}}
                    item{PAction("Cancelar"){pickerEpoch++;picker=null}}
                }
            }
        }
    }
}
@Composable private fun PPoster(item:JSONObject,imageHeight:androidx.compose.ui.unit.Dp,modifier:Modifier,onFocus:()->Unit,onClick:()->Unit){
    var focused by remember{mutableStateOf(false)}
    Column(modifier.width(imageHeight*0.67f+6.dp).onFocusChanged{focused=it.isFocused;if(it.isFocused)onFocus()}
        .border(if(focused)2.dp else 0.dp,if(focused)palantirAccent else Color.Transparent,RoundedCornerShape(6.dp)).tvButtonSemantics(true).clickable(onClick=onClick).padding(3.dp)){
        PImage(item.optString("poster"),Modifier.fillMaxWidth().height(imageHeight))
        BasicText(item.optString("title"),maxLines=2,overflow=TextOverflow.Ellipsis,style=TextStyle(Color.White,12.sp),modifier=Modifier.height(34.dp).padding(top=5.dp))
        BasicText(item.optInt("year").takeIf{it>0}?.toString().orEmpty(),style=TextStyle(palantirAccent,11.sp))
    }
}
@Composable private fun PImage(url:String,modifier:Modifier){
    AndroidView(factory={AppCompatImageView(it).apply{scaleType=ImageView.ScaleType.CENTER_CROP}},update={view->if(view.tag!=url){view.tag=url;Glide.with(view).load(url.ifBlank{null}).override(240,360).into(view)}},modifier=modifier)
}
@Composable private fun PAction(label:String,initial:Boolean=false,modifier:Modifier=Modifier,enabled:Boolean=true,onClick:()->Unit){
    var focused by remember{mutableStateOf(false)}
    val requester=rememberTvInitialFocusRequester(initial,label)
    BasicText(label,maxLines=1,overflow=TextOverflow.Ellipsis,style=TextStyle(if(focused)Color(0xFF07101A)else Color.White,13.sp,FontWeight.Bold),
        modifier=modifier.heightIn(min=48.dp).then(if(initial)Modifier.focusRequester(requester)else Modifier).onFocusChanged{focused=it.isFocused}
            .background(if(focused)palantirAccent else Color.Transparent,RoundedCornerShape(4.dp)).tvButtonSemantics(enabled,label).clickable(enabled=enabled,onClick=onClick).padding(horizontal=12.dp,vertical=10.dp))
}
