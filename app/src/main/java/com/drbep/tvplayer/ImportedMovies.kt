package com.drbep.tvplayer

import android.app.Activity
import android.app.Dialog
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import kotlinx.coroutines.*
import org.json.JSONObject
import java.util.function.Consumer

internal data class MovieBrowsePosition(val filter:MovieFilter,val offset:Int,val id:String)
internal object ImportedMovies {
    internal val positions=mutableMapOf<String,MovieBrowsePosition>()
    interface Host {
        fun play(item:ChannelItem)
        fun allowed(item:ChannelItem):Boolean
        fun input(title:String,value:String,submit:Consumer<String>)
        fun back()
        fun closed()
    }
    @JvmStatic fun open(activity:Activity,view:ComposeView,controller:PersonalPlaylistController,base:String,headers:Map<String,String>,
        progress:Map<String,Long>,source:String,host:Host) {
        val repository=ImportedMovieRepository(activity,controller,base,headers,host::allowed)
        view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        var dialog:Dialog?=null
        val exit={dialog?.dismiss();host.back()}
        var backHandler=Runnable{exit()}
        view.setContent { MovieLibrary(repository,progress,source,host,exit,{backHandler=it}) {item->dialog?.dismiss();host.play(item)} }
        dialog=ComposeDialogHost.showFullscreen(activity,view,{host.back()},{host.closed()})
        dialog?.setOnKeyListener { _,code,event ->
            if(code==android.view.KeyEvent.KEYCODE_BACK) {
                if(event.action==android.view.KeyEvent.ACTION_UP)backHandler.run()
                true
            } else false
        }
    }
}

@Composable private fun MovieLibrary(repo:ImportedMovieRepository,progress:Map<String,Long>,source:String,
    host:ImportedMovies.Host,back:()->Unit,onBackChanged:(Runnable)->Unit,play:(ChannelItem)->Unit) {
    val saved=remember{ImportedMovies.positions[source]}
    var filter by remember { mutableStateOf(saved?.filter?:MovieFilter(source=source)) }
    var offset by remember { mutableStateOf(0) }
    var page by remember { mutableStateOf<MoviePage?>(null) }
    var selected by remember { mutableStateOf<ImportedMovie?>(null) }
    var details by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }
    var picker by remember { mutableStateOf<List<Pair<String,()->Unit>>?>(null) }
    var pickerTitle by remember { mutableStateOf("") }
    var revision by remember { mutableStateOf(0) }
    var ready by remember { mutableStateOf(false) }
    val scope=rememberCoroutineScope()
    val columnState=rememberLazyListState()
    val toolbarState=rememberLazyListState()
    val navigation=remember {MovieFocusNavigation(scope,columnState,toolbarState)}
    var loadedKey by remember {mutableStateOf<Pair<MovieFilter,Int>?>(null)}
    val loading=remember{mutableSetOf<String>()}
    fun extend(name:String,advance:Boolean) {
        val original=page?.shelves?.find{it.name==name}?:return
        if(!original.more||!loading.add(name))return
        val expectedFilter=filter
        scope.launch {
            try {
                val expanded=withContext(Dispatchers.IO){repo.loadShelf(original)}
                if(filter!=expectedFilter)return@launch
                val current=page?:return@launch
                val updated=current.copy(shelves=current.shelves.map{if(it.name==name)expanded else it})
                page=updated;navigation.shelves=updated.shelves
                if(advance&&navigation.focusedShelf==name&&navigation.focusedIndex==original.items.lastIndex&&expanded.items.size>original.items.size)
                    navigation.card(updated.shelves.indexOfFirst{it.name==name},original.items.size)
            } catch(e:CancellationException){throw e}
            catch(e:Exception){notice="No se pudieron cargar más películas. Pulsa derecha para reintentar."}
            finally{loading.remove(name)}
        }
    }
    SideEffect{navigation.loadMore={name,advance->extend(name,advance)}}
    LaunchedEffect(selected?.id,page?.shelves?.map{it.items.size}) {
        val shelf=page?.shelves?.find{it.name==navigation.focusedShelf}
        if(shelf!=null&&shelf.more&&navigation.focusedIndex>=shelf.items.size-6)extend(shelf.name,false)
    }

    DisposableEffect(filter,offset,selected?.id) {
        onDispose{ImportedMovies.positions[source]=MovieBrowsePosition(filter,offset,selected?.id.orEmpty())}
    }
    fun pick(title:String,options:List<Pair<String,()->Unit>>) {pickerTitle=title;picker=options}
    fun change(f:MovieFilter){if(f==filter&&offset==0){navigation.filter(navigation.filterIndex);return};navigation.cancel();filter=f;offset=0;selected=null;page=null}
    LaunchedEffect(Unit){withContext(Dispatchers.IO){repo.syncProgress(progress)};ready=true}
    LaunchedEffect(filter,offset,revision,ready) {
        if(!ready)return@LaunchedEffect
        try {
            val result=withContext(Dispatchers.IO){repo.page(filter,offset)}
            val previous=page
            val loaded=result.copy(shelves=result.shelves.filter{it.items.isNotEmpty()}.map { fresh ->
                val old=previous?.shelves?.find{it.name==fresh.name}
                if(old!=null&&old.nextOffset>fresh.nextOffset) {
                    val updates=fresh.items.associateBy{it.id}
                    old.copy(items=old.items.map{updates[it.id]?:it})
                } else fresh
            })
            val fresh=loadedKey!=(filter to offset)
            val firstLoad=loadedKey==null
            page=loaded
            navigation.shelves=loaded.shelves
            navigation.hasPrevious=false;navigation.hasNext=false
            if(fresh) {
                selected=loaded.shelves.flatMap{it.items}.find{firstLoad&&it.id==saved?.id}?:loaded.shelves.firstOrNull()?.items?.firstOrNull()
                loadedKey=filter to offset
                if(loaded.shelves.isEmpty())navigation.filter(0)
                else {
                    val row=loaded.shelves.indexOfFirst{it.items.any{movie->movie.id==selected?.id}}.coerceAtLeast(0)
                    val index=loaded.shelves[row].items.indexOfFirst{it.id==selected?.id}.coerceAtLeast(0)
                    navigation.card(row,index)
                }
            }
        }
        catch(e:Exception){notice="No se pudo abrir el catálogo. Vuelve a intentarlo."}
    }
    LaunchedEffect(selected?.id) {
        val item=selected?:return@LaunchedEffect
        delay(700)
        val enriched=withContext(Dispatchers.IO){repo.resolve(item)}
        if(selected?.id==item.id)selected=enriched
    }
    // Small batches while this screen is open; opening the app never triggers catalogue enrichment.
    LaunchedEffect(ready,filter.source) {
        if(!ready)return@LaunchedEffect
        delay(2000)
        while(isActive) {
            val batch=withContext(Dispatchers.IO){repo.pending(filter.source)}
            if(batch.isEmpty())break
            for(item in batch){ensureActive();withContext(Dispatchers.IO){repo.resolve(item)};delay(500)}
            revision++;delay(30000)
        }
    }
    fun closePicker(){picker=null;if(details)return;navigation.filter(navigation.filterIndex)}
    fun closeDetails(){details=false;navigation.currentCard()}
    val goBack=Runnable {when{picker!=null->closePicker();details->closeDetails();else->back()}}
    SideEffect{onBackChanged(goBack)}
    Box(Modifier.fillMaxSize().tvPanelBackHandler(goBack).background(Color(0xFF0B1018))) {
        selected?.backdrop?.takeIf{it.isNotBlank()}?.let{MovieArt(it,Modifier.fillMaxSize(),true)}
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xFF0B1018),Color(0xDD0B1018),Color(0x550B1018)))))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent,Color(0xEE0B1018)))))
        Column(Modifier.fillMaxSize().focusProperties{canFocus=picker==null}.padding(horizontal=28.dp,vertical=16.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                BasicText(if(details)"Ficha de película" else "Películas · Mis listas",style=TextStyle(Color.White,24.sp,FontWeight.Bold))
                Spacer(Modifier.weight(1f));MovieButton("Volver",modifier=Modifier.focusRequester(navigation.back).movieDirections{if(it==android.view.KeyEvent.KEYCODE_DPAD_DOWN)navigation.filter(navigation.filterIndex)}){goBack.run()}
            }
            if(!details) LazyRow(state=toolbarState,horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.padding(vertical=8.dp)) {
                item {MovieButton("Lista: "+(page?.sources?.find{it.first==filter.source}?.second?:"Todas"),modifier=filterModifier(navigation,0)){
                    pick("Seleccionar lista",listOf("Todas las listas" to {change(filter.copy(source="",group=""))})+page?.sources.orEmpty().map{(id,name)->name to {change(filter.copy(source=id,group=""))}})
                }}
                item {MovieButton("Grupo: "+filter.group.ifBlank{"Todos"},modifier=filterModifier(navigation,1)){pick("Categorías de tus listas",listOf("Todos los grupos" to {change(filter.copy(group=""))})+page?.groups.orEmpty().map{g->g to {change(filter.copy(group=g))}})}}
                item {MovieButton("Género: "+filter.genre.ifBlank{"Todos"},modifier=filterModifier(navigation,2)){pick("Géneros identificados",listOf("Todos los géneros" to {change(filter.copy(genre=""))})+page?.genres.orEmpty().map{g->g to {change(filter.copy(genre=g))}})}}
                item {MovieButton("Buscar"+if(filter.search.isBlank())"" else ": ${filter.search}",modifier=filterModifier(navigation,3)){host.input("Buscar películas",filter.search,Consumer{change(filter.copy(search=it))})}}
                item {MovieButton("Ordenar",modifier=filterModifier(navigation,4)){pick("Ordenar películas",listOf("Añadidas recientemente" to "added","Fecha de estreno" to "release","Título A–Z" to "title","Valoración" to "rating").map{(label,id)->label to {change(filter.copy(order=id))}})}}
            }
            val item=selected
            Column(Modifier.fillMaxWidth().height(if(details)250.dp else 126.dp).padding(vertical=7.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                BasicText(item?.title?:"Tu biblioteca de películas",maxLines=2,overflow=TextOverflow.Ellipsis,style=TextStyle(Color.White,26.sp,FontWeight.Bold))
                BasicText(item?.meta.orEmpty(),style=TextStyle(OfflineTvTheme.Colors.accentCyan,12.sp))
                BasicText(item?.description?.ifBlank{"Ficha pendiente de identificar. Puedes reproducir con los datos de tu lista."}?:"Cargando catálogo local…",maxLines=if(details)6 else 2,overflow=TextOverflow.Ellipsis,style=TextStyle(Color(0xFFCFD7E0),13.sp))
                if(details&&item!=null){
                    val cast=item.metadata?.optJSONArray("cast")
                    BasicText(cast?.let{a->"Reparto: "+(0 until a.length()).joinToString(", "){a.getString(it)}}.orEmpty(),maxLines=2,style=TextStyle(Color(0xFF9FB0C2),12.sp))
                    BasicText("Origen: ${item.provider} · ${item.group}",style=TextStyle(OfflineTvTheme.Colors.accentCyan,12.sp))
                }
            }
            if(details&&item!=null) {
                Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    MovieButton(if(item.progress>30000)"Continuar / desde el inicio" else "Reproducir",true) {
                        if(item.channel.playlistPlaybackError.isNotEmpty())notice=item.channel.playlistPlaybackError else play(item.channel)
                    }
                    MovieButton("Corregir ficha TMDB") {
                        host.input("Título para buscar en TMDB",item.title,Consumer { title->scope.launch {
                            notice="Buscando coincidencias…"
                            try{
                                val hits=withContext(Dispatchers.IO){repo.candidates(title)}
                                notice=if(hits.isEmpty())"No se han encontrado coincidencias." else ""
                                pick("Elige la película correcta",hits.map{hit->"${hit.optString("title")} (${hit.optString("release_date").take(4)})" to {scope.launch {
                                    val updated=withContext(Dispatchers.IO){repo.resolve(item,hit.getInt("id"))};selected=updated;revision++
                                };Unit}})
                            }catch(e:Exception){notice="TMDB no está disponible. Puedes seguir reproduciendo."}
                        }})
                    }
                    MovieButton("Cerrar ficha"){closeDetails()}
                }
            } else {
                if(page?.shelves?.all{it.items.isEmpty()}==true)BasicText("No hay películas con estos filtros.",style=TextStyle(Color.White,16.sp))
                LazyColumn(Modifier.weight(1f),state=columnState,verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    itemsIndexed(page?.shelves.orEmpty(),key={_,shelf->shelf.name}) { row,shelf ->
                        Column {
                            BasicText(shelf.name,style=TextStyle(OfflineTvTheme.Colors.accentCyan,16.sp,FontWeight.Bold))
                            val rowState=rememberLazyListState()
                            DisposableEffect(shelf.name,rowState){navigation.rowStates[shelf.name]=rowState;onDispose{navigation.rowStates.remove(shelf.name)}}
                            LazyRow(state=rowState,horizontalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(vertical=6.dp),modifier=Modifier.focusGroup()) {
                                itemsIndexed(shelf.items,key={_,movie->movie.id}){index,movie->
                                    var focus by remember {mutableStateOf(false)}
                                    val requester=remember{FocusRequester()}
                                    val focusKey=navigation.key(shelf.name,movie.id)
                                    DisposableEffect(focusKey,requester){navigation.cards[focusKey]=requester;onDispose{navigation.cards.remove(focusKey)}}

                                    Column(Modifier.width(110.dp).focusRequester(requester).onFocusChanged{focus=it.isFocused;if(focus){selected=movie;navigation.focusedShelf=shelf.name;navigation.focusedIndex=index}}
                                        .movieDirections{navigation.fromCard(row,index,it)}
                                        .border(if(focus)2.dp else 0.dp,if(focus)Color.White else Color.Transparent,RoundedCornerShape(4.dp))
                                        .semantics(mergeDescendants=true){contentDescription="${movie.title} · ${shelf.name}";role=Role.Button}.clickable{selected=movie;details=true}.padding(3.dp)) {
                                        MovieArt(if(selected?.id==movie.id) selected!!.poster else movie.poster,Modifier.fillMaxWidth().height(140.dp),false)
                                        BasicText(movie.title,maxLines=2,overflow=TextOverflow.Ellipsis,style=TextStyle(Color.White,12.sp,FontWeight.Bold),modifier=Modifier.padding(top=5.dp))
                                        BasicText(if(movie.progress>30000)"Continuar · ${movie.progress/60000} min" else movie.group.ifBlank{movie.provider},maxLines=1,overflow=TextOverflow.Ellipsis,style=TextStyle(Color(0xFF9FB0C2),10.sp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if(notice.isNotBlank())BasicText(notice,style=TextStyle(Color(0xFFFFC56B),12.sp))
            BasicText("TMDB ${page?.identified?:0}/${page?.total?:0} · Fichas y géneros se completan mientras exploras · Disponibles después sin conexión",style=TextStyle(Color(0xFF8094A8),10.sp),modifier=Modifier.padding(top=6.dp))
        }
        picker?.let {options->
            Column(Modifier.fillMaxHeight().width(360.dp).background(OfflineTvTheme.Menu.background).background(Color(0xED101722)).padding(24.dp)) {
                BasicText(pickerTitle,style=TextStyle(Color.White,20.sp,FontWeight.Bold));Spacer(Modifier.height(12.dp))
                MovieButton("Cerrar",true){closePicker()}
                LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)) {items(options.size){i->MovieButton(options[i].first){val action=options[i].second;picker=null;action()}}}
            }
        }
    }
}
@Composable private fun MovieButton(label:String,initial:Boolean=false,modifier:Modifier=Modifier,onClick:()->Unit) {
    var focused by remember{mutableStateOf(false)}
    val requester=rememberTvInitialFocusRequester(initial,label)
    BasicText(label,maxLines=2,style=TextStyle(if(focused)Color(0xFF101722)else Color.White,12.sp,FontWeight.Bold),modifier=modifier
        .then(if(initial)Modifier.focusRequester(requester)else Modifier)
        .onFocusChanged{focused=it.isFocused}.background(if(focused)Color.White else Color.Transparent,RoundedCornerShape(4.dp))
        .semantics(mergeDescendants=true){contentDescription=label;role=Role.Button}.clickable(onClick=onClick).padding(horizontal=12.dp,vertical=10.dp))
}
@Composable private fun MovieArt(url:String,modifier:Modifier,wide:Boolean) {
    AndroidView(factory={ctx->AppCompatImageView(ctx).apply{scaleType=ImageView.ScaleType.CENTER_CROP;importantForAccessibility=android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO}},
        update={view->if(view.tag!=url){view.tag=url;Glide.with(view).load(url.ifBlank{null}).override(if(wide)960 else 220,if(wide)540 else 300).into(view)}},modifier=modifier)
}

private fun filterModifier(nav:MovieFocusNavigation,index:Int)=Modifier.focusRequester(nav.filters[index])
    .onFocusChanged{if(it.isFocused)nav.filterIndex=index}.movieDirections{nav.fromFilter(index,it)}
