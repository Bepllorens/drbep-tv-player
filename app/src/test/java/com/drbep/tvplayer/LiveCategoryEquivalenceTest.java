package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;

public class LiveCategoryEquivalenceTest {
    private static ChannelItem channel(int i, String group, List<String> groups) {
        ChannelItem c = new ChannelItem("ch"+i, "Canal "+i, "", "", group, "", "", 0, 0,
                false, false, i % 7, i % 13 == 0 ? " " : "Operador "+(i % 7), groups, "", "", "", false);
        c.customGroupLogos.put("deportes", "logo-"+i);
        return c;
    }
    private static List<String> values(List<StartupHomeHubUiModel.Provider> categories) {
        List<String> result = new ArrayList<>();
        for (StartupHomeHubUiModel.Provider c : categories) {
            result.add(c.key+"|"+c.title+"|"+c.logo);
        }
        return result;
    }
    @Test public void mixedCatalogueMatchesPreviousResultsForAllPermissionModes() {
        List<ChannelItem> channels = new ArrayList<>();
        String[] labels={"Deportes", " deportes ", "Cine", "Adultos", "", "  ", null};
        Random random=new Random(731);
        for(int i=0;i<400;i++) channels.add(channel(i,labels[random.nextInt(labels.length)],
                Arrays.asList(labels[random.nextInt(labels.length)], "Deportes", "Deportes")));
        channels.add(null);
        for(int mask=0;mask<32;mask++) {
            OfflinePermissions p=new OfflinePermissions();
            p.admin=(mask&1)!=0; p.liveAccessMetadataKnown=(mask&2)!=0; p.liveEnabled=(mask&4)!=0;
            if((mask&8)!=0) p.allowedLiveGroups.addAll(Arrays.asList("deportes","adultos"));
            if((mask&16)!=0) p.allowedPlatformIds.addAll(Arrays.asList(0,2,5));
            assertEquals("permissions "+mask,values(reference(channels,p)), values(LiveHomePolicy.categories(channels,p)));
        }
        assertEquals(values(reference(channels,null)),values(LiveHomePolicy.categories(channels,null)));
    }
    @Test public void firstNameLogoAndSavedOrderRemainIdentical() {
        OfflinePermissions p=new OfflinePermissions(); p.liveAccessMetadataKnown=true;
        p.allowedLiveGroups.add("deportes");p.allowedPlatformIds.addAll(Arrays.asList(1,2));
        List<ChannelItem> list=Arrays.asList(channel(1," Deportes ", Arrays.asList("Deportes")),
                channel(2,"deportes", Arrays.asList("Deportes")));
        List<String> order=Arrays.asList("live:2","group:deportes","live:1","group:missing");
        assertEquals(values(LiveCategoryOrder.apply(reference(list,p),order)),
                values(LiveCategoryOrder.apply(LiveHomePolicy.categories(list,p),order)));
        assertEquals("Deportes",LiveHomePolicy.categories(list,p).get(2).title);
        assertEquals("logo-1",LiveHomePolicy.categories(list,p).get(2).logo);
    }
    @Test public void permissionAndLogoChangesAreVisibleOnNextCalculation() {
        OfflinePermissions p=new OfflinePermissions();p.liveAccessMetadataKnown=true;
        List<ChannelItem> list=Arrays.asList(channel(1,"Deportes",Collections.emptyList()));
        assertEquals(1,LiveHomePolicy.categories(list,p).size());
        p.allowedLiveGroups.add("deportes");
        assertEquals(2,LiveHomePolicy.categories(list,p).size());
        list.get(0).customGroupLogos.put("deportes","updated");
        assertEquals("updated",LiveHomePolicy.categories(list,p).get(1).logo);
        p.allowedLiveGroups.clear();
        assertEquals(1,LiveHomePolicy.categories(list,p).size());
    }
    static List<StartupHomeHubUiModel.Provider> reference(List<ChannelItem> visible, OfflinePermissions p) {
        LinkedHashMap<String, StartupHomeHubUiModel.Provider> result = new LinkedHashMap<>();
        if (p == null || !p.liveEnabled || visible.isEmpty()) return new ArrayList<>();
        result.put("ALL", new StartupHomeHubUiModel.Provider("ALL", "Todos", ""));
        for (ChannelItem c : visible) {
            if (!LiveHomePolicy.visibleInHome(c, p)) continue;
            if (LiveHomePolicy.platformAllowed(p, c.platformId) && !LiveHomePolicy.normalized(c.platformName).isEmpty()) {
                String key = "live:" + c.platformId;
                result.putIfAbsent(key, new StartupHomeHubUiModel.Provider(key, c.platformName, c.platformLogoUrl));
            }
            for (String group : LiveHomePolicy.groups(c)) {
                if (LiveHomePolicy.normalized(group).isEmpty() || !LiveHomePolicy.groupAllowed(p, group)) continue;
                String key = "group:" + LiveHomePolicy.normalized(group);
                String logo = c.customGroupLogos == null ? "" : c.customGroupLogos.get(LiveHomePolicy.normalized(group));
                result.putIfAbsent(key, new StartupHomeHubUiModel.Provider(key, group, logo));
            }
        }
        return new ArrayList<>(result.values());
    }
}
