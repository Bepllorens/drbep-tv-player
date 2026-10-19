package com.drbep.tvplayer

import android.widget.ImageView
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import kotlinx.coroutines.android.awaitFrame
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object U7dCatalogComposeBinder {
    @JvmStatic fun bind(view: ComposeView, state: U7dCatalogState, close: Runnable, refresh: Runnable) {
        view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        view.setContent { Catalog(state, close, refresh) }
    }
}
private val archiveDayColors=listOf(Color(0xFF65D9ED),Color(0xFF8EB6FF),Color(0xFFBC9FFF),Color(0xFFFFAB86),Color(0xFFE8CF79),Color(0xFF79D9B2),Color(0xFFF39EC9),Color(0xFFAABDDC))
private val archiveLiveColor=Color(0xFF79D9A3)
private val archiveSavedColor=Color(0xFFF2CB70)
private fun dayAccent(day:String):Color = runCatching {
    archiveDayColors[Math.floorMod(LocalDate.parse(day).toEpochDay(),archiveDayColors.size.toLong()).toInt()]
}.getOrDefault(archiveDayColors.first())
private fun time(ms: Long) = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(ms))
@Composable private fun Catalog(s: U7dCatalogState, close: Runnable, refresh: Runnable) {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    // Only advance the visible clock. Network refresh remains explicit / on day change.
    LaunchedEffect(Unit) { while(true) { delay(60_000); now=System.currentTimeMillis(); if(now>=s.nextChange)s.queryPage.run() } }
    val today=java.time.Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate()
    val days=(0..7).map { today.minusDays(it.toLong()).toString() }
    var previousToday by remember { mutableStateOf(today.toString()) }
    LaunchedEffect(today) {
        if(s.day.isBlank() || s.day !in days || s.day==previousToday) s.day=today.toString()
        previousToday=today.toString()
    }
    var firstDayEffect by remember { mutableStateOf(true) }
    LaunchedEffect(s.day) { if(firstDayEffect)firstDayEffect=false else if(s.day.isNotBlank())refresh.run() }
    val pageReady=s.presentationKey==ArchivePageIdentity.key(s.day,s.channel,s.genre,s.query,s.savedOnly,s.offset)
    val publishedDay=s.publishedDay
    val entries=s.entries
    val visible=remember(entries,s.pageIds) { entries.filter { it.id in s.pageIds }.sortedWith(compareByDescending<U7dCatalogEntry> { it.start }.thenBy { it.id }) }
    var submittedFilters by remember { mutableStateOf<List<Any>?>(null) }
    fun filters(): List<Any> = listOf(s.day,s.channel,s.genre,s.query,s.savedOnly)
    LaunchedEffect(s.day,s.channel,s.genre,s.query,s.savedOnly) {
        if (submittedFilters != filters()) {
            s.offset=0
            if(s.query.isNotBlank())delay(250)
            submittedFilters=filters()
            s.queryPage.run()
        }
    }
    val cellSlots=remember { ArchiveGridSlots() }
    // Use the published page identity, not the pending filter: old results stay visible while loading.
    val gridKeys=remember(visible,s.presentationKey) { cellSlots.assign(s.presentationKey,visible.map { it.id }) }
    var picker by remember { mutableStateOf("") }
    var pickerQuery by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf(false) }
    val listState=rememberLazyGridState()
    val initial=remember { FocusRequester() }
    var initialFocusDone by remember { mutableStateOf(false) }
    val windowFocused=androidx.compose.ui.platform.LocalWindowInfo.current.isWindowFocused
    LaunchedEffect(windowFocused) {
        if(windowFocused && !initialFocusDone) {
            awaitFrame();awaitFrame()
            initialFocusDone=runCatching { initial.requestFocus();true }.getOrDefault(false)
        }
    }
    val entryFocus=remember { mutableMapOf<String,FocusRequester>() }
    LaunchedEffect(visible) { entryFocus.keys.retainAll(visible.map { it.id }.toSet()) }
    val channelFocus=remember { FocusRequester() }
    val genreFocus=remember { FocusRequester() }
    var returnToGrid by remember { mutableStateOf(false) }
    var pendingGridFocus by remember { mutableStateOf(false) }
    LaunchedEffect(visible,pendingGridFocus,s.querying,pageReady) {
        if(pageReady && !s.querying && pendingGridFocus && visible.isNotEmpty()) {
            listState.scrollToItem(0)
            val target=visible.first().id
            repeat(12) {
                awaitFrame()
                if(pendingGridFocus) {
                    entryFocus[target]?.requestFocus()
                    if(s.selectedId==target)pendingGridFocus=false
                }
            }
        }
    }
    var returnToFilter by remember { mutableStateOf("") }
    LaunchedEffect(detail) {
        if(!detail && returnToGrid) {
            returnToGrid=false
            val i=visible.indexOfFirst { it.id==s.selectedId }
            if(i>=0) { listState.scrollToItem(i);awaitFrame();awaitFrame();entryFocus[s.selectedId]?.requestFocus() }
        }
    }
    LaunchedEffect(picker) {
        if(picker.isEmpty() && returnToFilter.isNotEmpty()) {
            awaitFrame();awaitFrame()
            if(returnToFilter=="Canales") channelFocus.requestFocus() else genreFocus.requestFocus()
            returnToFilter=""
        }
    }
    LaunchedEffect(s.day,s.channel,s.genre,s.savedOnly,s.query) {
        // A new filter has a new list. Do not repeatedly reset scroll on background updates.
        s.selectedId=""
        listState.scrollToItem(0)
    }
    val back=Runnable { if(picker.isNotEmpty()) picker="" else if(detail) { returnToGrid=true;detail=false } else close.run() }
    Column(Modifier.fillMaxSize().background(Color(0xD9090E16)).background(OfflineTvTheme.Menu.background).tvPanelBackHandler(back).padding(24.dp)) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            Column {
                Text("Catchup · Últimos 7 días",24,true)
                ArchiveStatus(s,pageReady)
            }
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Button("Movistar: ${s.format}") { s.format=when(s.format){"Auto"->"HLS";"HLS"->"MPEG-TS";else->"Auto"} }
                Button("Actualizar") { refresh.run() }
                Button("Volver") { back.run() }
            }
        }
        LazyRow(modifier=Modifier.focusProperties { down=channelFocus },horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            items(days,key={it}) { d ->
                val label=when(d) { today.toString()->"Hoy";today.minusDays(1).toString()->"Ayer";else->LocalDate.parse(d).format(DateTimeFormatter.ofPattern("EEE d",java.util.Locale.getDefault())) }
                Button(label,if(d==days.first()) Modifier.focusRequester(initial) else Modifier,active=d==s.day,tint=dayAccent(d)) {
                    if(s.day!=d)ArchivePageTrace.begin(d)
                    val changed=s.day!=d || s.savedOnly
                    s.day=d;s.savedOnly=false;detail=false
                    if(changed) {
                        s.offset=0
                        submittedFilters=filters()
                        s.queryPage.run()
                    }
                }
            }
            item { Button(if(s.savedOnly) "★ Guardados ✓" else "★ Guardados",active=s.savedOnly,tint=archiveSavedColor) { s.savedOnly=!s.savedOnly;detail=false } }
        }
        Row(Modifier.onPreviewKeyEvent { event ->
            val native=event.nativeKeyEvent
            if(native.action==android.view.KeyEvent.ACTION_DOWN && native.keyCode==android.view.KeyEvent.KEYCODE_DPAD_DOWN && picker.isEmpty() && !detail) {
                pendingGridFocus=true;true
            } else { if(native.keyCode==android.view.KeyEvent.KEYCODE_DPAD_UP || native.keyCode==android.view.KeyEvent.KEYCODE_BACK)pendingGridFocus=false;false }
        },horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Button(s.channels[s.channel] ?: "Todos los canales",Modifier.focusRequester(channelFocus)) { returnToFilter="Canales";picker="Canales";pickerQuery="" }
            Button(s.genre.ifBlank { "Géneros" },Modifier.focusRequester(genreFocus)) { returnToFilter="Géneros";picker="Géneros";pickerQuery="" }
            BasicTextField(s.query,{ s.query=it;detail=false },Modifier.weight(1f).padding(10.dp).border(1.dp,Color.Gray).padding(8.dp),singleLine=true,
                textStyle=TextStyle(Color.White,14.sp),decorationBox={ inner -> if(s.query.isEmpty()) Text("Buscar título o canal…",14);inner() })
        }
        if(picker.isNotEmpty()) {
            Text("Filtrar: $picker",18,true)
            BasicTextField(pickerQuery,{pickerQuery=it},Modifier.fillMaxWidth().padding(8.dp).border(1.dp,Color.Gray).padding(10.dp),singleLine=true,textStyle=TextStyle(Color.White,16.sp),decorationBox={inner->if(pickerQuery.isEmpty())Text("Buscar…",14);inner()})
            val options=if(picker=="Canales") s.channels.map { it.key to it.value }.sortedBy { it.second } else s.genres.map { it to it }
            LazyColumn {
                item { Button("Todos") { if(picker=="Canales")s.channel="" else s.genre="";picker="" } }
                items(options.filter { it.second.contains(pickerQuery,true) }) { (id,name) -> Button(name) { if(picker=="Canales")s.channel=id else s.genre=id;picker="";detail=false } }
            }
        } else Row(Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(20.dp)) {
            if(!detail) Box(Modifier.fillMaxSize()) {
                if(pageReady && visible.isEmpty()) Text(if(s.loading) "Cargando programas del día…" else if(s.savedOnly) "No hay programas guardados con estos filtros." else "No hay programas disponibles con estos filtros.",16)
                LazyVerticalGrid(columns=GridCells.Adaptive(160.dp),state=listState,modifier=Modifier.fillMaxSize().graphicsLayer { alpha=if(pageReady)1f else 0f }.onPreviewKeyEvent {
                        !pageReady && it.nativeKeyEvent.keyCode!=android.view.KeyEvent.KEYCODE_BACK
                    }.drawWithContent { drawContent();if(pageReady)ArchivePageTrace.drawn(publishedDay) },
                    horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(vertical=8.dp)) {
                    itemsIndexed(visible,key={index,_->gridKeys[index]}) { _,e ->
                        val requester=remember(e.id) { FocusRequester() }
                        DisposableEffect(e.id) { entryFocus[e.id]=requester;onDispose { entryFocus.remove(e.id) } }
                        ArchiveGridCard(e,now,requester,
                            onFocused={ s.selectedId=e.id },
                            onOpen={ if(pageReady) { s.selectedId=e.id;detail=true } })
                    }
                }
                if(!pageReady) Box(Modifier.fillMaxSize().focusProperties { canFocus=false }.clickable { },contentAlignment=Alignment.TopStart) {
                    Text("Cargando programas…",16,color=Color(0xFFCFD7E4))
                }
            }
            if(detail) (visible.firstOrNull { it.id==s.selectedId } ?: visible.firstOrNull())?.let { e ->
                Row(Modifier.fillMaxSize(),horizontalArrangement=Arrangement.spacedBy(24.dp)) {
                    ArchiveArtwork(e,Modifier.weight(.8f).fillMaxHeight())
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    ProgrammeSummary(e.title,"${e.channel} · ${time(e.start)}–${time(e.end)} · ${e.genre}")
                    Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        if(e.start<=now && e.end>now)Text("● En emisión",12,color=archiveLiveColor)
                        if(e.saved)Text("★ Guardado",12,color=archiveSavedColor)
                    }
                    val metadata=rememberProgrammeMetadata(e.programmeMetadata)
                    ProgrammeMetadataRow(metadata)
                    val expired=e.expires<=now
                    Text(if(expired) "Caducado · ya no está en el archivo" else "Disponibilidad estimada hasta "+java.text.SimpleDateFormat("d MMM HH:mm",java.util.Locale.getDefault()).format(java.util.Date(e.expires)),11,color=if(expired)Color(0xFF9CA6B4) else Color(0xFFCFD7E4))
                    Box(Modifier.weight(1f).verticalScroll(rememberScrollState())) { Text(metadata.description.ifBlank { e.description }.ifBlank { "Sin información adicional de la fuente." },14,max=20) }
                    if(detail) {
                        val playFocus=rememberTvInitialFocusRequester(true,e.id)
                        if(!expired) Button("Ver desde el principio",Modifier.focusRequester(playFocus)) { e.play.run() }
                        Button(if(e.saved) "Quitar de guardados" else "Guardar",if(expired)Modifier.focusRequester(playFocus) else Modifier) { e.save.run() }
                        Button("Volver al directo") { e.live.run() }
                    } else Text("OK: ver opciones",12)
                }
                }
            }
        }
        if(!detail && picker.isEmpty() && s.total>60) Row(modifier=Modifier.graphicsLayer { alpha=if(pageReady)1f else 0f },horizontalArrangement=Arrangement.spacedBy(16.dp)) {
            if(s.offset>0)Button("← Anteriores") { if(pageReady) { s.offset=(s.offset-60).coerceAtLeast(0);s.queryPage.run();pendingGridFocus=true } }
            Text("${s.offset+1}–${(s.offset+visible.size).coerceAtMost(s.total)} de ${s.total}",12)
            if(s.offset+60<s.total)Button("Siguientes →") { if(pageReady) { s.offset+=60;s.queryPage.run();pendingGridFocus=true } }
        }
        Text("Días y filtros arriba · OK: ficha · Atrás: volver",11)
    }
}

@Composable private fun ArchiveStatus(s:U7dCatalogState,pageReady:Boolean) {
    Text(if(pageReady) "${s.total} programas · ${s.status}" else "Preparando la selección…",11)
}

@Immutable private data class ArchiveCardVisual(
    val id:String,val title:String,val channel:String,val genre:String,val day:String,
    val start:Long,val end:Long,val saved:Boolean,val subtitle:String,val schedule:String,
    val artworkKey:String,val hasProgramArtwork:Boolean,val live:Boolean,val expired:Boolean)

@Composable private fun ArchiveGridCard(e:U7dCatalogEntry, now:Long, requester:FocusRequester,
    onFocused:()->Unit, onOpen:()->Unit) {
    // Fresh actions are retained without making them part of visual equality.
    val latestEntry=rememberUpdatedState(e)
    val latestFocused=rememberUpdatedState(onFocused)
    val latestOpen=rememberUpdatedState(onOpen)
    val bind=remember { java.util.function.Consumer<ImageView> { latestEntry.value.artwork.accept(it) } }
    val focused=remember { { latestFocused.value.invoke() } }
    val open=remember { { latestOpen.value.invoke() } }
    val visual=ArchiveCardVisual(e.id,e.title,e.channel,e.genre,e.day,e.start,e.end,e.saved,
        e.subtitle,e.schedule,e.artworkKey,e.hasProgramArtwork,e.start<=now && e.end>now,e.expires<=now)
    ArchiveCardBody(visual,requester,bind,focused,open)
}

/** Focus invalidates this card only, not the whole catalogue or its neighbours. */
@Composable private fun ArchiveCardBody(e:ArchiveCardVisual, requester:FocusRequester, bind:java.util.function.Consumer<ImageView>,
    onFocused:()->Unit, onOpen:()->Unit) {
    android.os.Trace.beginSection("Archive.cardCompose")
    val schedule=remember(e.schedule,e.start,e.end,e.channel) { e.schedule.ifEmpty { "${time(e.start)}–${time(e.end)} · ${e.channel}" } }
    val accent=remember(e.day) { dayAccent(e.day) }
    val genreColor=remember(e.genre) { Color(android.graphics.Color.parseColor(ProgrammePresentation.color(e.genre))) }
    var focus by remember(e.id) { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()
        .focusRequester(requester)
        .onFocusChanged { focus=it.isFocused;if(focus)onFocused() }
        .border(2.dp,if(focus)Color.White else Color.Transparent,RoundedCornerShape(8.dp))
        .clickable(onClick=onOpen).padding(4.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(4f/3f).clip(RoundedCornerShape(7.dp))) {
            ArchiveArtworkView(e.id,e.artworkKey,e.channel,e.hasProgramArtwork,bind,Modifier.fillMaxSize())
            if(e.live) Box(Modifier.padding(6.dp).background(Color(0xE6297051),RoundedCornerShape(4.dp)).padding(horizontal=6.dp)) { Text("EN EMISIÓN",10,true) }
            if(e.saved) Box(Modifier.align(Alignment.TopEnd).padding(6.dp).background(Color(0xDD11151F),RoundedCornerShape(4.dp)).padding(horizontal=5.dp)) { Text("★",11,color=archiveSavedColor) }
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(3.dp).background(genreColor))
        }
        Text(e.title,14,true)
        Text(schedule,11,color=accent)
        Text(if(e.expired) "Caducado" else e.subtitle.ifBlank { e.genre },11,color=Color(0xFFB9C1CD))
    }
    android.os.Trace.endSection()
}

@Composable private fun Text(text:String,size:Int,bold:Boolean=false,max:Int=1,color:Color=Color.White) {
    BasicText(text,Modifier.padding(vertical=3.dp),style=TextStyle(color,size.sp,if(bold)FontWeight.Bold else FontWeight.Normal),maxLines=max,overflow=TextOverflow.Ellipsis)
}
@Composable private fun Button(label:String,modifier:Modifier=Modifier,active:Boolean=false,tint:Color?=null,action:()->Unit) {
    var focus by remember { mutableStateOf(false) }
    Box(modifier.widthIn(max=180.dp).onFocusChanged{focus=it.isFocused}.border(2.dp,if(focus)OfflineTvTheme.Colors.focus else Color.Transparent)
        .background(if(active && tint!=null)tint.copy(alpha=.20f) else if(focus)OfflineTvTheme.Menu.selected else Color.Transparent)
        .clickable(onClick=action).padding(8.dp)) {
        Column { Text(label,14,color=tint?:Color.White);if(tint!=null)Box(Modifier.width(38.dp).height(if(active)3.dp else 1.dp).background(tint)) }
    }
}

@Composable fun U7dEmissionStrip(entries:List<U7dCatalogEntry>, remoteFocus:Boolean=false, remoteIndex:Int=0, loadingId:String="") {
    if(entries.isEmpty())return
    val now=System.currentTimeMillis()
    val selected=entries.indexOfLast { it.start<=now }.coerceAtLeast(0)
    val state=rememberLazyListState(selected)
    LaunchedEffect(remoteFocus,remoteIndex) { if(remoteFocus) state.animateScrollToItem(remoteIndex.coerceIn(0,entries.lastIndex)) }
    LaunchedEffect(loadingId) { if(loadingId.isNotEmpty()) { val index=entries.indexOfFirst{it.id==loadingId}; if(index>=0)state.scrollToItem(index) } }
    LazyRow(state=state,horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.fillMaxWidth().height(140.dp)) {
        items(entries,key={it.id}) { e ->
            var focus by remember { mutableStateOf(false) }
            Column(Modifier.width(180.dp).border(2.dp,if(focus || (remoteFocus && entries.getOrNull(remoteIndex)?.id==e.id))OfflineTvTheme.Colors.focus else Color.Transparent)
                .onFocusChanged{focus=it.isFocused}.clickable{if(e.end>System.currentTimeMillis())e.live.run() else e.play.run()}.padding(4.dp)) {
                Box(Modifier.fillMaxWidth().height(92.dp).clip(RoundedCornerShape(7.dp))) {
                    ArchiveArtwork(e,Modifier.fillMaxSize())
                    if(e.id==loadingId) U7dThumbnailLoadingRing()
                    if(e.start<=now && e.end>now) Box(Modifier.padding(5.dp).background(Color(0xE6297051),RoundedCornerShape(4.dp)).padding(horizontal=5.dp)) { Text("EN EMISIÓN",10,true) }
                }
                Text(e.title,12,true)
                Text("${time(e.start)}–${time(e.end)}",11,color=dayAccent(e.day))
            }
        }
    }
}

/** A recycled view must cancel its former request before binding another programme. */
private class ArchiveImageView(context:android.content.Context):ImageView(context) {
    var boundId:String?=null
    var boundArtwork:String?=null
    fun clearBinding() {
        com.bumptech.glide.Glide.with(this).clear(this)
        setImageDrawable(null)
        tag=null
        boundId=null
        boundArtwork=null
    }
}

/** Keep aspect ratio and reuse image views when lazy-grid items leave the viewport. */
@Composable private fun ArchiveArtwork(e:U7dCatalogEntry,modifier:Modifier=Modifier) {
    ArchiveArtworkView(e.id,e.artworkKey,e.channel,e.hasProgramArtwork,e.artwork,modifier)
}

@Composable private fun ArchiveArtworkView(id:String,artworkKey:String,channel:String,hasProgramArtwork:Boolean,bind:java.util.function.Consumer<ImageView>,modifier:Modifier=Modifier) {
    Box(modifier.background(Color(0xFF111C29)),contentAlignment=Alignment.Center) {
        AndroidView(factory={c->
            android.os.Trace.beginSection("Archive.imageCreate")
            try { ArchiveImageView(c) } finally { android.os.Trace.endSection() }
        }, onReset={it.clearBinding()}, onRelease={it.clearBinding()}, update={view->
            if(view.boundId!=id || view.boundArtwork!=artworkKey) {
                android.os.Trace.beginSection("Archive.imageBind")
                try {
                    view.clearBinding()
                    view.boundId=id
                    view.boundArtwork=artworkKey
                    bind.accept(view)
                } finally { android.os.Trace.endSection() }
            }
        }, modifier=if(hasProgramArtwork) Modifier.fillMaxSize() else Modifier.size(46.dp).padding(3.dp))
        if(!hasProgramArtwork) BasicText(channel,Modifier.align(Alignment.BottomCenter).padding(5.dp),
            style=TextStyle(Color(0xFFADB9C9),10.sp),maxLines=1,overflow=TextOverflow.Ellipsis)
    }
}

@Composable private fun U7dThumbnailLoadingRing() {
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label="u7d-loading")
    val sweep by transition.animateFloat(initialValue=12f,targetValue=360f,
        animationSpec=androidx.compose.animation.core.infiniteRepeatable(
            androidx.compose.animation.core.tween(1500,easing=androidx.compose.animation.core.LinearEasing)),label="u7d-fill")
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.32f)),contentAlignment=Alignment.Center) {
        Canvas(Modifier.size(42.dp).semantics { contentDescription="Cargando programa" }) {
            drawCircle(Color.Black.copy(alpha=.45f))
            drawArc(Color.White.copy(alpha=.24f),-90f,360f,false,style=androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx()))
            drawArc(Color(0xFFFFAA4C),-90f,sweep,false,style=androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx(),cap=androidx.compose.ui.graphics.StrokeCap.Round))
        }
    }
}
