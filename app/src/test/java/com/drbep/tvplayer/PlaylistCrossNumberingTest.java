package com.drbep.tvplayer;
import com.drbep.tvplayer.playlists.M3uStreamReader;
import com.drbep.tvplayer.playlists.PlaylistDatabase;
import java.io.StringReader;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class PlaylistCrossNumberingTest {
 private M3uStreamReader.Entry parse(String kind,String title,String attributes) throws Exception {
  List<M3uStreamReader.Entry> entries=new ArrayList<>();
  new M3uStreamReader().read(new StringReader("#EXTM3U\n#EXTINF:-1 type=\""+kind+"\" "+attributes+","+title+"\nhttps://example.test/video\n"),entries::add);
  assertEquals(1,entries.size());return entries.get(0);
 }
 @Test public void crossNotationGroupsSeries() throws Exception {
  M3uStreamReader.Entry e=parse("series","Example - 1x02 - Episode","");
  assertEquals("Example",e.series);assertEquals(1,e.season);assertEquals(2,e.episode);
 }
 @Test public void specialsAndThreeDigitEpisode() throws Exception {
  M3uStreamReader.Entry e=parse("series","Example - 0X123 - Special","");
  assertEquals(0,e.season);assertEquals(123,e.episode);
 }
 @Test public void explicitSeriesTitleWins() throws Exception {
  M3uStreamReader.Entry e=parse("series","Alternative 1x02","serie-title=\"Official (2024)\"");
  assertEquals("Official (2024)",e.series);
 }
 @Test public void standardNotationKeepsPriority() throws Exception {
  M3uStreamReader.Entry e=parse("series","Example 1x02 - S03E04","");
  assertEquals(3,e.season);assertEquals(4,e.episode);
 }
 @Test public void noAutomaticMovieOrLiveReclassification() throws Exception {
  for(String kind:new String[]{"movie","live","unknown"}) {
   M3uStreamReader.Entry e=parse(kind,"Example 1x02","");
   assertEquals(kind,e.kind);assertEquals(-1,e.season);assertEquals(-1,e.episode);
  }
 }
 @Test public void ambiguousNumbersAndDimensionsRemainUnknown() throws Exception {
  for(String title:new String[]{"Example 16x9","Example 1920x1080","Example A1x02B","Example é1x02","Example 1x02ñ","Example 1x0200","Example 2.1x02","Example 1x02.5"}) {
   M3uStreamReader.Entry e=parse("series",title,"");
   assertEquals(title,-1,e.season);assertEquals(title,-1,e.episode);assertEquals(title,e.series);
  }
 }
 @Test public void metadataAndUrlRemainUntouched() throws Exception {
  M3uStreamReader.Entry e=parse("series","Example 1x02","group-title=\"Series\" tvg-logo=\"https://example.test/logo\"");
  assertEquals("Series",e.attributes.get("group-title"));assertEquals("https://example.test/logo",e.attributes.get("tvg-logo"));
  assertEquals("https://example.test/video",e.url);assertEquals("Example 1x02",e.title);
 }
 @Test public void refreshPreservesPreexistingIdentity() throws Exception {
  M3uStreamReader.Entry e=parse("series","Example 1x02","");
  java.lang.reflect.Method identity=PlaylistDatabase.class.getDeclaredMethod("identity",M3uStreamReader.Entry.class);
  identity.setAccessible(true);Object current=identity.invoke(null,e);
  e.series=e.title;e.season=-1;e.episode=-1;
  assertEquals(current,identity.invoke(null,e));
 }
}
