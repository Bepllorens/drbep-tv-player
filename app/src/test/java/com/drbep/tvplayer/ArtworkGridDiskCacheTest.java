package com.drbep.tvplayer;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import static org.junit.Assert.*;
public class ArtworkGridDiskCacheTest {
 @Rule public TemporaryFolder folder=new TemporaryFolder();
 private File file(String text)throws Exception{
  File f=folder.newFile();Files.write(f.toPath(),text.getBytes(StandardCharsets.UTF_8));return f;
 }
 @Test public void freshValidPayloadIsPreserved()throws Exception{
  File f=file("[{\"Titulo\":\"Película\",\"Canal\":{\"CodCadenaTv\":\"A\"}}]");
  assertEquals(new String(Files.readAllBytes(f.toPath()),StandardCharsets.UTF_8),ArtworkGridDiskCache.read(f,f.lastModified()+1,true));
 }
 @Test public void expiredFallbackDoesNotRenewTimestamp()throws Exception{
  File f=file("[]");long timestamp=f.lastModified(),now=timestamp+ArtworkGridDiskCache.FRESH_MS;
  assertNull(ArtworkGridDiskCache.read(f,now,true));
  assertEquals("[]",ArtworkGridDiskCache.read(f,now,false));assertEquals(timestamp,f.lastModified());
  assertNull(ArtworkGridDiskCache.read(f,now,true));
 }
 @Test public void corruptOrWrongShapeCacheIsIgnored()throws Exception{
  for(String value:new String[]{"broken","{\"error\":\"failed\"}","["})
   assertNull(ArtworkGridDiskCache.read(file(value),System.currentTimeMillis(),false));
 }
 @Test public void futureTimestampIsNotFreshButMayBeFallback()throws Exception{
  File f=file("[]");assertNull(ArtworkGridDiskCache.read(f,f.lastModified()-1,true));
  assertEquals("[]",ArtworkGridDiskCache.read(f,f.lastModified()-1,false));
 }
 @Test public void oversizedCacheIsIgnoredWithoutDeletingIt()throws Exception{
  File f=folder.newFile();try(RandomAccessFile out=new RandomAccessFile(f,"rw")){out.setLength(ArtworkGridDiskCache.MAX_BYTES+1L);}
  assertNull(ArtworkGridDiskCache.read(f,System.currentTimeMillis(),false));assertTrue(f.exists());
 }
 @Test public void missingFileIsCacheMiss()throws Exception{
  assertNull(ArtworkGridDiskCache.read(new File(folder.getRoot(),"missing"),System.currentTimeMillis(),false));
 }

 @Test public void parsedPayloadPreservesOriginalBytesAndProgrammeFields()throws Exception{
  String text=" [ { \"Titulo\":\"Película\", \"FechaHoraInicio\":1234, \"Canal\":{\"CodCadenaTv\":\"LA1\"}, \"Imagenes\":[{\"id\":\"horizontal\",\"uri\":\"https://example.com/image.jpg\"}]}, null ] ";
  File f=file(text);
  ArtworkGridDiskCache.Payload payload=ArtworkGridDiskCache.readPayload(f,f.lastModified()+1,true);
  assertNotNull(payload);assertEquals(text,payload.text);assertEquals(2,payload.programs.length());
  assertEquals("Película",payload.programs.getJSONObject(0).getString("Titulo"));
  assertEquals(1234,payload.programs.getJSONObject(0).getLong("FechaHoraInicio"));
  assertEquals("LA1",payload.programs.getJSONObject(0).getJSONObject("Canal").getString("CodCadenaTv"));
  assertEquals("https://example.com/image.jpg",MovistarArtworkRepository.horizontal(payload.programs.getJSONObject(0)));
  assertNull(payload.programs.optJSONObject(1));
 }
 @Test public void parsedFallbackKeepsFreshnessAndRejectsInvalidPayloads()throws Exception{
  File f=file("[]");long timestamp=f.lastModified();
  assertNull(ArtworkGridDiskCache.readPayload(f,timestamp+ArtworkGridDiskCache.FRESH_MS,true));
  assertEquals(0,ArtworkGridDiskCache.readPayload(f,timestamp+ArtworkGridDiskCache.FRESH_MS,false).programs.length());
  assertEquals(timestamp,f.lastModified());
  assertNull(ArtworkGridDiskCache.readPayload(file("{\"error\":true}"),timestamp,false));
 }
 @Test public void networkPayloadUsesSameValidation()throws Exception{
  assertEquals(0,new ArtworkGridDiskCache.Payload("[]").programs.length());
  for(String invalid:new String[]{"broken","{\"error\":true}","["}){
   try{new ArtworkGridDiskCache.Payload(invalid);fail("Expected invalid array");}
   catch(org.json.JSONException expected){}
  }
 }
}
