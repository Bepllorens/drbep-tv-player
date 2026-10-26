package com.drbep.tvplayer;

import android.content.SharedPreferences;
import java.util.IdentityHashMap;
import java.util.Map;

/** Application preferences only. Pending progress survives Activity recreation within the process. */
final class ResumeProgressPersistence {
    private static final Map<SharedPreferences,ResumeProgressStore> stores=new IdentityHashMap<>();
    private ResumeProgressPersistence(){}
    static synchronized ResumeProgressStore forPreferences(SharedPreferences prefs) {
        ResumeProgressStore store=stores.get(prefs);
        if(store==null) {
            store=new ResumeProgressStore((key,snapshot)->{
                long start=System.nanoTime();boolean ok=false;
                try {ok=prefs.edit().putString(key,snapshot.encode()).commit();return ok;}
                finally {BlackBox.event(BlackBoxStore.Event.PROGRESS_WRITE,0,BlackBox.elapsed(start),snapshot.entries.size(),ok?0:1);}
            });
            stores.put(prefs,store);
        }
        return store;
    }
}
