package com.drbep.tvplayer;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.view.PixelCopy;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/** Only captures this application's active window within an authenticated panel session. */
final class DevicePanelScreen {
 private final Activity activity;
 private final Handler main=new Handler(Looper.getMainLooper());
 DevicePanelScreen(Activity activity){this.activity=activity;}
 private Window active(){Window dialog=ComposeDialogHost.topWindow(activity);return dialog!=null?dialog:activity.getWindow();}
 private void check(Window w)throws IOException{if(activity.isFinishing()||activity.isDestroyed()||w==null||(w.getAttributes().flags&WindowManager.LayoutParams.FLAG_SECURE)!=0)throw new IOException("Screen unavailable");}
 byte[] capture()throws Exception{
  CompletableFuture<Bitmap> result=new CompletableFuture<>();
  main.post(()->{try{Window w=active();check(w);View root=w.getDecorView();int width=root.getWidth(),height=root.getHeight();if(width<=0||height<=0)throw new IOException("Screen not ready");float scale=Math.min(1f,960f/width);Bitmap b=Bitmap.createBitmap(Math.max(1,Math.round(width*scale)),Math.max(1,Math.round(height*scale)),Bitmap.Config.ARGB_8888);
   PixelCopy.request(w,b,code->{if(code==PixelCopy.SUCCESS){if(!result.complete(b))b.recycle();}else{b.recycle();result.completeExceptionally(new IOException("Screen unavailable"));}},main);
  }catch(Exception e){result.completeExceptionally(e);}});
  Bitmap bitmap;try{bitmap=result.get(3,TimeUnit.SECONDS);}catch(Exception e){result.cancel(false);throw e;}
  try(ByteArrayOutputStream out=new ByteArrayOutputStream()){bitmap.compress(Bitmap.CompressFormat.JPEG,75,out);return out.toByteArray();}finally{bitmap.recycle();}
 }
 String ui()throws Exception{
  CompletableFuture<String> result=new CompletableFuture<>();main.post(()->{try{Window w=active();check(w);View root=w.getDecorView();JSONArray nodes=new JSONArray();collect(root,nodes,0);result.complete(new JSONObject().put("dialog",w!=activity.getWindow()).put("width",root.getWidth()).put("height",root.getHeight()).put("nodes",nodes).toString());}catch(Exception e){result.completeExceptionally(e);}});return result.get(2,TimeUnit.SECONDS);
 }
 private static void collect(View v,JSONArray nodes,int depth)throws Exception{
  if(depth>18||nodes.length()>=160||!v.isShown())return;
  if(v instanceof EditText)return; // Input values and passwords are never exported to the UI tree.
  if(v.isFocusable()||v instanceof TextView){Rect r=new Rect();if(v.getGlobalVisibleRect(r)){String label="";if(v instanceof TextView){CharSequence text=((TextView)v).getText();if(text!=null)label=text.toString();}if(label.contains("://")||label.length()>200)label="";nodes.put(new JSONObject().put("text",label).put("type",v.getClass().getSimpleName()).put("focused",v.hasFocus()).put("selected",v.isSelected()).put("x",r.left).put("y",r.top).put("width",r.width()).put("height",r.height()));}}
  if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount()&&nodes.length()<160;i++)collect(g.getChildAt(i),nodes,depth+1);}
 }
}
