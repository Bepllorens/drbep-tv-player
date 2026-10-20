package com.drbep.tvplayer;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
import java.time.*;

public class U7dIndexedStoreDeviceTest {
    private EpgRepository.EpgProgram programme(String channel,int n,long now) {
        return new EpgRepository.EpgProgram(channel,"Canal "+channel,"tvg","Título "+n,"","Descripción",Instant.ofEpochMilli(now-1000L*(n+1)).toString(),Instant.ofEpochMilli(now+3600000).toString(),n%2==0?"Cine":"Series",0);
    }
    @Test public void indexedPagesFiltersAndAccountIsolation() throws Exception {
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();String file="test-index-"+System.nanoTime()+".db";
        long now=System.currentTimeMillis();String day=ArchiveWindow.day(Instant.ofEpochMilli(now).toString());
        try(U7dCatalogStore db=new U7dCatalogStore(c,file,null)) {
            List<EpgRepository.EpgProgram> items=new ArrayList<>();for(int i=0;i<175;i++)items.add(programme("a",i,now));
            db.index("one","a",items);assertFalse(db.fresh("one","a",now));
            db.write("one","a",items);db.write("two","a",Collections.singletonList(programme("a",900,now)));
            U7dCatalogStore.Page first=db.page("one",Arrays.asList("a"),day,"","","",false,0,60,now);
            U7dCatalogStore.Page second=db.page("one",Arrays.asList("a"),day,"","","",false,60,60,now);
            assertEquals(175,first.total);assertEquals(60,first.programs.size());assertEquals(60,second.offset);
            Set<String> seen=new HashSet<>();for(EpgRepository.EpgProgram p:first.programs)seen.add(U7dCatalogStore.id(p));for(EpgRepository.EpgProgram p:second.programs)assertFalse(seen.contains(U7dCatalogStore.id(p)));
            assertEquals(55,db.page("one",Arrays.asList("a"),day,"","","",false,120,60,now).programs.size());
            assertEquals(88,db.page("one",Arrays.asList("a"),day,"","Cine","",false,0,60,now).total);
            assertEquals(1,db.page("one",Arrays.asList("a"),day,"","","Título 174",false,0,60,now).total);
            assertEquals(0,db.page("one",Arrays.asList("forbidden"),day,"","","",false,0,60,now).total);
            assertEquals(1,db.page("two",Arrays.asList("a"),day,"","","",false,0,60,now).total);
            db.setSaved("one",items.get(0),now+1000,true);assertTrue(db.saved("one",U7dCatalogStore.id(items.get(0))));
            assertEquals(1,db.page("one",Arrays.asList("a"),"any","","","",true,0,60,now).total);
            db.invalidate("one","a");assertEquals(1,db.read("one","a").size());assertFalse(db.fresh("one","a",now));assertEquals(1,db.savedPrograms("one").size());
        }finally{c.deleteDatabase(file);}
    }
    @Test public void savedLookupSmallAndLargeKeepPermissionsAndPages() throws Exception {
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String file="test-saved-lookup-"+System.nanoTime()+".db";long now=System.currentTimeMillis();
        try(U7dCatalogStore db=new U7dCatalogStore(c,file,null)) {
            List<EpgRepository.EpgProgram> items=new ArrayList<>();
            for(int i=0;i<513;i++)items.add(programme("a",i,now));
            db.index("one","a",items);db.index("other","a",items);
            for(int i=0;i<513;i++) {
                db.setSaved("one",items.get(i),now+3600000,true);
                if(i==47||i==511||i==512) {
                    U7dCatalogStore.Page page=db.page("one",Arrays.asList("a"),"ignored","","","",true,0,60,now);
                    assertEquals(i+1,page.total);assertEquals(Math.min(60,i+1),page.programs.size());
                    assertEquals(U7dCatalogStore.id(items.get(0)),U7dCatalogStore.id(page.programs.get(0)));
                    assertEquals(0,db.page("other",Arrays.asList("a"),"","","","",true,0,60,now).total);
                    assertEquals(0,db.page("one",Arrays.asList("forbidden"),"","","","",true,0,60,now).total);
                }
            }
            U7dCatalogStore.Page second=db.page("one",Arrays.asList("a"),"","","","",true,60,60,now);
            assertEquals(U7dCatalogStore.id(items.get(60)),U7dCatalogStore.id(second.programs.get(0)));
        }finally{c.deleteDatabase(file);}
    }
    @Test public void genreSeekAndFallbackMatchOriginalSql() throws Exception {
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String file="test-genres-"+System.nanoTime()+".db";
        try(U7dCatalogStore store=new U7dCatalogStore(c,file,null)) {
            SQLiteDatabase db=store.getWritableDatabase();
            for(int size:new int[]{0,8,256,257,500}) {
                String scope="genres-"+size;
                for(int i=0;i<size;i++)db.execSQL("INSERT INTO programs VALUES(?,?,?,?,?,?,?,?,?)",
                    new Object[]{scope,""+i,"0","0",i,i+1,i==0?"":i==1?"Niños":String.format(java.util.Locale.ROOT,"g%04d",i),"x","{}"});
                List<String> expected=new ArrayList<>();
                try(android.database.Cursor cur=db.rawQuery(ArchiveQuerySql.GENRES_ALL,new String[]{scope})) {
                    while(cur.moveToNext())expected.add(cur.getString(0));
                }
                assertEquals(expected,store.genres(scope));
            }
            assertTrue(store.genres("missing").isEmpty());
        }finally{c.deleteDatabase(file);}
    }
    @Test public void committedWritesInvalidateEmptyAndExistingGenres() throws Exception {
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String file="test-genre-invalidation-"+System.nanoTime()+".db";long now=System.currentTimeMillis();
        try(U7dCatalogStore db=new U7dCatalogStore(c,file,null)) {
            assertTrue(db.genres("one").isEmpty());assertNotNull(db.cachedGenres("one",false));
            db.index("one","a",Collections.singletonList(programme("a",0,now)));
            assertNull(db.cachedGenres("one",false));assertEquals(Arrays.asList("Cine"),db.genres("one"));
            db.write("other","a",Collections.singletonList(programme("a",1,now)));
            assertEquals(Arrays.asList("Cine"),db.cachedGenres("one",false));
            Map<String,List<EpgRepository.EpgProgram>> batch=new HashMap<>();
            batch.put("a",Collections.singletonList(programme("a",1,now)));db.writeAll("one",batch);
            assertNull(db.cachedGenres("one",false));assertEquals(Arrays.asList("Cine","Series"),db.genres("one"));
            db.invalidate("one","a");assertNull(db.cachedGenres("one",false));assertTrue(db.genres("one").isEmpty());
            db.setSaved("one",programme("a",0,now),now+1000,true);
            assertNull(db.cachedGenres("one",false));assertEquals(Arrays.asList("Cine"),db.genres("one"));
            batch.put("b",Collections.singletonList(programme("b",1,now)));db.indexAll("one",batch);
            assertNull(db.cachedGenres("one",false));assertEquals(Arrays.asList("Cine","Series"),db.genres("one"));
        }finally{c.deleteDatabase(file);}
    }
    @Test public void legacyMigrationKeepsSavedAndOriginalDatabase() throws Exception {
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();String oldName="test-old-"+System.nanoTime()+".db",newName=oldName+"-new";
        long now=System.currentTimeMillis();EpgRepository.EpgProgram p=programme("a",0,now);
        try {
            try(SQLiteDatabase old=c.openOrCreateDatabase(oldName,0,null)) {
                old.execSQL("CREATE TABLE catalog(scope TEXT, channel TEXT, updated INTEGER,payload TEXT)");
                old.execSQL("CREATE TABLE saved(scope TEXT,id TEXT,channel TEXT,expires INTEGER,payload TEXT)");
                old.execSQL("INSERT INTO catalog VALUES(?,?,?,?)",new Object[]{"s","a",now,new org.json.JSONArray().put(U7dCatalogStore.encode(p)).toString()});
                old.execSQL("INSERT INTO saved VALUES(?,?,?,?,?)",new Object[]{"s",U7dCatalogStore.id(p),"a",now,U7dCatalogStore.encode(p).toString()});
            }
            try(U7dCatalogStore db=new U7dCatalogStore(c,newName,c.getDatabasePath(oldName))){assertEquals(1,db.read("s","a").size());assertTrue(db.saved("s",U7dCatalogStore.id(p)));}
            try(U7dCatalogStore db=new U7dCatalogStore(c,newName,c.getDatabasePath(oldName))){assertEquals(1,db.read("s","a").size());}
            try(SQLiteDatabase old=SQLiteDatabase.openDatabase(c.getDatabasePath(oldName).getPath(),null,SQLiteDatabase.OPEN_READONLY);android.database.Cursor cur=old.rawQuery("SELECT payload FROM catalog",null)){assertTrue(cur.moveToFirst());assertTrue(cur.getString(0).contains("Título"));}
        }finally{c.deleteDatabase(oldName);c.deleteDatabase(newName);}
    }
    @Test public void realCacheQueryMetrics() throws Exception {
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
        try(U7dCatalogStore db=new U7dCatalogStore(c)) {
            String scope="";List<String> channels=new ArrayList<>();
            try(android.database.Cursor cur=db.getReadableDatabase().rawQuery("SELECT scope,count(*) AS n FROM programs GROUP BY scope ORDER BY n DESC LIMIT 1",null)){if(cur.moveToFirst())scope=cur.getString(0);}
            try(android.database.Cursor cur=db.getReadableDatabase().rawQuery("SELECT DISTINCT channel FROM programs WHERE scope=?",new String[]{scope})){while(cur.moveToNext())channels.add(cur.getString(0));}
            long now=System.currentTimeMillis();String day=ArchiveWindow.day(Instant.ofEpochMilli(now).toString());
            List<Long> timings=new ArrayList<>();int total=0;
            for(int i=0;i<10;i++) {long t=android.os.SystemClock.elapsedRealtime();U7dCatalogStore.Page page=db.page(scope,channels,day,"","","",false,i*60,60,now);timings.add(android.os.SystemClock.elapsedRealtime()-t);total=page.total;assertTrue(page.programs.size()<=60);}
            Collections.sort(timings);System.out.println("Real indexed cache: channels="+channels.size()+" total="+total+" query median_ms="+timings.get(5)+" max_ms="+timings.get(9));
            try(java.io.FileWriter out=new java.io.FileWriter(new java.io.File(c.getFilesDir(),"catchup-query-metrics.json"))){out.write(new org.json.JSONObject().put("channels",channels.size()).put("total",total).put("median_ms",timings.get(5)).put("max_ms",timings.get(9)).toString());}
            assertTrue("Real catalogue available",total>60);
        }
    }
}
