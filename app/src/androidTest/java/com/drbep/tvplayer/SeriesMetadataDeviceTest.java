package com.drbep.tvplayer;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;import org.junit.runner.RunWith;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class SeriesMetadataDeviceTest extends MovieContinuousDeviceTest {
 @Test public void liveSeriesMetadataAndCachedCatalogue() throws Exception {
  try(ActivityScenario<MainActivity> sc=ActivityScenario.launch(MainActivity.class)) {
   Thread.sleep(8000);final ImportedMovieRepository[] repos=new ImportedMovieRepository[2];
   sc.onActivity(a->{try{
    PersonalPlaylistController controller=(PersonalPlaylistController)CatchupFocusDeviceTest.call(a,"personalPlaylists");
    CatalogSnapshotStore store=(CatalogSnapshotStore)CatchupFocusDeviceTest.field(a,"catalogSnapshotStore");
    java.util.Map<String,String> h=new java.util.HashMap<>();h.put("Authorization","Bearer "+store.getAccessToken());h.put("X-DRBEP-Device-Id",store.getDeviceId());
    String base=(String)CatchupFocusDeviceTest.field(a,"baseUrl");
    repos[0]=new ImportedMovieRepository(a,controller,base,h,c->true,true);repos[1]=new ImportedMovieRepository(a,controller,base,h,c->true);
   }catch(Exception e){throw new AssertionError(e);}});
   MovieFilter filter=new MovieFilter("","","","","added");
   MoviePage page=repos[0].page(filter,0);
   ImportedMovie selected=page.getShelves().stream().filter(s->s.getName().equals("Añadidas recientemente")).findFirst().get().getItems().get(0);
   ImportedMovie enriched=repos[0].resolve(selected,0);
   assertNotNull("Real TV metadata from backend",enriched.getMetadata());
   assertFalse(enriched.getDescription().isEmpty());assertTrue(enriched.getPoster().startsWith("https://image.tmdb.org/"));assertFalse(enriched.getBackdrop().isEmpty());
   assertEquals(enriched.getMetadata().toString(),repos[0].resolve(enriched,0).getMetadata().toString());
   page=repos[0].page(filter,0);assertFalse("Genres exposed",page.getGenres().isEmpty());
   String genre=enriched.getMetadata().getJSONArray("genres").getString(0);
   MoviePage filtered=repos[0].page(new MovieFilter("","",genre,"","added"),0);
   assertTrue("Genre includes matched series",filtered.getShelves().stream().anyMatch(s->s.getItems().stream().anyMatch(i->i.getId().equals(selected.getId()))));
   ImportedMovie movie=repos[1].page(filter,0).getShelves().get(0).getItems().get(0);assertNotNull("Movie metadata compatibility",repos[1].resolve(movie,0).getMetadata());
   System.out.println("TV metadata, poster, backdrop, genres, persistent cache and movie compatibility verified");
   sc.onActivity(a->{try{ComposeDialogHost.dismissAll(a);java.lang.reflect.Method m=MainActivity.class.getDeclaredMethod("showImportedSeries",String.class,Runnable.class);m.setAccessible(true);m.invoke(a,"",(Runnable)()->{});}catch(Exception e){throw new AssertionError(e);}});
   Thread.sleep(8500);shot("series694-tmdb.png");
  }
 }
}
