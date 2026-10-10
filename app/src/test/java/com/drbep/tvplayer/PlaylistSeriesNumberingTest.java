package com.drbep.tvplayer;
import com.drbep.tvplayer.playlists.M3uStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import org.junit.Test;
import static org.junit.Assert.*;
public class PlaylistSeriesNumberingTest {
 @Test public void spanishAndStandardNumbering() throws Exception {
  ArrayList<M3uStreamReader.Entry> items=new ArrayList<>();
  new M3uStreamReader().read(new StringReader("#EXTM3U\n#EXTINF:-1 type=\"series\" serie-title=\"Example\",Example T12 - E03 - Episode\nhttps://example.test/1\n#EXTINF:-1 type=\"series\",Example - S02E04 - Episode\nhttps://example.test/2\n"),items::add);
  assertEquals(2,items.size());assertEquals(12,items.get(0).season);assertEquals(3,items.get(0).episode);
  assertEquals(2,items.get(1).season);assertEquals(4,items.get(1).episode);assertEquals("Example",items.get(1).series);
 }
}
