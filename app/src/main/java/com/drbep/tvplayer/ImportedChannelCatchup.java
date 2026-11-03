package com.drbep.tvplayer;
import org.json.JSONObject;
final class ImportedChannelCatchup {
 static boolean configured(ChannelItem c){
  return c!=null&&!c.isVod&&c.importedCatchupDays>0&&c.importedCatchupDays<=365
   &&c.importedCatchupSource!=null&&!c.importedCatchupSource.isEmpty()
   &&!U7dChannelPolicy.supports(c); // Keep the existing provider routes authoritative.
 }
 static String url(ChannelItem c,long start,long end,long now)throws Exception{
  if(!configured(c))throw new IllegalArgumentException("No imported catchup");
  JSONObject a=new JSONObject().put("catchup-type",c.importedCatchupType)
   .put("catchup-days",c.importedCatchupDays).put("catchup-source",c.importedCatchupSource);
  return PlaylistCatchup.url(c.playUrl,a,start,end,now);
 }
 static boolean available(ChannelItem c,long start,long end,long now){
  try{url(c,start,end,now);return true;}catch(Exception ignored){return false;}
 }
 static boolean replay(ChannelItem c){return c!=null&&"imported_catchup".equals(c.playbackProfile);}
 static ChannelItem item(ChannelItem c,String title,String poster,long start,long end,long now)throws Exception{
  String resolved=url(c,start,end,now);
  ChannelItem item=new ChannelItem("imported-catchup:"+c.id+":"+start,title,c.tvgId,poster,
   "Catchup",resolved,"",c.originalOrder,c.dashboardOrder,true,c.isAdultVod,c.platformId,c.platformName,
   c.customGroups,c.drmScheme,c.drmLicenseUrl,"",true,"","",Math.max(0,(end-start)/1000),"imported_catchup");
  item.playlistStreamHeaders=c.playlistStreamHeaders;
  item.playlistLicenseHeaders=c.playlistLicenseHeaders;
  // The archive can differ from the live container; let Media3 detect the resolved URL.
  return item;
 }
}
