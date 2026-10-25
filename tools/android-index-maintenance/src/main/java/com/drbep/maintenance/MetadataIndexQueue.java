package com.drbep.maintenance;

/** Queue membership only: no artwork, progress or resolved metadata joins. */
final class MetadataIndexQueue {
    static final String CLEAN_STALE = "DELETE FROM metadata_index WHERE NOT EXISTS(SELECT 1 FROM sources s WHERE s.id=metadata_index.source AND COALESCE(s.active,s.preview)=metadata_index.generation)";
    interface Transaction {
        void begin();
        void successful();
        void end();
    }
    // Preferences cannot share a SQLite transaction. Commit SQL first; a retry
    // after a marker failure is safe because preparation uses INSERT OR IGNORE.
    static boolean prepare(Transaction transaction, java.util.function.BooleanSupplier active,
                           Runnable rebuild, Runnable markPrepared) {
        return prepareStages(transaction, active, java.util.Collections.singletonList(rebuild), markPrepared);
    }

    static boolean prepareStages(Transaction transaction, java.util.function.BooleanSupplier active,
                                 java.util.List<Runnable> stages, Runnable markPrepared) {
        if (!active.getAsBoolean()) return false;
        transaction.begin();
        try {
            for (Runnable stage : stages) {
                if (!active.getAsBoolean()) return false;
                stage.run();
            }
            if (!active.getAsBoolean()) return false;
            transaction.successful();
        } finally {
            transaction.end();
        }
        markPrepared.run();
        return true;
    }

    static final String INSERT = "INSERT OR IGNORE INTO metadata_index(kind,source,generation,identity,occurrence,series,state) SELECT ?,e.source,e.generation,e.identity,e.occurrence,e.series,'pending'";
    static final String BASE_FROM = " FROM entries e JOIN sources s ON s.id=e.source AND COALESCE(s.active,s.preview)=e.generation LEFT JOIN preferences p ON p.source=e.source AND p.identity=e.identity AND p.occurrence=e.occurrence ";
    static final String SERIES_PREFIX = " FROM (SELECT e.source,e.generation,e.series,MIN(e.rowid) AS representative_id";
    static final String SERIES_SUFFIX = " WHERE e.kind='series' AND COALESCE(p.hidden,0)=0 GROUP BY e.source,e.generation,e.series) grouped JOIN entries e ON e.rowid=grouped.representative_id JOIN sources s ON s.id=e.source LEFT JOIN preferences p ON p.source=e.source AND p.identity=e.identity AND p.occurrence=e.occurrence ";
    static final String FILTER = " WHERE e.kind=? AND COALESCE(p.hidden,0)=0 AND (?='' OR e.source=?)";

    static String sql(boolean series, boolean withGroup, boolean withSource) {
        if (!withSource) return sql(series, withGroup);
        if (!series) {
            // A selected movie list is an equality, not an optional filter.
            // Let SQLite constrain sources/entries before visiting other lists.
            return INSERT + BASE_FROM + FILTER.replace("(?='' OR e.source=?)", "e.source=?")
                    + (withGroup ? " AND " + MetadataIndexSelection.groupPredicate("e", "movie") : "");
        }
        // Restrict before grouping; the visible representative and group EXISTS
        // must still be computed across all episodes of this source.
        String scoped = SERIES_SUFFIX.replace(" GROUP BY", " AND e.source=? GROUP BY");
        return INSERT + SERIES_PREFIX + BASE_FROM + scoped + FILTER
                + (withGroup ? " AND " + MetadataIndexSelection.groupPredicate("e", "series") : "");
    }

    static String[] args(boolean series, String kind, String source, String group) {
        java.util.List<String> values = new java.util.ArrayList<>();
        values.add(kind);
        if (series && !source.isEmpty()) values.add(source);
        values.add(kind);
        values.add(source);
        if (series || source.isEmpty()) values.add(source);
        if (!group.isEmpty()) values.add(group);
        return values.toArray(new String[0]);
    }

    static String sql(boolean series, boolean withGroup) {
        String from = series ? SERIES_PREFIX + BASE_FROM + SERIES_SUFFIX : BASE_FROM;
        return INSERT + from + FILTER
                + (withGroup ? " AND " + MetadataIndexSelection.groupPredicate("e",series?"series":"movie") : "");
    }
}
