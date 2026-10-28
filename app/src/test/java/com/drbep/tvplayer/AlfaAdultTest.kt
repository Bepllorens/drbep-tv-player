package com.drbep.tvplayer
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.*
class AlfaAdultTest {
 @Test fun defaultsAndGeneralVodNeverGrantAdultCatalog(){
  assertFalse(OfflinePermissions().allowsAlfaAdult())
  for(vod in listOf(false,true))for(grant in listOf(false,true)){
   val p=CatalogRepository.parseOfflinePermissions(JSONObject().put("permissions",JSONObject().put("vod",vod).put("alfa_adult_vod",grant)))
   assertEquals(vod&&grant,p.allowsAlfaAdult())
  }
 }
 @Test fun distributionHeadersNeverGoToPublicCDN(){
  val data=JSONObject().put("id","alfa-adult:"+"a".repeat(32)).put("title","Sample")
   .put("play_url","https://cdn.xvideos-cdn.com/sample.m3u8")
   .put("headers",JSONObject().put("Authorization","Bearer synthetic-session").put("Cookie","synthetic-cookie").put("Referer","https://www.xvideos.com/video.abc/sample").put("User-Agent","DRBEP"))
  val item=alfaPlaybackItem(data)
  assertTrue(item.isVod);assertTrue(item.isAdultVod)
  assertEquals("",item.logoUrl)
  assertNull(item.playlistStreamHeaders["Authorization"]);assertNull(item.playlistStreamHeaders["Cookie"])
  assertEquals(2,item.playlistStreamHeaders.size)
  assertTrue(PrivateChannelPolicy.privateItem(item,OfflinePermissions()))
  assertTrue(AlfaAdultPrivacy.privateID(item.id))
  assertFalse(AlfaAdultPrivacy.privateID("palantir:sample"))
  assertFalse(AlfaAdultPrivacy.privateID(null))
 }
 @Test fun playerDefaultHeadersAlsoDropBackendCredentials(){
  val headers=AlfaAdultPrivacy.mediaHeaders(mapOf("Authorization" to "Bearer synthetic", "X-DRBEP-Access-Token" to "synthetic", "X-DRBEP-Device-Id" to "device", "Referer" to "https://www.xvideos.com/", "User-Agent" to "DRBEP"))
  assertEquals(setOf("Referer","User-Agent"),headers.keys)
 }
 @Test fun untrustedPlaybackOriginsAreRejected(){
  for(url in listOf("http://cdn.xvideos-cdn.com/a.mp4","https://cdn.xvideos-cdn.com.evil.invalid/a.mp4","https://127.0.0.1/a.mp4","https://user:pass@cdn.xvideos-cdn.com/a.mp4")){
   try{alfaPlaybackItem(JSONObject().put("id","alfa-adult:"+"b".repeat(32)).put("play_url",url));fail(url)}catch(expected:IllegalArgumentException){}
  }
 }
 @Test fun binaryVersion17RetainsNewGrantAndVersion16DefaultsToDenied(){
  val permission=OfflinePermissions();permission.vodEnabled=true;permission.alfaAdultEnabled=true;permission.palantirEnabled=true
  val bytes=ByteArrayOutputStream();CatalogSnapshotStore.writeOfflinePermissions(DataOutputStream(bytes),permission)
  val current=DataInputStream(ByteArrayInputStream(bytes.toByteArray()))
  val restored=CatalogSnapshotStore.readOfflinePermissions(current,17)
  assertTrue(restored.allowsAlfaAdult());assertTrue(restored.allowsPalantir());assertEquals(-1,current.read())
  val legacy=DataInputStream(ByteArrayInputStream(bytes.toByteArray().dropLast(1).toByteArray()))
  val previous=CatalogSnapshotStore.readOfflinePermissions(legacy,16)
  assertFalse(previous.allowsAlfaAdult());assertTrue(previous.allowsPalantir());assertEquals(-1,legacy.read())
 }

 @Test fun newSourcePlaybackDomainsAreAcceptedWithTheExistingPrivacyBoundary(){
  for(url in listOf("https://pornstar4k.com/sample.mp4","https://www.nubilefilm.xxx/sample.mp4","https://video.externulls.com/sample.m3u8","https://video.beeg.com/sample.m3u8","https://edge9-mxp.live.mmcdn.com/sample.m3u8")){
   val data=JSONObject().put("id","alfa-adult:"+"c".repeat(32)).put("title","Sample").put("play_url",url)
   val item=alfaPlaybackItem(data)
   assertTrue(item.isVod);assertTrue(item.isAdultVod)
   assertTrue(AlfaAdultPrivacy.privateID(item.id));assertEquals("",item.logoUrl)
  }
  for(url in listOf("https://video.externulls.com.evil.invalid/sample.m3u8","https://edge.live.mmcdn.com.evil.invalid/sample.m3u8","http://pornstar4k.com/sample.mp4")){
   try{alfaPlaybackItem(JSONObject().put("id","alfa-adult:"+"d".repeat(32)).put("play_url",url));fail("Unsafe source accepted")}catch(expected:IllegalArgumentException){}
  }
 }
 @Test fun liveOriginIsRestrictedAndDoesNotCarryCredentials(){
  val input=JSONObject().put("Authorization","Bearer synthetic").put("Cookie","synthetic").put("Origin","https://chaturbate.com").put("Referer","https://chaturbate.com/sample_room/")
  val data=JSONObject().put("id","alfa-adult:"+"e".repeat(32)).put("title","Sample").put("play_url","https://edge.live.mmcdn.com/sample.m3u8").put("mime","application/x-mpegURL").put("is_live",true).put("headers",input)
  val item=alfaPlaybackItem(data,"f".repeat(32))
  assertTrue(item.alfaChaturbateLive);assertEquals("f".repeat(32),item.alfaLiveKey);assertFalse(item.alfaLiveRenewed)
  assertTrue(alfaPlaybackItem(data,"f".repeat(32),true).alfaLiveRenewed)
  assertFalse(alfaPlaybackItem(data).alfaChaturbateLive)
  assertEquals("https://chaturbate.com",item.playlistStreamHeaders["Origin"])
  assertNull(item.playlistStreamHeaders["Authorization"]);assertNull(item.playlistStreamHeaders["Cookie"])
  assertEquals(item.playlistStreamHeaders,AlfaAdultPrivacy.mediaHeaders(item.playlistStreamHeaders))
  assertFalse(AlfaAdultPrivacy.mediaHeaders(mapOf("Origin" to "https://evil.invalid")).containsKey("Origin"))
  assertFalse(AlfaAdultPrivacy.mediaHeaders(mapOf("Origin" to "https://chaturbate.com\r\nCookie: synthetic")).containsKey("Origin"))
  data.put("mime","video/mp4")
  try{alfaPlaybackItem(data);fail("Invalid live MIME accepted")}catch(expected:IllegalArgumentException){}
 }
}
