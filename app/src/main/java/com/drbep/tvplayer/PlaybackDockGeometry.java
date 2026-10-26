package com.drbep.tvplayer;

/** Fixed action cells. Focus scrolls only as far as needed to reveal the whole target. */
final class PlaybackDockGeometry {
    static final int CELL_DP=48, GAP_DP=8, CAPTION_DP=20;
    private PlaybackDockGeometry(){}
    static int scrollTarget(int selected,int count,int cellPx,int gapPx,int viewportPx,int currentPx) {
        if(count<=0 || cellPx<=0 || viewportPx<=0)return 0;
        long content=(long)count*cellPx+(long)Math.max(0,count-1)*Math.max(0,gapPx);
        long max=Math.max(0,content-viewportPx);
        long current=Math.max(0,Math.min(max,currentPx));
        long index=Math.max(0,Math.min(count-1,selected));
        long start=index*(cellPx+(long)Math.max(0,gapPx)),end=start+cellPx;
        long target=current;
        if(start<current)target=start-Math.max(0,gapPx);
        else if(end>current+viewportPx)target=end-viewportPx+Math.max(0,gapPx);
        return (int)Math.min(Integer.MAX_VALUE,Math.max(0,Math.min(max,target)));
    }
}
