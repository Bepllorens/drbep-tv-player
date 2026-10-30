package com.drbep.tvplayer;
import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

/** Disposable SVG files only. Never touches the Glide journal, catalogues or temporary writes. */
final class SvgCachePruner {
    static int prune(File directory, long budget, int maxDeletes, long now) {
        File[] files=directory.listFiles((d,n)->n.endsWith(".svg"));
        if(files==null || maxDeletes<=0)return 0;
        long size=0;
        for(File file:files)if(file.isFile())size+=file.length();
        if(size<=budget)return 0;
        Arrays.sort(files,Comparator.comparingLong(File::lastModified));
        int deleted=0;
        for(File file:files) {
            if(size<=budget || deleted>=maxDeletes)break;
            // Keep fresh writes; a later pass can reclaim them if still necessary.
            if(!file.isFile() || now-file.lastModified()<60_000L)continue;
            long bytes=file.length();
            if(file.delete()){size-=bytes;deleted++;}
        }
        return deleted;
    }
}
