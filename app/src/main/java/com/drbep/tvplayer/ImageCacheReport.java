package com.drbep.tvplayer;
import java.io.File;
import java.io.IOException;
import java.util.function.LongSupplier;
import org.json.JSONObject;

/** Read-only, fixed directories, no filenames or URLs in the response. */
final class ImageCacheReport {
    private final File root;
    private final LongSupplier clock;
    private long measuredAt;
    private String snapshot;
    ImageCacheReport(File root) { this(root, System::nanoTime); }
    ImageCacheReport(File root, LongSupplier clock) { this.root=root; this.clock=clock; }
    synchronized String read(long configuredBudget) throws Exception {
        long now=clock.getAsLong();
        if(snapshot!=null && now-measuredAt>=0 && now-measuredAt<30_000_000_000L)return snapshot;
        JSONObject result=new JSONObject()
            .put("measured_at",System.currentTimeMillis())
            .put("reuse_seconds",30)
            .put("glide",scan(new File(root,"image_manager_disk_cache")))
            .put("svg",scan(new File(root,"channel-svg-v1")))
            .put("glide_budget_bytes",configuredBudget>=0?configuredBudget:JSONObject.NULL)
            .put("svg_budget_bytes",8L*1024*1024);
        snapshot=result.toString();measuredAt=now;return snapshot;
    }
    private JSONObject scan(File directory) throws Exception {
        long bytes=0;int count=0,examined=0;boolean complete=true;
        if(!directory.exists())return new JSONObject().put("bytes",0).put("files",0).put("complete",true);
        // Do not follow a substituted directory or descend into any child directory.
        if(!directory.getCanonicalFile().equals(new File(root.getCanonicalFile(),directory.getName())))
            return new JSONObject().put("bytes",0).put("files",0).put("complete",false);
        File[] files=directory.listFiles();
        if(files==null)complete=false;
        else for(File file:files) {
            if(++examined>10000){complete=false;break;}
            try {
                if(file.isFile() && file.getCanonicalFile().equals(new File(directory.getCanonicalFile(),file.getName()))) {
                    bytes+=file.length();count++;
                }
            } catch(IOException unreadable){complete=false;}
        }
        return new JSONObject().put("bytes",bytes).put("files",count).put("complete",complete);
    }
}
