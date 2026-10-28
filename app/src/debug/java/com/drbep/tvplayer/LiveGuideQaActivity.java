package com.drbep.tvplayer;
import android.os.Bundle;
import android.view.KeyEvent;
import androidx.activity.ComponentActivity;
import androidx.compose.ui.platform.ComposeView;
import java.time.*;import java.util.*;
/** Debug-only fixture: no repositories, network, accounts or media player. */
public final class LiveGuideQaActivity extends ComponentActivity {
 final LiveGuideSelection selection=new LiveGuideSelection();
 final LocalDate today=LocalDate.of(2026,10,28);
 LocalDate day=today;int index;boolean detail;
 ComposeView view;
 final List<String> ids=new ArrayList<>();
 @Override public void onCreate(Bundle saved){super.onCreate(saved);for(int i=0;i<48;i++)ids.add("fixture-"+i);view=new ComposeView(this);setContentView(view);render();}
 private void render(){
  index=selection.resolve(day.toString(),ids,24);
  List<LiveChannelGuideUiModel.Row> rows=new ArrayList<>();
  for(int i=0;i<ids.size();i++){
   LiveChannelGuideUiModel.Row row=new LiveChannelGuideUiModel.Row("",String.format(Locale.ROOT,"%02d:%02d",i/2,(i%2)*30),"Programa "+i,"Sinopsis sintetica "+i,0,i==24);
   row.selected=i==index;final int n=i;row.onClick=()->{selection.select(day.toString(),ids.get(n));detail=true;render();};rows.add(row);
  }
  LiveChannelGuideUiModel model=new LiveChannelGuideUiModel("Canal de prueba",detail?"Ficha "+index:"",rows);
  model.windowKey=day.toString();model.dateLabel=day.toString();model.selectedIndex=index;
  model.previousDay=()->{day=LiveGuideDay.move(day,-1,today);render();};
  model.nextDay=()->{day=LiveGuideDay.move(day,1,today);render();};
  LiveChannelGuideComposeBinder.bind(view,model);
 }
 @Override public boolean dispatchKeyEvent(KeyEvent event){
  int k=event.getKeyCode();
  if(k==KeyEvent.KEYCODE_DPAD_UP||k==KeyEvent.KEYCODE_DPAD_DOWN||k==KeyEvent.KEYCODE_DPAD_LEFT||k==KeyEvent.KEYCODE_DPAD_RIGHT||k==KeyEvent.KEYCODE_DPAD_CENTER||k==KeyEvent.KEYCODE_BACK){
   if(event.getAction()==KeyEvent.ACTION_DOWN){
    if(k==KeyEvent.KEYCODE_DPAD_UP||k==KeyEvent.KEYCODE_DPAD_DOWN)selection.move(day.toString(),ids,24,k==KeyEvent.KEYCODE_DPAD_UP?-1:1);
    else if(k==KeyEvent.KEYCODE_DPAD_LEFT||k==KeyEvent.KEYCODE_DPAD_RIGHT)day=LiveGuideDay.move(day,k==KeyEvent.KEYCODE_DPAD_LEFT?-1:1,today);
    else if(k==KeyEvent.KEYCODE_DPAD_CENTER)detail=true;
    else if(detail)detail=false;else {finish();return true;}
    render();
   }return true;
  }return super.dispatchKeyEvent(event);
 }
}
