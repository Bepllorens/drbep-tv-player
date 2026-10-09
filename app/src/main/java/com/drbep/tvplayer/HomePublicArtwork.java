package com.drbep.tvplayer;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import com.bumptech.glide.load.model.GlideUrl;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import okhttp3.CookieJar;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/** Public promotional artwork only. Never forwards catalog credentials or a provider session. */
final class HomePublicArtwork {
    private static final ExecutorService IO = Executors.newFixedThreadPool(2);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .cookieJar(CookieJar.NO_COOKIES).connectTimeout(4,TimeUnit.SECONDS)
            .readTimeout(4,TimeUnit.SECONDS).callTimeout(6,TimeUnit.SECONDS).build();
    private static final Map<String,String> CACHE = new ConcurrentHashMap<>();
    private static final Pattern META = Pattern.compile("<meta\\b[^>]*>",Pattern.CASE_INSENSITIVE);
    private static final Pattern CONTENT = Pattern.compile("\\bcontent\\s*=\\s*[\"']([^\"']+)[\"']",Pattern.CASE_INSENSITIVE);

    static Runnable resolve(Object original, Consumer<Object> ready) {
        if (original instanceof HomeTmdbArtwork.Source) return HomeTmdbArtwork.resolve((HomeTmdbArtwork.Source)original, ready);
        String raw = original instanceof GlideUrl ? ((GlideUrl)original).toStringUrl() : String.valueOf(original);
        Uri uri = Uri.parse(raw);
        if (!uri.isHierarchical()) { ready.accept(original); return () -> {}; }
        String path = uri.getPath(), id = uri.getQueryParameter("id");
        String page = "";
        if (path != null && id != null) {
            if (path.endsWith("/api/vod/private/netflix/poster") && id.matches("[0-9]{5,12}")) page="https://www.netflix.com/es/title/"+id;
            else if (path.endsWith("/api/vod/private/appletv/poster") && id.matches("umc\\.cmc\\.[a-z0-9]{1,64}")) page="https://tv.apple.com/es/movie/"+id;
        }
        if (page.isEmpty()) { ready.accept(original); return () -> {}; }
        String cached = CACHE.get(page);
        if (cached != null) { ready.accept(cached.isEmpty()?original:cached); return () -> {}; }
        final String publicPage=page;
        java.util.concurrent.atomic.AtomicBoolean cancelled = new java.util.concurrent.atomic.AtomicBoolean();
        java.util.concurrent.Future<?> task = IO.submit(() -> {
            String image = fetch(publicPage);
            if (cancelled.get()) return;
            if (CACHE.size()>80) CACHE.clear();
            CACHE.put(publicPage,image);
            MAIN.post(() -> { if (!cancelled.get()) ready.accept(image.isEmpty()?original:image); });
        });
        return () -> { cancelled.set(true); task.cancel(true); };
    }

    private static String fetch(String page) {
        // This client has no interceptors, authorization headers or cookie storage.
        try (Response response=HTTP.newCall(new Request.Builder().url(page)
                .header("User-Agent","Mozilla/5.0 (Android TV; DRBEP artwork)")
                .header("Accept","text/html").build()).execute()) {
            if (!response.isSuccessful() || response.body()==null) return "";
            String finalHost=response.request().url().host();
            if (!finalHost.equals("www.netflix.com") && !finalHost.equals("tv.apple.com")) return "";
            String id=page.substring(page.lastIndexOf('/')+1);
            if (!response.request().url().encodedPath().endsWith("/"+id)) return "";
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();
            try (InputStream input=response.body().byteStream()) {
                byte[] block=new byte[8192]; int n;
                while (bytes.size()<2_000_000 && (n=input.read(block,0,Math.min(block.length,2_000_000-bytes.size())))!=-1) { bytes.write(block,0,n); }
            }
            Matcher tags=META.matcher(new String(bytes.toByteArray(),StandardCharsets.UTF_8));
            while (tags.find()) {
                String tag=tags.group().replace('\n',' ').replace('\r',' ');
                if (!tag.toLowerCase(Locale.ROOT).matches(".*\\bproperty\\s*=\\s*[\"']og:image[\"'].*")) continue;
                Matcher content=CONTENT.matcher(tag);
                if (!content.find()) continue;
                String image=content.group(1).replace("&amp;","&");
                Uri uri=Uri.parse(image);
                String host=uri.getHost();
                if (!"https".equals(uri.getScheme()) || host==null || uri.getUserInfo()!=null || (uri.getPort()!=-1 && uri.getPort()!=443) || image.length()>8192) continue;
                host=host.toLowerCase(Locale.ROOT);
                if (host.endsWith(".nflxso.net") || host.endsWith(".mzstatic.com")) return image;
            }
        } catch (Exception ignored) { }
        return "";
    }
}
