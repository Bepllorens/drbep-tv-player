package com.drbep.tvplayer;
import android.content.Context;import com.drbep.tvplayer.playlists.PlaylistDatabase;
import java.io.*;import java.nio.charset.StandardCharsets;import okhttp3.*;import org.json.*;
/** Uses the current account's encrypted session; credentials never reach a playlist URL. */
final class ServerPlaylistSync {
 static void refresh(Context context,PlaylistDatabase db,String localId)throws Exception {
  refresh(context,db,localId,new PlaylistDatabase.Progress(){public void check()throws Exception{if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();}public void update(long n){}});
 }
 static void refresh(Context context,PlaylistDatabase db,String localId,PlaylistDatabase.Progress progress)throws Exception {
  CatalogSnapshotStore session=new CatalogSnapshotStore(context);HttpUrl endpoint=HttpUrl.parse(session.getSourceUrl(""));
  if(endpoint==null||session.getAccessToken().isEmpty())throw new IOException("Active account required");
  String base=endpoint.scheme()+"://"+endpoint.host()+((endpoint.port()==80||endpoint.port()==443)?"":":"+endpoint.port());
  OkHttpClient client=NetworkClients.withTimeouts(15000,60000).newBuilder().followRedirects(false).followSslRedirects(false).build();
  java.util.function.Function<String,Request.Builder> request=path->new Request.Builder().url(base+path).header("Authorization","Bearer "+session.getAccessToken()).header("X-DRBEP-Device-Id",session.getDeviceId());
  JSONObject data;
  try(Response response=client.newCall(request.apply("/api/offline/playlists").build()).execute()){
   if(!response.isSuccessful()||response.body()==null)throw new IOException("Server unavailable");
   try(InputStream input=new HttpClient.LimitedInputStream(response.body().byteStream(),1024*1024);ByteArrayOutputStream bytes=new ByteArrayOutputStream()){
    byte[] buffer=new byte[8192];int n;while((n=input.read(buffer))!=-1)bytes.write(buffer,0,n);data=new JSONObject(bytes.toString("UTF-8"));
   }
  }
  JSONArray sources=data.getJSONArray("sources");JSONObject found=null;
  for(int i=0;i<sources.length();i++){JSONObject source=sources.getJSONObject(i);String remote=source.getString("id");
   if(remote.matches("[a-f0-9]{32}")&&PlaylistDatabase.serverSourceId(base+":"+data.getLong("account_id")+":"+remote).equals(localId)){found=source;break;}
  }
  if(found==null)throw new IOException("Source is no longer assigned to this account or device");
  String remote=found.getString("id");long version=found.getLong("version");
  try {
   report(client,request,remote,version,"importing");
   try(Response response=client.newCall(request.apply("/api/offline/playlists/"+remote+"/content").build()).execute()){
    if(!response.isSuccessful()||response.body()==null)throw new IOException("Download denied");
    try(Reader input=new InputStreamReader(new HttpClient.LimitedInputStream(response.body().byteStream(),256*1024*1024),StandardCharsets.UTF_8)){db.importReader(localId,input,progress);}
   }
   report(client,request,remote,version,"ready");
  }catch(Exception e){report(client,request,remote,version,"error");throw e;}
 }
 private static void report(OkHttpClient client,java.util.function.Function<String,Request.Builder> request,String id,long version,String state){
  try(Response ignored=client.newCall(request.apply("/api/offline/playlists/"+id+"/status").post(RequestBody.create(MediaType.parse("application/json"),new JSONObject().put("version",version).put("state",state).toString())).build()).execute()){}catch(Exception ignored){}
 }
}
