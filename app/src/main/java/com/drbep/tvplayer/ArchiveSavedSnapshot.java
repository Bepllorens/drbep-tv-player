package com.drbep.tvplayer;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable saved state; old guide callbacks cannot undo a newer user action. */
final class ArchiveSavedSnapshot<T> extends AbstractList<T> {
    final long revision;
    private final List<T> items;
    ArchiveSavedSnapshot(long revision, List<T> items) {
        this.revision = revision;
        this.items = Collections.unmodifiableList(new ArrayList<>(items));
    }
    @Override public T get(int index) { return items.get(index); }
    @Override public int size() { return items.size(); }
    static boolean current(List<?> items, long revision) {
        return items instanceof ArchiveSavedSnapshot
                && ((ArchiveSavedSnapshot<?>) items).revision == revision;
    }
}
