package com.drbep.tvplayer.netflix;

import android.app.Dialog;
import android.view.*;
import android.widget.*;
import androidx.compose.ui.platform.ComposeView;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import com.drbep.tvplayer.*;
import com.bumptech.glide.Glide;
import java.util.*;

/** Uses the same HUD, timeline and option panels as every other DRBEP VOD source. */
final class NetflixPlaybackControls {
    interface Selection { void select(int video,int audio,int subtitle); }
    private final NetflixPlaybackActivity activity;
    private final ExoPlayer player;
    private final NetflixTracks tracks;
    private final Selection selection;
    private final ComposeView hud;
    private final boolean modern;
    private final com.bumptech.glide.load.model.GlideUrl posterModel;
    private final VodDisplayRate displayRate;
    private final NetflixFrameRate frameRate;
    private final List<Runnable> actions=new ArrayList<>();
    private final List<ZapActionItem> buttons=new ArrayList<>();
    private int video,audio,subtitle=-1,focus=4;
    private boolean disposed,timelineFocus;
    private Dialog dialog;
    private final Runnable autoHide;
    private final TouchControlsArtworkBinder artwork=new TouchControlsArtworkBinder(){
        public void bindLogo(ImageView view,String url,String name,int width,int height){bindPoster(view,url);}
        public void bindPoster(ImageView view,String url){
            if(posterModel==null || !posterModel.toStringUrl().equals(url) || url.equals(view.getTag()))return;
            view.setTag(url);
            Glide.with(activity).load(posterModel).fitCenter()
                .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                    public boolean onLoadFailed(com.bumptech.glide.load.engine.GlideException error,Object model,
                            com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,boolean first) {
                        view.setContentDescription("Carátula no disponible");return false;
                    }
                    public boolean onResourceReady(android.graphics.drawable.Drawable image,Object model,
                            com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                            com.bumptech.glide.load.DataSource source,boolean first) {
                        view.setContentDescription("Carátula de " + activity.getIntent().getStringExtra("title"));return false;
                    }
                }).into(view);
        }
    };
    NetflixPlaybackControls(NetflixPlaybackActivity activity,ExoPlayer player,PlayerView view,
            NetflixTracks tracks,NetflixFrameRate frameRate,Selection selection) {
        this.activity=activity;this.player=player;this.tracks=tracks;this.selection=selection;this.frameRate=frameRate;
        autoHide=()->{if(dialog==null && player.getPlayWhenReady())hide();};
        audio=tracks.defaultAudio();
        posterModel=PrivateVodArtwork.netflix(activity,activity.getIntent().getStringExtra("poster_base"),
                activity.getIntent().getStringExtra("poster_query"));
        displayRate=new VodDisplayRate(activity);
        modern=activity.getSharedPreferences("drbep_tv_prefs",0).getBoolean("playback_hud_modern",true);
        FrameLayout root=new FrameLayout(activity);
        root.addView(view,new FrameLayout.LayoutParams(-1,-1));
        view.setOnClickListener(v->show());
        hud=new ComposeView(activity); hud.setId(R.id.touchControlsBar);
        FrameLayout.LayoutParams layout=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);
        layout.leftMargin=layout.rightMargin=activity.getResources().getDimensionPixelSize(R.dimen.touch_controls_side_margin);
        layout.bottomMargin=activity.getResources().getDimensionPixelSize(R.dimen.touch_controls_margin_bottom);
        hud.setTranslationY(activity.getResources().getDimension(R.dimen.touch_controls_translation_y));
        root.addView(hud,layout);
        add(activity.getString(R.string.touch_button_tv),()->activity.exitPlayback(true));
        add(activity.getString(R.string.touch_button_vod_library),()->activity.exitPlayback(false));
        add(activity.getString(R.string.touch_button_vod_detail),this::details);
        add(activity.getString(R.string.touch_button_rewind),()->seek(-30000));
        add(activity.getString(R.string.touch_button_play_pause),this::togglePlayback);
        add(activity.getString(R.string.touch_button_forward),()->seek(30000));
        Runnable languages=()->{keepVisible();options("Idiomas",null,
                Arrays.asList(activity.getString(R.string.audio_track_title),activity.getString(R.string.subtitle_track_title)),
                Arrays.asList(this::audio,this::subtitles),false);};
        actions.add(languages);
        buttons.add(new ZapActionItem("Idiomas",true,false,false,"vod_languages","",languages,null));
        add("Calidad",this::quality);
        activity.setContentView(root);show();
        root.postDelayed(new Runnable(){public void run(){
            if(disposed || activity.isFinishing() || activity.isDestroyed())return;
            if(dialog==null && hud.getVisibility()==View.VISIBLE)render();
            root.postDelayed(this,2000);
        }},2000);
    }
    private void add(String label,Runnable action){
        Runnable click=()->{keepVisible();action.run();};
        actions.add(click);buttons.add(new ZapActionItem(label,true,false,false,click));
    }
    void dispose(){disposed=true;hud.removeCallbacks(autoHide);if(dialog!=null){dialog.setOnDismissListener(null);dialog.dismiss();dialog=null;}}
    void show(){if(disposed)return;hud.setVisibility(View.VISIBLE);render();keepVisible();}
    void hide(){hud.removeCallbacks(autoHide);hud.setVisibility(View.GONE);}
    private void keepVisible(){hud.removeCallbacks(autoHide);hud.postDelayed(autoHide,3000);}
    private void render(){
        NetflixTracks.Choice c=tracks.video.get(video);
        androidx.media3.common.VideoSize size=player.getVideoSize();
        androidx.media3.common.Format actualFormat=player.getVideoFormat();
        String mime=actualFormat==null?"":actualFormat.sampleMimeType;
        String codec="video/hevc".equals(mime)?"HEVC":"video/avc".equals(mime)?"H.264":"Vídeo";
        String stream=size.width>0?size.width+"×"+size.height+" · "+codec:"Preparando vídeo";
        stream+=" · Perfil "+NetflixTracks.bitrateLabel(c.bitrate);
        androidx.media3.common.Format format=player.getVideoFormat();
        float reported=format==null?-1:format.frameRate;
        boolean estimated=!(reported>0 && Float.isFinite(reported));
        float fps=estimated?frameRate.estimate():reported;
        stream+=" · "+(estimated && fps>0?"≈ ":"")+displayRate.label(fps);
        if(player.getPlaybackState()==Player.STATE_BUFFERING)stream+=" · Cargando…";
        long duration=Math.max(0,player.getDuration()),position=Math.max(0,player.getCurrentPosition());
        String title=activity.getIntent().getStringExtra("title");
        String poster=posterModel==null?"":posterModel.toStringUrl();
        String meta="Netflix"+(audio>=0?" · "+tracks.audio.get(audio).label:"");
        if(subtitle>=0)meta+=" · Subtítulos: "+tracks.subtitles.get(subtitle).label;
        TouchControlsNowPlayingUiModel playing=new TouchControlsNowPlayingUiModel(true,poster,"",
                activity.getString(R.string.tools_section_vod),title,meta,"",false,poster,
                duration>position?time(duration-position)+" restantes":"",
                duration>0?(int)Math.min(100,position*100/duration):0,duration>0,"",stream);
        TimeshiftBarUiModel timeline=new TimeshiftBarUiModel(time(position)+" / "+time(duration),
                duration>0?(int)Math.min(1000,position*1000/duration):0,false,null,
                ()->hud.removeCallbacks(autoHide),this::keepVisible,
                progress->time(duration*progress/1000),progress->{player.seekTo(duration*progress/1000);keepVisible();},
                timelineFocus,modern);
        TouchControlsComposeBinder.bind(hud,new TouchControlsBarUiModel("Netflix","Netflix",null,
                buttons,focus,playing,true,modern,modern?timeline:null),artwork);
    }
    private static String time(long ms){long s=Math.max(0,ms)/1000;return s>=3600?String.format(Locale.ROOT,"%d:%02d:%02d",s/3600,(s/60)%60,s%60):String.format(Locale.ROOT,"%02d:%02d",s/60,s%60);}
    private void togglePlayback(){if(player.getPlayWhenReady())player.pause();else player.play();render();keepVisible();}
    private void seek(long delta){long duration=player.getDuration();long p=Math.max(0,player.getCurrentPosition()+delta);
        player.seekTo(duration>0?Math.min(p,duration):p);render();keepVisible();}
    private void quality(){choose("Calidad de vídeo",tracks.video,video,index->{video=index;apply();});}
    private void audio(){choose(activity.getString(R.string.audio_track_title),tracks.audio,audio,index->{audio=index;apply();});}
    private void subtitles(){
        ArrayList<String> labels=new ArrayList<>();labels.add("Desactivados");
        for(NetflixTracks.Choice c:tracks.subtitles)labels.add(c.label);
        choices(activity.getString(R.string.subtitle_track_title),labels,subtitle+1,index->{subtitle=index-1;apply();});
    }
    private void details(){
        hide();
        ComposeView content=new ComposeView(activity);
        androidx.lifecycle.ViewTreeLifecycleOwner.set(content,activity);
        androidx.lifecycle.ViewTreeViewModelStoreOwner.set(content,activity);
        androidx.savedstate.ViewTreeSavedStateRegistryOwner.set(content,activity);
        final Dialog[] holder=new Dialog[1];
        Runnable close=()->holder[0].dismiss();
        String facts=activity.getIntent().getStringExtra("vod_facts");
        String synopsis=activity.getIntent().getStringExtra("vod_synopsis");
        List<VodPanelActionUiModel> primary=Arrays.asList(
                new VodPanelActionUiModel("Volver a la reproducción",true,close));
        List<VodPanelActionUiModel> secondary=Arrays.asList(
                new VodPanelActionUiModel("Calidad",false,()->{close.run();quality();}),
                new VodPanelActionUiModel("Audio",false,()->{close.run();audio();}),
                new VodPanelActionUiModel("Subtítulos",false,()->{close.run();subtitles();}),
                new VodPanelActionUiModel("Biblioteca VOD",false,()->{close.run();activity.exitPlayback(false);}),
                new VodPanelActionUiModel("TV",false,()->{close.run();activity.exitPlayback(true);}));
        VodDetailPanelComposeBinder.bind(content,new VodDetailPanelUiModel(
                activity.getIntent().getStringExtra("title"),
                "Netflix"+(facts==null || facts.trim().isEmpty()?"":" · "+facts),
                synopsis==null || synopsis.trim().isEmpty()?"Sin sinopsis disponible.":synopsis,
                time(player.getCurrentPosition())+" / "+time(player.getDuration()),
                posterModel==null?"":posterModel.toStringUrl(),
                "Reproducción","Opciones","",primary,secondary,close),
                (image,item)->artwork.bindPoster(image,item.posterUrl));
        dialog=ComposeDialogHost.showFullscreen(activity,content,()->{dialog=null;if(!disposed)show();});
        holder[0]=dialog;
    }
    private interface Pick {void pick(int index);}
    private void choose(String title,List<NetflixTracks.Choice> options,int checked,Pick pick){
        ArrayList<String> labels=new ArrayList<>();for(NetflixTracks.Choice c:options)labels.add(c.label);
        choices(title,labels,checked,pick);
    }
    private void choices(String title,List<String> labels,int checked,Pick pick){
        ArrayList<String> shown=new ArrayList<>();ArrayList<Runnable> changes=new ArrayList<>();
        for(int i=0;i<labels.size();i++){final int index=i;shown.add((i==checked?"✓ ":"")+labels.get(i));changes.add(()->pick.pick(index));}
        options(title,labels.isEmpty()?"No hay pistas disponibles en esta sesión.":null,shown,changes,true);
    }
    private void options(String title,String message,List<String> labels,List<Runnable> changes,boolean tracksPanel){
        hide();
        ComposeView content=new ComposeView(activity);
        androidx.lifecycle.ViewTreeLifecycleOwner.set(content,activity);
        androidx.lifecycle.ViewTreeViewModelStoreOwner.set(content,activity);
        androidx.savedstate.ViewTreeSavedStateRegistryOwner.set(content,activity);
        List<TvOptionsPanelRowUiModel> rows=new ArrayList<>();
        final Dialog[] holder=new Dialog[1];
        for(int i=0;i<labels.size();i++){
            final int index=i;
            rows.add(new TvOptionsPanelRowUiModel(labels.get(i),String.valueOf(i+1),()->{
                holder[0].dismiss();changes.get(index).run();
            },null,tracksPanel));
        }
        TvOptionsPanelComposeBinder.bind(content,new TvOptionsPanelUiModel(title,message,
                activity.getString(R.string.dialog_back),rows,()->holder[0].dismiss()));
        dialog=ComposeDialogHost.showFullscreen(activity,content,()->{dialog=null;if(!disposed)show();});
        holder[0]=dialog;
    }
    private void apply(){selection.select(video,audio,subtitle);render();keepVisible();}
    void back(){if(dialog!=null)return;if(hud.getVisibility()==View.VISIBLE){hide();return;}
        options("Salir de la reproducción",null,Arrays.asList("Volver al catálogo","Volver a TV","Continuar viendo","Renovar vinculación de Netflix"),
            Arrays.asList(()->activity.exitPlayback(false),()->activity.exitPlayback(true),this::show,()->activity.renewLink()),false);}
    boolean key(KeyEvent event){
        if(dialog!=null || disposed)return false;
        int key=event.getKeyCode();boolean down=event.getAction()==KeyEvent.ACTION_DOWN;
        boolean hidden=hud.getVisibility()!=View.VISIBLE;
        if(key==KeyEvent.KEYCODE_MENU || (hidden && (key==KeyEvent.KEYCODE_DPAD_CENTER || key==KeyEvent.KEYCODE_ENTER || key==KeyEvent.KEYCODE_DPAD_UP || key==KeyEvent.KEYCODE_DPAD_DOWN))){
            if(down && event.getRepeatCount()==0)show();return true;}
        if(key==KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || key==KeyEvent.KEYCODE_MEDIA_PLAY || key==KeyEvent.KEYCODE_MEDIA_PAUSE){
            if(down && event.getRepeatCount()==0){
                if(key==KeyEvent.KEYCODE_MEDIA_PAUSE)player.pause();else if(key==KeyEvent.KEYCODE_MEDIA_PLAY)player.play();else togglePlayback();
                render();keepVisible();}return true;}
        if(key==KeyEvent.KEYCODE_DPAD_LEFT || key==KeyEvent.KEYCODE_DPAD_RIGHT){
            if(down){int direction=key==KeyEvent.KEYCODE_DPAD_LEFT?-1:1;
                if(hidden || timelineFocus)seek(direction*10000L);else{focus=Math.max(0,Math.min(actions.size()-1,focus+direction));render();keepVisible();}}
            return true;}
        if(!hidden && (key==KeyEvent.KEYCODE_DPAD_CENTER || key==KeyEvent.KEYCODE_ENTER)){
            if(down && event.getRepeatCount()==0){if(timelineFocus)togglePlayback();else actions.get(focus).run();}return true;}
        if(!hidden && (key==KeyEvent.KEYCODE_DPAD_UP || key==KeyEvent.KEYCODE_DPAD_DOWN)){
            if(down){timelineFocus=modern && key==KeyEvent.KEYCODE_DPAD_UP;render();keepVisible();}return true;}
        return false;
    }
}
