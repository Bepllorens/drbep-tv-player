package com.drbep.tvplayer;
import java.util.*;
/** Reuse grid cells across page/filter changes; retain programme identity within a page. */
final class ArchiveGridSlots {
    private String context;
    private Map<String,Long> previous = new LinkedHashMap<>();
    private long next;
    List<Long> assign(String context, List<String> ids) {
        boolean newPage = !Objects.equals(this.context, context);
        List<Long> oldSlots = new ArrayList<>(previous.values());
        Map<String,Long> current = new LinkedHashMap<>();
        List<Long> result = new ArrayList<>();
        for (int i=0;i<ids.size();i++) {
            String id=ids.get(i);
            Long slot=newPage ? (i<oldSlots.size()?oldSlots.get(i):null) : previous.get(id);
            if(slot==null)slot=next++;
            current.put(id,slot);result.add(slot);
        }
        this.context=context;previous=current;
        return result;
    }
}
