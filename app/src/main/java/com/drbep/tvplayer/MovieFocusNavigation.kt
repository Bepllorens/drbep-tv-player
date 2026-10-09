package com.drbep.tvplayer

import android.view.KeyEvent
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import kotlinx.coroutines.*

/** Materialize lazy destinations before requesting focus. Never use geometric search across shelves. */
internal class MovieFocusNavigation(private val scope:CoroutineScope,val column:LazyListState,val toolbar:LazyListState) {
    var shelves:List<MovieShelf> = emptyList()
    val filters=List(5){FocusRequester()}
    val back=FocusRequester()
    val previous=FocusRequester()
    val next=FocusRequester()
    val rowStates=mutableMapOf<String,LazyListState>()
    val cards=mutableMapOf<String,FocusRequester>()
    var focusedShelf=""
    var focusedIndex=0
    var filterIndex=0
    var loadMore:(String,Boolean)->Unit={_,_->}
    var hasPrevious=false
    var hasNext=false
    private var job:Job?=null
    fun key(row:String,id:String)="$row\u0000$id"
    fun cancel(){job?.cancel()}
    private fun launchFocus(block:suspend ()->Unit){job?.cancel();job=scope.launch{block()}}
    private suspend fun focus(find:()->FocusRequester?) {
        repeat(24){withFrameNanos{};val target=find();if(target!=null && runCatching{target.requestFocus()}.isSuccess)return}
    }
    fun card(row:Int,index:Int) {
        val shelf=shelves.getOrNull(row)?:return
        if(shelf.items.isEmpty())return
        val item=shelf.items[index.coerceIn(0,shelf.items.lastIndex)]
        val identity=key(shelf.name,item.id)
        launchFocus {
            if(column.layoutInfo.visibleItemsInfo.none{it.index==row})column.animateScrollToItem(row)
            repeat(24){
                withFrameNanos{}
                val state=rowStates[shelf.name]
                if(state!=null){
                    val target=index.coerceIn(0,shelf.items.lastIndex)
                    val info=state.layoutInfo
                    val visible=info.visibleItemsInfo.firstOrNull{it.index==target}
                    if(visible!=null) {
                        val delta=when {
                            visible.offset<info.viewportStartOffset->visible.offset-info.viewportStartOffset
                            visible.offset+visible.size>info.viewportEndOffset->visible.offset+visible.size-info.viewportEndOffset
                            else->0
                        }
                        if(delta!=0)state.animateScrollBy(delta.toFloat())
                    } else {
                        val last=info.visibleItemsInfo.lastOrNull()
                        val first=info.visibleItemsInfo.firstOrNull()
                        when {
                            last!=null&&target==last.index+1->state.animateScrollBy((last.offset+last.size*2+info.mainAxisItemSpacing-info.viewportEndOffset).toFloat())
                            first!=null&&target==first.index-1->state.animateScrollBy(-(first.size+info.mainAxisItemSpacing).toFloat())
                            else->state.scrollToItem(target)
                        }
                    }
                    focus{cards[identity]};return@launchFocus
                }
            }
        }
    }
    fun currentCard(){val row=shelves.indexOfFirst{it.name==focusedShelf};card(row.coerceAtLeast(0),focusedIndex)}
    fun filter(index:Int){filterIndex=index.coerceIn(0,4);launchFocus{toolbar.scrollToItem(filterIndex);focus{filters[filterIndex]}}}
    fun header(){launchFocus{focus{back}}}
    fun footer(nextButton:Boolean=hasNext){launchFocus{column.scrollToItem(shelves.size);focus{if(nextButton&&hasNext)next else previous}}}
    fun fromCard(row:Int,index:Int,key:Int){
        when(key){
            KeyEvent.KEYCODE_DPAD_LEFT->if(index>0)card(row,index-1)
            KeyEvent.KEYCODE_DPAD_RIGHT->if(index<shelves[row].items.lastIndex)card(row,index+1) else if(shelves[row].more)loadMore(shelves[row].name,true)
            KeyEvent.KEYCODE_DPAD_UP->if(row==0)filter(filterIndex) else card(row-1,index)
            KeyEvent.KEYCODE_DPAD_DOWN->if(row<shelves.lastIndex)card(row+1,index) else if(hasNext||hasPrevious)footer()
        }
    }
    fun fromFilter(index:Int,key:Int){when(key){
        KeyEvent.KEYCODE_DPAD_LEFT->if(index>0)filter(index-1)
        KeyEvent.KEYCODE_DPAD_RIGHT->if(index<4)filter(index+1)
        KeyEvent.KEYCODE_DPAD_UP->header()
        KeyEvent.KEYCODE_DPAD_DOWN->currentCard()
    }}
    fun fromFooter(isNext:Boolean,key:Int){when(key){
        KeyEvent.KEYCODE_DPAD_UP->card(shelves.lastIndex,focusedIndex)
        KeyEvent.KEYCODE_DPAD_LEFT->if(isNext&&hasPrevious)footer(false)
        KeyEvent.KEYCODE_DPAD_RIGHT->if(!isNext&&hasNext)footer(true)
    }}
}
internal fun Modifier.movieDirections(action:(Int)->Unit)=onPreviewKeyEvent {event->
    val key=event.nativeKeyEvent.keyCode
    if(key in listOf(KeyEvent.KEYCODE_DPAD_LEFT,KeyEvent.KEYCODE_DPAD_RIGHT,KeyEvent.KEYCODE_DPAD_UP,KeyEvent.KEYCODE_DPAD_DOWN)) {
        if(event.nativeKeyEvent.action==KeyEvent.ACTION_DOWN)action(key)
        true
    }else false
}
