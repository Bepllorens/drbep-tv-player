package com.drbep.tvplayer;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;
import org.json.JSONException;
/** Validated local guide cache. Reading a fallback never changes its freshness. */
final class ArtworkGridDiskCache {
 static final long FRESH_MS=4*3600000L;
 static final int MAX_BYTES=4*1024*1024;
 static final class Payload {
  final String text;
  final JSONArray programs;
  Payload(String text) throws JSONException {
   this.text=text;
   this.programs=new JSONArray(text);
  }
 }
 private ArtworkGridDiskCache() {}
 static String read(File file,long now,boolean freshOnly) {
  Payload payload=readPayload(file,now,freshOnly);
  return payload==null?null:payload.text;
 }
 static Payload readPayload(File file,long now,boolean freshOnly) {
  long age=now-file.lastModified();
  if(!file.isFile() || file.length()>MAX_BYTES || (freshOnly && (age<0 || age>=FRESH_MS)))return null;
  try(InputStream in=new FileInputStream(file);ByteArrayOutputStream out=new ByteArrayOutputStream()) {
   byte[] buffer=new byte[8192];int n;
   while((n=in.read(buffer))!=-1) {
    if(out.size()+n>MAX_BYTES)return null;
    out.write(buffer,0,n);
   }
   String payload=new String(out.toByteArray(),StandardCharsets.UTF_8);
   return new Payload(payload);
  }catch(Exception invalid){return null;}
 }
}
