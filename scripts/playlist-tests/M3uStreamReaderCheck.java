import com.drbep.tvplayer.playlists.M3uStreamReader;
import java.io.*;
import java.util.*;
public final class M3uStreamReaderCheck {
    static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        String sample = "\uFEFF#EXTM3U url-tvg=\"https://example.org/epg.xml\"\n"
            + "#EXTINF:-1 type= \"MOVIE\" group-title=\"MAX CINE\" group-logo=\"https://example.org/group.png\" tvg-logo=\"https://example.org/movie.jpg\",¡Scooby! (2020)\nhttps://example.org/movie.mpd\n"
            + "#EXTINF:-1 type = \"series\" serie-title=\"#BringBackAlice (2023)\" tvg-serie=\"Fallback\",#BringBackAlice (2023) - S01E01 - Episodio 1\n"
            + "#KODIPROP:inputstream.adaptive.license_type=com.widevine.alpha\nhttps://example.org/episode.mpd\n"
            + "#EXTINF:-1 tvg-name=\"Name, with comma\",No type\nhttps://example.org/live\n";
        List<M3uStreamReader.Entry> entries = new ArrayList<>();
        M3uStreamReader.Result r = new M3uStreamReader().read(new StringReader(sample), entries::add);
        check(r.imported == 3 && r.rejected == 0, "entry count");
        check(entries.get(0).kind.equals("movie"), "movie type");
        check(entries.get(0).attributes.containsKey("group-logo"), "group logo");
        check(entries.get(1).series.equals("#BringBackAlice (2023)"), "series name");
        check(entries.get(1).season == 1 && entries.get(1).episode == 1, "episode coordinates");
        check(entries.get(2).kind.equals("unknown") && entries.get(2).directives.isEmpty(), "no type guess or leaked DRM");
        check(r.epgUrl.endsWith("epg.xml"), "EPG");
        r = new M3uStreamReader().read(new StringReader("#EXTINF:-1,Missing\n#EXTINF:-1,Good\nhttps://example.org\n"), e -> {});
        check(r.rejected == 1 && r.imported == 1, "truncated entry");
        try { new M3uStreamReader().read(new StringReader("#EXTM3U\n#EXT-X-TARGETDURATION:6\n"), e -> {}); throw new AssertionError("HLS accepted"); } catch (IOException expected) {}
        Thread.currentThread().interrupt();
        try { new M3uStreamReader().read(new StringReader(sample), e -> {}); throw new AssertionError("cancel ignored"); } catch (InterruptedIOException expected) { Thread.interrupted(); }
        // Generate incrementally; the fixture itself must not allocate a complete catalogue.
        Reader large = new Reader() {
            int index, offset; String chunk = "";
            public int read(char[] b, int off, int len) {
                if (offset == chunk.length()) {
                    if (index == 100000) return -1;
                    chunk = "#EXTINF:-1 type=\"movie\" group-title=\"Cinema\",Movie " + index + "\nhttps://example.org/" + index++ + "\n"; offset = 0;
                }
                int n = Math.min(len, chunk.length() - offset); chunk.getChars(offset, offset+n, b, off); offset += n; return n;
            }
            public void close() {}
        };
        long started = System.nanoTime();
        r = new M3uStreamReader().read(large, e -> {});
        check(r.imported == 100000, "large catalogue count");
        System.out.println("PASS: classification, series, logos, EPG, directives isolation, truncation, HLS rejection, cancellation; 100000 entries in " + ((System.nanoTime()-started)/1000000) + "ms with -Xmx32m");
    }
}
