package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
public class LiveCategoryOrderTest {
 private List<StartupHomeHubUiModel.Provider> rows(String... keys) { List<StartupHomeHubUiModel.Provider> out=new ArrayList<>(); for(String k:keys) out.add(new StartupHomeHubUiModel.Provider(k,k,"")); return out; }
 private List<String> keys(List<StartupHomeHubUiModel.Provider> rows) { List<String> out=new ArrayList<>(); for(StartupHomeHubUiModel.Provider r:rows) out.add(r.key); return out; }
 @Test public void savedOrderCannotGrantAccessAndAppendsNew() { assertEquals(Arrays.asList("ALL","b","a","c"), keys(LiveCategoryOrder.apply(rows("ALL","a","b","c"), Arrays.asList("removed","b","ALL","b","a")))); }
 @Test public void allCannotMoveOrBeDisplaced() { assertEquals(Arrays.asList("ALL","a","b"),keys(LiveCategoryOrder.move(rows("ALL","a","b"),"a",-1))); assertEquals(Arrays.asList("ALL","a","b"),keys(LiveCategoryOrder.move(rows("ALL","a","b"),"ALL",1))); }
 @Test public void movingDoesNotMutateCancelSnapshot() { List<StartupHomeHubUiModel.Provider> original=rows("ALL","a","b"); assertEquals(Arrays.asList("ALL","b","a"),keys(LiveCategoryOrder.move(original,"a",1))); assertEquals(Arrays.asList("ALL","a","b"),keys(original)); assertEquals(keys(original), keys(LiveCategoryOrder.move(original,"b",1))); }
}
