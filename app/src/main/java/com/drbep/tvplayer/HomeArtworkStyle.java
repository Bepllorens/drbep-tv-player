package com.drbep.tvplayer;
import android.graphics.*;
import android.graphics.drawable.*;

/** Small derived bitmaps; never stretches the original artwork. */
final class HomeArtworkStyle {
 static Bitmap sample(Drawable source, int width, int height) {
  Bitmap b=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
  if(source!=null){ Rect old=new Rect(source.getBounds()); source.setBounds(0,0,width,height); source.draw(new Canvas(b)); source.setBounds(old); }
  return b;
 }
 static int color(Drawable source) {
  if(source==null)return Color.rgb(37,55,71);
  Bitmap b=sample(source,16,16); long r=0,g=0,bl=0;
  for(int y=0;y<16;y++)for(int x=0;x<16;x++){int c=b.getPixel(x,y);r+=Color.red(c);g+=Color.green(c);bl+=Color.blue(c);}
  b.recycle(); return Color.rgb((int)(r/256*.38),(int)(g/256*.38),(int)(bl/256*.38));
 }
 static Drawable frame(Drawable art, boolean portrait) {
  if(art==null || portrait || art.getIntrinsicWidth()>=art.getIntrinsicHeight())return art;
  int w=488,h=274;
  Bitmap out=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888); Canvas canvas=new Canvas(out);
  Bitmap blur=sample(art,12,8); Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
  canvas.drawBitmap(blur,null,new Rect(0,0,w,h),paint); blur.recycle();
  canvas.drawColor(0x55000000);
  int fw=Math.max(1,Math.round(h*(float)Math.max(1,art.getIntrinsicWidth())/Math.max(1,art.getIntrinsicHeight())));
  Rect old=new Rect(art.getBounds()); art.setBounds((w-fw)/2,0,(w+fw)/2,h); art.draw(canvas); art.setBounds(old);
  return new BitmapDrawable(android.content.res.Resources.getSystem(),out);
 }
}
