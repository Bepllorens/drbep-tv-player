package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
public class PrivateChannelPolicyTest {
    private ChannelItem item(String name, String group, String platform) {
        return new ChannelItem(name,name,"","",group,"https://example.test/live","",0,0,false,false,42,platform,Collections.emptyList(),"","","",false);
    }
    @Test public void adultMetadataDoesNotRequireParentalLock() {
        assertTrue(PrivateChannelPolicy.privateItem(item("Canal", "Adultos", "Proveedor"),new OfflinePermissions()));
        assertTrue(PrivateChannelPolicy.adultLabel("Cine +18"));
        assertFalse(PrivateChannelPolicy.adultLabel("Deportes"));
    }
    @Test public void protectedChannelIsAlwaysPrivate() {
        OfflinePermissions p=new OfflinePermissions();p.protectedChannelIds.add("canal");
        assertTrue(PrivateChannelPolicy.privateItem(item("Canal","TV","Proveedor"),p));
    }
    @Test public void fallbackPrefersSafeMovistarIsm() {
        ChannelItem adult=item("Adulto","Adultos","Movistar ISM");
        ChannelItem tdt=item("TDT","TV","TDT");
        ChannelItem safe=item("Generalista","TV","Movistar ISM");
        assertSame(safe,PrivateChannelPolicy.fallback(Arrays.asList(adult,tdt,safe),new OfflinePermissions()));
        assertSame(tdt,PrivateChannelPolicy.fallback(Arrays.asList(adult,tdt),new OfflinePermissions()));
        assertNull(PrivateChannelPolicy.fallback(Arrays.asList(adult),new OfflinePermissions()));
    }
    @Test public void adultBouquetDetectedIndependentlyOfPin() {
        assertTrue(PrivateChannelPolicy.privateFilter(new ChannelFilter("group:adultos","Adultos",2,0,"Adultos"),new OfflinePermissions()));
    }
    @Test public void syncedAdultHistoryIsPurgedWithoutErasingSafeHistory() {
        RecentChannelsStore store = new RecentChannelsStore(null, "test");
        store.add("safe", "Generalista");
        store.mergeIds(Arrays.asList("adult", "safe"), Collections.singletonMap("adult", "Adultos HD"));
        store.removeIf(entry -> PrivateChannelPolicy.adultLabel(entry.channelName));
        assertEquals(1, store.getItems().size());
        assertEquals("safe", store.getItems().get(0).channelId);
    }
}
