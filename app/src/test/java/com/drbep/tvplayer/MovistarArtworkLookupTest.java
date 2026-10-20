package com.drbep.tvplayer;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class MovistarArtworkLookupTest {
    private static String previous(List<MovistarArtworkRepository.Row> rows,
            String provider,long start,String title) {
        if(rows==null)return "";
        String expected=MovistarArtworkRepository.titleKey(title),found="";
        for(MovistarArtworkRepository.Row row:rows) {
            if(!provider.equalsIgnoreCase(row.channel)||Math.abs(row.start-start)>120000
                    ||!expected.equals(MovistarArtworkRepository.titleKey(row.title)))continue;
            if(!found.isEmpty()&&!found.equals(row.image))return "";
            found=row.image;
        }
        return found;
    }
    @Test public void preservesSelectionAcrossTitlesTimesChannelsAndAmbiguity() {
        String[] titles={"Película (2024)","PELICULA","Noticias: edición","Serie - Episodio 2","",null,"Niños y niñas"};
        List<MovistarArtworkRepository.Row> rows=new ArrayList<>();
        for(int i=0;i<280;i++)rows.add(new MovistarArtworkRepository.Row(
                1000000L+i*60000,i%2==0?"LA1":"OTHER",titles[i%titles.length],"image"+i));
        int comparisons=0;
        for(String channel:Arrays.asList("LA1","la1","OTHER","absent"))
            for(String title:titles)
                for(int i=0;i<280;i+=3)
                    for(long offset:new long[]{-120001,-120000,0,120000,120001}) {
                        long time=1000000L+i*60000+offset;
                        assertEquals(previous(rows,channel,time,title),
                                MovistarArtworkRepository.match(rows,channel,time,title));
                        comparisons++;
                    }
        assertEquals(13160,comparisons);
    }
    @Test public void repeatedLookupBenchmark() {
        List<MovistarArtworkRepository.Row> rows=new ArrayList<>();
        for(int i=0;i<96;i++)rows.add(new MovistarArtworkRepository.Row(
                1000000L+i*1800000,"LA1","Programa número "+i+" (2024)","image"+i));
        for(int i=0;i<2000;i++){lookup(rows,i,false);lookup(rows,i,true);}
        long[] oldTimes=new long[7],newTimes=new long[7];
        for(int pass=0;pass<7;pass++){
            long before=System.nanoTime();
            for(int i=0;i<10000;i++)lookup(rows,i,false);
            oldTimes[pass]=System.nanoTime()-before;
            before=System.nanoTime();
            for(int i=0;i<10000;i++)lookup(rows,i,true);
            newTimes[pass]=System.nanoTime()-before;
        }
        Arrays.sort(oldTimes);Arrays.sort(newTimes);
        System.out.println("ARTWORK_LOOKUP_SYNTHETIC 10000 queries median_ms previous="
                +oldTimes[3]/1e6+" cachedTitles="+newTimes[3]/1e6);
        // Timings are evidence only: no flaky speed threshold.
    }
    private static void lookup(List<MovistarArtworkRepository.Row> rows,int i,boolean optimized){
        int n=i%96;String title="Programa número "+n;
        String result=optimized?MovistarArtworkRepository.match(rows,"LA1",1000000L+n*1800000,title)
                :previous(rows,"LA1",1000000L+n*1800000,title);
        assertEquals("image"+n,result);
    }
}
