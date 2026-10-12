package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Arrays;
public class MovistarArtworkSelectionTest {
 @Test public void matchesChannelTimeAndTitleRatherThanAnUnrelatedPoster(){
  java.util.List<MovistarArtworkRepository.Row> rows=Arrays.asList(new MovistarArtworkRepository.Row(1000000,"MPLUS","Los Goonies","https://example.com/wide.jpg"));
  assertEquals("https://example.com/wide.jpg",MovistarArtworkRepository.match(rows,"mplus",1060000,"Los Goonies"));
  assertEquals("https://example.com/wide.jpg",MovistarArtworkRepository.match(rows,"MPLUS",1000000,"Los Goonies (1985)"));
  assertEquals("",MovistarArtworkRepository.match(rows,"LA1",1000000,"Los Goonies"));
  assertEquals("",MovistarArtworkRepository.match(rows,"MPLUS",1130000,"Los Goonies"));
  assertEquals("",MovistarArtworkRepository.match(rows,"MPLUS",1000000,"Otro programa"));
 }
 @Test public void ambiguousEventsFallBackInsteadOfShowingTheWrongImage(){
  assertEquals("",MovistarArtworkRepository.match(Arrays.asList(new MovistarArtworkRepository.Row(1000000,"MPLUS","Película","https://example.com/a.jpg"),new MovistarArtworkRepository.Row(1000000,"MPLUS","Película","https://example.com/b.jpg")),"MPLUS",1000000,"Película"));
 }
}
