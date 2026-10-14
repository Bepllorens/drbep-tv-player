package com.drbep.tvplayer;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.*;
import android.os.*;
import android.view.*;
import java.nio.*;
import java.util.function.Consumer;
import org.json.JSONObject;

/** One EGL output, one external texture per decoder. All EGL/texture work stays on glThread.
 * The owner MUST detach/release players before calling close(). No playback/network ownership. */
final class SharedMultiViewCompositor extends SurfaceView implements SurfaceHolder.Callback {
    private final Handler main = new Handler(Looper.getMainLooper());
    private final HandlerThread thread = new HandlerThread("DRBEP-MultiGL");
    private final Handler gl;
    private final Consumer<Surface[]> ready;
    private final Runnable failure;
    private final int count;
    private volatile boolean closed, failed;
    private volatile long draws, consumed;
    private EGLDisplay display = EGL14.EGL_NO_DISPLAY;
    private EGLContext context = EGL14.EGL_NO_CONTEXT;
    private EGLSurface window = EGL14.EGL_NO_SURFACE;
    private Surface output;
    private final SurfaceTexture[] textures;
    private final Surface[] surfaces;
    private final int[] textureIds;
    private final boolean[] pending, received;
    private final float[][] transforms;
    private final float[] aspects, frameRates;
    private long lastDrawMs;
    private int[][] bounds;
    private int width, height, primary;
    private boolean pip, scheduled, layoutDirty=true;
    private Choreographer clock;
    private int program, position, uv, matrix, sampler;
    private final FloatBuffer vertices = buffer(new float[]{-1,-1, 1,-1, -1,1, 1,1});
    private final FloatBuffer coords = buffer(new float[]{0,0, 1,0, 0,1, 1,1});
    private final Choreographer.FrameCallback frame = time -> {
        scheduled=false;
        if(!closed&&!failed) try { draw(); } catch(RuntimeException e) { fail("draw",e); }
    };

    SharedMultiViewCompositor(Context ctx,int count,Consumer<Surface[]> ready,Runnable failure) {
        super(ctx); this.count=count;this.ready=ready;this.failure=failure;
        textures=new SurfaceTexture[count];surfaces=new Surface[count];textureIds=new int[count];
        pending=new boolean[count];received=new boolean[count];transforms=new float[count][16];
        frameRates=new float[count];java.util.Arrays.fill(frameRates,60f);
        aspects=new float[count];java.util.Arrays.fill(aspects,16f/9f);
        setZOrderMediaOverlay(true);setFocusable(false);getHolder().addCallback(this);
        thread.start();gl=new Handler(thread.getLooper());
    }
    private static FloatBuffer buffer(float[] a){FloatBuffer b=ByteBuffer.allocateDirect(a.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();b.put(a).position(0);return b;}
    @Override public void surfaceCreated(SurfaceHolder holder) {
        output=holder.getSurface();int w=getWidth(),h=getHeight();
        gl.post(()->{if(closed)return;width=w;height=h;try{init();}catch(RuntimeException e){fail("init",e);}});
    }
    @Override public void surfaceChanged(SurfaceHolder holder,int format,int w,int h){gl.post(()->{width=w;height=h;layoutDirty=true;schedule();});}
    @Override public void surfaceDestroyed(SurfaceHolder holder){
        android.util.Log.i("SharedMultiGL","output destroyed closed="+closed);
        if(!closed){failed=true;main.post(()->{if(!closed)failure.run();});}
    }
    void layout(int[][] value,boolean pip,int primary){
        int[][] copy=new int[value.length][];for(int i=0;i<value.length;i++)copy[i]=value[i].clone();
        gl.post(()->{bounds=copy;this.pip=pip;this.primary=primary;layoutDirty=true;schedule();});
    }
    void configureSlot(int slot,float value,float fps){if(slot<0||slot>=count||value<=0||!Float.isFinite(value))return;gl.post(()->{aspects[slot]=value;if(fps>=1f&&Float.isFinite(fps))frameRates[slot]=Math.min(60f,fps);layoutDirty=true;schedule();});}
    JSONObject diagnostics(){JSONObject j=new JSONObject();try{j.put("mode","shared-egl").put("draws",draws).put("inputFrames",consumed).put("failed",failed);}catch(Exception ignored){}return j;}
    private void schedule(){if(closed||failed||clock==null||scheduled)return;scheduled=true;
        float fps=1;for(int i=0;i<count;i++)if(visible(i))fps=Math.max(fps,frameRates[i]);
        long delay=layoutDirty?0:Math.max(0,lastDrawMs+(long)(1000f/fps)-SystemClock.uptimeMillis()-2);
        clock.postFrameCallbackDelayed(frame,delay);}
    private void init(){
        display=EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);int[] version=new int[2];
        require(EGL14.eglInitialize(display,version,0,version,1));
        EGLConfig[] config=new EGLConfig[1];int[] n=new int[1];
        require(EGL14.eglChooseConfig(display,new int[]{EGL14.EGL_RENDERABLE_TYPE,EGL14.EGL_OPENGL_ES2_BIT,EGL14.EGL_SURFACE_TYPE,EGL14.EGL_WINDOW_BIT,EGL14.EGL_RED_SIZE,8,EGL14.EGL_GREEN_SIZE,8,EGL14.EGL_BLUE_SIZE,8,EGL14.EGL_ALPHA_SIZE,8,EGL14.EGL_NONE},0,config,0,1,n,0)&&n[0]>0);
        context=EGL14.eglCreateContext(display,config[0],EGL14.EGL_NO_CONTEXT,new int[]{EGL14.EGL_CONTEXT_CLIENT_VERSION,2,EGL14.EGL_NONE},0);
        window=EGL14.eglCreateWindowSurface(display,config[0],output,new int[]{EGL14.EGL_NONE},0);
        require(context!=EGL14.EGL_NO_CONTEXT&&window!=EGL14.EGL_NO_SURFACE&&EGL14.eglMakeCurrent(display,window,window,context));
        int vs=shader(GLES20.GL_VERTEX_SHADER,"attribute vec2 p; attribute vec2 uv; uniform mat4 tex; varying vec2 v; void main(){gl_Position=vec4(p,0.,1.);v=(tex*vec4(uv,0.,1.)).xy;}");
        int fs=shader(GLES20.GL_FRAGMENT_SHADER,"#extension GL_OES_EGL_image_external : require\nprecision mediump float; uniform samplerExternalOES s; varying vec2 v; void main(){gl_FragColor=texture2D(s,v);}");
        program=GLES20.glCreateProgram();GLES20.glAttachShader(program,vs);GLES20.glAttachShader(program,fs);GLES20.glLinkProgram(program);
        GLES20.glDeleteShader(vs);GLES20.glDeleteShader(fs);int[] ok=new int[1];GLES20.glGetProgramiv(program,GLES20.GL_LINK_STATUS,ok,0);require(ok[0]!=0);
        position=GLES20.glGetAttribLocation(program,"p");uv=GLES20.glGetAttribLocation(program,"uv");matrix=GLES20.glGetUniformLocation(program,"tex");sampler=GLES20.glGetUniformLocation(program,"s");
        GLES20.glGenTextures(count,textureIds,0);
        for(int i=0;i<count;i++){
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,textureIds[i]);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_CLAMP_TO_EDGE);
            final int slot=i;textures[i]=new SurfaceTexture(textureIds[i]);surfaces[i]=new Surface(textures[i]);
            textures[i].setOnFrameAvailableListener(t->{if(!closed){pending[slot]=true;schedule();}},gl);
        }
        clock=Choreographer.getInstance();schedule();
        main.post(()->{if(!closed&&!failed)ready.accept(surfaces.clone());});
    }
    private boolean visible(int i){return bounds!=null&&i<bounds.length&&bounds[i][0]<width&&bounds[i][1]<height&&bounds[i][2]>1&&bounds[i][3]>1;}
    private void draw(){
        boolean changed=layoutDirty;layoutDirty=false;
        for(int i=0;i<count;i++)if(pending[i]){
            changed|=visible(i);
            pending[i]=false;textures[i].updateTexImage();textures[i].getTransformMatrix(transforms[i]);received[i]=true;consumed++;
        }
        // Drain hidden decoder queues, but do not redraw unchanged visible windows for them.
        if(!changed)return;
        GLES20.glViewport(0,0,width,height);GLES20.glClearColor(0,0,0,1);GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glUseProgram(program);GLES20.glActiveTexture(GLES20.GL_TEXTURE0);GLES20.glUniform1i(sampler,0);
        GLES20.glEnableVertexAttribArray(position);GLES20.glEnableVertexAttribArray(uv);
        GLES20.glVertexAttribPointer(position,2,GLES20.GL_FLOAT,false,0,vertices);GLES20.glVertexAttribPointer(uv,2,GLES20.GL_FLOAT,false,0,coords);
        if(bounds!=null){
            if(pip&&primary<count)drawSlot(primary);
            for(int i=0;i<count;i++)if(!pip||i!=primary)drawSlot(i);
        }
        require(EGL14.eglSwapBuffers(display,window));lastDrawMs=SystemClock.uptimeMillis();draws++;
    }
    private void drawSlot(int i){
        if(i>=bounds.length)return;int[] b=bounds[i];
        if(b[0]>=width||b[1]>=height||b[2]<=1||b[3]<=1)return;
        // Black letterbox also masks the primary video behind a PiP tile.
        GLES20.glEnable(GLES20.GL_SCISSOR_TEST);GLES20.glScissor(b[0],height-b[1]-b[3],b[2],b[3]);GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
        if(!received[i])return;
        int w=b[2],h=b[3];if((float)w/h>aspects[i])w=Math.max(1,Math.round(h*aspects[i]));else h=Math.max(1,Math.round(w/aspects[i]));
        GLES20.glViewport(b[0]+(b[2]-w)/2,height-b[1]-(b[3]+h)/2,w,h);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,textureIds[i]);GLES20.glUniformMatrix4fv(matrix,1,false,transforms[i],0);GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4);
    }
    private static void require(boolean ok){if(!ok)throw new IllegalStateException("Composition unavailable");}
    private static int shader(int type,String source){int s=GLES20.glCreateShader(type);GLES20.glShaderSource(s,source);GLES20.glCompileShader(s);int[] ok=new int[1];GLES20.glGetShaderiv(s,GLES20.GL_COMPILE_STATUS,ok,0);if(ok[0]==0){GLES20.glDeleteShader(s);throw new IllegalStateException("Shader unavailable");}return s;}
    private void fail(String stage,RuntimeException error){android.util.Log.w("SharedMultiGL",stage+" "+error.getClass().getSimpleName()+" egl="+Integer.toHexString(EGL14.eglGetError()));if(failed||closed)return;failed=true;main.post(()->{if(!closed)failure.run();});}
    void close(){
        if(closed)return;closed=true;
        gl.post(()->{try{
            if(clock!=null)clock.removeFrameCallback(frame);
            for(int i=0;i<count;i++){if(surfaces[i]!=null)surfaces[i].release();if(textures[i]!=null){textures[i].setOnFrameAvailableListener(null);textures[i].release();}}
            if(display!=EGL14.EGL_NO_DISPLAY){
                if(program!=0)GLES20.glDeleteProgram(program);GLES20.glDeleteTextures(count,textureIds,0);
                EGL14.eglMakeCurrent(display,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_CONTEXT);
                if(window!=EGL14.EGL_NO_SURFACE)EGL14.eglDestroySurface(display,window);
                if(context!=EGL14.EGL_NO_CONTEXT)EGL14.eglDestroyContext(display,context);
                EGL14.eglTerminate(display);EGL14.eglReleaseThread();
            }
        }finally{thread.quitSafely();}});
    }
}
