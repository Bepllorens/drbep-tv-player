package com.drbep.tvplayer;

import java.io.*;
import java.net.*;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Comparator;

/** Small, disposable raw-image cache; no tokens or URLs are written to filenames or logs. */
final class ChannelSvgDiskCache {
    private static final Object[] LOCKS = new Object[32];
    static { for(int i=0;i<LOCKS.length;i++)LOCKS[i]=new Object(); }
    static String key(String url)throws Exception {
        byte[] hash=MessageDigest.getInstance("SHA-256").digest(url.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        StringBuilder out=new StringBuilder();for(byte b:hash)out.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return out.toString();
    }
    static byte[] read(File cache,String url)throws Exception {
        synchronized(LOCKS[(url.hashCode()&Integer.MAX_VALUE)%LOCKS.length]) {
            long start=android.os.SystemClock.elapsedRealtime();
            File directory=new File(cache,"channel-svg-v1");directory.mkdirs();
            File file=new File(directory,key(url)+".svg");
            if(file.isFile()&&file.length()<=1024*1024&&System.currentTimeMillis()-file.lastModified()<7*86400000L) {
                byte[] data; try(InputStream in=new FileInputStream(file);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);data=out.toByteArray();}
                android.util.Log.println(android.util.Log.INFO, "LogoPerf","source=disk ms="+(android.os.SystemClock.elapsedRealtime()-start));return data;
            }
            HttpURLConnection connection=(HttpURLConnection)new URL(url).openConnection();
            connection.setConnectTimeout(4000);connection.setReadTimeout(4000);
            connection.setRequestProperty("User-Agent","DRBEP-TVPlayer/1.3.2");
            byte[] data;
            try {
                if(connection.getResponseCode()/100!=2)throw new IOException("Logo unavailable");
                try(InputStream input=connection.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()) {
                    byte[] buffer=new byte[8192];int n;
                    while((n=input.read(buffer))!=-1){if(out.size()+n>1024*1024)throw new IOException("Logo too large");out.write(buffer,0,n);}
                    data=out.toByteArray();
                }
                // Do not persist an HTML error masquerading as an image.
                com.caverock.androidsvg.SVG.getFromInputStream(new ByteArrayInputStream(data));
            } finally { connection.disconnect(); }
            File tmp=new File(file+".tmp");try(FileOutputStream out=new FileOutputStream(tmp)){out.write(data);}
            if(!tmp.renameTo(file))tmp.delete();prune(directory);
            android.util.Log.println(android.util.Log.INFO, "LogoPerf","source=network ms="+(android.os.SystemClock.elapsedRealtime()-start));return data;
        }
    }
    private static synchronized void prune(File directory) {
        File[] files=directory.listFiles((d,n)->n.endsWith(".svg"));if(files==null)return;
        Arrays.sort(files,Comparator.comparingLong(File::lastModified));long size=0;for(File f:files)size+=f.length();
        for(File f:files){if(size<=8*1024*1024)return;long bytes=f.length();if(f.delete())size-=bytes;}
    }
}
