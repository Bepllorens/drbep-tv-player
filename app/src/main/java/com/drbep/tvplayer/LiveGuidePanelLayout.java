package com.drbep.tvplayer;
final class LiveGuidePanelLayout {
 static int rail(int width){return Math.max(0,Math.min(64,width/4));}
 static int guide(int width){return Math.max(0,Math.min(320,Math.min((int)(width*.56),width-rail(width)-8)));}
}
