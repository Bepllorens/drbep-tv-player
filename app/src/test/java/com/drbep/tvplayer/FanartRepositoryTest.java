package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
public class FanartRepositoryTest {
 @Test public void matchesChannelQualityAndAccents(){assertEquals("la1",FanartRepository.channelKey("La 1 HD"));assertEquals("maccion",FanartRepository.channelKey("M+ Acción HD"));}
 @Test public void titleEpisodeNotation(){assertEquals(FanartRepository.titleKey("Saber y ganar T1 E5"),FanartRepository.titleKey("Saber y ganar - Episodio 5"));}
 @Test public void rejectsWrongTimeOrTitle(){List<FanartRepository.Entry> rows=Collections.singletonList(new FanartRepository.Entry(1000,"noticias","ABC"));assertEquals("ABC",FanartRepository.match(rows,1050,"noticias"));assertNull(FanartRepository.match(rows,1201,"noticias"));assertNull(FanartRepository.match(rows,1000,"cine"));}
 @Test public void rejectsAmbiguousArtwork(){List<FanartRepository.Entry> rows=Arrays.asList(new FanartRepository.Entry(1000,"noticias","A"),new FanartRepository.Entry(1000,"noticias","B"));assertNull(FanartRepository.match(rows,1000,"noticias"));}
}
